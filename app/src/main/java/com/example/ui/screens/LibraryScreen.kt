package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.DownloadDone
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.OfflinePin
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.model.Album
import com.example.core.model.Artist
import com.example.core.model.DownloadStatus
import com.example.core.model.PlayerUiState
import com.example.core.model.Song
import com.example.ui.components.FusionArtwork
import com.example.ui.components.SongListItem
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonGreen
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    songs: List<Song>,
    downloadedSongs: List<Song> = emptyList(),
    totalStorageBytes: Long = 0L,
    albums: List<Album>,
    artists: List<Artist>,
    playerState: PlayerUiState,
    downloadStates: Map<String, DownloadStatus> = emptyMap(),
    onPlaySong: (Song, List<Song>) -> Unit,
    onPlayAll: (List<Song>, Boolean) -> Unit,
    onToggleFavorite: (Song) -> Unit,
    onPlayNext: (Song) -> Unit,
    onAddToQueue: (Song) -> Unit,
    onAddToPlaylist: (Song) -> Unit,
    onDeleteDownload: ((Song) -> Unit)? = null,
    onNavigateToSearch: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf(
        "Canciones (${songs.size})",
        "Descargas (${downloadedSongs.size})",
        "Álbumes (${albums.size})",
        "Artistas (${artists.size})"
    )

    var selectedAlbumForDetail by remember { mutableStateOf<Album?>(null) }
    var selectedArtistForDetail by remember { mutableStateOf<Artist?>(null) }

    val formattedTotalStorage = remember(totalStorageBytes) {
        val mb = totalStorageBytes.toDouble() / (1024 * 1024)
        if (mb >= 1.0) "%.1f MB".format(mb) else "%.0f KB".format(totalStorageBytes.toDouble() / 1024)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(ObsidianDark)
    ) {
        // Title
        Text(
            text = "Mi Biblioteca",
            style = MaterialTheme.typography.displayMedium.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 24.sp
            ),
            color = TextPrimary,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)
        )

        // Tab Selector
        PrimaryTabRow(
            selectedTabIndex = selectedTab,
            containerColor = ObsidianDark,
            contentColor = NeonCyan,
            divider = { Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(ObsidianBorder)) }
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = {
                        Text(
                            text = title,
                            fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal,
                            color = if (selectedTab == index) NeonCyan else TextSecondary,
                            fontSize = 12.sp
                        )
                    }
                )
            }
        }

        // Tab Content
        when (selectedTab) {
            0 -> {
                // Songs Tab
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(top = 12.dp, bottom = 120.dp)
                ) {
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Button(
                                onClick = { onPlayAll(songs, false) },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = NeonPurpleLight,
                                    contentColor = Color.Black
                                ),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.PlayArrow, null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Reproducir Todo", fontWeight = FontWeight.Bold)
                            }

                            Button(
                                onClick = { onPlayAll(songs, true) },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = ObsidianCard,
                                    contentColor = NeonCyan
                                ),
                                border = androidx.compose.foundation.BorderStroke(1.dp, NeonCyan.copy(alpha = 0.4f)),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.Shuffle, null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Aleatorio")
                            }
                        }
                    }

                    items(songs, key = { it.id }) { song ->
                        SongListItem(
                            song = song,
                            isPlaying = playerState.isPlaying && playerState.currentSong?.id == song.id,
                            isCurrent = playerState.currentSong?.id == song.id,
                            onClick = { onPlaySong(song, songs) },
                            onToggleFavorite = { onToggleFavorite(song) },
                            onPlayNext = { onPlayNext(song) },
                            onAddToQueue = { onAddToQueue(song) },
                            onAddToPlaylist = { onAddToPlaylist(song) },
                            downloadStatus = downloadStates[song.id],
                            onDeleteDownload = if (song.isDownloaded && onDeleteDownload != null) { { onDeleteDownload(song) } } else null,
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )
                    }
                }
            }

            1 -> {
                // Descargas (Downloads) Tab
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(top = 12.dp, bottom = 120.dp)
                ) {
                    item {
                        // Downloads Header Summary Banner
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = ObsidianSurfaceVariant,
                            border = androidx.compose.foundation.BorderStroke(1.dp, NeonCyan.copy(alpha = 0.3f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 6.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(38.dp)
                                                .clip(RoundedCornerShape(10.dp))
                                                .background(NeonCyan.copy(alpha = 0.15f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.OfflinePin,
                                                contentDescription = null,
                                                tint = NeonCyan,
                                                modifier = Modifier.size(22.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column {
                                            Text(
                                                text = "Música Offline Guardada",
                                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                                color = TextPrimary
                                            )
                                            Text(
                                                text = "${downloadedSongs.size} canciones descargadas • $formattedTotalStorage ocupados",
                                                style = MaterialTheme.typography.bodyMedium.copy(color = TextSecondary, fontSize = 12.sp)
                                            )
                                        }
                                    }
                                }

                                if (downloadedSongs.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(14.dp))
                                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                        Button(
                                            onClick = { onPlayAll(downloadedSongs, false) },
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = NeonCyan,
                                                contentColor = Color.Black
                                            ),
                                            shape = RoundedCornerShape(10.dp),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Icon(Icons.Default.PlayArrow, null, modifier = Modifier.size(18.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("Reproducir Offline", fontWeight = FontWeight.Bold)
                                        }

                                        Button(
                                            onClick = { onPlayAll(downloadedSongs, true) },
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = ObsidianCard,
                                                contentColor = NeonPurpleLight
                                            ),
                                            border = androidx.compose.foundation.BorderStroke(1.dp, NeonPurpleLight.copy(alpha = 0.5f)),
                                            shape = RoundedCornerShape(10.dp),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Icon(Icons.Default.Shuffle, null, modifier = Modifier.size(18.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("Aleatorio")
                                        }
                                    }
                                }
                            }
                        }
                    }

                    if (downloadedSongs.isEmpty()) {
                        item {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 24.dp, vertical = 40.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(64.dp)
                                        .clip(CircleShape)
                                        .background(ObsidianSurfaceVariant),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CloudDownload,
                                        contentDescription = null,
                                        tint = NeonCyan.copy(alpha = 0.7f),
                                        modifier = Modifier.size(32.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = "Sin descargas offline",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = TextPrimary
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Busca tus canciones favoritas en YouTube Music y presiona «Descargar» para reproducirlas en cualquier lugar sin conexión a internet.",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        color = TextTertiary,
                                        textAlign = TextAlign.Center,
                                        lineHeight = 20.sp
                                    )
                                )
                                Spacer(modifier = Modifier.height(20.dp))
                                if (onNavigateToSearch != null) {
                                    Button(
                                        onClick = onNavigateToSearch,
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = NeonPurpleLight,
                                            contentColor = Color.Black
                                        ),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Icon(Icons.Default.Search, null, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Buscar en YouTube", fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    } else {
                        items(downloadedSongs, key = { it.id }) { song ->
                            SongListItem(
                                song = song,
                                isPlaying = playerState.isPlaying && playerState.currentSong?.id == song.id,
                                isCurrent = playerState.currentSong?.id == song.id,
                                onClick = { onPlaySong(song, downloadedSongs) },
                                onToggleFavorite = { onToggleFavorite(song) },
                                onPlayNext = { onPlayNext(song) },
                                onAddToQueue = { onAddToQueue(song) },
                                onAddToPlaylist = { onAddToPlaylist(song) },
                                downloadStatus = downloadStates[song.id],
                                onDeleteDownload = if (onDeleteDownload != null) { { onDeleteDownload(song) } } else null,
                                modifier = Modifier.padding(horizontal = 8.dp)
                            )
                        }
                    }
                }
            }

            2 -> {
                // Albums Tab (Grid)
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(albums, key = { it.id }) { album ->
                        Surface(
                            onClick = { selectedAlbumForDetail = album },
                            shape = RoundedCornerShape(14.dp),
                            color = ObsidianSurface,
                            border = androidx.compose.foundation.BorderStroke(1.dp, ObsidianBorder)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                FusionArtwork(
                                    artworkUri = album.artworkUri,
                                    size = 140.dp,
                                    cornerRadius = 12.dp,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = album.title,
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    ),
                                    color = TextPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = album.artist,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        color = TextSecondary,
                                        fontSize = 12.sp
                                    ),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "${album.songCount} canciones",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        color = NeonCyan,
                                        fontSize = 11.sp
                                    )
                                )
                            }
                        }
                    }
                }
            }

            3 -> {
                // Artists Tab
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(top = 12.dp, bottom = 120.dp)
                ) {
                    items(artists, key = { it.id }) { artist ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedArtistForDetail = artist }
                                .padding(horizontal = 16.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(52.dp)
                                    .clip(CircleShape)
                                    .background(NeonPurpleLight.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = null,
                                    tint = NeonCyan,
                                    modifier = Modifier.size(28.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(16.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = artist.name,
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp
                                    ),
                                    color = TextPrimary
                                )
                                Text(
                                    text = "${artist.songCount} canciones • ${artist.albumCount} álbumes",
                                    style = MaterialTheme.typography.bodyMedium.copy(color = TextSecondary),
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Album Detail Modal
    selectedAlbumForDetail?.let { album ->
        val albumSongs = songs.filter { it.album == album.title }
        ModalBottomSheet(
            onDismissRequest = { selectedAlbumForDetail = null },
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
                    FusionArtwork(artworkUri = album.artworkUri, size = 70.dp, cornerRadius = 12.dp)
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = album.title,
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = TextPrimary
                        )
                        Text(
                            text = album.artist,
                            style = MaterialTheme.typography.bodyMedium.copy(color = NeonCyan)
                        )
                        Text(
                            text = "${albumSongs.size} pistas",
                            style = MaterialTheme.typography.bodyMedium.copy(color = TextSecondary),
                            fontSize = 11.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(
                        onClick = {
                            onPlayAll(albumSongs, false)
                            selectedAlbumForDetail = null
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
                            onPlayAll(albumSongs, true)
                            selectedAlbumForDetail = null
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
                    items(albumSongs) { song ->
                        SongListItem(
                            song = song,
                            isPlaying = playerState.isPlaying && playerState.currentSong?.id == song.id,
                            isCurrent = playerState.currentSong?.id == song.id,
                            onClick = {
                                onPlaySong(song, albumSongs)
                                selectedAlbumForDetail = null
                            },
                            onToggleFavorite = { onToggleFavorite(song) },
                            onPlayNext = { onPlayNext(song) },
                            onAddToQueue = { onAddToQueue(song) },
                            onAddToPlaylist = { onAddToPlaylist(song) },
                            downloadStatus = downloadStates[song.id],
                            onDeleteDownload = if (song.isDownloaded && onDeleteDownload != null) { { onDeleteDownload(song) } } else null
                        )
                    }
                }
            }
        }
    }

    // Artist Detail Modal
    selectedArtistForDetail?.let { artist ->
        val artistSongs = songs.filter { it.artist == artist.name }
        ModalBottomSheet(
            onDismissRequest = { selectedArtistForDetail = null },
            containerColor = ObsidianDark
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 32.dp)
            ) {
                Text(
                    text = artist.name,
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = TextPrimary
                )
                Text(
                    text = "${artistSongs.size} canciones en tu biblioteca",
                    style = MaterialTheme.typography.bodyMedium.copy(color = NeonCyan)
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(
                        onClick = {
                            onPlayAll(artistSongs, false)
                            selectedArtistForDetail = null
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = NeonPurpleLight, contentColor = Color.Black),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.PlayArrow, null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Reproducir Todo")
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                LazyColumn(modifier = Modifier.height(300.dp)) {
                    items(artistSongs) { song ->
                        SongListItem(
                            song = song,
                            isPlaying = playerState.isPlaying && playerState.currentSong?.id == song.id,
                            isCurrent = playerState.currentSong?.id == song.id,
                            onClick = {
                                onPlaySong(song, artistSongs)
                                selectedArtistForDetail = null
                            },
                            onToggleFavorite = { onToggleFavorite(song) },
                            onPlayNext = { onPlayNext(song) },
                            onAddToQueue = { onAddToQueue(song) },
                            onAddToPlaylist = { onAddToPlaylist(song) },
                            downloadStatus = downloadStates[song.id],
                            onDeleteDownload = if (song.isDownloaded && onDeleteDownload != null) { { onDeleteDownload(song) } } else null
                        )
                    }
                }
            }
        }
    }
}
