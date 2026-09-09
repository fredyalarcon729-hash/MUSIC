package com.example.player

import android.content.Context
import android.content.Intent
import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import android.media.audiofx.Virtualizer
import android.net.Uri
import android.util.Log
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.audio.AudioSink
import androidx.media3.exoplayer.audio.DefaultAudioSink
import androidx.core.net.toUri
import com.example.core.model.EqualizerPreset
import com.example.core.model.MusicSource
import com.example.core.model.VisualizerStyle
import com.example.core.model.PlayerUiState
import com.example.core.model.RepeatMode
import com.example.core.model.Song
import com.example.core.source.LyricsResolver
import com.example.core.source.youtube.YouTubeAudioResolver
import com.example.service.FusionMediaService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.time.Duration.Companion.minutes

/**
 * Singleton player manager coordinating a single ExoPlayer instance (v11.0).
 * Highly optimized for stability, minimum latency, and zero crashes.
 */
@UnstableApi
class FusionPlayerManager private constructor(private val applicationContext: Context) {

    private val TAG = "FusionPlayerManager"
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var progressTickerJob: Job? = null
    private var sleepTimerJob: Job? = null
    private var lyricsFetchJob: Job? = null
    
    private val configManager = com.example.core.source.ConfigManager(applicationContext)

    private var equalizer: Equalizer? = null
    private var bassBoost: BassBoost? = null
    private var virtualizer: Virtualizer? = null

    private val youTubeResolver = YouTubeAudioResolver(applicationContext)
    private val lyricsResolver = LyricsResolver()

    private val vocalProcessor = VocalRemovalProcessor()
    private val normalizationProcessor = VolumeNormalizerProcessor()

    val exoPlayer: ExoPlayer by lazy {
        val audioAttributes = AudioAttributes.Builder()
            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
            .setUsage(C.USAGE_MEDIA)
            .build()

        val renderersFactory = object : DefaultRenderersFactory(applicationContext) {
            override fun buildAudioSink(
                context: Context,
                enableFloatOutput: Boolean,
                enableAudioTrackPlaybackParams: Boolean
            ): AudioSink {
                return DefaultAudioSink.Builder(context)
                    .setEnableFloatOutput(enableFloatOutput)
                    .setEnableAudioTrackPlaybackParams(enableAudioTrackPlaybackParams)
                    .setAudioProcessors(arrayOf(vocalProcessor, normalizationProcessor))
                    .build()
            }
        }

        ExoPlayer.Builder(applicationContext, renderersFactory)
            .setAudioAttributes(audioAttributes, !configManager.isIgnoreAudioFocusEnabled())
            .setHandleAudioBecomingNoisy(true)
            .build().apply {
                addListener(playerListener)
                _uiState.update { it.copy(volume = volume) }
            }
    }

    private val _uiState = MutableStateFlow(
        PlayerUiState(
            visualizerStyle = configManager.getVisualizerStyle(),
            isAmbientAuraEnabled = configManager.isAmbientAuraEnabled(),
            ambientAuraStyle = configManager.getAmbientAuraStyle(),
            ambientAuraIntensity = configManager.getAmbientAuraIntensity(),
            ambientAuraWeight = configManager.getAmbientAuraWeight(),
            isIgnoreAudioFocusEnabled = configManager.isIgnoreAudioFocusEnabled()
        )
    )
    val uiState: StateFlow<PlayerUiState> = _uiState.asStateFlow()

    private val _playbackPositionMs = MutableStateFlow(0L)
    val playbackPositionMs: StateFlow<Long> = _playbackPositionMs.asStateFlow()

    private val _errorFlow = kotlinx.coroutines.flow.MutableSharedFlow<String>()
    val errorFlow = _errorFlow.asSharedFlow()

    private var onSongCompletedCallback: ((Song) -> Unit)? = null

    fun setOnSongCompletedCallback(callback: (Song) -> Unit) {
        this.onSongCompletedCallback = callback
    }

