package com.example.alakey.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface PodcastDao {
    @Query("SELECT * FROM podcasts ORDER BY pubDate DESC")
    fun getAllPodcasts(): Flow<List<PodcastEntity>>

    @Query("SELECT * FROM podcasts WHERE isInQueue = 1 ORDER BY queueOrder ASC")
    fun getQueue(): Flow<List<PodcastEntity>>

    @Query("SELECT * FROM podcasts WHERE id = :id LIMIT 1")
    suspend fun getPodcastById(id: String): PodcastEntity?

    @Query("SELECT DISTINCT feedUrl FROM podcasts WHERE feedUrl != ''")
    suspend fun getSubscribedFeeds(): List<String>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertEpisodes(list: List<PodcastEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPodcast(p: PodcastEntity)

    @Query("UPDATE podcasts SET audioUrl = :path, isDownloaded = 1 WHERE id = :id")
    suspend fun updateAudioPath(id: String, path: String)

    @Query("UPDATE podcasts SET progress = :progress, lastPlayed = :timestamp WHERE id = :id")
    suspend fun updateProgress(id: String, progress: Long, timestamp: Long)

    @Query("UPDATE podcasts SET isInQueue = 1, queueOrder = :order WHERE id = :id")
    suspend fun addToQueue(id: String, order: Long)

    @Query("UPDATE podcasts SET isInQueue = 0 WHERE id = :id")
    suspend fun removeFromQueue(id: String)

    @Query("SELECT * FROM podcasts WHERE title = :title")
    suspend fun getEpisodesByTitle(title: String): List<PodcastEntity>

    @Query("DELETE FROM podcasts WHERE title = :title")
    suspend fun deleteByTitle(title: String)

    @Query("UPDATE podcasts SET lastPlayed = :timestamp WHERE id = :id")
    suspend fun updateLastPlayed(id: String, timestamp: Long)

    @Query("SELECT * FROM podcasts ORDER BY lastPlayed DESC LIMIT 1")
    suspend fun getLastPlayedPodcast(): PodcastEntity?
}
