package com.example.core.source.youtube

import android.content.Context
import android.util.Log
import com.example.core.model.MusicSource
import com.example.core.model.Song
import kotlinx.coroutines.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

/**
 * Minimal YouTube resolver (v18.0 - Legacy Support).
 * YouTube integration is currently disabled in favor of Deezer for stability.
 */
class YouTubeAudioResolver(
    private val context: Context,
    @Volatile
    private var apiKey: String? = null,
    @Volatile
    private var userToken: String? = null,
    @Volatile
    private var rapidApiKey: String? = null
) {
    // Minimal implementation to avoid build errors in other components
    fun updateApiKey(newKey: String?) { apiKey = newKey }
    fun updateRapidApiKey(newKey: String?) { rapidApiKey = newKey }
    fun updateUserToken(token: String?) { userToken = token }

    suspend fun search(query: String): List<Song> = emptyList()

    suspend fun resolveAudioStreamUrl(rawSongIdOrVideoId: String, songTitle: String? = null, songArtist: String? = null): String? {
        return null // Disabled
    }
}
