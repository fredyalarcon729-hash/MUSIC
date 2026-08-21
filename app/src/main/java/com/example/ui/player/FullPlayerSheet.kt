package com.example.ui.player

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import android.media.audiofx.Visualizer
import com.example.core.model.EqualizerPreset
import com.example.core.model.Lyrics
import com.example.core.model.PlayerUiState
import com.example.core.model.RepeatMode as DomainRepeatMode
import com.example.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FullPlayerSheet(
    playerState: PlayerUiState,
    accentColor: Color,
    positionMsProvider: () -> Long,
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
    onSelectEqualizer: (EqualizerPreset) -> Unit,
    onSetBandLevel: (Int, Int) -> Unit,
    onSetBassBoost: (Int) -> Unit,
    onSetVirtualizer: (Int) -> Unit,
    onToggleKaraokeMode: (Boolean) -> Unit,
    onSetPitch: (Int) -> Unit,
    onToggleVocalReduction: (Boolean) -> Unit,
    onSetVocalStrength: (Float) -> Unit,
    onToggleNormalization: () -> Unit,
    onSetVolume: (Float) -> Unit
) {
    val currentSong = playerState.currentSong ?: return
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // Smoothly animate the accent color when it changes
    val animatedAccentColor by animateColorAsState(
        targetValue = accentColor,
        animationSpec = tween(durationMillis = 1000, easing = LinearOutSlowInEasing),
        label = "accent_color_animation"
    )

    var showTimerDialog by remember { mutableStateOf(false) }
    var showEqDialog by remember { mutableStateOf(false) }
    var isDraggingSlider by remember { mutableStateOf(false) }
    var dragSliderValue by remember { mutableFloatStateOf(0f) }
    var showLyrics by remember { mutableStateOf(false) }

    var showVolumeOverlay by remember { mutableStateOf(false) }
    var volumeOverlayJob by remember { mutableStateOf<kotlinx.coroutines.Job?>(null) }
    val scope = rememberCoroutineScope()
    
    // Fix: Use rememberUpdatedState to avoid capturing stale values in pointerInput
    val currentVolume by rememberUpdatedState(playerState.volume)
    val currentOnSetVolume by rememberUpdatedState(onSetVolume)

    val currentPositionMs = positionMsProvider()
    val progress = if (playerState.durationMs > 0) (currentPositionMs.toFloat() / playerState.durationMs.toFloat()).coerceIn(0f, 1f) else 0f
    
    val formattedPosition = remember(currentPositionMs) {
        val totalSec = (currentPositionMs / 1000).coerceAtLeast(0)
        val min = totalSec / 60
        val sec = totalSec % 60
        "%d:%02d".format(min, sec)
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.background,
        scrimColor = Color.Black.copy(alpha = 0.85f),
        dragHandle = null,
        modifier = Modifier.fillMaxHeight()
    ) {
        Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
            // Immersive Blurred Background
            if (!currentSong.artworkUri.isNullOrBlank()) {
                AsyncImage(
                    model = currentSong.artworkUri,
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxSize()
                        .drawWithContent {
                            drawContent()
                            drawRect(
                                Brush.verticalGradient(
                                    listOf(Color.Black.copy(alpha = 0.4f), Color.Black.copy(alpha = 0.8f))
                                )
                            )
                        }
                        .blur(60.dp),
                    contentScale = ContentScale.Crop
                )
            }

            // Normal Content (Controls & Artwork)
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(16.dp))

                // Top Header Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.KeyboardArrowDown, null, tint = TextPrimary, modifier = Modifier.size(32.dp))
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = if (playerState.isKaraokeModeActive) "MODO KARAOKE" else "REPRODUCIENDO",
                            style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 2.sp, fontWeight = FontWeight.Bold),
                            color = if (playerState.isKaraokeModeActive) NeonPink else animatedAccentColor
                        )
                        Text(
                            text = currentSong.album.ifBlank { "Fusion Music" },
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                            color = TextSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Row {
                        IconButton(onClick = { onToggleKaraokeMode(!playerState.isKaraokeModeActive) }) {
                            Icon(
                                imageVector = if (playerState.isKaraokeModeActive) Icons.Default.Mic else Icons.Outlined.Mic,
                                contentDescription = null,
                                tint = if (playerState.isKaraokeModeActive) NeonPink else TextPrimary
                            )
                        }
                        IconButton(onClick = { showLyrics = !showLyrics }) {
                            Icon(
                                imageVector = Icons.Default.MusicNote,
                                contentDescription = null,
                                tint = if (showLyrics) animatedAccentColor else TextPrimary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Artwork Area
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.85f)
                        .aspectRatio(1f)
                        .shadow(40.dp, RoundedCornerShape(24.dp), spotColor = animatedAccentColor)
                        .clip(RoundedCornerShape(24.dp))
                        .border(1.dp, animatedAccentColor.copy(alpha = 0.3f), RoundedCornerShape(24.dp))
                        .pointerInput(Unit) {
                            detectVerticalDragGestures(
                                onDragStart = { showVolumeOverlay = true },
                                onDragEnd = {
                                    volumeOverlayJob?.cancel()
                                    volumeOverlayJob = scope.launch {
                                        delay(1500)
                                        showVolumeOverlay = false
                                    }
                                },
                                onVerticalDrag = { _, dragAmount ->
                                    // Use the latest volume value from the updated state
                                    val delta = -dragAmount / 600f // Slightly less sensitive for better control
                                    currentOnSetVolume((currentVolume + delta).coerceIn(0f, 1f))
                                    
                                    volumeOverlayJob?.cancel()
                                    showVolumeOverlay = true
                                }
                            )
                        },
                    contentAlignment = Alignment.Center
                ) {
                    if (!currentSong.artworkUri.isNullOrBlank()) {
                        AsyncImage(model = currentSong.artworkUri, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                    } else {
                        Icon(Icons.Default.MusicNote, null, tint = animatedAccentColor, modifier = Modifier.size(60.dp))
                    }

                    // Volume Overlay
                    androidx.compose.animation.AnimatedVisibility(
                        visible = showVolumeOverlay,
                        enter = fadeIn() + scaleIn(initialScale = 0.8f),
                        exit = fadeOut() + scaleOut(targetScale = 0.8f)
                    ) {
                        VolumeIndicator(playerState.volume, animatedAccentColor)
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Karaoke Technical Controls
                if (playerState.isKaraokeModeActive) {
                    KaraokeControlPanel(playerState, onSetPitch, onToggleVocalReduction, onSetVocalStrength)
                    Spacer(modifier = Modifier.height(16.dp))
                }

                // Song Info
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(currentSong.title, style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold), color = TextPrimary, maxLines = 1)
                        Text(currentSong.artist, style = MaterialTheme.typography.titleMedium, color = TextSecondary, maxLines = 1)
                    }
                    IconButton(onClick = onToggleFavorite) {
                        Icon(if (currentSong.isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder, null, tint = if (currentSong.isFavorite) NeonPink else TextTertiary, modifier = Modifier.size(30.dp))
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))
                NeonVisualizer(playerState.audioSessionId, playerState.isPlaying, if (playerState.isKaraokeModeActive) NeonPink else animatedAccentColor)
                Spacer(modifier = Modifier.height(12.dp))

                // Playback Slider
                val currentSliderValue = if (isDraggingSlider) dragSliderValue else progress
                Slider(
                    value = currentSliderValue,
                    onValueChange = { isDraggingSlider = true; dragSliderValue = it },
                    onValueChangeFinished = { isDraggingSlider = false; onSeekTo((dragSliderValue * playerState.durationMs).toLong()) },
                    colors = SliderDefaults.colors(thumbColor = Color.White, activeTrackColor = if (playerState.isKaraokeModeActive) NeonPink else animatedAccentColor)
                )
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(formattedPosition, color = TextSecondary)
                    Text(playerState.durationFormatted, color = TextSecondary)
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Main Controls
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceEvenly) {
                    IconButton(onClick = onToggleShuffle) { Icon(Icons.Default.Shuffle, null, tint = if (playerState.shuffleMode) animatedAccentColor else TextTertiary) }
                    IconButton(onClick = onSkipPrevious) { Icon(Icons.Default.SkipPrevious, null, tint = TextPrimary, modifier = Modifier.size(38.dp)) }
                    Surface(onClick = onTogglePlayPause, shape = CircleShape, color = Color.White, modifier = Modifier.size(68.dp)) {
                        Box(contentAlignment = Alignment.Center) {
                            if (playerState.isBuffering) CircularProgressIndicator(color = animatedAccentColor, modifier = Modifier.size(40.dp))
                            else Icon(if (playerState.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow, null, tint = Color.Black, modifier = Modifier.size(36.dp))
                        }
                    }
                    IconButton(onClick = onSkipNext) { Icon(Icons.Default.SkipNext, null, tint = TextPrimary, modifier = Modifier.size(38.dp)) }
                    IconButton(onClick = onCycleRepeat) { Icon(if (playerState.repeatMode == DomainRepeatMode.ONE) Icons.Default.RepeatOne else Icons.Default.Repeat, null, tint = if (playerState.repeatMode != DomainRepeatMode.OFF) animatedAccentColor else TextTertiary) }
                }

                Spacer(modifier = Modifier.weight(1f))
                
                // Extra Tools
                Row(modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp).clip(RoundedCornerShape(20.dp)).background(Color.White.copy(alpha = 0.05f)).padding(16.dp), horizontalArrangement = Arrangement.SpaceAround) {
                    ToolItem(Icons.Default.Timer, if (playerState.sleepTimerMinutesLeft != null) "${playerState.sleepTimerMinutesLeft}m" else "Timer", playerState.sleepTimerMinutesLeft != null, animatedAccentColor) { showTimerDialog = true }
                    ToolItem(Icons.Default.Equalizer, "EQ", true, animatedAccentColor) { showEqDialog = true }
                    ToolItem(Icons.Default.Tune, "Norm", playerState.isNormalizationEnabled, animatedAccentColor) { onToggleNormalization() }
                    ToolItem(Icons.Default.QueueMusic, "Cola", false, animatedAccentColor) { onOpenQueue() }
                }
            }

            // LYRICS OVERLAY - Using non-extension version
            androidx.compose.animation.AnimatedVisibility(
                visible = showLyrics,
                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.92f))
                ) {
                    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp)) {
                        Spacer(modifier = Modifier.height(48.dp))
                        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Letras Sincronizadas", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold), color = Color.White)
                            IconButton(onClick = { showLyrics = false }, modifier = Modifier.background(Color.White.copy(alpha = 0.1f), CircleShape)) {
                                Icon(Icons.Outlined.Close, null, tint = Color.White)
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(24.dp))
                        
                        LyricsDisplay(
                            lyrics = playerState.currentLyrics,
                            currentPositionMs = playerState.currentPositionMs,
                            animatedAccentColor = if (playerState.isKaraokeModeActive) NeonPink else animatedAccentColor
                        )
                    }
                }
            }
        }
    }

    // Dialogs...
    if (showTimerDialog) SleepTimerDialog(playerState.sleepTimerMinutesLeft, { showTimerDialog = false }, { onSelectSleepTimer(it); showTimerDialog = false })
    if (showEqDialog) ProfessionalEqualizerDialog(
        playerState = playerState,
        accentColor = animatedAccentColor,
        onDismiss = { showEqDialog = false },
        onSetPreset = onSelectEqualizer,
        onSetBandLevel = onSetBandLevel,
        onSetBassBoost = onSetBassBoost,
        onSetVirtualizer = onSetVirtualizer
    )
}

