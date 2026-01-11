package com.example.alakey.data

import android.content.Context
import android.util.Base64
import android.util.Log
import android.util.Xml
import androidx.room.*
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import org.xmlpull.v1.XmlPullParser
import java.io.*
import javax.inject.Inject
import javax.inject.Singleton

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
            // Delete audio file
            if (p.isDownloaded && p.audioUrl.startsWith("/")) { // Check for local path
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

object RssParser {
    fun parse(xml: String, feedUrl: String): List<PodcastEntity> {
        val list = mutableListOf<PodcastEntity>()
        try {
            val parser = Xml.newPullParser().apply {
                setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
                setInput(StringReader(xml))
            }

            var eventType = parser.eventType
            while (eventType != XmlPullParser.START_TAG && eventType != XmlPullParser.END_DOCUMENT) {
                eventType = parser.next()
            }

            if (eventType == XmlPullParser.END_DOCUMENT || parser.name.equals("html", ignoreCase = true)) {
                Log.e("RssParser", "Invalid feed: Found HTML or empty document.")
                return emptyList()
            }

            var inItem = false
            var title = ""
            var ep = ""
            var desc = ""
            var img = ""
            var audio = ""
            var link = ""

                while (parser.eventType != XmlPullParser.END_DOCUMENT) {
                when (parser.eventType) {
                    XmlPullParser.START_TAG -> {
                        when (parser.name) {
                            "item", "entry" -> inItem = true
                            "title" -> if (inItem) ep = readText(parser) else title = readText(parser)
                            "description" -> if (inItem) desc = readText(parser)
                            "content:encoded" -> if (inItem) {
                                val content = readText(parser)
                                if (content.length > desc.length) desc = content
                            }
                            "enclosure" -> if (inItem) audio = parser.getAttributeValue(null, "url") ?: ""
                            "itunes:image" -> img = parser.getAttributeValue(null, "href") ?: ""
                            "link" -> if (inItem) link = readText(parser)
                        }
                    }
                    XmlPullParser.END_TAG -> {
                        if (parser.name == "item" || parser.name == "entry") {
                            val cleanDesc = desc.replace(Regex("<.*?>"), " ").replace(Regex("\\s+"), " ").trim()
                            if (audio.isNotEmpty()) {
                                list.add(PodcastEntity((link + ep).hashCode().toString(), title, ep, cleanDesc, img, audio, feedUrl, 0, "", false, false, 0, 0, 0))
                            }
                            inItem = false; ep = ""; desc = ""; audio = ""; link = ""
                        }
                    }
                }
                parser.next()
            }
        } catch (e: Exception) {
            Log.e("RssParser", "Failed to parse XML", e)
            return emptyList()
        }
        return list
    }

    private fun readText(parser: XmlPullParser): String {
        var result = ""
        if (parser.next() == XmlPullParser.TEXT) {
            result = parser.text
            parser.nextTag()
        }
        return result
    }
}

@Dao interface PodcastDao {
    @Query("SELECT * FROM podcasts ORDER BY pubDate DESC") fun getAllPodcasts(): kotlinx.coroutines.flow.Flow<List<PodcastEntity>>
    @Query("SELECT * FROM podcasts WHERE isInQueue = 1 ORDER BY queueOrder ASC") fun getQueue(): kotlinx.coroutines.flow.Flow<List<PodcastEntity>>
    @Query("SELECT * FROM podcasts WHERE id = :id LIMIT 1") suspend fun getPodcastById(id: String): PodcastEntity?
    @Query("SELECT DISTINCT feedUrl FROM podcasts WHERE feedUrl != ''") suspend fun getSubscribedFeeds(): List<String>
    @Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun insertEpisodes(list: List<PodcastEntity>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertPodcast(p: PodcastEntity)
    @Query("UPDATE podcasts SET audioUrl = :path, isDownloaded = 1 WHERE id = :id") suspend fun updateAudioPath(id: String, path: String)
    @Query("UPDATE podcasts SET progress = :progress, lastPlayed = :timestamp WHERE id = :id") suspend fun updateProgress(id: String, progress: Long, timestamp: Long)
    @Query("UPDATE podcasts SET isInQueue = 1, queueOrder = :order WHERE id = :id") suspend fun addToQueue(id: String, order: Long)
    @Query("UPDATE podcasts SET isInQueue = 0 WHERE id = :id") suspend fun removeFromQueue(id: String)
    @Query("SELECT * FROM podcasts WHERE title = :title") suspend fun getEpisodesByTitle(title: String): List<PodcastEntity>
    @Query("DELETE FROM podcasts WHERE title = :title") suspend fun deleteByTitle(title: String)
    @Query("UPDATE podcasts SET lastPlayed = :timestamp WHERE id = :id") suspend fun updateLastPlayed(id: String, timestamp: Long)
    @Query("SELECT * FROM podcasts ORDER BY lastPlayed DESC LIMIT 1") suspend fun getLastPlayedPodcast(): PodcastEntity?
}

@Database(entities = [PodcastEntity::class], version = 8, exportSchema = false)
abstract class AppDatabase : RoomDatabase() { 
    abstract fun dao(): PodcastDao 
}

@Module @InstallIn(SingletonComponent::class) object DataModule {
    @Provides @Singleton fun provideDb(@ApplicationContext c: Context) = Room.databaseBuilder(c, AppDatabase::class.java, "db").fallbackToDestructiveMigration().build()
    @Provides fun provideDao(db: AppDatabase) = db.dao()
}
