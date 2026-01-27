package com.example.alakey.system

import android.content.Context
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.exoplayer.ExoPlayer
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AudioSystem @Inject constructor(
    @ApplicationContext private val context: Context
) : Component {

    var player: ExoPlayer? = null
        private set
        
    private var loudnessEnhancer: android.media.audiofx.LoudnessEnhancer? = null

    override fun start() {
        if (player != null) return

        val attr = AudioAttributes.Builder()
            .setContentType(C.AUDIO_CONTENT_TYPE_SPEECH)
            .setUsage(C.USAGE_MEDIA)
            .build()
            
        player = ExoPlayer.Builder(context)
            .setAudioAttributes(attr, true)
            .setHandleAudioBecomingNoisy(true)
            .setSkipSilenceEnabled(true)
            .build()

        try {
            loudnessEnhancer = android.media.audiofx.LoudnessEnhancer(player!!.audioSessionId)
            loudnessEnhancer?.setTargetGain(200)
            loudnessEnhancer?.enabled = true
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun stop() {
        player?.release()
        player = null
        loudnessEnhancer?.release()
        loudnessEnhancer = null
    }
}
