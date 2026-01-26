package com.example.alakey.service.components

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.media3.common.MediaMetadata
import androidx.media3.session.MediaStyleNotificationHelper

/**
 * Pure Policy: State -> Notification.
 * Does not manage playback. Just formats the specific notification style.
 */
class NotificationPolicy(private val context: Context) {
    @androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class) 
    fun createNotification(
        mediaMetadata: MediaMetadata,
        session: androidx.media3.session.MediaSession,
        isPlaying: Boolean
    ): Notification {
        return NotificationCompat.Builder(context, "playback_channel")
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentTitle(mediaMetadata.title)
            .setContentText(mediaMetadata.artist)
            .setStyle(MediaStyleNotificationHelper.MediaStyle(session))
            .setOngoing(isPlaying)
            .build()
    }
}
