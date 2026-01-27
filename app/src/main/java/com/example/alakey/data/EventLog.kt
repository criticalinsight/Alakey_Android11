package com.example.alakey.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Entity(tableName = "event_log")
data class EventLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: String, // "SYNC_FEED", "DOWNLOAD_EPISODE", "PLAY_EPISODE"
    val payload: String, // JSON or simple ID
    val status: String, // "PENDING", "COMPLETED", "FAILED"
    val timestamp: Long
)

@Dao
interface EventLogDao {
    @Insert
    suspend fun logEvent(event: EventLogEntity)

    @Query("SELECT * FROM event_log ORDER BY timestamp DESC LIMIT 50")
    suspend fun getRecentEvents(): List<EventLogEntity>

    @Query("SELECT * FROM event_log WHERE type = :type ORDER BY timestamp DESC LIMIT 50")
    suspend fun getEventsByType(type: String): List<EventLogEntity>

    @Query("SELECT * FROM event_log WHERE status = 'FAILED' ORDER BY timestamp DESC LIMIT 50")
    suspend fun getFailedEvents(): List<EventLogEntity>
    
    @Query("SELECT * FROM event_log WHERE payload LIKE '%' || :query || '%' ORDER BY timestamp DESC LIMIT 50")
    suspend fun grepLogs(query: String): List<EventLogEntity>
}
