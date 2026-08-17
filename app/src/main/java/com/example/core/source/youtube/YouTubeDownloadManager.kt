package com.example.core.source.youtube

import android.content.Context
import android.net.Uri
import android.util.Log
import com.example.core.model.DownloadStatus
import com.example.core.model.MusicSource
import com.example.core.model.Song
import com.example.data.local.dao.MusicDao
import com.example.data.local.entity.DownloadedSongEntity
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

/**
 * Robust manager for downloading YouTube audio and artwork to local device storage.
 * Handles progress emission, cancellation, file persistence, and Room database sync.
 */
class YouTubeDownloadManager(
    private val context: Context,
    private val musicDao: MusicDao,
    private val resolver: YouTubeAudioResolver = YouTubeAudioResolver()
) {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    private val activeDownloadJobs = ConcurrentHashMap<String, Job>()

    private val _downloadStates = MutableStateFlow<Map<String, DownloadStatus>>(emptyMap())
    val downloadStates: StateFlow<Map<String, DownloadStatus>> = _downloadStates.asStateFlow()

    private val downloadsDir: File by lazy {
        File(context.filesDir, "fusion_downloads").apply {
            if (!exists()) mkdirs()
        }
    }

    /**
     * Download a YouTube song with real audio stream, metadata, and artwork.
     */
    fun startDownload(song: Song) {
        val songId = song.id

        // Check if already downloading
        if (activeDownloadJobs.containsKey(songId)) {
            Log.d("YouTubeDownloadManager", "Song $songId is already downloading")
            return
        }

        val job = scope.launch {
            try {
                // Check if already stored in database
                val alreadyDownloaded = musicDao.isSongDownloaded(songId).firstOrNull() ?: false
                if (alreadyDownloaded) {
                    _downloadStates.update { it + (songId to DownloadStatus.Completed) }
                    return@launch
                }

                _downloadStates.update { it + (songId to DownloadStatus.Preparing) }

                // 1. Resolve direct audio stream
                val streamUrl = if (song.mediaUri.startsWith("http") && !song.mediaUri.contains("youtube.com/watch")) {
                    song.mediaUri
                } else {
                    resolver.resolveAudioStreamUrl(songId)
                }

                if (streamUrl.isNullOrEmpty()) {
                    _downloadStates.update { it + (songId to DownloadStatus.Failed("No se pudo obtener el audio")) }
                    return@launch
                }

                // 2. Prepare audio destination file
                val sanitizedId = songId.replace(Regex("[^a-zA-Z0-9_-]"), "_")
                val audioFile = File(downloadsDir, "$sanitizedId.m4a")
                val artworkFile = File(downloadsDir, "$sanitizedId.jpg")

                // 3. Download Audio with progress
                val audioSuccess = downloadFileWithProgress(streamUrl, audioFile, songId)
                if (!audioSuccess) {
                    audioFile.delete()
                    _downloadStates.update { it + (songId to DownloadStatus.Failed("Error al guardar archivo")) }
                    return@launch
                }

                // 4. Download Artwork (if available)
                if (!song.artworkUri.isNullOrEmpty()) {
                    try {
                        downloadSimpleFile(song.artworkUri, artworkFile)
                    } catch (e: Exception) {
                        Log.w("YouTubeDownloadManager", "Failed to cache artwork: ${e.message}")
                    }
                }

                // 5. Save to Room database
                val downloadedEntity = DownloadedSongEntity(
                    id = songId,
                    title = song.title,
                    artist = song.artist,
                    album = song.album.ifEmpty { "Descargas de YouTube" },
                    durationMs = song.durationMs,
                    localMediaUri = Uri.fromFile(audioFile).toString(),
                    localArtworkUri = if (artworkFile.exists()) Uri.fromFile(artworkFile).toString() else song.artworkUri,
                    originalThumbnailUrl = song.artworkUri,
                    sizeBytes = audioFile.length(),
                    downloadedAt = System.currentTimeMillis(),
                    source = MusicSource.YOUTUBE.name
                )

                musicDao.insertDownloadedSong(downloadedEntity)
                _downloadStates.update { it + (songId to DownloadStatus.Completed) }
                Log.d("YouTubeDownloadManager", "Download complete for ${song.title} (${audioFile.length()} bytes)")

            } catch (e: CancellationException) {
                Log.d("YouTubeDownloadManager", "Download cancelled for $songId")
                _downloadStates.update { it - songId }
                cleanPartialFiles(songId)
            } catch (e: Exception) {
                Log.e("YouTubeDownloadManager", "Download failed for $songId", e)
                _downloadStates.update { it + (songId to DownloadStatus.Failed(e.localizedMessage ?: "Error desconocido")) }
                cleanPartialFiles(songId)
            } finally {
                activeDownloadJobs.remove(songId)
            }
        }

        activeDownloadJobs[songId] = job
    }

    /**
     * Cancel an active download.
     */
    fun cancelDownload(songId: String) {
        val job = activeDownloadJobs.remove(songId)
        job?.cancel()
        _downloadStates.update { it - songId }
        scope.launch {
            cleanPartialFiles(songId)
        }
    }

    /**
     * Delete a downloaded track from storage and Room database.
     */
    suspend fun deleteDownload(songId: String) = withContext(Dispatchers.IO) {
        try {
            cancelDownload(songId)
            musicDao.deleteDownloadedSong(songId)
            cleanPartialFiles(songId)
            _downloadStates.update { it - songId }
        } catch (e: Exception) {
            Log.e("YouTubeDownloadManager", "Failed to delete download $songId", e)
        }
    }

    private fun cleanPartialFiles(songId: String) {
        try {
            val sanitizedId = songId.replace(Regex("[^a-zA-Z0-9_-]"), "_")
            File(downloadsDir, "$sanitizedId.m4a").delete()
            File(downloadsDir, "$sanitizedId.jpg").delete()
        } catch (e: Exception) {
            // Ignore
        }
    }

    private fun downloadFileWithProgress(url: String, destinationFile: File, songId: String): Boolean {
        val request = Request.Builder().url(url).build()
        val response = httpClient.newCall(request).execute()

        if (!response.isSuccessful) {
            response.close()
            return false
        }

        val body = response.body ?: return false
        val contentLength = body.contentLength()
        val inputStream: InputStream = body.byteStream()
        val outputStream = FileOutputStream(destinationFile)

        val buffer = ByteArray(8192)
        var totalBytesRead = 0L
        var lastEmittedTime = 0L

        try {
            inputStream.use { input ->
                outputStream.use { output ->
                    var bytesRead: Int
                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        output.write(buffer, 0, bytesRead)
                        totalBytesRead += bytesRead

                        val now = System.currentTimeMillis()
                        if (now - lastEmittedTime > 200 || totalBytesRead == contentLength) {
                            lastEmittedTime = now
                            val progress = if (contentLength > 0) {
                                totalBytesRead.toFloat() / contentLength.toFloat()
                            } else 0.5f

                            _downloadStates.update { map ->
                                map + (songId to DownloadStatus.Downloading(
                                    progress = progress.coerceIn(0f, 1f),
                                    downloadedBytes = totalBytesRead,
                                    totalBytes = if (contentLength > 0) contentLength else totalBytesRead
                                ))
                            }
                        }
                    }
                    output.flush()
                }
            }
            return destinationFile.exists() && destinationFile.length() > 0
        } finally {
            response.close()
        }
    }

    private fun downloadSimpleFile(url: String, destinationFile: File) {
        val request = Request.Builder().url(url).build()
        httpClient.newCall(request).execute().use { response ->
            if (response.isSuccessful) {
                response.body?.byteStream()?.use { input ->
                    FileOutputStream(destinationFile).use { output ->
                        input.copyTo(output)
                    }
                }
            }
        }
    }
}