@Composable
fun KaraokeControlPanel(
    playerState: PlayerUiState,
    onSetPitch: (Int) -> Unit,
    onToggleVocalReduction: (Boolean) -> Unit,
    onSetVocalStrength: (Float) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(NeonPink.copy(alpha = 0.12f))
            .border(1.dp, NeonPink.copy(alpha = 0.2f), RoundedCornerShape(20.dp))
            .padding(16.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column {
                Text("Tonalidad", style = MaterialTheme.typography.labelSmall, color = NeonPink)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { onSetPitch(playerState.pitchSemitones - 1) }) { Icon(Icons.Default.Remove, null, tint = Color.White) }
                    Text(text = if (playerState.pitchSemitones >= 0) "+${playerState.pitchSemitones}" else playerState.pitchSemitones.toString(), color = Color.White, fontWeight = FontWeight.Bold, modifier = Modifier.width(30.dp), textAlign = TextAlign.Center)
                    IconButton(onClick = { onSetPitch(playerState.pitchSemitones + 1) }) { Icon(Icons.Default.Add, null, tint = Color.White) }
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("Eliminar Voz", style = MaterialTheme.typography.labelSmall, color = NeonPink)
                Switch(checked = playerState.isVocalReductionEnabled, onCheckedChange = onToggleVocalReduction, colors = SwitchDefaults.colors(checkedThumbColor = Color.Black, checkedTrackColor = NeonPink))
            }
        }
        if (playerState.isVocalReductionEnabled) {
            Spacer(modifier = Modifier.height(8.dp))
            Text("Guía de Voz (Intensidad)", style = MaterialTheme.typography.labelSmall, color = NeonPink.copy(alpha = 0.6f))
            Slider(value = playerState.vocalReductionStrength, onValueChange = onSetVocalStrength, colors = SliderDefaults.colors(thumbColor = Color.White, activeTrackColor = NeonPink))
        }
    }
}

