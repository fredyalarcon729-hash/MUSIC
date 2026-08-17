package com.example.core.source

import android.util.Log
import com.example.core.model.LyricLine
import com.example.core.model.Lyrics
import com.example.core.model.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

class LyricsResolver {

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    suspend fun fetchLyrics(song: Song): Lyrics? = withContext(Dispatchers.IO) {
        try {
            val artist = URLEncoder.encode(song.artist, "UTF-8")
            val title = URLEncoder.encode(song.title, "UTF-8")
            val duration = song.durationMs / 1000

            val url = "https://lrclib.net/api/get?artist_name=$artist&track_name=$title&duration=$duration"
            
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "FusionMusic/1.0 (https://github.com/example/fusion-music)")
                .get()
                .build()

            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext null
                val bodyString = response.body?.string() ?: return@withContext null
                val json = JSONObject(bodyString)

                val synced = json.optString("syncedLyrics")
                val plain = json.optString("plainLyrics")

                return@withContext if (!synced.isNullOrBlank()) {
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
        } catch (e: Exception) {
            Log.e("LyricsResolver", "Error fetching lyrics", e)
            null
        }
    }
}
