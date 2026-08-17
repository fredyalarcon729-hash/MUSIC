package com.example.core.model

enum class RepeatMode {
    OFF,
    ALL,
    ONE
}

enum class AudioQuality(val label: String, val bitrate: String) {
    STANDARD("Estándar", "128 kbps"),
    HIGH("Alta Calidad", "256 kbps"),
    ULTRA("Ultra Lossless", "FLAC / 320 kbps")
}

enum class EqualizerPreset(val displayName: String, val bassGain: Float, val midGain: Float, val trebleGain: Float) {
    FLAT("Plano", 1.0f, 1.0f, 1.0f),
    BASS_BOOST("Refuerzo de Graves", 1.6f, 1.0f, 0.9f),
    VOCAL("Vocal / Acústico", 0.9f, 1.4f, 1.2f),
    ELECTRONIC("Electrónica / Dance", 1.5f, 1.1f, 1.4f),
    ROCK("Rock & Pop", 1.3f, 1.2f, 1.3f)
}

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
    val equalizerPreset: EqualizerPreset = EqualizerPreset.FLAT,
    val audioQuality: AudioQuality = AudioQuality.HIGH
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
