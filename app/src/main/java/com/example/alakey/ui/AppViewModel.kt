package com.example.alakey.ui

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.alakey.data.ItunesSearchResult
import com.example.alakey.data.PodcastEntity
import com.example.alakey.data.PodcastPalette
import com.example.alakey.data.UniversalRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject
import androidx.palette.graphics.Palette
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import coil.ImageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import androidx.compose.ui.graphics.toArgb
import android.graphics.Color as AndroidColor

@HiltViewModel
class AppViewModel @Inject constructor(
    private val repo: UniversalRepository,
    private val playbackClient: PlaybackClient,
    @ApplicationContext private val context: Context
) : ViewModel() {
    
    data class UiState(
        val podcasts: List<PodcastEntity> = emptyList(),
        val current: PodcastEntity? = null,
        val isPlaying: Boolean = false,
        val currentTime: Long = 0,
        val duration: Long = 1,
        val speed: Float = 1.0f,
        val queue: List<PodcastEntity> = emptyList(),
        val inbox: List<PodcastEntity> = emptyList(),
        val amplitude: Float = 0f,
        val dominantColor: Int = AndroidColor.CYAN
    )

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    private val _searchResults = MutableStateFlow<List<ItunesSearchResult>>(emptyList())
    val searchResults: StateFlow<List<ItunesSearchResult>> = _searchResults.asStateFlow()
    
    val sleepTimerSeconds: StateFlow<Int> = playbackClient.sleepTimerSeconds

    sealed interface UserEvent {
        data class ShowMessage(val message: String) : UserEvent
        data class ShowError(val message: String) : UserEvent
    }
    
    private val _userEvents = MutableSharedFlow<UserEvent>()
    val userEvents: SharedFlow<UserEvent> = _userEvents.asSharedFlow()

    init {
        // Hydrate Library
        viewModelScope.launch {
            repo.library.collect { podcasts ->
                _uiState.update { it.copy(podcasts = podcasts) }
                // Re-link current podcast object if library updates
                val currentId = playbackClient.state.value.currentMediaId
                if (currentId != null) {
                    val p = podcasts.find { it.id == currentId }
                    _uiState.update { it.copy(current = p) }
                }
            }
        }

        // Hydrate Queue and Inbox
        viewModelScope.launch {
            repo.queue.collect { queue ->
                _uiState.update { it.copy(queue = queue) }
            }
        }
        
        viewModelScope.launch {
            repo.inbox.collect { inbox ->
                _uiState.update { it.copy(inbox = inbox) }
            }
        }
        
        // Hydrate Playback State
        viewModelScope.launch {
            playbackClient.state.collect { pbState ->
                val podcast = _uiState.value.podcasts.find { it.id == pbState.currentMediaId }
                _uiState.update {
                    it.copy(
                        current = podcast,
                        isPlaying = pbState.isPlaying,
                        currentTime = pbState.currentPosition,
                        duration = pbState.duration,
                        speed = pbState.playbackSpeed,
                        amplitude = pbState.amplitude
                    )
                }
                
                // Extract color if current artwork changed
                if (podcast != null && podcast.imageUrl != _uiState.value.current?.imageUrl) {
                    extractColor(podcast.imageUrl)
                }
                
                // Update DB with progress
                if (pbState.isPlaying && podcast != null && pbState.currentPosition > 0) {
                     repo.updateProgress(podcast.id, pbState.currentPosition)
                     repo.updateLastPlayed(podcast.id, System.currentTimeMillis())
                }
            }
        }
    }

    private fun emitEvent(event: UserEvent) {
        viewModelScope.launch { _userEvents.emit(event) }
    }

    fun connect() {
        // No-op: Client connects on init. 
    }

    fun startSleepTimer(minutes: Int = 45) {
        playbackClient.startSleepTimer(minutes)
    }

    fun resetSleepTimer() {
        playbackClient.resetSleepTimer()
    }

    fun downloadEpisode(podcastId: String) {
        viewModelScope.launch {
            repo.downloadAudio(podcastId)
                .onFailure { e -> Log.e("AppViewModel", "Download failure for $podcastId", e) }
        }
    }

    fun checkForAutoDownloads() {
        viewModelScope.launch {
            repo.runSmartDownloads()
        }
    }

    override fun onCleared() {
        super.onCleared()
        playbackClient.cleanup()
    }

    fun importFeed(url: String) {
        viewModelScope.launch {
            repo.subscribe(url)
                .onSuccess { emitEvent(UserEvent.ShowMessage("Feed added successfully")) }
                .onFailure { emitEvent(UserEvent.ShowError("Failed to add feed: ${it.message}")) }
        }
    }

    fun searchPodcasts(query: String) {
        viewModelScope.launch {
            repo.searchPodcasts(query)
                .onSuccess { _searchResults.value = it }
                .onFailure { emitEvent(UserEvent.ShowError("Search failed: ${it.message}")) }
        }
    }

    fun play(p: PodcastEntity) {
        playbackClient.play(p)
    }

    fun togglePlay() {
        playbackClient.togglePlay()
    }

    fun seek(ms: Long) {
        playbackClient.seek(ms)
    }

    fun skip(sec: Int) {
        playbackClient.skip(sec)
    }

    fun setPlaybackSpeed(speed: Float) {
        playbackClient.setSpeed(speed)
    }

    fun unsubscribe(title: String) {
        viewModelScope.launch {
            repo.unsubscribe(title)
        }
    }

    fun marketplaceSubscribe(query: String) {
        viewModelScope.launch {
            repo.searchPodcasts(query)
                .onSuccess { results ->
                    if (results.isNotEmpty()) {
                        repo.subscribe(results.first().feedUrl)
                            .onSuccess { emitEvent(UserEvent.ShowMessage("Subscribed to $query!")) }
                            .onFailure { emitEvent(UserEvent.ShowError("Failed to subscribe")) }
                    } else {
                        emitEvent(UserEvent.ShowMessage("No results found for $query"))
                    }
                }
                .onFailure { emitEvent(UserEvent.ShowError("Search failed")) }
        }
    }

    fun addToQueue(podcast: PodcastEntity) {
        viewModelScope.launch {
            repo.addToQueue(podcast.id)
        }
    }

    fun removeFromQueue(podcast: PodcastEntity) {
        viewModelScope.launch {
            repo.removeFromQueue(podcast.id)
        }
    }

    fun resumeLastPlayed() {
        viewModelScope.launch {
            repo.getLastPlayedPodcast()?.let { play(it) }
        }
    }
    
    fun resumePlayback() {
        playbackClient.resume()
    }

    private fun extractColor(url: String) {
        viewModelScope.launch {
            val currentPodcast = _uiState.value.current
            
            // 1. Data-First: Check if we already have the fact stored
            if (currentPodcast != null && currentPodcast.palette != null && currentPodcast.imageUrl == url) {
                _uiState.update { it.copy(dominantColor = currentPodcast.palette.dominant) }
                return@launch
            }

            // 2. Intelligence: Extract context from raw bits
            try {
                val loader = ImageLoader(context)
                val request = ImageRequest.Builder(context)
                    .data(url)
                    .allowHardware(false) // Required for Palette
                    .build()
                
                val result = loader.execute(request)
                if (result is SuccessResult) {
                    val bitmap = (result.drawable as? BitmapDrawable)?.bitmap
                    if (bitmap != null) {
                        val palette = Palette.from(bitmap).generate()
                        val dominant = palette.getDominantColor(AndroidColor.CYAN)
                        val vibrant = palette.getVibrantColor(AndroidColor.CYAN)
                        val muted = palette.getMutedColor(AndroidColor.GRAY)
                        
                        // Update UI
                        _uiState.update { it.copy(dominantColor = vibrant) }
                        
                        // 3. Persistence: Write the fact back to the ledger
                        if (currentPodcast != null) {
                            repo.savePalette(currentPodcast.id, PodcastPalette(dominant, vibrant, muted))
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("AppViewModel", "Color extraction failed", e)
            }
        }
    }

    fun playRadio() {
        viewModelScope.launch {
            val candidate = repo.getRadioCandidate()
            if (candidate != null) {
                repo.addToQueue(candidate.id)
                play(candidate)
                emitEvent(UserEvent.ShowMessage("Radio: Now playing ${candidate.episodeTitle}"))
            } else {
                emitEvent(UserEvent.ShowError("Radio silence. No unplayed episodes found."))
            }
        }
    }
    
    fun markPlayed(p: PodcastEntity) {
        viewModelScope.launch {
            repo.markPlayed(p)
            emitEvent(UserEvent.ShowMessage("Marked as played"))
        }
    }

    fun markOlderPlayed(p: PodcastEntity) {
        viewModelScope.launch {
            repo.markOlderAsPlayed(p)
            emitEvent(UserEvent.ShowMessage("Archived older episodes"))
        }
    }

    fun deleteDownload(p: PodcastEntity) {
        viewModelScope.launch {
            repo.deleteDownload(p.id)
            emitEvent(UserEvent.ShowMessage("Download deleted"))
        }
    }
    
    fun playNext(p: PodcastEntity) {
        viewModelScope.launch {
            repo.addToQueue(p.id) // Currently adds to end. 
            // TODO: Implement "Add to Top" in Dao if strict "Play Next" needed.
            // For now, "Add to Queue" is sufficient context.
            emitEvent(UserEvent.ShowMessage("Added to queue"))
        }
    }
}
