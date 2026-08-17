package com.example.ui

import android.app.Application
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
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
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.time.Duration.Companion.milliseconds

enum class SearchFilter(val label: String) {
    ALL("Todo"),
    SONGS("Canciones"),
    YOUTUBE("YouTube"),
    ARTISTS("Artistas"),
    ALBUMS("Álbumes")
}

data class SearchUiResult(
    val songs: List<Song> = emptyList(),
    val youTubeSongs: List<Song> = emptyList(),
    val artists: List<Artist> = emptyList(),
    val albums: List<Album> = emptyList()
)

class FusionMainViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as FusionApplication
    val repository = app.musicRepository
    val playerManager = app.playerManager
    val servicesManager = app.servicesManager
    val downloadManager = app.downloadManager
    val configManager = app.configManager

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

    val servicesState: StateFlow<List<ExternalServiceDescriptor>> = servicesManager.servicesState

    private val _youtubeApiKey = MutableStateFlow(configManager.getYouTubeApiKey() ?: "")
    val youtubeApiKey: StateFlow<String> = _youtubeApiKey.asStateFlow()

    // Dynamic Theming
    private val _accentColor = MutableStateFlow(Color(0xFF00E5FF)) // Default NeonCyan
    val accentColor: StateFlow<Color> = _accentColor.asStateFlow()

    // Search History
    private val _searchHistory = MutableStateFlow(configManager.getSearchHistory())
    val searchHistory: StateFlow<List<String>> = _searchHistory.asStateFlow()

    init {
        // Observe player changes to update theme
        playerUiState
            .map { it.currentSong?.id }
            .distinctUntilChanged()
            .onEach { updateThemeForCurrentSong() }
            .launchIn(viewModelScope)
    }

    private fun updateThemeForCurrentSong() {
        val song = playerUiState.value.currentSong
        if (song?.artworkUri.isNullOrBlank()) {
            _accentColor.value = Color(0xFF00E5FF)
            return
        }

        viewModelScope.launch {
            val bitmap = loadBitmap(song.artworkUri!!)
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
            .allowHardware(false) // Required for Palette
            .build()
        val result = loader.execute(request)
        if (result is SuccessResult) {
            (result.drawable as? BitmapDrawable)?.bitmap
        } else null
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

    // Search state
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _searchFilter = MutableStateFlow(SearchFilter.ALL)
    val searchFilter: StateFlow<SearchFilter> = _searchFilter.asStateFlow()

    private val _youTubeSearchResults = MutableStateFlow<List<Song>>(emptyList())

    private val _isSearchingYouTube = MutableStateFlow(false)
    val isSearchingYouTube: StateFlow<Boolean> = _isSearchingYouTube.asStateFlow()

    private var youTubeSearchJob: Job? = null

    val searchResults: StateFlow<SearchUiResult> = combine(
        combine(_searchQuery, _searchFilter, _youTubeSearchResults) { query, filter, ytSongs ->
            Triple(query, filter, ytSongs)
        },
        combine(songs, artists, albums) { allSongs, allArtists, allAlbums ->
            Triple(allSongs, allArtists, allAlbums)
        }
    ) { (query, filter, ytSongs), (allSongs, allArtists, allAlbums) ->
        val q = query.trim().lowercase()
        if (q.isEmpty()) {
            SearchUiResult()
        } else {
            val matchedSongs = if ((filter == SearchFilter.ALL) || (filter == SearchFilter.SONGS)) {
                allSongs.filter {
                    (it.source == MusicSource.LOCAL) &&
                    (it.title.lowercase().contains(q) || it.artist.lowercase().contains(q) || it.album.lowercase().contains(q))
                }
            } else emptyList()

            val matchedYouTube = if ((filter == SearchFilter.ALL) || (filter == SearchFilter.YOUTUBE)) {
                ytSongs
            } else emptyList()

            val matchedArtists = if ((filter == SearchFilter.ALL) || (filter == SearchFilter.ARTISTS)) {
                allArtists.filter { it.name.lowercase().contains(q) }
            } else emptyList()

            val matchedAlbums = if ((filter == SearchFilter.ALL) || (filter == SearchFilter.ALBUMS)) {
                allAlbums.filter { it.title.lowercase().contains(q) || it.artist.lowercase().contains(q) }
            } else emptyList()

            SearchUiResult(
                songs = matchedSongs,
                youTubeSongs = matchedYouTube,
                artists = matchedArtists,
                albums = matchedAlbums,
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SearchUiResult())

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
        triggerYouTubeSearch(query)
        if (query.length >= 3) {
            addToSearchHistory(query)
        }
    }

    private fun addToSearchHistory(query: String) {
        configManager.addSearchQuery(query)
        _searchHistory.value = configManager.getSearchHistory()
    }

    fun clearSearchHistory() {
        configManager.clearSearchHistory()
        _searchHistory.value = emptyList()
    }

    fun updateSearchFilter(filter: SearchFilter) {
        _searchFilter.value = filter
    }

    private fun triggerYouTubeSearch(query: String) {
        youTubeSearchJob?.cancel()
        val trimmed = query.trim()
        if (trimmed.length < 2) {
            _youTubeSearchResults.value = emptyList()
            _isSearchingYouTube.value = false
            return
        }

        youTubeSearchJob = viewModelScope.launch {
            delay(350.milliseconds) // Debounce typing
            _isSearchingYouTube.value = true
            try {
                val results = repository.searchYouTube(trimmed)
                _youTubeSearchResults.value = results
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isSearchingYouTube.value = false
            }
        }
    }

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

    fun updateYouTubeApiKey(apiKey: String) {
        viewModelScope.launch {
            configManager.saveYouTubeApiKey(apiKey)
            _youtubeApiKey.value = apiKey
            repository.youtubeProvider.authenticate(mapOf("apiKey" to apiKey))
        }
    }

    fun rescanLocalLibrary() {
        viewModelScope.launch {
            repository.rescanLocalMusic()
        }
    }
}
