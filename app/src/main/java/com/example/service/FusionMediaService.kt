package com.example.service

import android.app.Notification
import android.app.PendingIntent
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.util.Log
import androidx.annotation.OptIn
import androidx.core.app.NotificationCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.DefaultMediaNotificationProvider
import androidx.media3.session.LibraryResult
import androidx.media3.session.MediaLibraryService
import androidx.media3.session.MediaSession
import coil.ImageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import com.example.MainActivity
import com.example.R
import com.example.player.FusionPlayerManager
import com.google.common.collect.ImmutableList
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Standard MediaLibraryService for professional background playback (v22.0).
 * Forced manual foreground management for maximum reliability (Alternative 1).
 */
class FusionMediaService : MediaLibraryService() {

    private var mediaLibrarySession: MediaLibrarySession? = null
    private lateinit var playerManager: FusionPlayerManager
    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    companion object {
        private const val SERVICE_TAG = "FusionMediaService"
        private const val NOTIFICATION_ID = 1001
        const val CHANNEL_ID = "fusion_playback_channel"
        const val ROOT_ID = "root"
        const val CATEGORY_SONGS = "category_songs"
        const val CATEGORY_ARTISTS = "category_artists"
        const val CATEGORY_ALBUMS = "category_albums"
        const val CATEGORY_PLAYLISTS = "category_playlists"
    }

    @OptIn(UnstableApi::class)
    override fun onCreate() {
        super.onCreate()
        Log.d(SERVICE_TAG, "onCreate: Iniciando MediaLibraryService (Manual Mode)")
        playerManager = FusionPlayerManager.getInstance(this)

        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        // Build the session
        mediaLibrarySession = MediaLibrarySession.Builder(this, playerManager.exoPlayer, LibraryCallback())
            .setSessionActivity(pendingIntent)
            .build()
        
        playerManager.exoPlayer.addListener(playerListener)
        
        Log.d(SERVICE_TAG, "onCreate: Sesión creada y listener registrado")
    }

    private val playerListener = object : Player.Listener {
        override fun onEvents(player: Player, events: Player.Events) {
            if (events.containsAny(
                    Player.EVENT_PLAY_WHEN_READY_CHANGED,
                    Player.EVENT_PLAYBACK_STATE_CHANGED,
                    Player.EVENT_MEDIA_METADATA_CHANGED,
                    Player.EVENT_IS_PLAYING_CHANGED,
                    Player.EVENT_MEDIA_ITEM_TRANSITION
                )
            ) {
                updateNotification()
            }
        }
    }

    private fun updateNotification() {
        val player = playerManager.exoPlayer
        val song = playerManager.uiState.value.currentSong ?: return

        serviceScope.launch {
            val artwork = loadArtwork(song.artworkUri)
            val notification = buildNotification(song, player.isPlaying, artwork)
            
            val notificationManager = getSystemService(android.content.Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
            
            if (player.isPlaying) {
                startForeground(NOTIFICATION_ID, notification)
            } else {
                notificationManager.notify(NOTIFICATION_ID, notification)
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                    stopForeground(STOP_FOREGROUND_DETACH)
                }
            }
        }
    }

    private suspend fun loadArtwork(uri: String?): Bitmap? = withContext(Dispatchers.IO) {
        if (uri.isNullOrBlank()) return@withContext null
        try {
            val loader = ImageLoader(this@FusionMediaService)
            val request = ImageRequest.Builder(this@FusionMediaService)
                .data(uri)
                .size(500, 500)
                .allowHardware(false)
                .build()
            val result = loader.execute(request)
            if (result is SuccessResult) (result.drawable as? BitmapDrawable)?.bitmap else null
        } catch (e: Exception) {
            null
        }
    }

    @OptIn(UnstableApi::class)
    private fun buildNotification(song: com.example.core.model.Song, isPlaying: Boolean, artwork: Bitmap?): Notification {
        val session = mediaLibrarySession ?: return NotificationCompat.Builder(this, CHANNEL_ID).build()
        
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE)

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(song.title)
            .setContentText(song.artist)
            .setLargeIcon(artwork)
            .setContentIntent(pendingIntent)
            .setOngoing(isPlaying)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            
        // Use Media3 MediaStyle for professional look and controls
        builder.setStyle(
            androidx.media3.session.MediaStyleNotificationHelper.MediaStyle(session)
                .setShowActionsInCompactView(0, 1, 2)
        )

        return builder.build()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(SERVICE_TAG, "onStartCommand recibido")
        updateNotification()
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaLibrarySession? {
        return mediaLibrarySession
    }

    override fun onDestroy() {
        playerManager.exoPlayer.removeListener(playerListener)
        mediaLibrarySession?.run {
            release()
            mediaLibrarySession = null
        }
        super.onDestroy()
        Log.d(SERVICE_TAG, "onDestroy: Servicio destruido")
    }

