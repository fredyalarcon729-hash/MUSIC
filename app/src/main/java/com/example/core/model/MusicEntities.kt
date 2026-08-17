package com.example.core.model

data class Album(
    val id: Long,
    val title: String,
    val artist: String,
    val artworkUri: String? = null,
    val songCount: Int = 0,
    val year: Int = 0
)

data class Artist(
    val id: Long,
    val name: String,
    val songCount: Int = 0,
    val albumCount: Int = 0,
    val artworkUri: String? = null
)

data class Playlist(
    val id: Long,
    val name: String,
    val description: String = "",
    val coverUri: String? = null,
    val songCount: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val isSmart: Boolean = false,
    val songs: List<Song> = emptyList()
)
