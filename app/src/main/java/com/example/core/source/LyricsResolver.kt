package com.example.core.source

import android.util.Log
import com.example.core.model.Lyrics
import com.example.core.model.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

class LyricsResolver {

    private val TAG = "LyricsResolver"
    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    suspend fun fetchLyrics(song: Song): Lyrics? = withContext(Dispatchers.IO) {
        val cleanArtist = sanitize(song.artist)
        val cleanTitle = sanitize(song.title)
        val duration = song.durationMs / 1000

        // 1. Try exact match first
        val exactLyrics = fetchExact(cleanArtist, cleanTitle, duration)
        if (exactLyrics != null) return@withContext exactLyrics

        // 2. Fallback: Search if exact match failed or title was "dirty"
        Log.d(TAG, "Exact match failed for $cleanTitle. Trying fuzzy search...")
        return@withContext searchFallback(cleanArtist, cleanTitle, duration)
    }

    private fun fetchExact(artist: String, title: String, duration: Long): Lyrics? {
        return try {
            val encArtist = URLEncoder.encode(artist, "UTF-8")
            val encTitle = URLEncoder.encode(title, "UTF-8")
            val url = "https://lrclib.net/api/get?artist_name=$encArtist&track_name=$encTitle&duration=$duration"
            
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "FusionMusic/1.0")
                .get()
                .build()

            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return null
                val bodyString = response.body?.string() ?: return null
                parseJsonResponse(JSONObject(bodyString))
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun searchFallback(artist: String, title: String, duration: Long): Lyrics? {
        return try {
            // Search query combining artist and title for better accuracy
            val query = URLEncoder.encode("$artist $title", "UTF-8")
            val url = "https://lrclib.net/api/search?q=$query"
            
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "FusionMusic/1.0")
                .get()
                .build()

            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return null
                val bodyString = response.body?.string() ?: return null
                val results = JSONArray(bodyString)
                
                if (results.length() == 0) return null

                // Pick the best match based on duration proximity (within 5 seconds)
                var bestMatch: JSONObject? = null
                var minDiff = Long.MAX_VALUE

                for (i in 0 until results.length()) {
                    val item = results.getJSONObject(i)
                    val itemDuration = item.optLong("duration", 0L)
                    val diff = Math.abs(itemDuration - duration)
                    if (diff < minDiff && diff < 10) { // Max 10s difference
                        minDiff = diff
                        bestMatch = item
                    }
                }

                // If no close duration match, pick the first result as last resort
                val finalSelection = bestMatch ?: results.getJSONObject(0)
                parseJsonResponse(finalSelection)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Search fallback failed", e)
            null
        }
    }

    private fun parseJsonResponse(json: JSONObject): Lyrics? {
        val synced = json.optString("syncedLyrics")
        val plain = json.optString("plainLyrics")

        return if (!synced.isNullOrBlank()) {
            Lyrics(
                lines = Lyrics.parseLrc(synced),
                isSynced = true,
                plainText = plain
            )
        } else if (!plain.isNullOrBlank()) {
            Lyrics(
                plainText = plain,
                isSynced = false
            )
        } else null
    }

    private fun sanitize(input: String): String {
        return input
            .replace(Regex("\\(.*?\\)"), "") // Remove anything in parenthesis
            .replace(Regex("\\[.*?\\]"), "") // Remove anything in brackets
            .replace(Regex("(?i)official video"), "")
            .replace(Regex("(?i)official audio"), "")
            .replace(Regex("(?i)full audio"), "")
            .replace(Regex("(?i)video oficial"), "")
            .replace(Regex("(?i)audio oficial"), "")
            .replace(Regex("(?i)lyrics"), "")
            .replace(Regex("(?i)letra"), "")
            .replace(Regex("(?i)hd"), "")
            .replace(Regex("(?i)4k"), "")
            .replace(Regex("(?i)feat\\.?"), "")
            .replace(Regex("(?i)ft\\.?"), "")
            .replace(Regex("\\d{4}"), "") // Remove years
            .replace(Regex("\\s+"), " ") // Normalize spaces
            .trim()
    }
}
