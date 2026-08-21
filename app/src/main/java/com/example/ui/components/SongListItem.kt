package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode as AnimRepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.OfflinePin
import androidx.compose.material.icons.filled.PlaylistAdd
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.model.DownloadStatus
import com.example.core.model.MusicSource
import com.example.core.model.Song
import com.example.ui.theme.*

@Composable
fun SongListItem(
    song: Song,
    isPlaying: Boolean,
    isCurrent: Boolean,
    onClick: () -> Unit,
    onToggleFavorite: () -> Unit,
    onPlayNext: () -> Unit,
    onAddToQueue: () -> Unit,
    onAddToPlaylist: () -> Unit,
    downloadStatus: DownloadStatus? = null,
    onDownload: (() -> Unit)? = null,
    onCancelDownload: (() -> Unit)? = null,
    onDeleteDownload: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var showMenu by remember { mutableStateOf(false) }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(if (isCurrent) NeonPurpleLight.copy(alpha = 0.12f) else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .testTag("song_item_${song.id}"),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Artwork & Play State Overlay
        Box(contentAlignment = Alignment.Center) {
            FusionArtwork(
                artworkUri = song.artworkUri,
                size = 52.dp,
                cornerRadius = 10.dp
            )
            if (isCurrent && isPlaying) {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color.Black.copy(alpha = 0.5f)),
                    contentAlignment = Alignment.Center
                ) {
                    EqualizerBars(tint = NeonCyan)
                }
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Title and Metadata
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = song.title,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium,
                    color = if (isCurrent) NeonCyan else MaterialTheme.colorScheme.onSurface
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Source badge if YouTube
                if (song.source == MusicSource.YOUTUBE) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color(0xFFE50914).copy(alpha = 0.2f))
                            .padding(horizontal = 4.dp, vertical = 1.dp)
                    ) {
                        Text(
                            text = "YT",
                            color = Color(0xFFFF4D4D),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Downloaded badge
                if (song.isDownloaded) {
                    Icon(
                        imageVector = Icons.Default.OfflinePin,
                        contentDescription = "Descargado",
                        tint = NeonGreen,
                        modifier = Modifier.size(13.dp)
                    )
                }

                Text(
                    text = song.artist,
                    style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurfaceVariant),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )

                Text(
                    text = if (song.sizeFormatted.isNotEmpty() && song.isDownloaded) {
                        "• ${song.durationFormatted} (${song.sizeFormatted})"
                    } else {
                        "• ${song.durationFormatted}"
                    },
                    style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)),
                    fontSize = 11.sp
                )
            }

            // Real-time downloading status progress text
            if (downloadStatus is DownloadStatus.Downloading) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Descargando: ${downloadStatus.formattedProgress}",
                    color = NeonCyan,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold
                )
            } else if (downloadStatus is DownloadStatus.Preparing) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Conectando stream de audio...",
                    color = NeonCyan.copy(alpha = 0.8f),
                    fontSize = 10.sp
                )
            }
        }

        // Download Action or Progress Indicator
        when (downloadStatus) {
            is DownloadStatus.Preparing -> {
                IconButton(
                    onClick = { onCancelDownload?.invoke() },
                    modifier = Modifier.size(36.dp)
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = NeonCyan
                    )
                }
            }
            is DownloadStatus.Downloading -> {
                Box(
                    modifier = Modifier.size(36.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        progress = { downloadStatus.progress },
                        modifier = Modifier.size(26.dp),
                        strokeWidth = 2.5.dp,
                        color = NeonCyan,
                        trackColor = NeonCyan.copy(alpha = 0.2f)
                    )
                    IconButton(
                        onClick = { onCancelDownload?.invoke() },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Cancelar descarga",
                            tint = NeonPink,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }
            else -> {
                // If not downloading and download callback is provided & not yet downloaded
                if (!song.isDownloaded && onDownload != null && song.source == MusicSource.YOUTUBE) {
                    IconButton(
                        onClick = onDownload,
                        modifier = Modifier
                            .size(38.dp)
                            .testTag("download_btn_${song.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Download,
                            contentDescription = "Descargar canción",
                            tint = NeonCyan.copy(alpha = 0.9f),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }

        // Favorite Heart Button
        IconButton(
            onClick = onToggleFavorite,
            modifier = Modifier
                .size(38.dp)
                .testTag("fav_btn_${song.id}")
        ) {
            Icon(
                imageVector = if (song.isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                contentDescription = if (song.isFavorite) "Quitar de favoritos" else "Agregar a favoritos",
                tint = if (song.isFavorite) NeonPink else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                modifier = Modifier.size(20.dp)
            )
        }

        // More Menu Button
        Box {
            IconButton(
                onClick = { showMenu = true },
                modifier = Modifier
                    .size(38.dp)
                    .testTag("more_btn_${song.id}")
            ) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = "Opciones",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier.size(20.dp)
                )
            }

            DropdownMenu(
                expanded = showMenu,
                onDismissRequest = { showMenu = false },
                modifier = Modifier.background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                // Download or Delete Download option
                if (song.isDownloaded && onDeleteDownload != null) {
                    DropdownMenuItem(
                        text = { Text("Eliminar descarga", color = Color(0xFFFF5252)) },
                        leadingIcon = { Icon(Icons.Default.DeleteOutline, null, tint = Color(0xFFFF5252)) },
                        onClick = {
                            showMenu = false
                            onDeleteDownload()
                        }
                    )
                } else if (!song.isDownloaded && onDownload != null) {
                    DropdownMenuItem(
                        text = { Text("Descargar para escuchar offline", color = MaterialTheme.colorScheme.onSurface) },
                        leadingIcon = { Icon(Icons.Default.Download, null, tint = NeonCyan) },
                        onClick = {
                            showMenu = false
                            onDownload()
                        }
                    )
                }

                DropdownMenuItem(
                    text = { Text("Reproducir siguiente", color = MaterialTheme.colorScheme.onSurface) },
                    leadingIcon = { Icon(Icons.Default.QueueMusic, null, tint = NeonCyan) },
                    onClick = {
                        showMenu = false
                        onPlayNext()
                    }
                )
                DropdownMenuItem(
                    text = { Text("Añadir a la cola", color = MaterialTheme.colorScheme.onSurface) },
                    leadingIcon = { Icon(Icons.Default.QueueMusic, null, tint = NeonPurpleLight) },
                    onClick = {
                        showMenu = false
                        onAddToQueue()
                    }
                )
                DropdownMenuItem(
                    text = { Text("Añadir a Playlist", color = MaterialTheme.colorScheme.onSurface) },
                    leadingIcon = { Icon(Icons.Default.PlaylistAdd, null, tint = NeonPink) },
                    onClick = {
                        showMenu = false
                        onAddToPlaylist()
                    }
                )
            }
        }
    }
}

@Composable
fun EqualizerBars(tint: Color = NeonCyan) {
    val transition = rememberInfiniteTransition(label = "eq_anim")
    
    val bar1Scale by transition.animateFloat(
        initialValue = 0.2f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(
            animation = tween(400, easing = FastOutSlowInEasing),
            repeatMode = AnimRepeatMode.Reverse
        ),
        label = "bar1"
    )
    val bar2Scale by transition.animateFloat(
        initialValue = 0.8f,
        targetValue = 0.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(320, easing = FastOutSlowInEasing),
            repeatMode = AnimRepeatMode.Reverse
        ),
        label = "bar2"
    )
    val bar3Scale by transition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(480, easing = FastOutSlowInEasing),
            repeatMode = AnimRepeatMode.Reverse
        ),
        label = "bar3"
    )

    Row(
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.Bottom,
        modifier = Modifier.height(20.dp)
    ) {
        EqualizerBar(tint, bar1Scale)
        EqualizerBar(tint, bar2Scale)
        EqualizerBar(tint, bar3Scale)
    }
}

@Composable
private fun EqualizerBar(tint: Color, scale: Float) {
    Box(
        modifier = Modifier
            .width(3.dp)
            .fillMaxHeight()
            .graphicsLayer { 
                scaleY = scale
                transformOrigin = TransformOrigin(0.5f, 1f)
            }
            .clip(RoundedCornerShape(1.5.dp))
            .background(tint)
    )
}
