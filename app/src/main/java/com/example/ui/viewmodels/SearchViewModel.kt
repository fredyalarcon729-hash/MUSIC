package com.example.ui.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.FusionApplication
import com.example.core.model.Album
import com.example.core.model.Artist
import com.example.core.model.MusicSource
import com.example.core.model.Song
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

enum class SearchFilter(val label: String) {
    ALL("Todo"),
    SONGS("Canciones"),
    DEEZER("Deezer"),
    ARTISTS("Artistas"),
    ALBUMS("Álbumes")
}

data class SearchUiResult(
    val songs: List<Song> = emptyList(),
    val deezerSongs: List<Song> = emptyList(),
    val artists: List<Artist> = emptyList(),
    val albums: List<Album> = emptyList()
)

class SearchViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as FusionApplication
    private val repository = app.musicRepository
    private val configManager = app.configManager

    // Dependencies from repository flows
    private val allSongs = repository.songsWithFavorites
    private val allArtists = repository.artistsFlow
    private val allAlbums = repository.albumsFlow

    // Search state
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _searchFilter = MutableStateFlow(SearchFilter.ALL)
    val searchFilter: StateFlow<SearchFilter> = _searchFilter.asStateFlow()

    private val _deezerSearchResults = MutableStateFlow<List<Song>>(emptyList())

    private var deezerSearchJob: Job? = null

    // Search History
    private val _searchHistory = MutableStateFlow(configManager.getSearchHistory())
    val searchHistory: StateFlow<List<String>> = _searchHistory.asStateFlow()

    val searchResults: StateFlow<SearchUiResult> = combine(
        combine(_searchQuery, _searchFilter, _deezerSearchResults) { query, filter, dzSongs ->
            Triple(query, filter, dzSongs)
        },
        combine(allSongs, allArtists, allAlbums) { songs, artists, albums ->
            Triple(songs, artists, albums)
        }
    ) { (query, filter, dzSongs), (songs, artists, albums) ->
        val q = query.trim().lowercase()
        if (q.isEmpty()) {
            SearchUiResult()
        } else {
            val matchedSongs = if ((filter == SearchFilter.ALL) || (filter == SearchFilter.SONGS)) {
                songs.filter {
                    (it.source == MusicSource.LOCAL) &&
                    (it.title.lowercase().contains(q) || it.artist.lowercase().contains(q) || it.album.lowercase().contains(q))
                }
            } else emptyList()

            val matchedDeezer = if ((filter == SearchFilter.ALL) || (filter == SearchFilter.DEEZER)) {
                dzSongs
            } else emptyList()

            val matchedArtists = if ((filter == SearchFilter.ALL) || (filter == SearchFilter.ARTISTS)) {
                artists.filter { it.name.lowercase().contains(q) }
            } else emptyList()

            val matchedAlbums = if ((filter == SearchFilter.ALL) || (filter == SearchFilter.ALBUMS)) {
                albums.filter { it.title.lowercase().contains(q) || it.artist.lowercase().contains(q) }
            } else emptyList()

            SearchUiResult(
                songs = matchedSongs,
                deezerSongs = matchedDeezer,
                artists = matchedArtists,
                albums = matchedAlbums,
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SearchUiResult())

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
        triggerDeezerSearch(query)
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

    private fun triggerDeezerSearch(query: String) {
        deezerSearchJob?.cancel()
        val trimmed = query.trim()
        if (trimmed.length < 2) {
            _deezerSearchResults.value = emptyList()
            return
        }

        deezerSearchJob = viewModelScope.launch {
            delay(300.milliseconds)
            try {
                val results = repository.searchDeezer(trimmed)
                _deezerSearchResults.value = results
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
