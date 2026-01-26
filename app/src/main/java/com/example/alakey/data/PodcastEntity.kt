package com.example.alakey.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "podcasts")
data class PodcastEntity(
    @PrimaryKey val id: String,
    val title: String,
    val episodeTitle: String,
    val description: String,
    val imageUrl: String,
    val audioUrl: String,
    val feedUrl: String = "",
    val duration: Long = 0,
    val pubDate: String = "",
    val isDownloaded: Boolean = false,
    val isInQueue: Boolean = false,
    val queueOrder: Long = 0,
    val progress: Long = 0,
    val lastPlayed: Long = 0
)

data class ItunesSearchResult(val collectionName: String, val feedUrl: String, val artworkUrl100: String)
data class ItunesSearchResponse(val results: List<ItunesSearchResult>)
