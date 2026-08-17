package com.example.player

import android.content.Context
import android.net.Uri
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.core.net.toUri
import com.example.core.model.EqualizerPreset
import com.example.core.model.MusicSource
import com.example.core.model.PlayerUiState
import com.example.core.model.RepeatMode
import com.example.core.model.Song
import com.example.core.source.LyricsResolver
import com.example.core.source.youtube.YouTubeAudioResolver
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

/**
 * Singleton player manager coordinating ExoPlayer instance, playback state, queue,
 * sleep timer, and reactive UI state updates.
 */
class FusionPlayerManager private constructor(private val applicationContext: Context) {

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var progressTickerJob: Job? = null
    private var sleepTimerJob: Job? = null
    private var lyricsFetchJob: Job? = null
    private val youTubeResolver = YouTubeAudioResolver()
    private val lyricsResolver = LyricsResolver()

    val exoPlayer: ExoPlayer by lazy {
        val audioAttributes = AudioAttributes.Builder()
            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
            .setUsage(C.USAGE_MEDIA)
            .build()

        ExoPlayer.Builder(applicationContext)
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
            val isBuffering = playbackState == Player.STATE_BUFFERING
            val duration = exoPlayer.duration.coerceAtLeast(0)
            _uiState.update {
                it.copy(
                    isBuffering = isBuffering,
                    durationMs = if (duration > 0) duration else it.durationMs,
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

    fun playSong(song: Song, queue: List<Song> = listOf(song), startIndex: Int = queue.indexOf(song).coerceAtLeast(0)) {
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

            val resolvedQueue = queue.map { track ->
                if ((track.source == MusicSource.YOUTUBE) && !track.isDownloaded && track.mediaUri.contains("youtube.com/watch")) {
                    val directUrl = youTubeResolver.resolveAudioStreamUrl(track.id)
                    if (!directUrl.isNullOrEmpty()) track.copy(mediaUri = directUrl) else track
                } else {
                    track
                }
            }

            val currentResolved = resolvedQueue.getOrNull(startIndex) ?: song

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

            _uiState.update {
                it.copy(
                    queue = resolvedQueue,
                    currentSong = currentResolved,
                    isBuffering = false
                )
            }

            exoPlayer.setMediaItems(mediaItems, startIndex, 0L)
            exoPlayer.prepare()
            exoPlayer.play()
        }
    }

    fun togglePlayPause() {
        if (exoPlayer.isPlaying) {
            exoPlayer.pause()
        } else {
            if ((exoPlayer.playbackState == Player.STATE_IDLE) && (_uiState.value.currentSong != null)) {
                val song = _uiState.value.currentSong!!
                playSong(song, _uiState.value.queue.ifEmpty { listOf(song) })
            } else {
                exoPlayer.play()
            }
        }
    }

    fun pause() {
        exoPlayer.pause()
    }

    fun seekTo(positionMs: Long) {
        val clamped = positionMs.coerceIn(0L, (_uiState.value.durationMs).coerceAtLeast(1L))
        exoPlayer.seekTo(clamped)
        _uiState.update { it.copy(currentPositionMs = clamped) }
    }

    fun skipToNext() {
        if (exoPlayer.hasNextMediaItem()) {
            exoPlayer.seekToNextMediaItem()
        } else if (_uiState.value.repeatMode == RepeatMode.ALL && _uiState.value.queue.isNotEmpty()) {
            exoPlayer.seekTo(0, 0L)
            exoPlayer.play()
        }
    }

    fun skipToPrevious() {
        if (exoPlayer.currentPosition > 3000) {
            exoPlayer.seekTo(0L)
        } else if (exoPlayer.hasPreviousMediaItem()) {
            exoPlayer.seekToPreviousMediaItem()
        } else {
            exoPlayer.seekTo(0L)
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

        when (next) {
            RepeatMode.OFF -> {
                exoPlayer.repeatMode = Player.REPEAT_MODE_OFF
            }
            RepeatMode.ALL -> {
                exoPlayer.repeatMode = Player.REPEAT_MODE_ALL
            }
            RepeatMode.ONE -> {
                exoPlayer.repeatMode = Player.REPEAT_MODE_ONE
            }
        }
        _uiState.update { it.copy(repeatMode = next) }
    }

    fun addToQueue(song: Song) {
        val currentQueue = _uiState.value.queue.toMutableList()
        currentQueue.add(song)
        val metadata = MediaMetadata.Builder()
            .setTitle(song.title)
            .setArtist(song.artist)
            .setAlbumTitle(song.album)
            .setArtworkUri(song.artworkUri?.toUri())
            .build()
        val mediaItem = MediaItem.Builder()
            .setMediaId(song.id)
            .setUri(song.mediaUri)
            .setMediaMetadata(metadata)
            .build()

        exoPlayer.addMediaItem(mediaItem)
        _uiState.update { it.copy(queue = currentQueue) }
    }

    fun playNext(song: Song) {
        val currentQueue = _uiState.value.queue.toMutableList()
        val nextIndex = (_uiState.value.currentQueueIndex + 1).coerceIn(0, currentQueue.size)
        currentQueue.add(nextIndex, song)

        val metadata = MediaMetadata.Builder()
            .setTitle(song.title)
            .setArtist(song.artist)
            .setAlbumTitle(song.album)
            .setArtworkUri(song.artworkUri?.toUri())
            .build()
        val mediaItem = MediaItem.Builder()
            .setMediaId(song.id)
            .setUri(song.mediaUri)
            .setMediaMetadata(metadata)
            .build()

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
            _uiState.update {
                it.copy(
                    queue = currentQueue,
                    currentQueueIndex = newCurrentIndex
                )
            }
        }
    }

    fun moveQueueItem(from: Int, to: Int) {
        val currentQueue = _uiState.value.queue.toMutableList()
        if (from in currentQueue.indices && to in currentQueue.indices) {
            val item = currentQueue.removeAt(from)
            currentQueue.add(to, item)
            exoPlayer.moveMediaItem(from, to)
            _uiState.update { it.copy(queue = currentQueue) }
        }
    }

    fun clearQueue() {
        exoPlayer.clearMediaItems()
        _uiState.update {
            it.copy(
                queue = emptyList(),
                currentSong = null,
                currentQueueIndex = -1,
                isPlaying = false,
                currentPositionMs = 0L,
                durationMs = 0L
            )
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
                delay(60_000L)
                remaining -= 1
                _uiState.update { it.copy(sleepTimerMinutesLeft = remaining) }
            }
            if (isActive) {
                pause()
                _uiState.update { it.copy(sleepTimerMinutesLeft = null) }
            }
        }
    }

    fun setEqualizerPreset(preset: EqualizerPreset) {
        _uiState.update { it.copy(equalizerPreset = preset) }
    }

    private fun handleTrackEnded() {
        _uiState.value.currentSong?.let { song ->
            onSongCompletedCallback?.invoke(song)
        }
        if (exoPlayer.hasNextMediaItem()) {
            exoPlayer.seekToNextMediaItem()
        } else if (_uiState.value.repeatMode == RepeatMode.ALL && _uiState.value.queue.isNotEmpty()) {
            exoPlayer.seekTo(0, 0L)
            exoPlayer.play()
        } else {
            _uiState.update { it.copy(isPlaying = false, currentPositionMs = 0L) }
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
                delay(400L)
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
