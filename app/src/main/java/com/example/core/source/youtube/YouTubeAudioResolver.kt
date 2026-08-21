package com.example.core.source.youtube

import android.util.Log
import com.example.core.model.MusicSource
import com.example.core.model.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

/**
 * Advanced YouTube audio search and media stream resolver (v6 - Hybrid Auth Strategy).
 * Simulates a Smart TV client or Official App and can use a Google Auth Token.
 */
class YouTubeAudioResolver(
    @Volatile
    private var apiKey: String? = "AIzaSyA3qUwtLHuoU64VzftaEoQMQVqGqttbgqE",
    @Volatile
    private var userToken: String? = null
) {

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    fun updateApiKey(newKey: String?) {
        if (!newKey.isNullOrEmpty()) apiKey = newKey
    }

    fun updateUserToken(token: String?) {
        userToken = token
    }

    suspend fun search(query: String): List<Song> = withContext(Dispatchers.IO) {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return@withContext emptyList()

        Log.d("YouTubeAudioResolver", "Searching for: $trimmed")

        apiKey?.let { key ->
            try {
                val officialResults = searchViaOfficialApi(trimmed, key)
                if (officialResults.isNotEmpty()) return@withContext officialResults
            } catch (e: Exception) {
                Log.w("YouTubeAudioResolver", "Official API search failed")
            }
        }
        return@withContext emptyList()
    }

    private fun searchViaOfficialApi(query: String, key: String): List<Song> {
        val encoded = URLEncoder.encode(query, "UTF-8")
        val url = "https://www.googleapis.com/youtube/v3/search?part=snippet&maxResults=25&q=$encoded&type=video&videoCategoryId=10&key=$key"
        val requestBuilder = Request.Builder().url(url).get()
        
        // Use token if available for search too
        userToken?.let { requestBuilder.header("Authorization", "Bearer $it") }

        httpClient.newCall(requestBuilder.build()).execute().use { response ->
            if (!response.isSuccessful) return emptyList()
            val root = JSONObject(response.body?.string() ?: return emptyList())
            val items = root.optJSONArray("items") ?: return emptyList()

            val songs = mutableListOf<Song>()
            for (i in 0 until items.length()) {
                val item = items.optJSONObject(i) ?: continue
                val idObj = item.optJSONObject("id") ?: continue
                val videoId = idObj.optString("videoId")
                if (videoId.isEmpty()) continue
                
                val snippet = item.optJSONObject("snippet") ?: continue
                
                songs.add(Song(
                    id = "yt_$videoId",
                    title = cleanTitle(snippet.optString("title")),
                    artist = snippet.optString("channelTitle"),
                    album = "YouTube Music",
                    durationMs = 210000L,
                    mediaUri = "https://www.youtube.com/watch?v=$videoId",
                    artworkUri = snippet.optJSONObject("thumbnails")?.optJSONObject("high")?.optString("url"),
                    source = MusicSource.YOUTUBE
                ))
            }
            return songs
        }
    }

    suspend fun resolveAudioStreamUrl(rawSongIdOrVideoId: String): String? = withContext(Dispatchers.IO) {
        val videoId = rawSongIdOrVideoId.removePrefix("yt_")
        Log.d("YouTubeAudioResolver", "Resolving stream for: $videoId (Auth: ${userToken != null})")

        // Strategy 1: Official Android Music Client Simulation
        try {
            val url = resolveViaMusicApp(videoId)
            if (!url.isNullOrEmpty()) return@withContext url
        } catch (e: Exception) { Log.w("YouTubeAudioResolver", "L1 (Music) Failed") }

        // Strategy 2: Cobalt High-Speed Extract
        try {
            val url = resolveViaCobalt(videoId)
            if (!url.isNullOrEmpty()) return@withContext url
        } catch (e: Exception) { Log.w("YouTubeAudioResolver", "L2 (Cobalt) Failed") }

        return@withContext null
    }

    private fun resolveViaMusicApp(videoId: String): String? {
        val payload = JSONObject().apply {
            put("context", JSONObject().apply {
                put("client", JSONObject().apply {
                    put("clientName", "ANDROID_MUSIC")
                    put("clientVersion", "6.45.54")
                    put("hl", "es")
                    put("gl", "US")
                })
            })
            put("videoId", videoId)
            put("playbackContext", JSONObject().apply {
                put("contentPlaybackContext", JSONObject().apply {
                    put("signatureTimestamp", 20153)
                })
            })
        }
        
        val url = "https://music.youtube.com/youtubei/v1/player?key=${apiKey ?: ""}&prettyPrint=false"
        
        val requestBuilder = Request.Builder()
            .url(url)
            .header("Content-Type", "application/json")
            .header("User-Agent", "com.google.android.apps.youtube.music/6.45.54 (Linux; U; Android 14; es_US) gzip")
            .header("X-YouTube-Client-Name", "16")
            .header("X-YouTube-Client-Version", "6.45.54")

        // Inject Auth Token if available
        userToken?.let { requestBuilder.header("Authorization", "Bearer $it") }

        val request = requestBuilder.post(payload.toString().toRequestBody(jsonMediaType)).build()

        try {
            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    Log.w("YouTubeAudioResolver", "Music App API HTTP Error: ${response.code}")
                    return null
                }
                val bodyString = response.body?.string() ?: return null
                val root = JSONObject(bodyString)
                val streamingData = root.optJSONObject("streamingData") ?: return null
                val formats = streamingData.optJSONArray("adaptiveFormats") ?: return null

                var bestUrl: String? = null
                var maxBitrate = 0
                for (i in 0 until formats.length()) {
                    val format = formats.optJSONObject(i) ?: continue
                    val mime = format.optString("mimeType", "")
                    if (mime.contains("audio/")) {
                        val streamUrl = format.optString("url")
                        val bitrate = format.optInt("bitrate", 0)
                        if (!streamUrl.isNullOrEmpty() && bitrate > maxBitrate) {
                            maxBitrate = bitrate
                            bestUrl = streamUrl
                        }
                    }
                }
                return bestUrl
            }
        } catch (e: Exception) { return null }
    }

    private fun resolveViaCobalt(videoId: String): String? {
        val payload = JSONObject().apply {
            put("url", "https://www.youtube.com/watch?v=$videoId")
            put("downloadMode", "audio")
            put("audioFormat", "mp3")
        }
        val request = Request.Builder()
            .url("https://api.cobalt.tools/api/json")
            .header("Accept", "application/json")
            .header("Content-Type", "application/json")
            .post(payload.toString().toRequestBody(jsonMediaType))
            .build()

        try {
            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return null
                val root = JSONObject(response.body?.string() ?: "")
                return root.optString("url")
            }
        } catch (e: Exception) { return null }
    }

    private fun cleanTitle(raw: String): String {
        return raw.replace(Regex("\\(.*?\\)|\\[.*?\\]|(?i)official video|audio|lyrics|letra|hd|4k|feat\\.?|ft\\.?|\\d{4}"), "").replace(Regex("\\s+"), " ").trim()
    }
}
