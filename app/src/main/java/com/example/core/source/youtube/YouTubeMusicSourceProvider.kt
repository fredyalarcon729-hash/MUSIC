package com.example.core.source.youtube

import com.example.core.model.MusicSource
import com.example.core.model.Song
import com.example.core.source.MusicSourceProvider
import com.example.core.source.PlaybackIntegrationType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Concrete provider implementation for YouTube Music within Fusion Music.
 * Supports online catalog search, direct audio stream resolution, and offline downloads.
 */
class YouTubeMusicSourceProvider(
    val resolver: YouTubeAudioResolver = YouTubeAudioResolver()
) : MusicSourceProvider {

    override val source: MusicSource = MusicSource.YOUTUBE
    override val displayName: String = "YouTube Music"
    override val providerDescription: String = "Búsqueda integrada, reproducción nativa y descargas offline de audio."
    override val integrationType: PlaybackIntegrationType = PlaybackIntegrationType.NATIVE_EXOPLAYER

    private val _isConfigured = MutableStateFlow(true)
    override val isConfigured: StateFlow<Boolean> = _isConfigured.asStateFlow()

    private val _isConnected = MutableStateFlow(true)
    override val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    override suspend fun authenticate(credentials: Map<String, String>): Result<Unit> {
        val apiKey = credentials["apiKey"]
        resolver.updateApiKey(apiKey)
        _isConnected.value = !apiKey.isNullOrEmpty()
        return Result.success(Unit)
    }

    override suspend fun disconnect() {
        _isConnected.value = false
    }

    override suspend fun searchSongs(query: String): List<Song> {
        return resolver.search(query)
    }

    override suspend fun resolveMediaUri(songId: String): String? {
        return resolver.resolveAudioStreamUrl(songId)
    }
}
