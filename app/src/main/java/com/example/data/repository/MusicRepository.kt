package com.example.data.repository

import com.example.core.model.Album
import com.example.core.model.Artist
import com.example.core.model.MusicSource
import com.example.core.model.Song
import com.example.core.source.LocalMusicSourceProvider
import com.example.core.source.youtube.YouTubeAudioResolver
import com.example.core.source.youtube.YouTubeDownloadManager
import com.example.core.source.youtube.YouTubeMusicSourceProvider
import com.example.data.local.dao.MusicDao
import com.example.data.local.entity.DownloadedSongEntity
import com.example.data.local.entity.FavoriteEntity
import com.example.data.local.entity.HistoryEntity
import com.example.data.local.entity.PlaylistEntity
import com.example.data.local.entity.PlaylistItemEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MusicRepository(
    private val localProvider: LocalMusicSourceProvider,
    private val musicDao: MusicDao,
    val downloadManager: YouTubeDownloadManager,
    val youtubeProvider: YouTubeMusicSourceProvider = YouTubeMusicSourceProvider()
) {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val _rawSongs = MutableStateFlow<List<Song>>(emptyList())

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    val favoritesFlow: Flow<List<FavoriteEntity>> = musicDao.getAllFavorites()
    val playlistsFlow: Flow<List<PlaylistEntity>> = musicDao.getAllPlaylists()

    // Downloaded songs from Room
    val downloadedSongsFlow: Flow<List<Song>> = combine(
        musicDao.getAllDownloadedSongs(),
        favoritesFlow
    ) { downloadedEntities, favs ->
        val favSet = favs.asSequence().map { it.songId }.toSet()
        downloadedEntities.map { entity ->
            Song(
                id = entity.id,
                title = entity.title,
                artist = entity.artist,
                album = entity.album,
                durationMs = entity.durationMs,
                mediaUri = entity.localMediaUri,
                artworkUri = entity.localArtworkUri ?: entity.originalThumbnailUrl,
                sizeBytes = entity.sizeBytes,
                dateAdded = entity.downloadedAt,
                source = MusicSource.YOUTUBE,
                isFavorite = favSet.contains(entity.id),
                isDownloaded = true,
                path = entity.localMediaUri,
            )
        }
    }.distinctUntilChanged()

    val totalDownloadedSizeBytes: Flow<Long> = musicDao.getTotalDownloadedSizeBytes().map { it ?: 0L }

    // Unified songs: Local MediaStore + Downloaded YouTube Tracks
    val songsWithFavorites: Flow<List<Song>> = combine(
        _rawSongs,
        downloadedSongsFlow,
        favoritesFlow
    ) { localSongs, downloadedSongs, favs ->
        val favSet = favs.asSequence().map { it.songId }.toSet()
        val localMapped = localSongs.map { song ->
            song.copy(isFavorite = favSet.contains(song.id))
        }

        // Merge, avoiding duplicate IDs if any
        val localIds = localMapped.asSequence().map { it.id }.toSet()
        val nonDuplicateDownloaded = downloadedSongs.filterNot { localIds.contains(it.id) }
        localMapped + nonDuplicateDownloaded
    }.distinctUntilChanged()

    val albumsFlow: Flow<List<Album>> = songsWithFavorites.map { songs ->
        localProvider.getLocalAlbums(songs)
    }

    val artistsFlow: Flow<List<Artist>> = songsWithFavorites.map { songs ->
        localProvider.getLocalArtists(songs)
    }

    init {
        scope.launch {
            rescanLocalMusic()
            seedDefaultPlaylistsIfNeeded()
        }
    }

    suspend fun rescanLocalMusic() = withContext(Dispatchers.IO) {
        _isScanning.value = true
        try {
            val loaded = localProvider.getLocalSongs()
            _rawSongs.value = loaded
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            _isScanning.value = false
        }
    }

    suspend fun toggleFavorite(song: Song) = withContext(Dispatchers.IO) {
        if (song.isFavorite) {
            musicDao.deleteFavorite(song.id)
        } else {
            musicDao.insertFavorite(FavoriteEntity(songId = song.id))
        }
    }

    suspend fun createPlaylist(name: String, description: String = ""): Long = withContext(Dispatchers.IO) {
        musicDao.insertPlaylist(
            PlaylistEntity(
                name = name.trim(),
                description = description.trim(),
                createdAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun deletePlaylist(playlistId: Long) = withContext(Dispatchers.IO) {
        musicDao.deletePlaylist(playlistId)
        musicDao.deletePlaylistItemsByPlaylistId(playlistId)
    }

    suspend fun addSongToPlaylist(playlistId: Long, songId: String) = withContext(Dispatchers.IO) {
        musicDao.insertPlaylistItem(
            PlaylistItemEntity(
                playlistId = playlistId,
                songId = songId,
                orderIndex = System.currentTimeMillis().toInt()
            )
        )
    }

    suspend fun removeSongFromPlaylist(playlistId: Long, songId: String) = withContext(Dispatchers.IO) {
        musicDao.deletePlaylistItem(playlistId, songId)
    }

    fun getSongsForPlaylist(playlistId: Long): Flow<List<Song>> {
        return combine(songsWithFavorites, musicDao.getPlaylistItems(playlistId)) { songs, items ->
            val songMap = songs.associateBy { it.id }
            items.mapNotNull { item -> songMap[item.songId] }
        }
    }

    suspend fun recordPlayedSong(songId: String, durationPlayedMs: Long = 0L) = withContext(Dispatchers.IO) {
        musicDao.insertHistory(
            HistoryEntity(
                songId = songId,
                playedAt = System.currentTimeMillis(),
                durationPlayedMs = durationPlayedMs
            )
        )
    }

    // YouTube Search
    suspend fun searchYouTube(query: String): List<Song> = withContext(Dispatchers.IO) {
        youtubeProvider.searchSongs(query)
    }

    // Download Actions
    fun startDownload(song: Song) {
        downloadManager.startDownload(song)
    }

    fun cancelDownload(songId: String) {
        downloadManager.cancelDownload(songId)
    }

    suspend fun deleteDownload(songId: String) {
        downloadManager.deleteDownload(songId)
    }

    private suspend fun seedDefaultPlaylistsIfNeeded() = withContext(Dispatchers.IO) {
        // Reactive database init
    }
}

