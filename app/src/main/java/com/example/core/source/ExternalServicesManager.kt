package com.example.core.source

import android.content.Context
import com.example.core.model.MusicSource
import com.example.core.model.Song
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class ExternalServiceDescriptor(
    val source: MusicSource,
    val title: String,
    val description: String,
    val apiDocUrl: String,
    val sdkType: String,
    val isConnected: Boolean,
    val isEnabled: Boolean,
    val badgeColorHex: Long,
    val officialRequirements: String
)

/**
 * Registry & Manager for external music service modules (Spotify, YouTube Music, Tidal, Deezer).
 * Provides a clean architecture ready for official SDK/OAuth2 integrations.
 */
class ExternalServicesManager(
    private val context: Context,
    val localProvider: LocalMusicSourceProvider
) {
    private val providers = mutableMapOf<MusicSource, MusicSourceProvider>()

    private val _servicesState = MutableStateFlow<List<ExternalServiceDescriptor>>(emptyList())
    val servicesState: StateFlow<List<ExternalServiceDescriptor>> = _servicesState.asStateFlow()

    init {
        providers[MusicSource.LOCAL] = localProvider
        refreshServicesDescriptors()
    }

    fun registerProvider(provider: MusicSourceProvider) {
        providers[provider.source] = provider
        refreshServicesDescriptors()
    }

    fun getProvider(source: MusicSource): MusicSourceProvider? {
        return providers[source]
    }

    fun refreshServicesDescriptors() {
        _servicesState.value = listOf(
            ExternalServiceDescriptor(
                source = MusicSource.LOCAL,
                title = "Almacenamiento Local (MediaStore)",
                description = "Explora y reproduce canciones, carátulas y metadatos del dispositivo.",
                apiDocUrl = "https://developer.android.com/training/data-storage/shared/media",
                sdkType = "Android MediaStore + Media3 ExoPlayer",
                isConnected = true,
                isEnabled = true,
                badgeColorHex = 0xFF00E5FF,
                officialRequirements = "Permisos READ_MEDIA_AUDIO / Almacenamiento local"
            ),
            ExternalServiceDescriptor(
                source = MusicSource.SPOTIFY,
                title = "Spotify",
                description = "Conexión oficial mediante Spotify App Remote SDK y Web API.",
                apiDocUrl = "https://developer.spotify.com/documentation/android",
                sdkType = "Spotify App Remote SDK + OAuth2 Web API",
                isConnected = false,
                isEnabled = false,
                badgeColorHex = 0xFF1DB954,
                officialRequirements = "Requiere Spotify Client ID y autenticación OAuth2 oficial"
            ),
            ExternalServiceDescriptor(
                source = MusicSource.YOUTUBE,
                title = "YouTube Music",
                description = "Búsqueda integrada, reproducción nativa y descargas offline de audio de alta fidelidad.",
                apiDocUrl = "https://developers.google.com/youtube/v3",
                sdkType = "YouTube Innertube + Media3 ExoPlayer Audio Engine",
                isConnected = true,
                isEnabled = true,
                badgeColorHex = 0xFFFF0000,
                officialRequirements = "Integración completa nativa con streaming y descargas habilitadas"
            ),
            ExternalServiceDescriptor(
                source = MusicSource.TIDAL,
                title = "TIDAL Hi-Fi",
                description = "Módulo de transmisión de alta fidelidad vía TIDAL Developer API.",
                apiDocUrl = "https://developer.tidal.com/",
                sdkType = "TIDAL Open API OAuth2",
                isConnected = false,
                isEnabled = false,
                badgeColorHex = 0xFF00FFFF,
                officialRequirements = "Módulo preparado para Client Credentials y streaming Lossless"
            ),
            ExternalServiceDescriptor(
                source = MusicSource.DEEZER,
                title = "Deezer Music",
                description = "Módulo de catálogo y listas personalizadas mediante Deezer Android SDK.",
                apiDocUrl = "https://developers.deezer.com/sdk/android",
                sdkType = "Deezer REST API + Android SDK",
                isConnected = false,
                isEnabled = false,
                badgeColorHex = 0xFFFF007F,
                officialRequirements = "Módulo modular para Deezer App ID y token de usuario"
            )
        )
    }

    suspend fun toggleService(source: MusicSource, enable: Boolean) {
        // Toggle simulation for external providers until official credentials are provided
        _servicesState.value = _servicesState.value.map { descriptor ->
            if (descriptor.source == source) {
                descriptor.copy(isEnabled = enable, isConnected = enable)
            } else {
                descriptor
            }
        }
    }
}
