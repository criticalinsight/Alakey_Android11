package com.example.alakey.domain

import com.example.alakey.data.ItunesSearchResult
import com.example.alakey.data.ItunesSearchResponse
import com.google.gson.Gson
import org.json.JSONObject

/**
 * Pure Logic: Network Transformation.
 * No side effects. String -> Data.
 */
object NetworkLogic {

    fun parseItunesResults(json: String): List<ItunesSearchResult> {
        if (json.isEmpty()) return emptyList()
        return try {
            Gson().fromJson(json, ItunesSearchResponse::class.java).results
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun extractProxyContent(json: String): String {
        return try {
            JSONObject(json).getString("contents")
        } catch (e: Exception) {
            ""
        }
    }
}
