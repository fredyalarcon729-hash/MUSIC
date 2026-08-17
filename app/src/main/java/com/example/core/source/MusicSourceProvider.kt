package com.example.core.source

import com.example.core.model.MusicSource
import com.example.core.model.Song
import kotlinx.coroutines.flow.StateFlow

/**
 * Playback capability classification for official SDKs / APIs.
 */
enum class PlaybackIntegrationType(val description: String) {
    NATIVE_EXOPLAYER("Reproducción nativa directa"),
    OFFICIAL_APP_REMOTE("Control remoto vía App Oficial (SDK)"),
    OFFICIAL_EMBED_PLAYER("Reproductor web embebido oficial"),
    REST_API_METADATA("Búsqueda y sincronización de bibliotecas vía REST API")
}

/**
 * Modular interface for any music source provider (Local, Spotify, YouTube Music, Tidal, Deezer).
 * Designed for future plug-and-play expansion using official developer APIs & SDKs.
 */
interface MusicSourceProvider {
    val source: MusicSource
    val displayName: String
    val providerDescription: String
    val integrationType: PlaybackIntegrationType
    val isConfigured: StateFlow<Boolean>
    val isConnected: StateFlow<Boolean>

    /**
     * Connect or authenticate with the official provider API/OAuth2 SDK.
     */
    suspend fun authenticate(credentials: Map<String, String>): Result<Unit>

    /**
     * Disconnect provider account.
     */
    suspend fun disconnect()

    /**
     * Search songs in this provider catalog.
     */
    suspend fun searchSongs(query: String): List<Song>

    /**
     * Resolve playable media URL or MediaItem URI for this track if allowed by official TOS.
     */
    suspend fun resolveMediaUri(songId: String): String?
}
