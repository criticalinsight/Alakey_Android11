package com.example.alakey.ui

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.alakey.data.ItunesSearchResult
import com.example.alakey.data.PodcastEntity
import com.example.alakey.data.UniversalRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

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
        val queue: List<PodcastEntity> = emptyList()
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

        // Hydrate Queue
        viewModelScope.launch {
            repo.queue.collect { queue ->
                _uiState.update { it.copy(queue = queue) }
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
                        speed = pbState.playbackSpeed
                    )
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

    fun connect(ctx: Context) {
        // No-op: Client connects on init. 
        // Keeping method signature for now to avoid breaking Activity call sites immediately, 
        // though ideally Activity should not need to call this.
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
            val podcasts = repo.library.first()
            if (podcasts.isNotEmpty()) {
                val latestEpisode = podcasts.first()
                repo.downloadAudio(latestEpisode.id)
            }
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
}
