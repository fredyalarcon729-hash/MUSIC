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
 * Registry & Manager for external music service modules.
 * Simplified (v18.0) to prioritize stable Deezer and Local sources.
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
                source = MusicSource.DEEZER,
                title = "Deezer Music (Recomendado)",
                description = "Streaming estable de alta fidelidad. Ideal para música latina y lanzamientos globales.",
                apiDocUrl = "https://developers.deezer.com/sdk/android",
                sdkType = "Deezer API Direct Stream",
                isConnected = true,
                isEnabled = true,
                badgeColorHex = 0xFFFF007F,
                officialRequirements = "Módulo oficial activado con streaming de alta velocidad"
            ),
            ExternalServiceDescriptor(
                source = MusicSource.YOUTUBE,
                title = "YouTube Music (Mantenimiento)",
                description = "Búsqueda integrada y reproducción. Actualmente en mantenimiento para mejorar estabilidad.",
                apiDocUrl = "https://developers.google.com/youtube/v3",
                sdkType = "YouTube Data API v3",
                isConnected = false,
                isEnabled = false,
                badgeColorHex = 0xFF666666,
                officialRequirements = "Servicio temporalmente deshabilitado"
            )
        )
    }

    suspend fun toggleService(source: MusicSource, enable: Boolean) {
        // Toggle simulation for external providers
        _servicesState.value = _servicesState.value.map { descriptor ->
            if (descriptor.source == source) {
                descriptor.copy(isEnabled = enable, isConnected = enable)
            } else {
                descriptor
            }
        }
    }
}
