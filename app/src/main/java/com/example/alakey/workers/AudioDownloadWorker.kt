package com.example.alakey.workers

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import com.example.alakey.data.UniversalRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class AudioDownloadWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val repository: UniversalRepository
) : CoroutineWorker(appContext, workerParams) {
    override suspend fun doWork(): Result {
        val podcastId = inputData.getString("podcastId") ?: return Result.failure()
        return try {
            repository.downloadAudio(podcastId)
            Log.d("AudioDownloadWorker", "Downloaded audio for $podcastId")
            Result.success()
        } catch (e: Exception) {
            Log.e("AudioDownloadWorker", "Error downloading audio", e)
            Result.failure()
        }
            Result.failure()
        }
    }

    override suspend fun getForegroundInfo(): ForegroundInfo {
        return ForegroundInfo(
            1, 
            androidx.core.app.NotificationCompat.Builder(applicationContext, "download_channel")
                .setContentTitle("Downloading Episode")
                .setSmallIcon(android.R.drawable.stat_sys_download)
                .build()
        )
    }
}
