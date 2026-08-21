package com.example.player

import android.content.Context
import android.content.Intent
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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
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
    
    private val youTubeResolver = YouTubeAudioResolver()
    private val lyricsResolver = LyricsResolver()

    private val vocalProcessor = VocalRemovalProcessor()

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
                    .setAudioProcessors(arrayOf(vocalProcessor))
                    .build()
            }
        }

        ExoPlayer.Builder(applicationContext, renderersFactory)
            .setAudioAttributes(audioAttributes, true)
            .setHandleAudioBecomingNoisy(true)
            .build().apply {
                addListener(playerListener)
            }
    }

    private val _uiState = MutableStateFlow(PlayerUiState())
    val uiState: StateFlow<PlayerUiState> = _uiState.asStateFlow()

    private var onSongCompletedCallback: ((Song) -> Unit)? = null

    fun setOnSongCompletedCallback(callback: (Song) -> Unit) {
        this.onSongCompletedCallback = callback
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
                it.copy(
                    isBuffering = isBuffering,
                    durationMs = if (duration > 0) duration else it.durationMs,
                    audioSessionId = if (exoPlayer.audioSessionId != C.AUDIO_SESSION_ID_UNSET) exoPlayer.audioSessionId else 0
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
            }
        }
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
        exoPlayer.playbackParameters = PlaybackParameters(1.0f, factor)
        _uiState.update { it.copy(pitchSemitones = semitones) }
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

    fun playSong(song: Song, queue: List<Song> = listOf(song), startIndex: Int = queue.indexOf(song).coerceAtLeast(0)) {
        Log.d(TAG, "playSong: Loading ${song.title}")
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

            val resolvedUri = if ((song.source == MusicSource.YOUTUBE) && !song.isDownloaded && song.mediaUri.contains("youtube.com/watch")) {
                youTubeResolver.resolveAudioStreamUrl(song.id) ?: song.mediaUri
            } else {
                song.mediaUri
            }

            val mediaItems = queue.map { track ->
                val finalUri = if (track.id == song.id) resolvedUri else track.mediaUri
                val metadata = MediaMetadata.Builder()
                    .setTitle(track.title)
                    .setArtist(track.artist)
                    .setAlbumTitle(track.album)
                    .setArtworkUri(track.artworkUri?.toUri())
                    .build()

                MediaItem.Builder()
                    .setMediaId(track.id)
                    .setUri(finalUri)
                    .setMediaMetadata(metadata)
                    .build()
            }

            exoPlayer.setMediaItems(mediaItems, startIndex, 0L)
            exoPlayer.prepare()
            exoPlayer.play()
            
            _uiState.update {
                it.copy(
                    currentSong = song.copy(mediaUri = resolvedUri),
                    isBuffering = false
                )
            }
        }
    }

    private fun startService() {
        Log.d(TAG, "startService: Sending intent to FusionMediaService")
        val intent = Intent(applicationContext, FusionMediaService::class.java)
        applicationContext.startService(intent)
    }

    fun togglePlayPause() {
        if (exoPlayer.isPlaying) {
            exoPlayer.pause()
        } else {
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
    }

    fun playNext(song: Song) {
        val currentQueue = _uiState.value.queue.toMutableList()
        val nextIndex = (exoPlayer.currentMediaItemIndex + 1).coerceIn(0, currentQueue.size)
        currentQueue.add(nextIndex, song)
        val metadata = MediaMetadata.Builder().setTitle(song.title).setArtist(song.artist).build()
        val mediaItem = MediaItem.Builder().setMediaId(song.id).setUri(song.mediaUri).setMediaMetadata(metadata).build()
        exoPlayer.addMediaItem(nextIndex, mediaItem)
        _uiState.update { it.copy(queue = currentQueue) }
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
        }
    }

    fun clearQueue() {
        exoPlayer.clearMediaItems()
        _uiState.update {
            it.copy(queue = emptyList(), currentSong = null, currentQueueIndex = -1, isPlaying = false)
        }
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
                exoPlayer.pause()
                _uiState.update { it.copy(sleepTimerMinutesLeft = null) }
            }
        }
    }

    fun setEqualizerPreset(preset: EqualizerPreset) {
        _uiState.update { it.copy(equalizerPreset = preset) }
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
            while (isActive) {
                if (exoPlayer.isPlaying) {
                    val pos = exoPlayer.currentPosition.coerceAtLeast(0L)
                    val dur = exoPlayer.duration.coerceAtLeast(0L)
                    val buf = exoPlayer.bufferedPosition.coerceAtLeast(0L)
                    
                    _uiState.update {
                        it.copy(
                            currentPositionMs = pos,
                            durationMs = if (dur > 0) dur else it.durationMs,
                            bufferedPositionMs = buf
                        )
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