@Composable
fun ToolItem(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, isActive: Boolean, accent: Color, onClick: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { onClick() }) {
        Icon(icon, null, tint = if (isActive) accent else TextSecondary, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text(label, color = if (isActive) accent else TextSecondary)
    }
}

@Composable
fun LyricsDisplay(
    lyrics: Lyrics?,
    currentPositionMs: Long,
    animatedAccentColor: Color
) {
    var isLoadingTimeout by remember { mutableStateOf(false) }
    // Constants for fine-tuning
    val latencyCompensation = 200L // 200ms ahead to match audio output
    val compensatedPosition = currentPositionMs + latencyCompensation

    LaunchedEffect(lyrics) {
        if (lyrics == null) {
            isLoadingTimeout = false
            kotlinx.coroutines.delay(8000)
            if (lyrics == null) isLoadingTimeout = true
        }
    }

    if (lyrics == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            if (isLoadingTimeout) Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.Info, null, tint = TextTertiary, modifier = Modifier.size(48.dp))
                Spacer(modifier = Modifier.height(16.dp))
                Text("Letras no disponibles", color = TextSecondary, textAlign = TextAlign.Center)
            } else Column(horizontalAlignment = Alignment.CenterHorizontally) {
                CircularProgressIndicator(color = animatedAccentColor, modifier = Modifier.size(32.dp))
                Spacer(modifier = Modifier.height(16.dp))
                Text("Buscando...", color = TextSecondary)
            }
        }
        return
    }

    if (!lyrics.isSynced || lyrics.lines.isEmpty()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 16.dp)
        ) {
            Text(
                text = lyrics.plainText ?: "No hay letras.",
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.Bold,
                    lineHeight = 36.sp,
                    textAlign = TextAlign.Center
                ),
                color = TextPrimary,
                modifier = Modifier.fillMaxWidth()
            )
        }
    } else {
        val listState = rememberLazyListState()
        val currentLineIndex = lyrics.lines.indexOfLast { it.timeMs <= compensatedPosition }.coerceAtLeast(0)

        LaunchedEffect(currentLineIndex) {
            if (currentLineIndex >= 0) {
                listState.animateScrollToItem(currentLineIndex, -400) // Improved centering
            }
        }

        Box(modifier = Modifier
            .fillMaxSize()
            .graphicsLayer { alpha = 0.99f }
            .drawWithContent {
                drawContent()
                drawRect(
                    Brush.verticalGradient(
                        listOf(Color.Transparent, Color.Black, Color.Black, Color.Transparent),
                        startY = 0f,
                        endY = size.height
                    ),
                    blendMode = androidx.compose.ui.graphics.BlendMode.DstIn
                )
            }) {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(vertical = 250.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                itemsIndexed(lyrics.lines) { index, line ->
                    val isPast = index < currentLineIndex
                    val isCurrent = index == currentLineIndex
                    val isUpcoming = index == currentLineIndex + 1
                    
                    // Detect if upcoming line is close to trigger pre-highlight
                    val timeToNext = line.timeMs - compensatedPosition
                    val isVeryClose = isUpcoming && timeToNext < 600L

                    val scale by animateFloatAsState(
                        targetValue = if (isCurrent) 1.15f else 1f,
                        animationSpec = tween(400, easing = FastOutSlowInEasing),
                        label = "scale"
                    )
                    
                    val targetAlpha = when {
                        isCurrent -> 1f
                        isVeryClose -> 0.7f // Glow slightly before it starts
                        isPast -> 0.25f // Faded past
                        else -> 0.4f // Distant future
                    }
                    
                    val alpha by animateFloatAsState(
                        targetValue = targetAlpha,
                        animationSpec = tween(400),
                        label = "alpha"
                    )

                    Text(
                        text = line.text,
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = if (isCurrent) FontWeight.Black else FontWeight.Bold,
                            fontSize = if (isCurrent) 28.sp else 24.sp,
                            lineHeight = 38.sp,
                            textAlign = TextAlign.Center
                        ),
                        color = (if (isCurrent) Color.White else TextPrimary).copy(alpha = alpha),
                        modifier = Modifier
                            .padding(vertical = 12.dp, horizontal = 32.dp)
                            .fillMaxWidth()
                            .graphicsLayer {
                                scaleX = scale
                                scaleY = scale
                            }
                    )
                }
            }
        }
    }
}

