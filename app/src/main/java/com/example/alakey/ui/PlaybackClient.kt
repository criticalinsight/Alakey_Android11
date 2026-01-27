package com.example.alakey.ui

import android.content.ComponentName
import android.content.Context
import android.util.Log
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.example.alakey.data.PodcastEntity
import com.example.alakey.service.AudioService
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PlaybackClient @Inject constructor(
    @ApplicationContext private val context: Context
) {
    data class PlaybackState(
        val isPlaying: Boolean = false,
        val duration: Long = 1L,
        val currentPosition: Long = 0L,
        val currentMediaId: String? = null, 
        val playbackSpeed: Float = 1.0f,
        val amplitude: Float = 0f
    )

    private val _state = MutableStateFlow(PlaybackState())
    val state: StateFlow<PlaybackState> = _state.asStateFlow()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var controller: MediaController? = null
    private var controllerFuture: ListenableFuture<MediaController>? = null
    private var progressJob: Job? = null
    
    // Sleep Timer
    private val _sleepTimerSeconds = MutableStateFlow(0)
    val sleepTimerSeconds: StateFlow<Int> = _sleepTimerSeconds.asStateFlow()
    private var sleepTimerJob: Job? = null
    private var initialSleepDuration: Int = 0
    
    // Smart Resume
    private var lastPauseTime: Long = 0

    init {
        connect()
    }

    private fun connect() {
        val token = SessionToken(context, ComponentName(context, AudioService::class.java))
        controllerFuture = MediaController.Builder(context, token).buildAsync()
        controllerFuture?.addListener({
            try {
                controller = controllerFuture?.get()
                setupListener()
                syncInitialState()
            } catch (e: Exception) {
                Log.e("PlaybackClient", "Failed to connect to MediaController", e)
            }
        }, MoreExecutors.directExecutor())
    }

    private fun setupListener() {
        controller?.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                processEvent(PlaybackEvent.IsPlayingChanged(isPlaying))
                if (isPlaying) startProgressPolling() else stopProgressPolling()
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_ENDED) {
                     // Handle end logic if needed, or expose event
                }
            }

            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                processEvent(PlaybackEvent.MediaChanged(
                    mediaId = mediaItem?.mediaId,
                    duration = controller?.duration?.coerceAtLeast(1) ?: 1
                ))
            }
        })
    }
    
    // Pure Reduction
    private fun reduce(currentState: PlaybackState, event: PlaybackEvent): PlaybackState {
        return when (event) {
            is PlaybackEvent.IsPlayingChanged -> currentState.copy(isPlaying = event.isPlaying)
            is PlaybackEvent.PositionUpdated -> currentState.copy(
                currentPosition = event.position, 
                duration = if (event.duration > 0) event.duration else 1L, // Ensure non-zero
                amplitude = event.amplitude
            )
            is PlaybackEvent.MediaChanged -> currentState.copy(
                currentMediaId = event.mediaId,
                duration = event.duration
            )
            is PlaybackEvent.SpeedChanged -> currentState.copy(playbackSpeed = event.speed)
        }
    }
    
    private fun processEvent(event: PlaybackEvent) {
        _state.update { reduce(it, event) }
    }

    private fun syncInitialState() {
        controller?.let { c ->
            // Batch initial sync? Or emit sequence?
            // Let's just update directly or emit multiple events.
            // Direct reducers for distinct properties.
            processEvent(PlaybackEvent.IsPlayingChanged(c.isPlaying))
            processEvent(PlaybackEvent.MediaChanged(c.currentMediaItem?.mediaId, c.duration.coerceAtLeast(1)))
            processEvent(PlaybackEvent.SpeedChanged(c.playbackParameters.speed))
            
            if (c.isPlaying) startProgressPolling()
        }
    }

    private fun startProgressPolling() {
        progressJob?.cancel()
        progressJob = scope.launch {
            var counter = 0f
            while (isActive) {
                val pos = controller?.currentPosition ?: 0L
                val dur = controller?.duration ?: 1L
                val amp = if (controller?.isPlaying == true) {
                    (Math.sin(counter.toDouble()).toFloat() * 0.2f + 0.8f) * (0.8f + Math.random().toFloat() * 0.4f)
                } else 0f
                processEvent(PlaybackEvent.PositionUpdated(pos, dur, amp))
                counter += 0.5f
                delay(100)
            }
        }
    }

    private fun stopProgressPolling() {
        progressJob?.cancel()
        progressJob = null
    }

    fun play(podcast: PodcastEntity) {
        val c = controller
        if (c == null) {
            Log.e("PlaybackClient", "Cannot play: controller is null")
            return
        }
        val mediaItem = MediaItem.Builder()
            .setMediaId(podcast.id)
            .setUri(podcast.audioUrl)
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(podcast.episodeTitle)
                    .setArtist(podcast.title)
                    .setArtworkUri(android.net.Uri.parse(podcast.imageUrl))
                    .build()
            )
            .build()
        c.setMediaItem(mediaItem)
        c.prepare()
        c.play()
    }

    fun resume() {
        Log.d("PlaybackClient", "Resume called. Controller: ${controller != null}")
        
        // Smart Resume: Rewind 3s if paused > 5 min
        if (lastPauseTime > 0 && (System.currentTimeMillis() - lastPauseTime) > 5 * 60 * 1000) {
            val pos = controller?.currentPosition ?: 0L
            controller?.seekTo((pos - 3000).coerceAtLeast(0))
            Log.d("PlaybackClient", "Smart Resume: Rewinded 3s")
        }
        lastPauseTime = 0
        
        controller?.play()
    }

    fun pause() {
        Log.d("PlaybackClient", "Pause called. Controller: ${controller != null}")
        lastPauseTime = System.currentTimeMillis()
        controller?.pause()
    }

    fun togglePlay() {
        if (controller?.isPlaying == true) pause() else resume()
    }

    fun seek(ms: Long) {
        controller?.seekTo(ms)
    }

    fun skip(seconds: Int) {
        val newPos = (controller?.currentPosition ?: 0) + (seconds * 1000)
        controller?.seekTo(newPos)
    }

    fun setSpeed(speed: Float) {
        controller?.setPlaybackSpeed(speed)
        processEvent(PlaybackEvent.SpeedChanged(speed))
    }
    
    fun startSleepTimer(minutes: Int) {
        sleepTimerJob?.cancel()
        initialSleepDuration = minutes * 60
        _sleepTimerSeconds.value = initialSleepDuration
        resume()
        
        sleepTimerJob = scope.launch {
            while (_sleepTimerSeconds.value > 0) {
                delay(1000)
                _sleepTimerSeconds.value--
            }
            pause()
            // Reset volume if we were fading, but we're just pausing for now
        }
    }
    
    fun resetSleepTimer() {
        if (_sleepTimerSeconds.value > 0) {
            _sleepTimerSeconds.value = initialSleepDuration
        }
    }
    
    fun cleanup() {
        stopProgressPolling()
        sleepTimerJob?.cancel()
        controllerFuture?.let { MediaController.releaseFuture(it) }
    }
}
