package com.example.alakey.data

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UniversalRepository @Inject constructor(
    private val dao: PodcastDao,
    @ApplicationContext private val context: Context
) {
    val library = dao.getAllPodcasts()
    val queue = dao.getQueue()
    private val client = OkHttpClient()

    private suspend fun <T> safeApiCall(retries: Int = 3, initialDelay: Long = 2000, apiCall: suspend () -> T): Result<T> {
        return withContext(Dispatchers.IO) {
            var currentDelay = initialDelay
            repeat(retries - 1) {
                try {
                    return@withContext Result.success(apiCall())
                } catch (e: Exception) {
                    Log.w("UniversalRepository", "API Call Failed. Retrying in ${currentDelay}ms", e)
                    delay(currentDelay)
                    currentDelay *= 2
                }
            }
            try {
                Result.success(apiCall())
            } catch (e: Exception) {
                Log.e("UniversalRepository", "API Call Failed after retries", e)
                Result.failure(e)
            }
        }
    }

    suspend fun searchPodcasts(query: String): Result<List<ItunesSearchResult>> = safeApiCall {
        val request = Request.Builder().url("https://itunes.apple.com/search?term=$query&entity=podcast&media=podcast").build()
        val response = client.newCall(request).execute()
        if (!response.isSuccessful) throw Exception("iTunes search failed: ${response.code}")
        
        val json = response.body!!.string()
        Gson().fromJson(json, ItunesSearchResponse::class.java).results
    }

    suspend fun subscribe(url: String): Result<Boolean> = safeApiCall {
        var xmlContent: String? = null
        
        try {
            Log.d("UniversalRepository", "Attempting direct fetch for: $url")
            xmlContent = fetchDirect(url)
        } catch (e: Exception) {
            Log.w("UniversalRepository", "Direct fetch failed: ${e.message}")
        }

        if (xmlContent == null || xmlContent.trim().startsWith("<!DOCTYPE html", ignoreCase = true) || !xmlContent.trim().startsWith("<")) {
            try {
                Log.d("UniversalRepository", "Attempting proxy fetch for: $url")
                xmlContent = fetchWithProxy(url)
            } catch (e: Exception) {
                Log.w("UniversalRepository", "Proxy fetch failed: ${e.message}")
            }
        }

        if (xmlContent == null || xmlContent.trim().startsWith("<!DOCTYPE html", ignoreCase = true) || !xmlContent.trim().startsWith("<")) {
            throw Exception("Failed to fetch valid feed content")
        }
        
        val items = RssParser.parse(xmlContent, url)
        if (items.isNotEmpty()) {
            dao.insertEpisodes(items)
            Log.d("UniversalRepository", "Successfully subscribed to $url, ${items.size} items found.")
            true
        } else {
            throw Exception("No episodes found in feed")
        }
    }

    suspend fun syncAll() = withContext(Dispatchers.IO) {
        dao.getSubscribedFeeds().forEach { url -> 
            try {
                subscribe(url)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    suspend fun savePodcast(p: PodcastEntity) = dao.insertPodcast(p)
    suspend fun getPodcastsByTitle(title: String) = dao.getEpisodesByTitle(title)
    suspend fun updateProgress(id: String, progress: Long) = dao.updateProgress(id, progress, System.currentTimeMillis()) 
    suspend fun updateLastPlayed(id: String, timestamp: Long) = dao.updateLastPlayed(id, timestamp)

    private suspend fun fetchWithProxy(url: String): String = withContext(Dispatchers.IO) {
        val request = Request.Builder().url("https://api.allorigins.win/get?url=$url").build()
        client.newCall(request).execute().body!!.string().let { JSONObject(it).getString("contents") }
    }

    private suspend fun fetchDirect(url: String): String = withContext(Dispatchers.IO) {
        val request = Request.Builder().url(url).build()
        client.newCall(request).execute().body!!.string()
    }

    suspend fun downloadAudio(podcastId: String): Result<String> = safeApiCall {
         val podcast = dao.getPodcastById(podcastId) ?: throw Exception("Podcast not found")
         if (podcast.audioUrl.isEmpty()) throw Exception("No audio URL")
         
         val request = Request.Builder().url(podcast.audioUrl).build()
         val response = client.newCall(request).execute()
         if (!response.isSuccessful) throw Exception("Download failed: ${response.code}")
         
         val file = File(context.filesDir, "${podcastId}.mp3")
         val inputStream = response.body!!.byteStream()
         FileOutputStream(file).use { output ->
             inputStream.copyTo(output)
         }
         dao.updateAudioPath(podcastId, file.absolutePath)
         file.absolutePath
    }

    suspend fun unsubscribe(title: String) = withContext(Dispatchers.IO) {
        val episodes = dao.getEpisodesByTitle(title)
        episodes.forEach { p ->
            if (p.isDownloaded && p.audioUrl.startsWith("/")) {
                 val file = File(p.audioUrl)
                 if (file.exists()) file.delete()
            }
        }
        dao.deleteByTitle(title)
    }

    suspend fun addToQueue(id: String) = dao.addToQueue(id, System.currentTimeMillis())
    suspend fun removeFromQueue(id: String) = dao.removeFromQueue(id)
    suspend fun getLastPlayedPodcast(): PodcastEntity? = dao.getLastPlayedPodcast()
    suspend fun saveProgress(id: String, progress: Long) = dao.updateProgress(id, progress, System.currentTimeMillis())
}