@Composable
fun NeonVisualizer(audioSessionId: Int, isPlaying: Boolean, animatedAccentColor: Color) {
    var magnitudes by remember { mutableStateOf(FloatArray(32) { 0.1f }) }
    DisposableEffect(audioSessionId) {
        if (audioSessionId <= 0) return@DisposableEffect onDispose {}
        val visualizer = try {
            Visualizer(audioSessionId).apply {
                captureSize = 128
                setDataCaptureListener(object : Visualizer.OnDataCaptureListener {
                    override fun onWaveFormDataCapture(v: Visualizer?, waveform: ByteArray?, samplingRate: Int) {}
                    override fun onFftDataCapture(v: Visualizer?, fft: ByteArray?, samplingRate: Int) {
                        if (fft == null) return
                        val newMagnitudes = FloatArray(32)
                        for (i in 0 until 32) {
                            val real = fft[i * 2].toInt(); val imag = fft[i * 2 + 1].toInt()
                            val mag = Math.sqrt((real * real + imag * imag).toDouble()).toFloat()
                            newMagnitudes[i] = (mag / 50f).coerceIn(0.1f, 1f)
                        }
                        magnitudes = newMagnitudes
                    }
                }, Visualizer.getMaxCaptureRate() / 2, false, true)
                enabled = true
            }
        } catch (e: Exception) { null }
        onDispose { visualizer?.enabled = false; visualizer?.release() }
    }
    Row(modifier = Modifier.fillMaxWidth().height(48.dp), horizontalArrangement = Arrangement.spacedBy(3.dp), verticalAlignment = Alignment.Bottom) {
        magnitudes.forEach { mag ->
            Box(modifier = Modifier.weight(1f).fillMaxHeight(if (isPlaying) mag else 0.1f).clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp)).background(Brush.verticalGradient(listOf(animatedAccentColor, animatedAccentColor.copy(alpha = 0.4f), Color.Transparent))))
        }
    }
}

