package com.example.alakey.service.components

import android.content.Context
import androidx.media3.exoplayer.ExoPlayer

/**
 * Pure Mechanism: ExoPlayer Setup.
 * Encapsulates the configuration of the playback engine.
 */
object PlaybackEngine {
    fun create(context: Context): ExoPlayer {
        return ExoPlayer.Builder(context)
            .setHandleAudioBecomingNoisy(true)
            .setWakeMode(android.os.PowerManager.PARTIAL_WAKE_LOCK)
            .build()
    }
}
