package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlaylistPlay
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.model.PlayerUiState
import com.example.core.model.Playlist
import com.example.core.model.Song
import com.example.ui.components.SongListItem
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonPink
import com.example.ui.theme.NeonPurpleLight
import com.example.ui.theme.ObsidianBorder
import com.example.ui.theme.ObsidianCard
import com.example.ui.theme.ObsidianDark
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.ObsidianSurfaceVariant
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextTertiary
import kotlinx.coroutines.flow.Flow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaylistsScreen(
    playlists: List<Playlist>,
    favoriteSongs: List<Song>,
    playerState: PlayerUiState,
    onOpenCreateDialog: () -> Unit,
    onDeletePlaylist: (Long) -> Unit,
    onPlaySong: (Song, List<Song>) -> Unit,
    onPlayAll: (List<Song>, Boolean) -> Unit,
    onToggleFavorite: (Song) -> Unit,
    onPlayNext: (Song) -> Unit,
    onAddToQueue: (Song) -> Unit,
    onAddToPlaylist: (Song) -> Unit,
    onRemoveFromPlaylist: (playlistId: Long, songId: String) -> Unit,
    getSongsForPlaylist: (Long) -> Flow<List<Song>>,
    modifier: Modifier = Modifier
) {
    var selectedPlaylist by remember { mutableStateOf<Playlist?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(ObsidianDark)
            .padding(top = 16.dp)
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Playlists",
                style = MaterialTheme.typography.displayMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 24.sp
                ),
                color = TextPrimary
            )

            Button(
                onClick = onOpenCreateDialog,
                colors = ButtonDefaults.buttonColors(
                    containerColor = NeonPurpleLight,
                    contentColor = Color.Black
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.testTag("create_playlist_btn")
            ) {
                Icon(Icons.Default.Add, null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Crear", fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 120.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Smart Playlist: Favoritos
            item {
                Surface(
                    onClick = {
                        selectedPlaylist = Playlist(
                            id = -1L,
                            name = "Mis Favoritos",
                            description = "Tus canciones marcadas con corazón",
                            isSmart = true
                        )
                    },
                    shape = RoundedCornerShape(14.dp),
                    color = ObsidianSurface,
                    border = androidx.compose.foundation.BorderStroke(1.dp, NeonPink.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(50.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(NeonPink.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Favorite,
                                contentDescription = null,
                                tint = NeonPink,
                                modifier = Modifier.size(28.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Mis Favoritos",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = TextPrimary
                            )
                            Text(
                                text = "${favoriteSongs.size} canciones guardadas",
                                style = MaterialTheme.typography.bodyMedium.copy(color = TextSecondary),
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }

            // User Playlists
            items(playlists, key = { it.id }) { playlist ->
                Surface(
                    onClick = { selectedPlaylist = playlist },
                    shape = RoundedCornerShape(14.dp),
                    color = ObsidianSurface,
                    border = androidx.compose.foundation.BorderStroke(1.dp, ObsidianBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(50.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(NeonPurpleLight.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlaylistPlay,
                                contentDescription = null,
                                tint = NeonCyan,
                                modifier = Modifier.size(30.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = playlist.name,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = TextPrimary
                            )
                            Text(
                                text = playlist.description.ifBlank { "Playlist personalizada" },
                                style = MaterialTheme.typography.bodyMedium.copy(color = TextSecondary),
                                fontSize = 12.sp
                            )
                        }

                        IconButton(
                            onClick = { onDeletePlaylist(playlist.id) },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Eliminar playlist",
                                tint = TextTertiary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }
    }

    // Playlist Detail Bottom Sheet
    selectedPlaylist?.let { playlist ->
        val isSmart = playlist.isSmart
        val songsFlow = if (isSmart) null else getSongsForPlaylist(playlist.id)
        val playlistSongs = if (isSmart) {
            favoriteSongs
        } else {
            val listState by songsFlow!!.collectAsState(initial = emptyList())
            listState
        }

        ModalBottomSheet(
            onDismissRequest = { selectedPlaylist = null },
            containerColor = ObsidianDark
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 32.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .size(60.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isSmart) NeonPink.copy(alpha = 0.2f) else NeonPurpleLight.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isSmart) Icons.Default.Favorite else Icons.Default.PlaylistPlay,
                            contentDescription = null,
                            tint = if (isSmart) NeonPink else NeonCyan,
                            modifier = Modifier.size(32.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = playlist.name,
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = TextPrimary
                        )
                        Text(
                            text = "${playlistSongs.size} canciones",
                            style = MaterialTheme.typography.bodyMedium.copy(color = NeonCyan),
                            fontSize = 12.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (playlistSongs.isNotEmpty()) {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(
                            onClick = {
                                onPlayAll(playlistSongs, false)
                                selectedPlaylist = null
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = NeonPurpleLight, contentColor = Color.Black),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.PlayArrow, null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Reproducir")
                        }

                        Button(
                            onClick = {
                                onPlayAll(playlistSongs, true)
                                selectedPlaylist = null
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = ObsidianCard, contentColor = NeonCyan),
                            border = androidx.compose.foundation.BorderStroke(1.dp, NeonCyan),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Shuffle, null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Aleatorio")
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    LazyColumn(modifier = Modifier.height(300.dp)) {
                        items(playlistSongs) { song ->
                            SongListItem(
                                song = song,
                                isPlaying = playerState.isPlaying && playerState.currentSong?.id == song.id,
                                isCurrent = playerState.currentSong?.id == song.id,
                                onClick = {
                                    onPlaySong(song, playlistSongs)
                                    selectedPlaylist = null
                                },
                                onToggleFavorite = { onToggleFavorite(song) },
                                onPlayNext = { onPlayNext(song) },
                                onAddToQueue = { onAddToQueue(song) },
                                onAddToPlaylist = { onAddToPlaylist(song) }
                            )
                        }
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(150.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Esta lista está vacía.\nAñade canciones desde tu biblioteca.",
                            style = MaterialTheme.typography.bodyMedium.copy(color = TextSecondary),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}
