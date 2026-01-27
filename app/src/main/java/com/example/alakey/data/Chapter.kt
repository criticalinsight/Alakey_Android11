package com.example.alakey.data

data class Chapter(
    val start: Long, // Start time in milliseconds
    val title: String,
    val imageUrl: String? = null,
    val link: String? = null
)
