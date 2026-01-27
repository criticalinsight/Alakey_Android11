package com.example.alakey.di

import android.content.Context
import androidx.room.Room
import com.example.alakey.data.AppDatabase
import com.example.alakey.data.PodcastDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DataModule {
    @Provides
    @Singleton
    fun provideDb(@ApplicationContext c: Context): AppDatabase = 
        Room.databaseBuilder(c, AppDatabase::class.java, "db")
            .fallbackToDestructiveMigration()
            .build()

    @Provides
    fun provideDao(db: AppDatabase): PodcastDao = db.dao()

    @Provides
    fun provideEventLogDao(db: AppDatabase): com.example.alakey.data.EventLogDao = db.eventLogDao()

    @Provides
    fun provideFactDao(db: AppDatabase): com.example.alakey.data.FactDao = db.factDao()
}
