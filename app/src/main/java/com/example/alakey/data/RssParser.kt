package com.example.alakey.data

import android.util.Log
import android.util.Xml
import org.xmlpull.v1.XmlPullParser
import java.io.StringReader

/**
 * Pure functional parser.
 * No state. No side effects. Just String -> ListView.
 */
object RssParser {
    fun parse(xml: String, feedUrl: String): List<PodcastEntity> {
        return try {
            val parser = Xml.newPullParser().apply {
                setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
                setInput(StringReader(xml))
            }
            readFeed(parser, feedUrl)
        } catch (e: Exception) {
            Log.e("RssParser", "Parse failure", e)
            emptyList()
        }
    }

    private fun readFeed(parser: XmlPullParser, feedUrl: String): List<PodcastEntity> {
        val entries = mutableListOf<PodcastEntity>()
        var eventType = parser.eventType
        // Store channel title
        var channelTitle = "Podcast" 
        var channelImage = ""

        // Fast-forward to root
        while (eventType != XmlPullParser.START_TAG && eventType != XmlPullParser.END_DOCUMENT) {
            eventType = parser.next()
        }
        
        if (eventType == XmlPullParser.END_DOCUMENT) return emptyList()

        while (parser.next() != XmlPullParser.END_DOCUMENT) {
            if (parser.eventType == XmlPullParser.START_TAG) {
                if (parser.name == "title" && channelTitle == "Podcast") {
                     channelTitle = readText(parser)
                } else if (parser.name == "itunes:image" || parser.name == "image") {
                     // Capture channel-level artwork
                     val href = parser.getAttributeValue(null, "href")
                     val url = if (!href.isNullOrEmpty()) href else {
                         // Some feeds use <image><url>...</url></image>
                         // But we'll stick to attributes for simplicity in this pass, or handle sub-tags?
                         // Most RSS 2.0 uses <itunes:image href="...">.
                         // Standard <image> usually has a <url> child. We might miss that with just attributes.
                         // Let's assume attribute first.
                         ""
                     }
                     if (url.isNotEmpty()) channelImage = url
                } else if (parser.name == "item" || parser.name == "entry") {
                    readEntry(parser, feedUrl, channelTitle, channelImage)?.let { entries.add(it) }
                }
            }
        }
        return entries
    }

    private fun readEntry(parser: XmlPullParser, feedUrl: String, channelTitle: String, channelImage: String): PodcastEntity? {
        var title = ""
        var description = ""
        var link = ""
        var audioUrl = ""
        var imageUrl = ""
        var pubDate = ""
        var season = 0
        var episodeType = "full"

        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.eventType != XmlPullParser.START_TAG) continue
            when (parser.name) {
                "title" -> title = readText(parser)
                "description", "summary", "content" -> description = readText(parser)
                "link" -> link = readText(parser)
                "enclosure" -> audioUrl = parser.getAttributeValue(null, "url") ?: ""
                "itunes:image" -> imageUrl = parser.getAttributeValue(null, "href") ?: ""
                "pubDate", "published" -> pubDate = readText(parser)
                "itunes:season" -> season = readText(parser).toIntOrNull() ?: 0
                "itunes:episodeType" -> episodeType = readText(parser)
                else -> skip(parser)
            }
        }
        
        // Validate required fields ("Simplicity is reliability" - reject malformed data early)
        if (audioUrl.isEmpty() || title.isEmpty()) return null

        // Strip HTML from description for purity
        val cleanDesc = description.replace(Regex("<.*?>"), " ").trim().take(500)

        // Generate deterministic ID
        val id = (feedUrl + audioUrl).hashCode().toString()

        return PodcastEntity(
            id = id,
            title = channelTitle, 
            episodeTitle = title,
            description = cleanDesc,
            imageUrl = if (imageUrl.isNotEmpty()) imageUrl else channelImage,
            audioUrl = audioUrl,
            feedUrl = feedUrl,
            pubDate = pubDate,
            season = season,
            episodeType = episodeType
        )
    }

    private fun readText(parser: XmlPullParser): String {
        var result = ""
        if (parser.next() == XmlPullParser.TEXT) {
            result = parser.text
            parser.nextTag()
        }
        return result
    }

    private fun skip(parser: XmlPullParser) {
        if (parser.eventType != XmlPullParser.START_TAG) return
        var depth = 1
        while (depth != 0) {
            when (parser.next()) {
                XmlPullParser.END_TAG -> depth--
                XmlPullParser.START_TAG -> depth++
            }
        }
    }
}