    /**
     * Restores the last known playback state from persistent storage.
     */
    fun restorePlaybackState() {
        scope.launch {
            val lastSong = configManager.getLastSong() ?: return@launch
            val lastQueue = configManager.getLastQueue().ifEmpty { listOf(lastSong) }
            val lastIndex = configManager.getLastQueueIndex().coerceIn(0, lastQueue.size - 1)
            val lastPosition = configManager.getLastPosition()

            _uiState.update {
                it.copy(
                    queue = lastQueue,
                    currentSong = lastSong,
                    currentQueueIndex = lastIndex,
                    durationMs = lastSong.durationMs,
                    currentPositionMs = lastPosition,
                    isBuffering = false,
                    isPlaying = false
                )
            }
            _playbackPositionMs.value = lastPosition

            val mediaItems = lastQueue.map { track ->
                val metadata = MediaMetadata.Builder()
                    .setTitle(track.title)
                    .setArtist(track.artist)
                    .setAlbumTitle(track.album)
                    .setArtworkUri(track.artworkUri?.toUri())
                    .build()

                MediaItem.Builder()
                    .setMediaId(track.id)
                    .setUri(track.mediaUri)
                    .setMediaMetadata(metadata)
                    .build()
            }

            exoPlayer.setMediaItems(mediaItems, lastIndex, lastPosition)
            exoPlayer.prepare()
            Log.d(TAG, "restorePlaybackState: Restored ${lastSong.title} at $lastPosition ms")
        }
    }

    private val playerListener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            _uiState.update { it.copy(isPlaying = isPlaying) }
            if (isPlaying) {
                startProgressTicker()
            } else {
                stopProgressTicker()
            }
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            val isBuffering = exoPlayer.playbackState == Player.STATE_BUFFERING
            val duration = exoPlayer.duration.coerceAtLeast(0)
            
            if (playbackState == Player.STATE_IDLE && exoPlayer.playerError != null) {
                handlePlaybackError()
            }

            _uiState.update {
                val newSessionId = if (exoPlayer.audioSessionId != C.AUDIO_SESSION_ID_UNSET) exoPlayer.audioSessionId else 0
                if (newSessionId != 0 && newSessionId != it.audioSessionId) {
                    updateAudioEffects(newSessionId)
                }
                it.copy(
                    isBuffering = isBuffering,
                    durationMs = if (duration > 0) duration else it.durationMs,
                    audioSessionId = newSessionId
                )
            }

