package com.example.core.source

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import com.example.core.model.Album
import com.example.core.model.Artist
import com.example.core.model.MusicSource
import com.example.core.model.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

/**
 * Local MediaStore Provider for scanning device storage and providing audio tracks.
 */
class LocalMusicSourceProvider(
    private val context: Context
) : MusicSourceProvider {

    override val source: MusicSource = MusicSource.LOCAL
    override val displayName: String = "Almacenamiento Local"
    override val providerDescription: String = "Música sin conexión guardada en la memoria del dispositivo y tarjeta SD."
    override val integrationType: PlaybackIntegrationType = PlaybackIntegrationType.NATIVE_EXOPLAYER

    private val _isConfigured = MutableStateFlow(true)
    override val isConfigured: StateFlow<Boolean> = _isConfigured.asStateFlow()

    private val _isConnected = MutableStateFlow(true)
    override val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    override suspend fun authenticate(credentials: Map<String, String>): Result<Unit> {
        return Result.success(Unit)
    }

    override suspend fun disconnect() {
        // Local source is always available
    }

    override suspend fun searchSongs(query: String): List<Song> {
        val all = getLocalSongs()
        val q = query.trim().lowercase()
        return all.filter {
            it.title.lowercase().contains(q) ||
            it.artist.lowercase().contains(q) ||
            it.album.lowercase().contains(q)
        }
    }

    override suspend fun resolveMediaUri(songId: String): String? {
        val all = getLocalSongs()
        return all.find { it.id == songId }?.mediaUri
    }

    /**
     * Scans MediaStore for local audio files. If empty or no permission yet, returns
     * high-quality built-in demo tracks so the user can test the player immediately.
     */
    suspend fun getLocalSongs(filterVoiceNotes: Boolean = true): List<Song> = withContext(Dispatchers.IO) {
        val songsList = mutableListOf<Song>()
        val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        }

        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.ALBUM_ID,
            MediaStore.Audio.Media.ARTIST_ID,
            MediaStore.Audio.Media.TRACK,
            MediaStore.Audio.Media.YEAR,
            MediaStore.Audio.Media.SIZE,
            MediaStore.Audio.Media.DATE_ADDED,
            MediaStore.Audio.Media.DATA
        )

        val selection = if (filterVoiceNotes) {
            val excludedFolders = listOf("WhatsApp", "Telegram", "Recorder", "Voice Recorder", "Notifications", "Alarms", "Ringtones")
            val selectionBuilder = StringBuilder()
            selectionBuilder.append("${MediaStore.Audio.Media.IS_MUSIC} != 0 AND ${MediaStore.Audio.Media.DURATION} >= 20000")
            excludedFolders.forEach { folder ->
                selectionBuilder.append(" AND ${MediaStore.Audio.Media.DATA} NOT LIKE '%/$folder/%'")
            }
            selectionBuilder.toString()
        } else {
            "${MediaStore.Audio.Media.IS_MUSIC} != 0 AND ${MediaStore.Audio.Media.DURATION} >= 5000"
        }

        val sortOrder = "${MediaStore.Audio.Media.TITLE} ASC"

        try {
            context.contentResolver.query(
                collection,
                projection,
                selection,
                null,
                sortOrder
            )?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                val titleCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
                val artistCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
                val albumCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
                val durationCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
                val albumIdCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
                val artistIdCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST_ID)
                val trackCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TRACK)
                val yearCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.YEAR)
                val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.SIZE)
                val dateCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_ADDED)
                val dataCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idCol)
                    val title = cursor.getString(titleCol) ?: "Sin título"
                    val artist = cursor.getString(artistCol) ?: "Artista desconocido"
                    val album = cursor.getString(albumCol) ?: "Álbum desconocido"
                    val duration = cursor.getLong(durationCol)
                    val albumId = cursor.getLong(albumIdCol)
                    val artistId = cursor.getLong(artistIdCol)
                    val track = cursor.getInt(trackCol)
                    val year = cursor.getInt(yearCol)
                    val size = cursor.getLong(sizeCol)
                    val dateAdded = cursor.getLong(dateCol)
                    val path = cursor.getString(dataCol) ?: ""

                    val contentUri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id)
                    val artworkUri = ContentUris.withAppendedId(
                        Uri.parse("content://media/external/audio/albumart"),
                        albumId
                    ).toString()

                    songsList.add(
                        Song(
                            id = "local_$id",
                            title = title,
                            artist = if (artist == "<unknown>") "Artista Desconocido" else artist,
                            album = if (album == "<unknown>") "Álbum Desconocido" else album,
                            durationMs = duration,
                            mediaUri = contentUri.toString(),
                            artworkUri = artworkUri,
                            albumId = albumId,
                            artistId = artistId,
                            trackNumber = track,
                            year = year,
                            sizeBytes = size,
                            dateAdded = dateAdded,
                            source = MusicSource.LOCAL,
                            path = path
                        )
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // If local storage query yields no results (e.g., emulator with empty media store),
        // fallback to curated royalty-free synthesized demo tracks for immediate delight!
        if (songsList.isEmpty()) {
            getDemoSongs()
        } else {
            songsList
        }
    }

    suspend fun getLocalAlbums(songs: List<Song>): List<Album> = withContext(Dispatchers.Default) {
        songs.groupBy { it.album }
            .map { (albumTitle, songGroup) ->
                val first = songGroup.first()
                Album(
                    id = first.albumId,
                    title = albumTitle,
                    artist = first.artist,
                    artworkUri = first.artworkUri,
                    songCount = songGroup.size,
                    year = first.year
                )
            }.sortedBy { it.title }
    }

    suspend fun getLocalArtists(songs: List<Song>): List<Artist> = withContext(Dispatchers.Default) {
        songs.groupBy { it.artist }
            .map { (artistName, songGroup) ->
                val first = songGroup.first()
                val albumCount = songGroup.map { it.album }.distinct().size
                Artist(
                    id = first.artistId,
                    name = artistName,
                    songCount = songGroup.size,
                    albumCount = albumCount,
                    artworkUri = first.artworkUri
                )
            }.sortedBy { it.name }
    }

    /**
     * Curated sample audio tracks with direct audio streams and artwork for quick playback demonstration.
     */
    fun getDemoSongs(): List<Song> {
        return listOf(
            Song(
                id = "demo_1",
                title = "Cyber Pulse Symphony",
                artist = "Neon Mirage",
                album = "Fusion Horizons",
                durationMs = 214000,
                mediaUri = "https://cdn.pixabay.com/download/audio/2022/05/27/audio_1808fbf07a.mp3?filename=electronic-future-beats-117997.mp3",
                artworkUri = "https://picsum.photos/seed/cyberpulse/500/500",
                albumId = 101L,
                artistId = 201L,
                trackNumber = 1,
                year = 2024,
                source = MusicSource.LOCAL
            ),
            Song(
                id = "demo_2",
                title = "Midnight Horizon",
                artist = "Aura Eclipse",
                album = "Neon Drift",
                durationMs = 188000,
                mediaUri = "https://cdn.pixabay.com/download/audio/2022/03/15/audio_c8c8a73467.mp3?filename=lofi-study-112191.mp3",
                artworkUri = "https://picsum.photos/seed/midnighthorizon/500/500",
                albumId = 102L,
                artistId = 202L,
                trackNumber = 2,
                year = 2024,
                source = MusicSource.LOCAL
            ),
            Song(
                id = "demo_3",
                title = "Quantum Echoes",
                artist = "Starlight Theory",
                album = "Subatomic Waves",
                durationMs = 245000,
                mediaUri = "https://cdn.pixabay.com/download/audio/2022/01/18/audio_d0a13f69d2.mp3?filename=ambient-piano-amp-strings-10711.mp3",
                artworkUri = "https://picsum.photos/seed/quantumechoes/500/500",
                albumId = 103L,
                artistId = 203L,
                trackNumber = 3,
                year = 2023,
                source = MusicSource.LOCAL
            ),
            Song(
                id = "demo_4",
                title = "Sunset Boulevard Drive",
                artist = "RetroWave Syndicate",
                album = "Outrun Memories",
                durationMs = 195000,
                mediaUri = "https://cdn.pixabay.com/download/audio/2022/10/14/audio_9939f792cb.mp3?filename=synthwave-80s-110045.mp3",
                artworkUri = "https://picsum.photos/seed/sunsetdrive/500/500",
                albumId = 104L,
                artistId = 204L,
                trackNumber = 4,
                year = 2024,
                source = MusicSource.LOCAL
            ),
            Song(
                id = "demo_5",
                title = "Cosmic Groove",
                artist = "Velocty Funk",
                album = "Supernova Beats",
                durationMs = 162000,
                mediaUri = "https://cdn.pixabay.com/download/audio/2022/08/02/audio_884fe92c21.mp3?filename=groove-funk-114400.mp3",
                artworkUri = "https://picsum.photos/seed/cosmicgroove/500/500",
                albumId = 105L,
                artistId = 205L,
                trackNumber = 5,
                year = 2024,
                source = MusicSource.LOCAL
            ),
            Song(
                id = "demo_6",
                title = "Velvet Rain Nocturne",
                artist = "Luna Vibe",
                album = "Chill Hop Diaries",
                durationMs = 173000,
                mediaUri = "https://cdn.pixabay.com/download/audio/2021/08/04/audio_12b0c7443c.mp3?filename=lofi-chill-medium-version-159456.mp3",
                artworkUri = "https://picsum.photos/seed/velvetrain/500/500",
                albumId = 106L,
                artistId = 206L,
                trackNumber = 6,
                year = 2024,
                source = MusicSource.LOCAL
            )
        )
    }
}
