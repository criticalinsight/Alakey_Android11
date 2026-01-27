package com.example.alakey.ui

sealed interface PlaybackEvent {
    data class IsPlayingChanged(val isPlaying: Boolean) : PlaybackEvent
    data class PositionUpdated(val position: Long, val duration: Long, val amplitude: Float) : PlaybackEvent
    data class MediaChanged(val mediaId: String?, val duration: Long) : PlaybackEvent
    data class SpeedChanged(val speed: Float) : PlaybackEvent
}
