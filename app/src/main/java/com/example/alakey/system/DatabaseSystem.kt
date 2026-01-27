package com.example.alakey.system

import android.content.Context
import androidx.room.Room
import com.example.alakey.data.AppDatabase
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DatabaseSystem @Inject constructor(
    @ApplicationContext private val context: Context
) : Component {

    private var _db: AppDatabase? = null
    val db: AppDatabase get() = _db ?: throw IllegalStateException("DatabaseSystem not started")

    override fun start() {
        if (_db != null) return
        _db = Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "alakey-db"
        ).fallbackToDestructiveMigration().build()
    }

    override fun stop() {
        _db?.close()
        _db = null
    }
}
