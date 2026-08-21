package com.example.service

import android.app.PendingIntent
import android.content.Intent
import android.util.Log
import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.DefaultMediaNotificationProvider
import androidx.media3.session.LibraryResult
import androidx.media3.session.MediaLibraryService
import androidx.media3.session.MediaSession
import com.example.MainActivity
import com.example.R
import com.example.player.FusionPlayerManager
import com.google.common.collect.ImmutableList
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture

class FusionMediaService : MediaLibraryService() {

    private var mediaLibrarySession: MediaLibrarySession? = null
    private lateinit var playerManager: FusionPlayerManager

    companion object {
        private const val SERVICE_TAG = "FusionMediaService"
        const val NOTIFICATION_ID = 1001
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
        Log.d(SERVICE_TAG, "onCreate: Starting MediaLibraryService")
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

        val session = MediaLibrarySession.Builder(this, playerManager.exoPlayer, LibraryCallback())
            .setSessionActivity(pendingIntent)
            .build()
        
        mediaLibrarySession = session

        setMediaNotificationProvider(DefaultMediaNotificationProvider.Builder(this)
            .setChannelId(CHANNEL_ID)
            .setChannelName(R.string.playback_channel_name)
            .build())
        
        Log.d(SERVICE_TAG, "onCreate: Session created and notification provider set")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(SERVICE_TAG, "onStartCommand: Received intent: ${intent?.action}")
        super.onStartCommand(intent, flags, startId)
        return START_STICKY
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaLibrarySession? {
        Log.d(SERVICE_TAG, "onGetSession: Controller connected: ${controllerInfo.packageName}")
        return mediaLibrarySession
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        val player = mediaLibrarySession?.player
        if (player == null || !player.playWhenReady || player.mediaItemCount == 0) {
            stopSelf()
        }
    }

    override fun onDestroy() {
        mediaLibrarySession?.run {
            release()
            mediaLibrarySession = null
        }
        super.onDestroy()
    }

    private inner class LibraryCallback : MediaLibrarySession.Callback {

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
            val items = mutableListOf<MediaItem>()

            when (parentId) {
                ROOT_ID -> {
                    items.add(createFolderItem(CATEGORY_SONGS, "Todas las Canciones"))
                    items.add(createFolderItem(CATEGORY_ARTISTS, "Artistas"))
                    items.add(createFolderItem(CATEGORY_ALBUMS, "Álbumes"))
                    items.add(createFolderItem(CATEGORY_PLAYLISTS, "Listas de Reproducción"))
                }
                CATEGORY_SONGS -> {
                    val queue = playerManager.uiState.value.queue
                    queue.forEach { song ->
                        items.add(
                            MediaItem.Builder()
                                .setMediaId(song.id)
                                .setUri(song.mediaUri)
                                .setMediaMetadata(
                                    MediaMetadata.Builder()
                                        .setTitle(song.title)
                                        .setArtist(song.artist)
                                        .setAlbumTitle(song.album)
                                        .setIsPlayable(true)
                                        .setIsBrowsable(false)
                                        .build()
                                )
                                .build()
                        )
                    }
                }
            }

            return Futures.immediateFuture(LibraryResult.ofItemList(items, params))
        }

        private fun createFolderItem(id: String, title: String): MediaItem {
            return MediaItem.Builder()
                .setMediaId(id)
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        .setTitle(title)
                        .setIsBrowsable(true)
                        .setIsPlayable(false)
                        .setMediaType(MediaMetadata.MEDIA_TYPE_FOLDER_MIXED)
                        .build()
                )
                .build()
        }
    }
}
