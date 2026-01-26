package com.example.alakey.data

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [PodcastEntity::class, EventLogEntity::class], version = 9, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun dao(): PodcastDao
    abstract fun eventLogDao(): EventLogDao
}
