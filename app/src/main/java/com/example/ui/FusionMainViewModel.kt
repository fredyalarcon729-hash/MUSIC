package com.example.ui

import android.app.Application
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.util.Log
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.palette.graphics.Palette
import coil.ImageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import com.example.FusionApplication
import com.example.core.model.Album
import com.example.core.model.Artist
import com.example.core.model.DownloadStatus
import com.example.core.model.EqualizerPreset
import com.example.core.model.MusicSource
import com.example.core.model.PlayerUiState
import com.example.core.model.Playlist
import com.example.core.model.Song
import com.example.core.source.ExternalServiceDescriptor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class FusionMainViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as FusionApplication
    val repository = app.musicRepository
    val playerManager = app.playerManager
    val servicesManager = app.servicesManager
    val downloadManager = app.downloadManager
    val configManager = app.configManager
    val authManager = app.authManager

    val isScanning: StateFlow<Boolean> = repository.isScanning

    val songs: StateFlow<List<Song>> = repository.songsWithFavorites
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val downloadedSongs: StateFlow<List<Song>> = repository.downloadedSongsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val totalDownloadedStorageBytes: StateFlow<Long> = repository.totalDownloadedSizeBytes
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0L)

    val downloadStates: StateFlow<Map<String, DownloadStatus>> = downloadManager.downloadStates

    val albums: StateFlow<List<Album>> = repository.albumsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val artists: StateFlow<List<Artist>> = repository.artistsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val playerUiState: StateFlow<PlayerUiState> = playerManager.uiState

    val playbackPositionMs: StateFlow<Long> = playerManager.playbackPositionMs

    val servicesState: StateFlow<List<ExternalServiceDescriptor>> = servicesManager.servicesState

    // UI Events (errors, toasts)
    private val _eventFlow = MutableSharedFlow<String>()
    val eventFlow: SharedFlow<String> = _eventFlow.asSharedFlow()

    // Statistics
    val totalListeningTimeMs: StateFlow<Long> = repository.totalListeningTimeMs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0L)

    val topArtists: StateFlow<List<Pair<String, Int>>> = repository.topArtistsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val hourlyActivity: StateFlow<Map<Int, Int>> = repository.hourlyActivityFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    // Dynamic Theming
    private val _accentColor = MutableStateFlow(Color(0xFF00E5FF)) // Default NeonCyan
    val accentColor: StateFlow<Color> = _accentColor.asStateFlow()

    val userSession = authManager.userState

    fun getDiagnosticInfo() = authManager.getDiagnosticInfo()

    init {
        // Initialize player with saved settings
        playerManager.setVisualizerStyle(configManager.getVisualizerStyle())

        // Observe player changes to update theme
        playerUiState
            .map { it.currentSong?.id }
            .distinctUntilChanged()
            .onEach { updateThemeForCurrentSong() }
            .launchIn(viewModelScope)

        // Observe player errors
        playerManager.errorFlow
            .onEach { _eventFlow.emit(it) }
            .launchIn(viewModelScope)
    }

    fun updateThemeForCurrentSong() {
        // If user has set a fixed custom accent color, use it
        val customColor = configManager.getCustomAccentColor()
        if (customColor != 0L) {
            _accentColor.value = Color(customColor)
            return
        }

        val song = playerUiState.value.currentSong
        val artworkUri = song?.artworkUri
        if (artworkUri.isNullOrBlank()) {
            _accentColor.value = Color(0xFF00E5FF)
            return
        }

        viewModelScope.launch {
            val bitmap = loadBitmap(artworkUri)
            if (bitmap != null) {
                val palette = Palette.from(bitmap).generate()
                val vibrant = palette.getVibrantColor(0xFF00E5FF.toInt())
                _accentColor.value = Color(vibrant)
            }
        }
    }

    private suspend fun loadBitmap(uri: String): Bitmap? = withContext(Dispatchers.IO) {
        val loader = ImageLoader(app)
        val request = ImageRequest.Builder(app)
            .data(uri)
            .allowHardware(false)
            .build()
        val result = loader.execute(request)
        if (result is SuccessResult) (result.drawable as? BitmapDrawable)?.bitmap else null
    }

    // Favorites
    val favoriteSongs: StateFlow<List<Song>> = songs.map { songList ->
        songList.filter { it.isFavorite }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Playlists
    val playlists: StateFlow<List<Playlist>> = repository.playlistsFlow.map { playlistEntities ->
        playlistEntities.map { entity ->
            Playlist(
                id = entity.id,
                name = entity.name,
                description = entity.description,
                createdAt = entity.createdAt,
                songCount = 0,
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Playback Controls
    fun playSong(song: Song, queue: List<Song> = emptyList(), startIndex: Int = -1) {
        val actualQueue = queue.ifEmpty { listOf(song) }
        val idx = if (startIndex >= 0) startIndex else actualQueue.indexOf(song).coerceAtLeast(0)
        playerManager.playSong(song, actualQueue, idx)
    }

    fun playAll(songsList: List<Song>, shuffle: Boolean = false) {
        if (songsList.isEmpty()) return
        val listToPlay = if (shuffle) songsList.shuffled() else songsList
        playerManager.playSong(listToPlay.first(), listToPlay, 0)
    }

    fun togglePlayPause() = playerManager.togglePlayPause()
    fun seekTo(positionMs: Long) = playerManager.seekTo(positionMs)
    fun setPlaybackSpeed(speed: Float) = playerManager.setPlaybackSpeed(speed)
    fun skipToNext() = playerManager.skipToNext()
    fun skipToPrevious() = playerManager.skipToPrevious()
    fun toggleShuffle() = playerManager.toggleShuffle()
    fun cycleRepeatMode() = playerManager.cycleRepeatMode()
    fun addToQueue(song: Song) = playerManager.addToQueue(song)
    fun playNext(song: Song) = playerManager.playNext(song)
    fun removeFromQueue(index: Int) = playerManager.removeFromQueue(index)
    fun clearQueue() = playerManager.clearQueue()
    fun setSleepTimer(minutes: Int?) = playerManager.setSleepTimer(minutes)
    fun setEqualizerPreset(preset: EqualizerPreset) = playerManager.setEqualizerPreset(preset)
    fun setBandLevel(band: Int, level: Int) = playerManager.setBandLevel(band, level)
    fun setBassBoost(strength: Int) = playerManager.setBassBoost(strength)
    fun setVirtualizer(strength: Int) = playerManager.setVirtualizer(strength)
    fun setSkipSilenceEnabled(enabled: Boolean) = playerManager.setSkipSilenceEnabled(enabled)
    fun setCrossfadeDuration(seconds: Int) = playerManager.setCrossfadeDuration(seconds)
    fun setMezclaProEnabled(enabled: Boolean) = playerManager.setMezclaProEnabled(enabled)
    fun setPitchSemitones(semitones: Int) = playerManager.setPitchSemitones(semitones)
    fun setVocalReductionEnabled(enabled: Boolean) = playerManager.setVocalReductionEnabled(enabled)
    fun setVocalReductionStrength(strength: Float) = playerManager.setVocalReductionStrength(strength)
    fun setKaraokeModeActive(active: Boolean) = playerManager.setKaraokeModeActive(active)

    fun toggleNormalization() {
        val next = !playerUiState.value.isNormalizationEnabled
        playerManager.setNormalizationEnabled(next)
    }

    fun setVolume(volume: Float) = playerManager.setVolume(volume)

    // Downloads
    fun startDownload(song: Song) {
        repository.startDownload(song)
    }

    fun cancelDownload(songId: String) {
        repository.cancelDownload(songId)
    }

    fun deleteDownload(song: Song) {
        viewModelScope.launch {
            repository.deleteDownload(song.id)
        }
    }

    // Favorites
    fun toggleFavorite(song: Song) {
        viewModelScope.launch {
            repository.toggleFavorite(song)
        }
    }

    // Playlists
    fun createPlaylist(name: String, description: String = "") {
        viewModelScope.launch {
            repository.createPlaylist(name, description)
        }
    }

    fun deletePlaylist(playlistId: Long) {
        viewModelScope.launch {
            repository.deletePlaylist(playlistId)
        }
    }

    fun addSongToPlaylist(playlistId: Long, songId: String) {
        viewModelScope.launch {
            repository.addSongToPlaylist(playlistId, songId)
        }
    }

    fun removeSongFromPlaylist(playlistId: Long, songId: String) {
        viewModelScope.launch {
            repository.removeSongFromPlaylist(playlistId, songId)
        }
    }

    fun getSongsForPlaylist(playlistId: Long) = repository.getSongsForPlaylist(playlistId)

    // External Services
    fun toggleExternalService(source: MusicSource, enabled: Boolean) {
        viewModelScope.launch {
            servicesManager.toggleService(source, enabled)
        }
    }

    fun rescanLocalLibrary() {
        viewModelScope.launch {
            repository.rescanLocalMusic()
        }
    }

    fun signInWithGoogle(context: android.content.Context, googleClientId: String?) {
        if (googleClientId.isNullOrBlank()) {
            viewModelScope.launch {
                _eventFlow.emit("Client ID de Google no configurado")
            }
            return
        }
        viewModelScope.launch {
            Log.d("FusionMainViewModel", "Attempting sign-in with ClientID: $googleClientId")
            
            val result = authManager.signIn(context, googleClientId)
            result.onSuccess { session ->
                repository.youtubeProvider.resolver.updateUserToken(session.idToken)
                _eventFlow.emit("Bienvenido, ${session.displayName}")
            }
            result.onFailure { error ->
                _eventFlow.emit(error.message ?: "Fallo al iniciar sesión")
            }
        }
    }

    fun signOut() {
        viewModelScope.launch {
            authManager.signOut()
            repository.youtubeProvider.resolver.updateUserToken(null)
            _eventFlow.emit("Sesión cerrada")
        }
    }

    fun shutdownApp() {
        playerManager.initiateManualShutdown()
    }

    fun resetShutdownState() {
        playerManager.resetShutdownState()
    }
}
