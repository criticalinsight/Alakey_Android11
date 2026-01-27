package com.example.alakey.service

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.cache.NoOpCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.offline.*
import androidx.media3.session.*
import com.example.alakey.MainActivity
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import java.io.File
import java.util.concurrent.Executors
import javax.inject.Inject

@AndroidEntryPoint
class AudioService : MediaLibraryService() {
    @Inject lateinit var audioSystem: com.example.alakey.system.AudioSystem

    private var session: MediaLibrarySession? = null
    
    // De-complected: Sleep Timer logic moved to PlaybackClient (Pure Logic Event Stream)
    // We keep the service as a humble shell for the Player instance.

    @OptIn(UnstableApi::class)
    override fun onCreate() {
        super.onCreate()
        audioSystem.start()
        
        // Safety check: System might fail to start if Context is null? (Unlikely with Hilt)
        val player = audioSystem.player ?: throw IllegalStateException("AudioSystem failed to start")

        session = MediaLibrarySession.Builder(this, player, object : MediaLibrarySession.Callback {} )
            .setSessionActivity(PendingIntent.getActivity(this, 0, Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE))
            .build()
    }

    override fun onGetSession(info: MediaSession.ControllerInfo) = session
    override fun onDestroy() { 
        session?.release()
        audioSystem.stop()
        super.onDestroy() 
    }
}

@UnstableApi
class DownloadService : androidx.media3.exoplayer.offline.DownloadService(
    101, 1000, "dl", com.example.alakey.R.string.download_channel_name, com.example.alakey.R.string.download_channel_description
) {
    override fun getDownloadManager() = DownloadUtil.getManager(this)
    override fun getScheduler() = null
    override fun getForegroundNotification(downloads: MutableList<Download>, notMet: Int) =
        DownloadNotificationHelper(this, "dl").buildProgressNotification(this, com.example.alakey.R.drawable.ic_launcher_foreground, null, null, downloads, notMet)
}

@UnstableApi
object DownloadUtil {
    private var manager: DownloadManager? = null
    @Synchronized fun getManager(c: Context): DownloadManager {
        if (manager == null) {
            val db = StandaloneDatabaseProvider(c)
            val cache = SimpleCache(File(c.externalCacheDir, "downloads"), NoOpCacheEvictor(), db)
            manager = DownloadManager(c, db, cache, DefaultHttpDataSource.Factory(), Executors.newFixedThreadPool(6))
        }
        return manager!!
    }
}