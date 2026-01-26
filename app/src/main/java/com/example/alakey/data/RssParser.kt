package com.example.alakey.data

import android.util.Log
import android.util.Xml
import org.xmlpull.v1.XmlPullParser
import java.io.StringReader

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
