package com.example.ui.player

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.core.model.EqualizerPreset
import com.example.core.model.PlayerUiState
import com.example.core.model.RepeatMode as DomainRepeatMode
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonPink
import com.example.ui.theme.NeonPurpleDark
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
fun FullPlayerSheet(
    playerState: PlayerUiState,
    onDismiss: () -> Unit,
    onTogglePlayPause: () -> Unit,
    onSeekTo: (Long) -> Unit,
    onSkipNext: () -> Unit,
    onSkipPrevious: () -> Unit,
    onToggleShuffle: () -> Unit,
    onCycleRepeat: () -> Unit,
    onToggleFavorite: () -> Unit,
    onOpenQueue: () -> Unit,
    onSelectSleepTimer: (Int?) -> Unit,
    onSelectEqualizer: (EqualizerPreset) -> Unit
) {
    val currentSong = playerState.currentSong ?: return
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var showTimerDialog by remember { mutableStateOf(false) }
    var showEqDialog by remember { mutableStateOf(false) }
    var isDraggingSlider by remember { mutableStateOf(false) }
    var dragSliderValue by remember { mutableFloatStateOf(0f) }

    // Vinyl Rotation animation
    val infiniteTransition = rememberInfiniteTransition(label = "vinyl_rotate")
    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(12000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = ObsidianDark,
        scrimColor = Color.Black.copy(alpha = 0.75f),
        dragHandle = null,
        modifier = Modifier.fillMaxHeight()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            NeonPurpleDark.copy(alpha = 0.35f),
                            ObsidianSurface,
                            ObsidianDark
                        )
                    )
                )
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // Top Header Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.testTag("full_player_dismiss")
                ) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = "Cerrar reproductor",
                        tint = TextPrimary,
                        modifier = Modifier.size(32.dp)
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "REPRODUCIENDO",
                        style = MaterialTheme.typography.labelSmall.copy(
                            letterSpacing = 2.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        color = NeonCyan
                    )
                    Text(
                        text = currentSong.album.ifBlank { "Fusion Music" },
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                        color = TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                IconButton(
                    onClick = onOpenQueue,
                    modifier = Modifier.testTag("full_player_open_queue")
                ) {
                    Icon(
                        imageVector = Icons.Default.QueueMusic,
                        contentDescription = "Ver cola",
                        tint = TextPrimary,
                        modifier = Modifier.size(26.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Center Artwork Vinyl & Glow
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.82f)
                    .aspectRatio(1f)
                    .shadow(32.dp, CircleShape, spotColor = NeonPurpleLight)
                    .clip(RoundedCornerShape(24.dp))
                    .border(
                        2.dp,
                        Brush.radialGradient(
                            listOf(NeonCyan.copy(alpha = 0.6f), NeonPurpleLight.copy(alpha = 0.4f), Color.Transparent)
                        ),
                        RoundedCornerShape(24.dp)
                    )
                    .background(ObsidianCard),
                contentAlignment = Alignment.Center
            ) {
                if (!currentSong.artworkUri.isNullOrBlank()) {
                    AsyncImage(
                        model = currentSong.artworkUri,
                        contentDescription = "Carátula",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    // Modern Vinyl Texture fallback
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.radialGradient(
                                    colors = listOf(
                                        Color(0xFF2A2A40),
                                        Color(0xFF141420),
                                        Color(0xFF0A0A10)
                                    )
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(90.dp)
                                .clip(CircleShape)
                                .background(NeonPurpleLight.copy(alpha = 0.2f))
                                .border(2.dp, NeonCyan, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.MusicNote,
                                contentDescription = null,
                                tint = NeonCyan,
                                modifier = Modifier.size(40.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Song Info & Favorite
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = currentSong.title,
                        style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                        color = TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = currentSong.artist,
                        style = MaterialTheme.typography.titleMedium.copy(color = TextSecondary),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                IconButton(
                    onClick = onToggleFavorite,
                    modifier = Modifier.testTag("full_player_fav")
                ) {
                    Icon(
                        imageVector = if (currentSong.isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                        contentDescription = if (currentSong.isFavorite) "Quitar de favoritos" else "Agregar a favoritos",
                        tint = if (currentSong.isFavorite) NeonPink else TextTertiary,
                        modifier = Modifier.size(30.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Dynamic Audio Waveform Visualizer
            DynamicWaveformBar(
                isPlaying = playerState.isPlaying,
                progress = playerState.progress
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Seek Slider
            val currentSliderValue = if (isDraggingSlider) dragSliderValue else playerState.progress
            Slider(
                value = currentSliderValue,
                onValueChange = {
                    isDraggingSlider = true
                    dragSliderValue = it
                },
                onValueChangeFinished = {
                    isDraggingSlider = false
                    val targetMs = (dragSliderValue * playerState.durationMs).toLong()
                    onSeekTo(targetMs)
                },
                colors = SliderDefaults.colors(
                    thumbColor = NeonCyan,
                    activeTrackColor = NeonCyan,
                    inactiveTrackColor = ObsidianBorder
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("full_player_seek_slider")
            )

            // Timestamps
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = playerState.positionFormatted,
                    style = MaterialTheme.typography.bodyMedium.copy(color = TextSecondary)
                )
                Text(
                    text = playerState.durationFormatted,
                    style = MaterialTheme.typography.bodyMedium.copy(color = TextSecondary)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Playback Controls Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                // Shuffle Button
                IconButton(
                    onClick = onToggleShuffle,
                    modifier = Modifier.testTag("full_player_shuffle")
                ) {
                    Icon(
                        imageVector = Icons.Default.Shuffle,
                        contentDescription = "Modo aleatorio",
                        tint = if (playerState.shuffleMode) NeonCyan else TextTertiary,
                        modifier = Modifier.size(26.dp)
                    )
                }

                // Previous Track
                IconButton(
                    onClick = onSkipPrevious,
                    modifier = Modifier.testTag("full_player_prev")
                ) {
                    Icon(
                        imageVector = Icons.Default.SkipPrevious,
                        contentDescription = "Anterior",
                        tint = TextPrimary,
                        modifier = Modifier.size(38.dp)
                    )
                }

                // Play / Pause FAB
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(72.dp)
                ) {
                    if (playerState.isBuffering) {
                        CircularProgressIndicator(
                            color = NeonCyan,
                            strokeWidth = 3.dp,
                            modifier = Modifier.size(48.dp)
                        )
                    } else {
                        Surface(
                            onClick = onTogglePlayPause,
                            shape = CircleShape,
                            color = NeonPurpleLight,
                            shadowElevation = 8.dp,
                            modifier = Modifier
                                .size(64.dp)
                                .testTag("full_player_play_pause")
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = if (playerState.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                    contentDescription = if (playerState.isPlaying) "Pausar" else "Reproducir",
                                    tint = Color.Black,
                                    modifier = Modifier.size(34.dp)
                                )
                            }
                        }
                    }
                }

                // Next Track
                IconButton(
                    onClick = onSkipNext,
                    modifier = Modifier.testTag("full_player_next")
                ) {
                    Icon(
                        imageVector = Icons.Default.SkipNext,
                        contentDescription = "Siguiente",
                        tint = TextPrimary,
                        modifier = Modifier.size(38.dp)
                    )
                }

                // Repeat Mode
                IconButton(
                    onClick = onCycleRepeat,
                    modifier = Modifier.testTag("full_player_repeat")
                ) {
                    Icon(
                        imageVector = when (playerState.repeatMode) {
                            DomainRepeatMode.ONE -> Icons.Default.RepeatOne
                            else -> Icons.Default.Repeat
                        },
                        contentDescription = "Repetir",
                        tint = if (playerState.repeatMode != DomainRepeatMode.OFF) NeonCyan else TextTertiary,
                        modifier = Modifier.size(26.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Bottom Audio Tools Row (Sleep timer, Equalizer)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(ObsidianSurfaceVariant)
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Sleep Timer
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clickable { showTimerDialog = true }
                        .padding(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Timer,
                        contentDescription = "Temporizador de apagado",
                        tint = if (playerState.sleepTimerMinutesLeft != null) NeonCyan else TextSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (playerState.sleepTimerMinutesLeft != null) "${playerState.sleepTimerMinutesLeft}m" else "Timer",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Medium,
                            color = if (playerState.sleepTimerMinutesLeft != null) NeonCyan else TextSecondary
                        )
                    )
                }

                // Equalizer
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clickable { showEqDialog = true }
                        .padding(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Equalizer,
                        contentDescription = "Ecualizador",
                        tint = NeonPurpleLight,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = playerState.equalizerPreset.displayName,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Medium,
                            color = TextSecondary
                        )
                    )
                }

                // Audio Quality Badge
                Surface(
                    color = ObsidianCard,
                    shape = RoundedCornerShape(8.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, ObsidianBorder)
                ) {
                    Text(
                        text = playerState.audioQuality.label,
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = NeonCyan,
                            fontWeight = FontWeight.Bold
                        ),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }

    // Timer Dialog
    if (showTimerDialog) {
        SleepTimerDialog(
            currentMinutes = playerState.sleepTimerMinutesLeft,
            onDismiss = { showTimerDialog = false },
            onSelect = {
                onSelectSleepTimer(it)
                showTimerDialog = false
            }
        )
    }

    // Equalizer Preset Dialog
    if (showEqDialog) {
        EqualizerDialog(
            currentPreset = playerState.equalizerPreset,
            onDismiss = { showEqDialog = false },
            onSelect = {
                onSelectEqualizer(it)
                showEqDialog = false
            }
        )
    }
}

@Composable
fun DynamicWaveformBar(
    isPlaying: Boolean,
    progress: Float,
    barCount: Int = 28
) {
    val transition = rememberInfiniteTransition(label = "waveform")
    val animWave by transition.animateFloat(
        initialValue = 0.2f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "wave_val"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(28.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        for (i in 0 until barCount) {
            val barProgress = i.toFloat() / barCount.toFloat()
            val isPast = barProgress <= progress

            val dynamicFactor = if (isPlaying) {
                val offset = (i % 5) * 0.15f
                ((animWave + offset) % 1.0f).coerceIn(0.15f, 1f)
            } else {
                0.2f
            }

            val heightFraction = (0.25f + 0.75f * kotlin.math.sin(i * 0.4f).toFloat().let { kotlin.math.abs(it) } * dynamicFactor).coerceIn(0.15f, 1f)

            Box(
                modifier = Modifier
                    .width(3.dp)
                    .fillMaxHeight(heightFraction)
                    .clip(RoundedCornerShape(1.5.dp))
                    .background(
                        if (isPast) NeonCyan else ObsidianBorder
                    )
            )
        }
    }
}

@Composable
fun SleepTimerDialog(
    currentMinutes: Int?,
    onDismiss: () -> Unit,
    onSelect: (Int?) -> Unit
) {
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Temporizador de Apagado", color = TextPrimary) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(null, 15, 30, 45, 60, 90).forEach { mins ->
                    val isSelected = currentMinutes == mins
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) NeonPurpleLight.copy(alpha = 0.2f) else Color.Transparent)
                            .clickable { onSelect(mins) }
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (mins == null) "Desactivado" else "$mins minutos",
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) NeonCyan else TextPrimary
                            )
                        )
                    }
                }
            }
        },
        confirmButton = {
            androidx.compose.material3.TextButton(onClick = onDismiss) {
                Text("Cancelar", color = NeonCyan)
            }
        },
        containerColor = ObsidianSurfaceVariant
    )
}

@Composable
fun EqualizerDialog(
    currentPreset: EqualizerPreset,
    onDismiss: () -> Unit,
    onSelect: (EqualizerPreset) -> Unit
) {
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Perfil de Ecualizador", color = TextPrimary) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                EqualizerPreset.values().forEach { preset ->
                    val isSelected = currentPreset == preset
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) NeonPurpleLight.copy(alpha = 0.2f) else Color.Transparent)
                            .clickable { onSelect(preset) }
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = preset.displayName,
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) NeonCyan else TextPrimary
                            )
                        )
                    }
                }
            }
        },
        confirmButton = {
            androidx.compose.material3.TextButton(onClick = onDismiss) {
                Text("Cerrar", color = NeonCyan)
            }
        },
        containerColor = ObsidianSurfaceVariant
    )
}