            if (playbackState == Player.STATE_ENDED) {
                handleTrackEnded()
            }
        }

        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            val currentIndex = exoPlayer.currentMediaItemIndex
            val queue = _uiState.value.queue
            if (currentIndex in queue.indices) {
                val song = queue[currentIndex]
                _uiState.update {
                    it.copy(
                        currentSong = song,
                        currentQueueIndex = currentIndex,
                        currentPositionMs = 0L,
                        durationMs = song.durationMs,
                    )
                }
                fetchLyricsForSong(song)
                saveCurrentState()
            }
        }
    }

    private fun saveCurrentState() {
        val state = _uiState.value
        configManager.saveLastPlaybackState(
            state.currentSong,
            state.queue,
            state.currentQueueIndex,
            exoPlayer.currentPosition
        )
    }

    private fun fetchLyricsForSong(song: Song) {
        lyricsFetchJob?.cancel()
        _uiState.update { it.copy(currentLyrics = null) }
        lyricsFetchJob = scope.launch {
            val lyrics = lyricsResolver.fetchLyrics(song)
            _uiState.update { it.copy(currentLyrics = lyrics) }
        }
    }

    fun setSkipSilenceEnabled(enabled: Boolean) {
        exoPlayer.skipSilenceEnabled = enabled
        _uiState.update { it.copy(isSkipSilenceEnabled = enabled) }
    }

    fun setCrossfadeDuration(seconds: Int) {
        _uiState.update { it.copy(crossfadeDuration = 0) }
    }

    fun setMezclaProEnabled(enabled: Boolean) {
        _uiState.update { it.copy(isMezclaProEnabled = false) }
    }

    fun setPitchSemitones(semitones: Int) {
        val factor = Math.pow(2.0, semitones / 12.0).toFloat()
        exoPlayer.playbackParameters = PlaybackParameters(_uiState.value.playbackSpeed, factor)
        _uiState.update { it.copy(pitchSemitones = semitones) }
    }

    fun setPlaybackSpeed(speed: Float) {
        val factor = Math.pow(2.0, _uiState.value.pitchSemitones / 12.0).toFloat()
        exoPlayer.playbackParameters = PlaybackParameters(speed, factor)
        _uiState.update { it.copy(playbackSpeed = speed) }
    }

    fun setVocalReductionEnabled(enabled: Boolean) {
        vocalProcessor.setEnabled(enabled)
        _uiState.update { it.copy(isVocalReductionEnabled = enabled) }
    }

    fun setVocalReductionStrength(strength: Float) {
        vocalProcessor.setStrength(strength)
        _uiState.update { it.copy(vocalReductionStrength = strength) }
    }

    fun setKaraokeModeActive(active: Boolean) {
        _uiState.update { it.copy(isKaraokeModeActive = active) }
    }

    fun setNormalizationEnabled(enabled: Boolean) {
        normalizationProcessor.setEnabled(enabled)
        _uiState.update { it.copy(isNormalizationEnabled = enabled) }
    }

    fun setVisualizerStyle(style: VisualizerStyle) {
        _uiState.update { it.copy(visualizerStyle = style) }
    }

    fun setAmbientAuraEnabled(enabled: Boolean) {
        _uiState.update { it.copy(isAmbientAuraEnabled = enabled) }
    }

    fun setAmbientAuraStyle(style: com.example.core.model.AmbientAuraStyle) {
        _uiState.update { it.copy(ambientAuraStyle = style) }
    }

    fun setAmbientAuraIntensity(intensity: Float) {
        _uiState.update { it.copy(ambientAuraIntensity = intensity) }
    }

    fun setAmbientAuraWeight(weight: Float) {
        _uiState.update { it.copy(ambientAuraWeight = weight) }
    }

    fun setIgnoreAudioFocus(ignore: Boolean) {
        val audioAttributes = AudioAttributes.Builder()
            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
            .setUsage(C.USAGE_MEDIA)
            .build()
        // We update the player behavior without stopping playback
        exoPlayer.setAudioAttributes(audioAttributes, !ignore)
        _uiState.update { it.copy(isIgnoreAudioFocusEnabled = ignore) }
    }

    fun setVolume(volume: Float) {
        val clamped = volume.coerceIn(0f, 1f)
        exoPlayer.volume = clamped
        _uiState.update { it.copy(volume = clamped) }
    }

    fun playSong(song: Song, queue: List<Song> = listOf(song), startIndex: Int = queue.indexOf(song).coerceAtLeast(0)) {
        Log.d(TAG, "playSong: Cargando ${song.title}")
        startService()
        scope.launch {
            _uiState.update {
                it.copy(
                    queue = queue,
                    currentSong = song,
                    currentQueueIndex = startIndex,
                    durationMs = song.durationMs,
                    currentPositionMs = 0L,
                    isBuffering = true
                )
            }
            fetchLyricsForSong(song)

            // For Firebase tracks, we need to resolve the signed URL before setting it to ExoPlayer
            val app = applicationContext as com.example.FusionApplication
            val resolvedQueue = queue.map { track ->
                if (track.source == MusicSource.FIREBASE && !track.mediaUri.startsWith("http")) {
                    val resolved = app.firebaseProvider.resolveMediaUri(track.id)
                    if (resolved != null) track.copy(mediaUri = resolved) else track
                } else {
                    track
                }
            }

            val mediaItems = resolvedQueue.map { track ->
                val metadata = MediaMetadata.Builder()
                    .setTitle(track.title)
                    .setArtist(track.artist)
                    .setAlbumTitle(track.album)
                    .setArtworkUri(track.artworkUri?.toUri())
                    .build()

                MediaItem.Builder()
                    .setMediaId(track.id)
                    .setUri(track.mediaUri)
                    .setMediaMetadata(metadata)
                    .build()
            }

            exoPlayer.setMediaItems(mediaItems, startIndex, 0L)
            exoPlayer.prepare()
            exoPlayer.play()
            
            _uiState.update {
                it.copy(
                    queue = resolvedQueue,
                    currentSong = resolvedQueue[startIndex],
                    isBuffering = false,
                    isPlaying = true
                )
            }
        }
    }

    private fun startService() {
        Log.d(TAG, "startService: Despertando FusionMediaService")
        val intent = Intent(applicationContext, FusionMediaService::class.java).apply {
            action = "androidx.media3.session.MediaLibraryService"
        }
        try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                applicationContext.startForegroundService(intent)
            } else {
                applicationContext.startService(intent)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error al iniciar el servicio de medios", e)
        }
    }

    fun togglePlayPause() {
        if (exoPlayer.isPlaying) {
            exoPlayer.pause()
        } else {
            startService() // Ensure service is awake when resuming
            if (exoPlayer.playbackState == Player.STATE_IDLE && _uiState.value.currentSong != null) {
                val song = _uiState.value.currentSong!!
                playSong(song, _uiState.value.queue.ifEmpty { listOf(song) })
            } else {
                exoPlayer.play()
            }
        }
    }

    fun seekTo(positionMs: Long) {
        val clamped = positionMs.coerceIn(0L, (_uiState.value.durationMs).coerceAtLeast(1L))
        exoPlayer.seekTo(clamped)
        _uiState.update { it.copy(currentPositionMs = clamped) }
    }

    fun skipToNext() {
        if (exoPlayer.hasNextMediaItem()) {
            exoPlayer.seekToNextMediaItem()
        }
    }

    fun skipToPrevious() {
        if (exoPlayer.currentPosition > 3000) {
            exoPlayer.seekTo(0L)
        } else if (exoPlayer.hasPreviousMediaItem()) {
            exoPlayer.seekToPreviousMediaItem()
        }
    }

    fun toggleShuffle() {
        val nextShuffle = !_uiState.value.shuffleMode
        exoPlayer.shuffleModeEnabled = nextShuffle
        _uiState.update { it.copy(shuffleMode = nextShuffle) }
    }

    fun cycleRepeatMode() {
        val next = when (_uiState.value.repeatMode) {
            RepeatMode.OFF -> RepeatMode.ALL
            RepeatMode.ALL -> RepeatMode.ONE
            RepeatMode.ONE -> RepeatMode.OFF
        }
        val exoMode = when (next) {
            RepeatMode.OFF -> Player.REPEAT_MODE_OFF
            RepeatMode.ALL -> Player.REPEAT_MODE_ALL
            RepeatMode.ONE -> Player.REPEAT_MODE_ONE
        }
        exoPlayer.repeatMode = exoMode
        _uiState.update { it.copy(repeatMode = next) }
    }

    fun addToQueue(song: Song) {
        val currentQueue = _uiState.value.queue.toMutableList()
        currentQueue.add(song)
        val metadata = MediaMetadata.Builder().setTitle(song.title).setArtist(song.artist).build()
        val mediaItem = MediaItem.Builder().setMediaId(song.id).setUri(song.mediaUri).setMediaMetadata(metadata).build()
        exoPlayer.addMediaItem(mediaItem)
        _uiState.update { it.copy(queue = currentQueue) }
        saveCurrentState()
    }

    fun playNext(song: Song) {
        val currentQueue = _uiState.value.queue.toMutableList()
        val nextIndex = (exoPlayer.currentMediaItemIndex + 1).coerceIn(0, currentQueue.size)
        currentQueue.add(nextIndex, song)
        val metadata = MediaMetadata.Builder().setTitle(song.title).setArtist(song.artist).build()
        val mediaItem = MediaItem.Builder().setMediaId(song.id).setUri(song.mediaUri).setMediaMetadata(metadata).build()
        exoPlayer.addMediaItem(nextIndex, mediaItem)
        _uiState.update { it.copy(queue = currentQueue) }
        saveCurrentState()
    }

    fun removeFromQueue(index: Int) {
        val currentQueue = _uiState.value.queue.toMutableList()
        if (index in currentQueue.indices) {
            currentQueue.removeAt(index)
            exoPlayer.removeMediaItem(index)
            val newCurrentIndex = if (index < _uiState.value.currentQueueIndex) {
                _uiState.value.currentQueueIndex - 1
            } else {
                _uiState.value.currentQueueIndex
            }
            _uiState.update { it.copy(queue = currentQueue, currentQueueIndex = newCurrentIndex) }
            saveCurrentState()
        }
    }

    fun clearQueue() {
        exoPlayer.clearMediaItems()
        _uiState.update {
            it.copy(queue = emptyList(), currentSong = null, currentQueueIndex = -1, isPlaying = false)
        }
        saveCurrentState()
    }

    fun setSleepTimer(minutes: Int?) {
        sleepTimerJob?.cancel()
        if (minutes == null || minutes <= 0) {
            _uiState.update { it.copy(sleepTimerMinutesLeft = null) }
            return
        }
        _uiState.update { it.copy(sleepTimerMinutesLeft = minutes) }
        sleepTimerJob = scope.launch {
            var remaining = minutes
            while (isActive && remaining > 0) {
                delay(1.minutes)
                remaining -= 1
                _uiState.update { it.copy(sleepTimerMinutesLeft = remaining) }
            }
            if (isActive) {
                initiateManualShutdown()
            }
        }
    }

    fun initiateManualShutdown() {
        exoPlayer.pause()
        _uiState.update { it.copy(isShuttingDown = true, sleepTimerMinutesLeft = null) }
    }

    fun resetShutdownState() {
        _uiState.update { it.copy(isShuttingDown = false) }
    }

    fun setEqualizerPreset(preset: EqualizerPreset) {
        _uiState.update { it.copy(equalizerPreset = preset) }
        applyEqualizerPreset(preset)
    }

    fun setBandLevel(band: Int, level: Int) {
        try {
            equalizer?.setBandLevel(band.toShort(), level.toShort())
            val currentLevels = _uiState.value.bandLevels.toMutableMap()
            currentLevels[band] = level
            _uiState.update { it.copy(bandLevels = currentLevels, equalizerPreset = EqualizerPreset.CUSTOM) }
        } catch (e: Exception) {
            Log.e(TAG, "Error setting band level", e)
        }
    }

    fun setBassBoost(strength: Int) {
        try {
            bassBoost?.setStrength(strength.toShort())
            _uiState.update { it.copy(bassBoostStrength = strength) }
        } catch (e: Exception) {
            Log.e(TAG, "Error setting bass boost", e)
        }
    }

    fun setVirtualizer(strength: Int) {
        try {
            virtualizer?.setStrength(strength.toShort())
            _uiState.update { it.copy(virtualizerStrength = strength) }
        } catch (e: Exception) {
            Log.e(TAG, "Error setting virtualizer", e)
        }
    }

    private fun updateAudioEffects(sessionId: Int) {
        try {
            equalizer?.release()
            bassBoost?.release()
            virtualizer?.release()

            equalizer = Equalizer(0, sessionId).apply { enabled = true }
            bassBoost = BassBoost(0, sessionId).apply { enabled = true }
            virtualizer = Virtualizer(0, sessionId).apply { enabled = true }

            // Restore state
            val state = _uiState.value
            state.bandLevels.forEach { (band, level) ->
                equalizer?.setBandLevel(band.toShort(), level.toShort())
            }
            bassBoost?.setStrength(state.bassBoostStrength.toShort())
            virtualizer?.setStrength(state.virtualizerStrength.toShort())
            
            if (state.equalizerPreset != EqualizerPreset.CUSTOM) {
                applyEqualizerPreset(state.equalizerPreset)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing audio effects", e)
        }
    }

    private fun applyEqualizerPreset(preset: EqualizerPreset) {
        if (preset == EqualizerPreset.CUSTOM) return
        val eq = equalizer ?: return
        try {
            val numBands = eq.numberOfBands.toInt()
            val minLevel = eq.bandLevelRange[0]
            val maxLevel = eq.bandLevelRange[1]
            val range = (maxLevel - minLevel).toFloat()

            for (i in 0 until numBands) {
                // Map bassGain, midGain, trebleGain to bands
                val gain = when {
                    i < numBands / 3 -> preset.bassGain
                    i < 2 * numBands / 3 -> preset.midGain
                    else -> preset.trebleGain
                }
                // Normalize gain 1.0 -> 0dB, >1.0 -> boost, <1.0 -> cut
                val level = (minLevel + (range * (gain / 2.0f))).toInt()
                    .coerceIn(minLevel.toInt(), maxLevel.toInt())
                eq.setBandLevel(i.toShort(), level.toShort())
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error applying preset", e)
        }
    }

    private fun handlePlaybackError() {
        val currentSong = _uiState.value.currentSong ?: return
        if (currentSong.source == MusicSource.YOUTUBE) {
            scope.launch {
                val newUrl = youTubeResolver.resolveAudioStreamUrl(currentSong.id)
                if (!newUrl.isNullOrEmpty()) {
                    val mediaItem = MediaItem.Builder()
                        .setMediaId(currentSong.id)
                        .setUri(newUrl)
                        .setMediaMetadata(exoPlayer.currentMediaItem?.mediaMetadata ?: MediaMetadata.EMPTY)
                        .build()
                    exoPlayer.replaceMediaItem(exoPlayer.currentMediaItemIndex, mediaItem)
                    exoPlayer.prepare()
                    exoPlayer.play()
                } else {
                    skipToNext()
                }
            }
        }
    }

    private fun handleTrackEnded() {
        _uiState.value.currentSong?.let { onSongCompletedCallback?.invoke(it) }
        if (exoPlayer.hasNextMediaItem()) {
            exoPlayer.seekToNextMediaItem()
        }
    }

    private fun startProgressTicker() {
        progressTickerJob?.cancel()
        progressTickerJob = scope.launch {
            var saveCounter = 0
            while (isActive) {
                if (exoPlayer.isPlaying) {
                    val pos = exoPlayer.currentPosition.coerceAtLeast(0L)
                    val dur = exoPlayer.duration.coerceAtLeast(0L)
                    val buf = exoPlayer.bufferedPosition.coerceAtLeast(0L)
                    
                    _playbackPositionMs.value = pos

                    // Only update the heavy UI state if duration or buffer significantly changed
                    val currentState = _uiState.value
                    if (currentState.durationMs != dur || (buf - currentState.bufferedPositionMs) > 1000) {
                        _uiState.update {
                            it.copy(
                                durationMs = if (dur > 0) dur else it.durationMs,
                                bufferedPositionMs = buf
                            )
                        }
                    }

                    // Save state every ~5 seconds (50 * 100ms)
                    saveCounter++
                    if (saveCounter >= 50) {
                        saveCurrentState()
                        saveCounter = 0
                    }
                }
                delay(100L)
            }
        }
    }

    private fun stopProgressTicker() {
        progressTickerJob?.cancel()
    }

    companion object {
        @Volatile
        private var INSTANCE: FusionPlayerManager? = null
        fun getInstance(context: Context): FusionPlayerManager {
            return INSTANCE ?: synchronized(this) {
                val instance = FusionPlayerManager(context.applicationContext)
                INSTANCE = instance
                instance
            }
        }
    }
}
