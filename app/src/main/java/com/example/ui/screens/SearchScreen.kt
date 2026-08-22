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
import com.example.ui.viewmodels.SearchFilter
import com.example.ui.viewmodels.SearchUiResult
import com.example.ui.components.FusionArtwork
import com.example.ui.components.SongListItem
import com.example.ui.theme.*

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SearchScreen(
    query: String,
    selectedFilter: SearchFilter,
    searchResults: SearchUiResult,
    playerState: PlayerUiState,
    allSongs: List<Song>,
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
            .background(MaterialTheme.colorScheme.background)
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
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Search Input Bar
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChanged,
            placeholder = { Text("Canciones, artistas, Deezer...", color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)) },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = null,
                    tint = if (query.isNotBlank()) NeonCyan else MaterialTheme.colorScheme.onSurfaceVariant
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
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(24.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = Color.White.copy(alpha = 0.05f),
                unfocusedContainerColor = Color.White.copy(alpha = 0.03f),
                focusedBorderColor = NeonCyan.copy(alpha = 0.5f),
                unfocusedBorderColor = Color.White.copy(alpha = 0.1f),
                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                unfocusedTextColor = MaterialTheme.colorScheme.onSurface
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
                val chipColor = if (filter == SearchFilter.DEEZER) Color(0xFFFF007F) else NeonPurpleLight

                FilterChip(
                    selected = isSelected,
                    onClick = { onFilterChanged(filter) },
                    label = { Text(filter.label) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = chipColor,
                        selectedLabelColor = if (filter == SearchFilter.DEEZER) Color.White else Color.Black,
                        containerColor = MaterialTheme.colorScheme.surface,
                        labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = isSelected,
                        borderColor = if (isSelected) chipColor else MaterialTheme.colorScheme.outline
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
                                color = MaterialTheme.colorScheme.onBackground
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
                                    color = MaterialTheme.colorScheme.surface,
                                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                                ) {
                                    Text(
                                        text = historyQuery,
                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
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
                        color = MaterialTheme.colorScheme.onBackground
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
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
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
                            text = "Busca cualquier canción en tu teléfono o en el catálogo global.\nPuedes escuchar online con máxima estabilidad vía Deezer.",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                                lineHeight = 20.sp
                            )
                        )
                    }
                }
            }
        } else {
            val hasResults = searchResults.songs.isNotEmpty() ||
                    searchResults.deezerSongs.isNotEmpty() ||
                    searchResults.artists.isNotEmpty() ||
                    searchResults.albums.isNotEmpty()

            if (!hasResults) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No se encontraron resultados para «$query»",
                        style = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurfaceVariant),
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
                                onDeleteDownload = if (song.isDownloaded && onDeleteDownload != null) { { onDeleteDownload(song) } } else null,
                                modifier = Modifier.padding(horizontal = 8.dp)
                            )
                        }
                    }

                    // 2. Deezer Online Songs (Priority over others)
                    if (searchResults.deezerSongs.isNotEmpty()) {
                        item(key = "dz_songs_header") {
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
                                            .background(Color(0xFFFF007F))
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Deezer Hi-Fi Online (${searchResults.deezerSongs.size})",
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFFFF4D94)
                                        )
                                    )
                                }
                            }
                        }

                        items(searchResults.deezerSongs, key = { it.id }) { song ->
                            SongListItem(
                                song = song,
                                isPlaying = playerState.isPlaying && playerState.currentSong?.id == song.id,
                                isCurrent = playerState.currentSong?.id == song.id,
                                onClick = { onPlaySong(song, searchResults.deezerSongs) },
                                onToggleFavorite = { onToggleFavorite(song) },
                                onPlayNext = { onPlayNext(song) },
                                onAddToQueue = { onAddToQueue(song) },
                                onAddToPlaylist = { onAddToPlaylist(song) },
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
                                    Text(artist.name, style = MaterialTheme.typography.titleMedium.copy(color = MaterialTheme.colorScheme.onSurface))
                                    Text("${artist.songCount} canciones", style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp))
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
                                    Text(album.title, style = MaterialTheme.typography.titleMedium.copy(color = MaterialTheme.colorScheme.onSurface))
                                    Text("${album.artist} • ${album.songCount} pistas", style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
