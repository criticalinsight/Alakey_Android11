package com.example.alakey.ui

import android.content.ComponentName
import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.example.alakey.data.ItunesSearchResult
import com.example.alakey.data.PodcastEntity
import com.example.alakey.data.UniversalRepository
import com.example.alakey.service.AudioService
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

@HiltViewModel
class AppViewModel @Inject constructor(
    private val repo: UniversalRepository,
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

    private var controller: MediaController? = null
    private var controllerFuture: ListenableFuture<MediaController>? = null
    private var playerListener: Player.Listener? = null
    private var progressPollingJob: Job? = null
    
    private val _searchResults = MutableStateFlow<List<ItunesSearchResult>>(emptyList())
    val searchResults: StateFlow<List<ItunesSearchResult>> = _searchResults.asStateFlow()
    
    private val _sleepTimerSeconds = MutableStateFlow(0)
    val sleepTimerSeconds: StateFlow<Int> = _sleepTimerSeconds.asStateFlow()
    
    private var sleepTimerJob: Job? = null
    private var initialSleepDuration: Int = 0 

    sealed interface UserEvent {
        data class ShowMessage(val message: String) : UserEvent
        data class ShowError(val message: String) : UserEvent
    }
    
    private val _userEvents = MutableSharedFlow<UserEvent>()
    val userEvents: SharedFlow<UserEvent> = _userEvents.asSharedFlow()

    init {
        viewModelScope.launch {
            repo.library.collect { podcasts ->
                val currentPodcast = _uiState.value.current
                val updatedPodcast = currentPodcast?.let { cp -> podcasts.find { it.id == cp.id } }
                _uiState.update { it.copy(podcasts = podcasts, current = updatedPodcast) }
            }
        }

        viewModelScope.launch {
            repo.queue.collect { queue ->
                _uiState.update { it.copy(queue = queue) }
            }
        }
    }

    private fun emitEvent(event: UserEvent) {
        viewModelScope.launch { _userEvents.emit(event) }
    }

    fun connect(ctx: Context) {
        val token = SessionToken(ctx, ComponentName(ctx, AudioService::class.java))
        controllerFuture = MediaController.Builder(ctx, token).buildAsync()
        controllerFuture?.addListener({
            controller = controllerFuture?.get()

            playerListener = object : Player.Listener {
                override fun onIsPlayingChanged(isPlaying: Boolean) {
                    _uiState.update { it.copy(isPlaying = isPlaying) }
                    if (isPlaying) startProgressPolling()
                    else stopProgressPolling()
                }

                override fun onPlaybackStateChanged(playbackState: Int) {
                    if (playbackState == Player.STATE_ENDED) {
                        val current = _uiState.value.current
                        if (current != null) {
                            deleteAudioFile(current)
                            playNextNewerEpisode(current)
                        }
                    }
                }

                override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                    val podcast = _uiState.value.podcasts.find { it.id == mediaItem?.mediaId }
                    _uiState.update {
                        it.copy(
                            current = podcast,
                            duration = controller?.duration?.coerceAtLeast(1) ?: 1
                        )
                    }
                }
            }.also { controller?.addListener(it) }

            controller?.let { c ->
                val podcast = _uiState.value.podcasts.find { p -> p.id == c.currentMediaItem?.mediaId }
                _uiState.update {
                    it.copy(
                        current = podcast,
                        isPlaying = c.isPlaying,
                        duration = c.duration.coerceAtLeast(1)
                    )
                }
                if (c.isPlaying) startProgressPolling()
                if (c.currentMediaItem == null && _uiState.value.podcasts.isNotEmpty()) {
                     resumeLastPlayed()
                }
            }
        }, MoreExecutors.directExecutor())
    }

    fun startSleepTimer(minutes: Int = 45) {
        sleepTimerJob?.cancel()
        initialSleepDuration = minutes * 60
        _sleepTimerSeconds.value = initialSleepDuration
         controller?.play()
        sleepTimerJob = viewModelScope.launch {
            while (_sleepTimerSeconds.value > 0) {
                delay(1000)
                _sleepTimerSeconds.value--
            }
            controller?.pause()
            controller?.volume = 1.0f
        }
    }

    fun resetSleepTimer() {
        if (_sleepTimerSeconds.value > 0) {
             _sleepTimerSeconds.value = initialSleepDuration
             controller?.volume = 1.0f
        }
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
                Log.d("AppViewModel", "Auto-downloading episode: ${latestEpisode.episodeTitle}")
            }
        }
    }

    private fun startProgressPolling() {
        progressPollingJob?.cancel()
        progressPollingJob = viewModelScope.launch {
            while (isActive) {
                val current = controller?.currentPosition ?: 0L
                val duration = controller?.duration ?: 1L
                _uiState.update { it.copy(currentTime = current, duration = if (duration > 0) duration else 1L) }
                
                _uiState.value.current?.let { p ->
                    if (current > 0) {
                         repo.updateProgress(p.id, current)
                         repo.updateLastPlayed(p.id, System.currentTimeMillis())
                    }
                }
                delay(1000)
            }
        }
    }

    private fun stopProgressPolling() {
        progressPollingJob?.cancel()
        progressPollingJob = null
    }

    override fun onCleared() {
        super.onCleared()
        stopProgressPolling()
        sleepTimerJob?.cancel()
        playerListener?.let { controller?.removeListener(it) }
        controllerFuture?.let { MediaController.releaseFuture(it) }
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
        viewModelScope.launch {
            val mediaItem = MediaItem.Builder()
                .setMediaId(p.id)
                .setUri(p.audioUrl)
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        .setTitle(p.episodeTitle)
                        .setArtist(p.title)
                        .setArtworkUri(android.net.Uri.parse(p.imageUrl))
                        .build()
                )
                .build()
            controller?.setMediaItem(mediaItem)
            controller?.prepare()
            controller?.play()
        }
    }

    fun togglePlay() {
        if (controller?.isPlaying == true) controller?.pause() else controller?.play()
    }

    fun seek(ms: Long) {
        controller?.seekTo(ms)
    }

    fun skip(sec: Int) {
        controller?.seekTo((controller?.currentPosition ?: 0) + (sec * 1000))
    }

    fun setPlaybackSpeed(speed: Float) {
        controller?.setPlaybackSpeed(speed)
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
        controller?.play()
    }

    private fun deleteAudioFile(podcast: PodcastEntity) {
        viewModelScope.launch {
             if (podcast.audioUrl.startsWith(context.getExternalFilesDir(null)?.absolutePath ?: "file://")) {
                 try {
                     val file = File(podcast.audioUrl)
                     if (file.exists()) {
                         file.delete() 
                         repo.savePodcast(podcast.copy(isDownloaded = false, audioUrl = podcast.feedUrl))
                     }
                 } catch (e: Exception) {
                     emitEvent(UserEvent.ShowError("Failed to delete file"))
                 }
             }
        }
    }

    private fun playNextNewerEpisode(current: PodcastEntity) {
        viewModelScope.launch {
            val allEpisodes = repo.getPodcastsByTitle(current.title).sortedBy { it.pubDate }
            val currentIndex = allEpisodes.indexOfFirst { it.id == current.id }
            if (currentIndex != -1 && currentIndex + 1 < allEpisodes.size) {
                 val nextEpisode = allEpisodes[currentIndex + 1]
                 play(nextEpisode)
                 emitEvent(UserEvent.ShowMessage("Playing next episode: ${nextEpisode.episodeTitle}"))
            } else {
                 emitEvent(UserEvent.ShowMessage("All caught up!"))
            }
        }
    }
}
