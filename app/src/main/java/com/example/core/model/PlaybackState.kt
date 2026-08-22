package com.example.core.model

import androidx.compose.runtime.Immutable

@Immutable
enum class RepeatMode {
    OFF,
    ALL,
    ONE
}

@Immutable
enum class AudioQuality(val label: String, val bitrate: String) {
    STANDARD("Estándar", "128 kbps"),
    HIGH("Alta Calidad", "256 kbps"),
    ULTRA("Ultra Lossless", "FLAC / 320 kbps")
}

@Immutable
enum class EqualizerPreset(val displayName: String, val bassGain: Float, val midGain: Float, val trebleGain: Float) {
    FLAT("Plano", 1.0f, 1.0f, 1.0f),
    BASS_BOOST("Refuerzo de Graves", 1.6f, 1.0f, 0.9f),
    VOCAL("Vocal / Acústico", 0.9f, 1.4f, 1.2f),
    ELECTRONIC("Electrónica / Dance", 1.5f, 1.1f, 1.4f),
    ROCK("Rock & Pop", 1.3f, 1.2f, 1.3f),
    CUSTOM("Personalizado", 1.0f, 1.0f, 1.0f)
}

@Immutable
enum class VisualizerStyle(val displayName: String) {
    BARS("Barras Neón"),
    SYMMETRIC("Simétrico"),
    DOTS("Puntos Rebotantes"),
    PIXELS("Píxeles Retro"),
    RING("Anillo Circular"),
    WAVE("Onda Fluida"),
    MIRROR("Espectro Espejo")
}

@Immutable
data class PlayerUiState(
    val currentSong: Song? = null,
    val isPlaying: Boolean = false,
    val isBuffering: Boolean = false,
    val currentPositionMs: Long = 0L,
    val durationMs: Long = 0L,
    val bufferedPositionMs: Long = 0L,
    val shuffleMode: Boolean = false,
    val repeatMode: RepeatMode = RepeatMode.OFF,
    val queue: List<Song> = emptyList(),
    val currentQueueIndex: Int = -1,
    val sleepTimerMinutesLeft: Int? = null,
    val playbackSpeed: Float = 1.0f,
    val visualizerStyle: VisualizerStyle = VisualizerStyle.BARS,
    val equalizerPreset: EqualizerPreset = EqualizerPreset.FLAT,
    val audioQuality: AudioQuality = AudioQuality.HIGH,
    val currentLyrics: Lyrics? = null,
    val isSkipSilenceEnabled: Boolean = false,
    val crossfadeDuration: Int = 0, // In seconds, 0 means disabled
    val isMezclaProEnabled: Boolean = false,
    val audioSessionId: Int = 0,
    val pitchSemitones: Int = 0,
    val isVocalReductionEnabled: Boolean = false,
    val vocalReductionStrength: Float = 1.0f,
    val isKaraokeModeActive: Boolean = false,
    val isNormalizationEnabled: Boolean = false,
    val bandLevels: Map<Int, Int> = emptyMap(), // Band index to level in milliBels
    val bassBoostStrength: Int = 0, // 0 to 1000
    val virtualizerStrength: Int = 0, // 0 to 1000
    val volume: Float = 1.0f,
    val isShuttingDown: Boolean = false
) {
    val progress: Float
        get() = if (durationMs > 0) (currentPositionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f) else 0f

    val positionFormatted: String
        get() {
            val totalSec = (currentPositionMs / 1000).coerceAtLeast(0)
            val min = totalSec / 60
            val sec = totalSec % 60
            return "%d:%02d".format(min, sec)
        }

    val durationFormatted: String
        get() {
            val totalSec = (durationMs / 1000).coerceAtLeast(0)
            val min = totalSec / 60
            val sec = totalSec % 60
            return "%d:%02d".format(min, sec)
        }
}
