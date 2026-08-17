package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "favorites")
data class FavoriteEntity(
    @PrimaryKey
    val songId: String,
    val addedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "playlists")
data class PlaylistEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val name: String,
    val description: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val isSmart: Boolean = false
)

@Entity(
    tableName = "playlist_items",
    primaryKeys = ["playlistId", "songId"]
)
data class PlaylistItemEntity(
    val playlistId: Long,
    val songId: String,
    val orderIndex: Int = 0,
    val addedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "playback_history")
data class HistoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val songId: String,
    val playedAt: Long = System.currentTimeMillis(),
    val durationPlayedMs: Long = 0L
)

@Entity(tableName = "downloaded_songs")
data class DownloadedSongEntity(
    @PrimaryKey
    val id: String,
    val title: String,
    val artist: String,
    val album: String,
    val durationMs: Long,
    val localMediaUri: String,
    val localArtworkUri: String? = null,
    val originalThumbnailUrl: String? = null,
    val sizeBytes: Long = 0L,
    val downloadedAt: Long = System.currentTimeMillis(),
    val source: String = "YOUTUBE"
)

