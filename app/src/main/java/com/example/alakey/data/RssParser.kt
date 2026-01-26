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
        
        // Fast-forward to root
        while (eventType != XmlPullParser.START_TAG && eventType != XmlPullParser.END_DOCUMENT) {
            eventType = parser.next()
        }
        
        if (eventType == XmlPullParser.END_DOCUMENT) return emptyList()

        while (parser.next() != XmlPullParser.END_DOCUMENT) {
            if (parser.eventType == XmlPullParser.START_TAG && (parser.name == "item" || parser.name == "entry")) {
                readEntry(parser, feedUrl)?.let { entries.add(it) }
            }
        }
        return entries
    }

    private fun readEntry(parser: XmlPullParser, feedUrl: String): PodcastEntity? {
        var title = ""
        var description = ""
        var link = ""
        var audioUrl = ""
        var imageUrl = ""
        var pubDate = ""

        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.eventType != XmlPullParser.START_TAG) continue
            when (parser.name) {
                "title" -> title = readText(parser)
                "description", "summary", "content" -> description = readText(parser)
                "link" -> link = readText(parser)
                "enclosure" -> audioUrl = parser.getAttributeValue(null, "url") ?: ""
                "itunes:image" -> imageUrl = parser.getAttributeValue(null, "href") ?: ""
                "pubDate", "published" -> pubDate = readText(parser)
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
            title = "Podcast", // Feed title would be passed down in a recursive parser, keeping it simple here
            episodeTitle = title,
            description = cleanDesc,
            imageUrl = imageUrl,
            audioUrl = audioUrl,
            feedUrl = feedUrl,
            pubDate = pubDate
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
