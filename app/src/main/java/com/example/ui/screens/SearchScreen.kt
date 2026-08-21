package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.model.DownloadStatus
import com.example.core.model.PlayerUiState
import com.example.core.model.Song
import com.example.ui.SearchFilter
import com.example.ui.SearchUiResult
import com.example.ui.components.FusionArtwork
import com.example.ui.components.SongListItem
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonPurpleLight
import com.example.ui.theme.ObsidianBorder
import com.example.ui.theme.ObsidianDark
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.ObsidianSurfaceVariant
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextTertiary

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SearchScreen(
    query: String,
    selectedFilter: SearchFilter,
    searchResults: SearchUiResult,
    playerState: PlayerUiState,
    allSongs: List<Song>,
    isSearchingYouTube: Boolean = false,
    downloadStates: Map<String, DownloadStatus> = emptyMap(),
    searchHistory: List<String> = emptyList(),
    onQueryChanged: (String) -> Unit,
    onFilterChanged: (SearchFilter) -> Unit,
    onPlaySong: (Song, List<Song>) -> Unit,
    onToggleFavorite: (Song) -> Unit,
    onPlayNext: (Song) -> Unit,
    onAddToQueue: (Song) -> Unit,
    onAddToPlaylist: (Song) -> Unit,
    onClearHistory: () -> Unit = {},
    onDownloadSong: ((Song) -> Unit)? = null,
    onCancelDownload: ((String) -> Unit)? = null,
    onDeleteDownload: ((Song) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(ObsidianDark)
            .statusBarsPadding()
            .padding(top = 8.dp)
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Buscar",
                style = MaterialTheme.typography.displayMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 24.sp
                ),
                color = TextPrimary
            )

            if (isSearchingYouTube) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(14.dp),
                        strokeWidth = 2.dp,
                        color = Color(0xFFFF4D4D)
                    )
                    Text(
                        text = "Buscando en YouTube...",
                        color = Color(0xFFFF4D4D),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Search Input Bar
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChanged,
            placeholder = { Text("Canciones, artistas, YouTube...", color = TextTertiary) },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = null,
                    tint = if (query.isNotBlank()) NeonCyan else TextTertiary
                )
            },
            trailingIcon = {
                if (query.isNotEmpty()) {
                    IconButton(
                        onClick = { onQueryChanged("") },
                        modifier = Modifier.testTag("clear_search_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Clear,
                            contentDescription = "Limpiar búsqueda",
                            tint = TextSecondary
                        )
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = ObsidianSurfaceVariant,
                unfocusedContainerColor = ObsidianSurface,
                focusedBorderColor = NeonCyan,
                unfocusedBorderColor = ObsidianBorder,
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .testTag("search_input_field")
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Filter Chips Row
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(SearchFilter.values()) { filter ->
                val isSelected = selectedFilter == filter
                val chipColor = if (filter == SearchFilter.YOUTUBE) Color(0xFFFF4D4D) else NeonPurpleLight

                FilterChip(
                    selected = isSelected,
                    onClick = { onFilterChanged(filter) },
                    label = { Text(filter.label) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = chipColor,
                        selectedLabelColor = Color.Black,
                        containerColor = ObsidianSurface,
                        labelColor = TextSecondary
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = isSelected,
                        borderColor = if (isSelected) chipColor else ObsidianBorder
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Content / Results
        if (query.isBlank()) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                if (searchHistory.isNotEmpty()) {
                    item(key = "history_header") {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Búsquedas Recientes",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = TextPrimary
                            )
                            Text(
                                text = "Limpiar",
                                style = MaterialTheme.typography.labelMedium,
                                color = NeonCyan,
                                modifier = Modifier.clickable { onClearHistory() }
                            )
                        }
                    }

                    item(key = "history_flow") {
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            searchHistory.forEach { historyQuery ->
                                Surface(
                                    onClick = { onQueryChanged(historyQuery) },
                                    shape = RoundedCornerShape(20.dp),
                                    color = ObsidianSurface,
                                    border = androidx.compose.foundation.BorderStroke(1.dp, ObsidianBorder)
                                ) {
                                    Text(
                                        text = historyQuery,
                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = TextSecondary
                                    )
                                }
                            }
                        }
                    }
                }

                item(key = "categories_header") {
                    Text(
                        text = "Explora Categorías",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = TextPrimary
                    )
                }

                item(key = "categories_tags") {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        listOf("Electrónica", "Lo-Fi Beats", "Synthwave", "Rock").forEach { tag ->
                            Surface(
                                onClick = { onQueryChanged(tag) },
                                shape = RoundedCornerShape(12.dp),
                                color = ObsidianSurfaceVariant,
                                border = androidx.compose.foundation.BorderStroke(1.dp, ObsidianBorder),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = tag,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Medium,
                                        color = NeonCyan,
                                        textAlign = TextAlign.Center
                                    ),
                                    modifier = Modifier.padding(vertical = 12.dp)
                                )
                            }
                        }
                    }
                }

                item(key = "search_footer") {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Busca cualquier canción en tu teléfono o en YouTube Music.\nPuedes escuchar online o descargar para reproducir offline sin conexión.",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = TextTertiary,
                                textAlign = TextAlign.Center,
                                lineHeight = 20.sp
                            )
                        )
                    }
                }
            }
        } else {
            val hasResults = searchResults.songs.isNotEmpty() ||
                    searchResults.youTubeSongs.isNotEmpty() ||
                    searchResults.artists.isNotEmpty() ||
                    searchResults.albums.isNotEmpty()

            if (!hasResults && !isSearchingYouTube) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No se encontraron resultados para «$query»",
                        style = MaterialTheme.typography.bodyLarge.copy(color = TextSecondary),
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 120.dp)
                ) {
                    // 1. Local Songs matches
                    if (searchResults.songs.isNotEmpty()) {
                        item(key = "local_songs_header") {
                            Text(
                                text = "Biblioteca Local (${searchResults.songs.size})",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = NeonCyan
                                ),
                                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                            )
                        }
                        items(searchResults.songs, key = { it.id }) { song ->
                            SongListItem(
                                song = song,
                                isPlaying = playerState.isPlaying && playerState.currentSong?.id == song.id,
                                isCurrent = playerState.currentSong?.id == song.id,
                                onClick = { onPlaySong(song, searchResults.songs) },
                                onToggleFavorite = { onToggleFavorite(song) },
                                onPlayNext = { onPlayNext(song) },
                                onAddToQueue = { onAddToQueue(song) },
                                onAddToPlaylist = { onAddToPlaylist(song) },
                                downloadStatus = downloadStates[song.id],
                                onDownload = if (onDownloadSong != null) { { onDownloadSong(song) } } else null,
                                onCancelDownload = if (onCancelDownload != null) { { onCancelDownload(song.id) } } else null,
                                onDeleteDownload = if (onDeleteDownload != null) { { onDeleteDownload(song) } } else null,
                                modifier = Modifier.padding(horizontal = 8.dp)
                            )
                        }
                    }

                    // 2. YouTube Online Songs
                    if (searchResults.youTubeSongs.isNotEmpty()) {
                        item(key = "yt_songs_header") {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 20.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(10.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFFFF0000))
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "YouTube Music Online (${searchResults.youTubeSongs.size})",
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFFFF6666)
                                        )
                                    )
                                }

                                Text(
                                    text = "Reproducir / Descargar",
                                    fontSize = 11.sp,
                                    color = TextTertiary
                                )
                            }
                        }

                        items(searchResults.youTubeSongs, key = { it.id }) { song ->
                            SongListItem(
                                song = song,
                                isPlaying = playerState.isPlaying && playerState.currentSong?.id == song.id,
                                isCurrent = playerState.currentSong?.id == song.id,
                                onClick = { onPlaySong(song, searchResults.youTubeSongs) },
                                onToggleFavorite = { onToggleFavorite(song) },
                                onPlayNext = { onPlayNext(song) },
                                onAddToQueue = { onAddToQueue(song) },
                                onAddToPlaylist = { onAddToPlaylist(song) },
                                downloadStatus = downloadStates[song.id],
                                onDownload = if (onDownloadSong != null) { { onDownloadSong(song) } } else null,
                                onCancelDownload = if (onCancelDownload != null) { { onCancelDownload(song.id) } } else null,
                                onDeleteDownload = if (onDeleteDownload != null) { { onDeleteDownload(song) } } else null,
                                modifier = Modifier.padding(horizontal = 8.dp)
                            )
                        }
                    }

                    // 3. Artists matches
                    if (searchResults.artists.isNotEmpty()) {
                        item(key = "artists_header") {
                            Text(
                                text = "Artistas (${searchResults.artists.size})",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = NeonCyan
                                ),
                                modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)
                            )
                        }
                        items(searchResults.artists, key = { it.id }) { artist ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onQueryChanged(artist.name) }
                                    .padding(horizontal = 16.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(NeonPurpleLight.copy(alpha = 0.2f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.Person, null, tint = NeonCyan, modifier = Modifier.size(22.dp))
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(artist.name, style = MaterialTheme.typography.titleMedium.copy(color = TextPrimary))
                                    Text("${artist.songCount} canciones", style = MaterialTheme.typography.bodyMedium.copy(color = TextSecondary, fontSize = 11.sp))
                                }
                            }
                        }
                    }

                    // 4. Albums matches
                    if (searchResults.albums.isNotEmpty()) {
                        item(key = "albums_header") {
                            Text(
                                text = "Álbumes (${searchResults.albums.size})",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = NeonPurpleLight
                                ),
                                modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)
                            )
                        }
                        items(searchResults.albums, key = { it.id }) { album ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onQueryChanged(album.title) }
                                    .padding(horizontal = 16.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                FusionArtwork(artworkUri = album.artworkUri, size = 44.dp, cornerRadius = 8.dp)
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(album.title, style = MaterialTheme.typography.titleMedium.copy(color = TextPrimary))
                                    Text("${album.artist} • ${album.songCount} pistas", style = MaterialTheme.typography.bodyMedium.copy(color = TextSecondary, fontSize = 11.sp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