@Composable
fun SleepTimerDialog(currentMinutes: Int?, onDismiss: () -> Unit, onSelect: (Int?) -> Unit) {
    AlertDialog(onDismissRequest = onDismiss, title = { Text("Apagado automático") }, text = {
        Column {
            listOf(null, 15, 30, 45, 60).forEach { mins ->
                TextItem(if (mins == null) "Desactivado" else "$mins minutos", currentMinutes == mins) { onSelect(mins) }
            }
        }
    }, confirmButton = { TextButton(onClick = onDismiss) { Text("Cerrar") } }, containerColor = ObsidianSurfaceVariant)
}

@Composable
fun TextItem(label: String, isSelected: Boolean, onClick: () -> Unit) {
    Text(label, color = if (isSelected) Color.White else TextPrimary, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal, modifier = Modifier.fillMaxWidth().clickable { onClick() }.padding(12.dp))
}

@Composable
fun VolumeIndicator(volume: Float, accentColor: Color) {
    Box(
        modifier = Modifier
            .size(100.dp)
            .clip(CircleShape)
            .background(Color.Black.copy(alpha = 0.6f))
            .border(2.dp, accentColor.copy(alpha = 0.5f), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = when {
                    volume > 0.6f -> Icons.Default.VolumeUp
                    volume > 0.1f -> Icons.Default.VolumeDown
                    else -> Icons.Default.VolumeMute
                },
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(32.dp)
            )
            Text(
                text = "${(volume * 100).toInt()}%",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = Color.White
            )
        }
    }
}
