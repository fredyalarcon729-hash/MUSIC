package com.example.core.model

/**
 * Supported music sources in Fusion Music architecture.
 */
enum class MusicSource(val displayName: String, val badgeColorHex: Long) {
    LOCAL("Local", 0xFF00E5FF),
    YOUTUBE("YouTube Music", 0xFFFF0000),
    SPOTIFY("Spotify", 0xFF1DB954),
    TIDAL("TIDAL", 0xFF00FFFF),
    DEEZER("Deezer", 0xFFFF007F)
}

/**
 * Core representation of a musical track across local files and external services.
 */
data class Song(
    val id: String,
    val title: String,
    val artist: String,
    val album: String,
    val durationMs: Long,
    val mediaUri: String,
    val artworkUri: String? = null,
    val albumId: Long = 0L,
    val artistId: Long = 0L,
    val trackNumber: Int = 1,
    val year: Int = 2024,
    val sizeBytes: Long = 0L,
    val dateAdded: Long = System.currentTimeMillis(),
    val source: MusicSource = MusicSource.LOCAL,
    val isFavorite: Boolean = false,
    val isDownloaded: Boolean = false,
    val playCount: Int = 0,
    val path: String = ""
) {
    val durationFormatted: String
        get() {
            val totalSeconds = (durationMs / 1000).coerceAtLeast(0)
            val minutes = totalSeconds / 60
            val remainingSeconds = totalSeconds % 60
            return "%d:%02d".format(minutes, remainingSeconds)
        }

    val sizeFormatted: String
        get() {
            if (sizeBytes <= 0L) return ""
            val mb = sizeBytes.toDouble() / (1024 * 1024)
            return if (mb >= 1.0) {
                "%.1f MB".format(mb)
            } else {
                val kb = sizeBytes.toDouble() / 1024
                "%.0f KB".format(kb)
            }
        }
}