    private inner class LibraryCallback : MediaLibrarySession.Callback {
        override fun onConnect(
            session: MediaSession,
            controller: MediaSession.ControllerInfo
        ): MediaSession.ConnectionResult {
            return MediaSession.ConnectionResult.accept(
                MediaSession.ConnectionResult.DEFAULT_SESSION_COMMANDS,
                MediaSession.ConnectionResult.DEFAULT_PLAYER_COMMANDS
            )
        }

        override fun onGetLibraryRoot(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            params: LibraryParams?
        ): ListenableFuture<LibraryResult<MediaItem>> {
            val rootItem = MediaItem.Builder()
                .setMediaId(ROOT_ID)
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        .setTitle("Fusion Music")
                        .setIsBrowsable(true)
                        .setIsPlayable(false)
                        .setMediaType(MediaMetadata.MEDIA_TYPE_FOLDER_MIXED)
                        .build()
                )
                .build()
            return Futures.immediateFuture(LibraryResult.ofItem(rootItem, params))
        }

        override fun onGetChildren(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            parentId: String,
            page: Int,
            pageSize: Int,
            params: LibraryParams?
        ): ListenableFuture<LibraryResult<ImmutableList<MediaItem>>> {
            val app = application as com.example.FusionApplication
            val repository = app.musicRepository
            val items = mutableListOf<MediaItem>()

            when {
                parentId == ROOT_ID -> {
                    items.add(createFolderItem(CATEGORY_SONGS, "Todas las Canciones", MediaMetadata.MEDIA_TYPE_MUSIC))
                    items.add(createFolderItem(CATEGORY_ARTISTS, "Artistas", MediaMetadata.MEDIA_TYPE_ARTIST))
                    items.add(createFolderItem(CATEGORY_ALBUMS, "Álbumes", MediaMetadata.MEDIA_TYPE_ALBUM))
                    items.add(createFolderItem(CATEGORY_PLAYLISTS, "Listas de Reproducción", MediaMetadata.MEDIA_TYPE_PLAYLIST))
                }
                parentId == CATEGORY_SONGS -> {
                    repository.songsState.value.forEach { song: com.example.core.model.Song ->
                        items.add(mapSongToMediaItem(song))
                    }
                }
                parentId == CATEGORY_ALBUMS -> {
                    val albumList = repository.songsState.value.map { it.album }.distinct().filter { it.isNotBlank() }
                    albumList.forEach { albumName: String ->
                        items.add(createFolderItem("album|$albumName", albumName, MediaMetadata.MEDIA_TYPE_ALBUM))
                    }
                }
                parentId.startsWith("album|") -> {
                    val albumName = parentId.removePrefix("album|")
                    repository.songsState.value.filter { it.album == albumName }.forEach { song: com.example.core.model.Song ->
                        items.add(mapSongToMediaItem(song))
                    }
                }
                parentId == CATEGORY_ARTISTS -> {
                    val artistList = repository.songsState.value.map { it.artist }.distinct().filter { it.isNotBlank() }
                    artistList.forEach { artistName: String ->
                        items.add(createFolderItem("artist|$artistName", artistName, MediaMetadata.MEDIA_TYPE_ARTIST))
                    }
                }
                parentId.startsWith("artist|") -> {
                    val artistName = parentId.removePrefix("artist|")
                    repository.songsState.value.filter { it.artist == artistName }.forEach { song: com.example.core.model.Song ->
                        items.add(mapSongToMediaItem(song))
                    }
                }
                parentId == CATEGORY_PLAYLISTS -> {
                    items.add(createFolderItem("playlist|favorites", "Mis Favoritos", MediaMetadata.MEDIA_TYPE_PLAYLIST))
                    repository.playlistsState.value.forEach { playlist ->
                        items.add(createFolderItem("playlist|${playlist.id}", playlist.name, MediaMetadata.MEDIA_TYPE_PLAYLIST))
                    }
                }
                parentId == "playlist|favorites" -> {
                    repository.songsState.value.filter { it.isFavorite }.forEach { song ->
                        items.add(mapSongToMediaItem(song))
                    }
                }
                parentId.startsWith("playlist|") -> {
                    // This one is harder because we need to fetch songs for the playlist ID.
                    // For now, let's keep it simple or implement a quick lookup if the repository allows it.
                }
            }
            return Futures.immediateFuture(LibraryResult.ofItemList(items, params))
        }

        private fun createFolderItem(id: String, title: String, mediaType: @MediaMetadata.MediaType Int): MediaItem {
            return MediaItem.Builder()
                .setMediaId(id)
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        .setTitle(title)
                        .setIsBrowsable(true)
                        .setIsPlayable(false)
                        .setMediaType(mediaType)
                        .build()
                )
                .build()
        }

        private fun mapSongToMediaItem(song: com.example.core.model.Song): MediaItem {
            val metadata = MediaMetadata.Builder()
                .setTitle(song.title)
                .setArtist(song.artist)
                .setAlbumTitle(song.album)
                .setArtworkUri(song.artworkUri?.let { android.net.Uri.parse(it) })
                .setIsBrowsable(false)
                .setIsPlayable(true)
                .setMediaType(MediaMetadata.MEDIA_TYPE_MUSIC)
                .build()

            return MediaItem.Builder()
                .setMediaId(song.id)
                .setUri(song.mediaUri)
                .setMediaMetadata(metadata)
                .build()
        }
    }
}
