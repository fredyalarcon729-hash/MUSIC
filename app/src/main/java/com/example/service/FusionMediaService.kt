package com.example.service

import android.app.PendingIntent
import android.content.Intent
import android.os.Bundle
import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.LibraryResult
import androidx.media3.session.MediaLibraryService
import androidx.media3.session.MediaSession
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionResult
import com.example.MainActivity
import com.example.player.FusionPlayerManager
import com.google.common.collect.ImmutableList
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture

class FusionMediaService : MediaLibraryService() {

    private var mediaLibrarySession: MediaLibrarySession? = null
    private lateinit var playerManager: FusionPlayerManager

    companion object {
        const val ROOT_ID = "root"
        const val CATEGORY_SONGS = "category_songs"
        const val CATEGORY_ARTISTS = "category_artists"
        const val CATEGORY_ALBUMS = "category_albums"
        const val CATEGORY_PLAYLISTS = "category_playlists"
    }

    @OptIn(UnstableApi::class)
    override fun onCreate() {
        super.onCreate()
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

        mediaLibrarySession = MediaLibrarySession.Builder(this, playerManager.exoPlayer, LibraryCallback())
            .setSessionActivity(pendingIntent)
            .build()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaLibrarySession? {
        return mediaLibrarySession
    }

    override fun onDestroy() {
        mediaLibrarySession?.run {
            player.release()
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
