package com.example.ui.player

import android.media.audiofx.Visualizer
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.animation.core.RepeatMode as AnimRepeatMode
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.core.model.*
import com.example.core.model.RepeatMode as DomainRepeatMode
import com.example.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

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
    onSetPlaybackSpeed: (Float) -> Unit,
    onToggleNormalization: () -> Unit,
    onSetVolume: (Float) -> Unit
) {
    val currentSong = playerState.currentSong ?: return
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val animatedAccentColor by animateColorAsState(
        targetValue = accentColor,
        animationSpec = tween(durationMillis = 1000, easing = LinearOutSlowInEasing),
        label = "accent_color_animation"
    )

    var showTimerDialog by remember { mutableStateOf(false) }
    var showEqDialog by remember { mutableStateOf(false) }
    var showSpeedDialog by remember { mutableStateOf(false) }
    var showLyrics by remember { mutableStateOf(false) }

    var magnitudes by remember { mutableStateOf(FloatArray(32) { 0.1f }) }
    DisposableEffect(playerState.audioSessionId) {
        if (playerState.audioSessionId <= 0) return@DisposableEffect onDispose {}
        val visualizer = try {
            Visualizer(playerState.audioSessionId).apply {
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
        val configuration = LocalConfiguration.current
        val isLandscape = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE

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
                        .blur(70.dp),
                    contentScale = ContentScale.Crop
                )
            }

            // Ambient Aura Effect (Reactive to Bass)
            if (playerState.isAmbientAuraEnabled) {
                AmbientAura(
                    magnitudes = magnitudes,
                    accentColor = animatedAccentColor,
                    isPlaying = playerState.isPlaying,
                    style = playerState.ambientAuraStyle,
                    intensity = playerState.ambientAuraIntensity,
                    weight = playerState.ambientAuraWeight
                )
            }

            // Animated Particle Background
            AnimatedParticleBackground(animatedAccentColor)

            if (isLandscape) {
                LandscapePlayerContent(
                    playerState = playerState,
                    currentSong = currentSong,
                    animatedAccentColor = animatedAccentColor,
                    magnitudes = magnitudes,
                    progress = progress,
                    formattedPosition = formattedPosition,
                    onDismiss = onDismiss,
                    onTogglePlayPause = onTogglePlayPause,
                    onSeekTo = onSeekTo,
                    onSkipNext = onSkipNext,
                    onSkipPrevious = onSkipPrevious,
                    onToggleShuffle = onToggleShuffle,
                    onCycleRepeat = onCycleRepeat,
                    onToggleFavorite = onToggleFavorite,
                    onOpenQueue = onOpenQueue,
                    onToggleKaraokeMode = onToggleKaraokeMode,
                    onSetVolume = onSetVolume,
                    onToggleNormalization = onToggleNormalization,
                    onSelectEqualizer = { showEqDialog = true },
                    onSelectSleepTimer = { showTimerDialog = true },
                    onSelectPlaybackSpeed = { showSpeedDialog = true },
                    onShowLyrics = { showLyrics = true },
                    onSetPitch = onSetPitch,
                    onToggleVocalReduction = onToggleVocalReduction,
                    onSetVocalStrength = onSetVocalStrength
                )
            } else {
                PortraitPlayerContent(
                    playerState = playerState,
                    currentSong = currentSong,
                    animatedAccentColor = animatedAccentColor,
                    magnitudes = magnitudes,
                    progress = progress,
                    formattedPosition = formattedPosition,
                    onDismiss = onDismiss,
                    onTogglePlayPause = onTogglePlayPause,
                    onSeekTo = onSeekTo,
                    onSkipNext = onSkipNext,
                    onSkipPrevious = onSkipPrevious,
                    onToggleShuffle = onToggleShuffle,
                    onCycleRepeat = onCycleRepeat,
                    onToggleFavorite = onToggleFavorite,
                    onOpenQueue = onOpenQueue,
                    onToggleKaraokeMode = onToggleKaraokeMode,
                    onSetVolume = onSetVolume,
                    onToggleNormalization = onToggleNormalization,
                    onSelectEqualizer = { showEqDialog = true },
                    onSelectSleepTimer = { showTimerDialog = true },
                    onSelectPlaybackSpeed = { showSpeedDialog = true },
                    onShowLyrics = { showLyrics = true },
                    onSetPitch = onSetPitch,
                    onToggleVocalReduction = onToggleVocalReduction,
                    onSetVocalStrength = onSetVocalStrength
                )
            }

            // LYRICS OVERLAY
            androidx.compose.animation.AnimatedVisibility(
                visible = showLyrics,
                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
            ) {
                Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.92f))) {
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

    if (showTimerDialog) SleepTimerDialog(playerState.sleepTimerMinutesLeft, { showTimerDialog = false }, { onSelectSleepTimer(it); showTimerDialog = false })
    if (showSpeedDialog) PlaybackSpeedDialog(playerState.playbackSpeed, { showSpeedDialog = false }, { onSetPlaybackSpeed(it); showSpeedDialog = false })
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
fun PortraitPlayerContent(
    playerState: PlayerUiState,
    currentSong: Song,
    animatedAccentColor: Color,
    magnitudes: FloatArray,
    progress: Float,
    formattedPosition: String,
    onDismiss: () -> Unit,
    onTogglePlayPause: () -> Unit,
    onSeekTo: (Long) -> Unit,
    onSkipNext: () -> Unit,
    onSkipPrevious: () -> Unit,
    onToggleShuffle: () -> Unit,
    onCycleRepeat: () -> Unit,
    onToggleFavorite: () -> Unit,
    onOpenQueue: () -> Unit,
    onToggleKaraokeMode: (Boolean) -> Unit,
    onSetVolume: (Float) -> Unit,
    onToggleNormalization: () -> Unit,
    onSelectEqualizer: () -> Unit,
    onSelectSleepTimer: () -> Unit,
    onSelectPlaybackSpeed: () -> Unit,
    onShowLyrics: () -> Unit,
    onSetPitch: (Int) -> Unit,
    onToggleVocalReduction: (Boolean) -> Unit,
    onSetVocalStrength: (Float) -> Unit
) {
    var isDraggingSlider by remember { mutableStateOf(false) }
    var dragSliderValue by remember { mutableFloatStateOf(0f) }
    var showVolumeOverlay by remember { mutableStateOf(false) }
    var volumeOverlayJob by remember { mutableStateOf<kotlinx.coroutines.Job?>(null) }
    var lastVolumeBeforeMute by remember { mutableFloatStateOf(0.5f) }
    val scope = rememberCoroutineScope()
    val currentVolume by rememberUpdatedState(playerState.volume)
    val currentOnSetVolume by rememberUpdatedState(onSetVolume)

    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        // Header
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            IconButton(onClick = onDismiss) { Icon(Icons.Default.KeyboardArrowDown, null, tint = TextPrimary, modifier = Modifier.size(32.dp)) }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(text = if (playerState.isKaraokeModeActive) "MODO KARAOKE" else "REPRODUCIENDO", style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 2.sp, fontWeight = FontWeight.Bold), color = if (playerState.isKaraokeModeActive) NeonPink else animatedAccentColor)
                Text(text = currentSong.album.ifBlank { "Fusion Music" }, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium), color = TextSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Row {
                IconButton(onClick = { onToggleKaraokeMode(!playerState.isKaraokeModeActive) }) { Icon(if (playerState.isKaraokeModeActive) Icons.Default.Mic else Icons.Outlined.Mic, null, tint = if (playerState.isKaraokeModeActive) NeonPink else TextPrimary) }
                IconButton(onClick = onShowLyrics) { Icon(Icons.Default.MusicNote, null, tint = TextPrimary) }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Artwork
        val avgMagnitude = magnitudes.average().toFloat()
        val pulseScale by animateFloatAsState(targetValue = if (playerState.isPlaying) 1f + (avgMagnitude * 0.05f) else 1f, animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow))

        Box(
            modifier = Modifier.fillMaxWidth(0.85f).aspectRatio(1f).graphicsLayer { scaleX = pulseScale; scaleY = pulseScale }.shadow(40.dp, RoundedCornerShape(24.dp), spotColor = animatedAccentColor).clip(RoundedCornerShape(24.dp)).border(1.dp, animatedAccentColor.copy(alpha = 0.3f), RoundedCornerShape(24.dp))
                .pointerInput(Unit) {
                    detectTapGestures(onDoubleTap = {
                        if (currentVolume > 0f) { lastVolumeBeforeMute = currentVolume; currentOnSetVolume(0f) } else { currentOnSetVolume(lastVolumeBeforeMute) }
                        showVolumeOverlay = true; volumeOverlayJob?.cancel(); volumeOverlayJob = scope.launch { delay(1500); showVolumeOverlay = false }
                    })
                }
                .pointerInput(Unit) {
                    detectVerticalDragGestures(onDragStart = { showVolumeOverlay = true }, onDragEnd = { volumeOverlayJob?.cancel(); volumeOverlayJob = scope.launch { delay(1500); showVolumeOverlay = false } }, onVerticalDrag = { _, dragAmount ->
                        val delta = -dragAmount / 600f; currentOnSetVolume((currentVolume + delta).coerceIn(0f, 1f)); volumeOverlayJob?.cancel(); showVolumeOverlay = true
                    })
                },
            contentAlignment = Alignment.Center
        ) {
            if (!currentSong.artworkUri.isNullOrBlank()) AsyncImage(model = currentSong.artworkUri, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            else Icon(Icons.Default.MusicNote, null, tint = animatedAccentColor, modifier = Modifier.size(60.dp))

            androidx.compose.animation.AnimatedVisibility(visible = showVolumeOverlay, enter = fadeIn() + scaleIn(initialScale = 0.8f), exit = fadeOut() + scaleOut(targetScale = 0.8f)) {
                VolumeIndicator(playerState.volume, animatedAccentColor)
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
        if (playerState.isKaraokeModeActive) { KaraokeControlPanel(playerState, onSetPitch, onToggleVocalReduction, onSetVocalStrength); Spacer(modifier = Modifier.height(16.dp)) }

        // Song Info
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(currentSong.title, style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold, shadow = androidx.compose.ui.graphics.Shadow(color = animatedAccentColor.copy(alpha = 0.5f), blurRadius = 15f)), color = TextPrimary, maxLines = 1)
                Text(currentSong.artist, style = MaterialTheme.typography.titleMedium, color = TextSecondary, maxLines = 1)
            }
            IconButton(onClick = onToggleFavorite) { Icon(if (currentSong.isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder, null, tint = if (currentSong.isFavorite) NeonPink else TextTertiary, modifier = Modifier.size(30.dp)) }
        }

        Spacer(modifier = Modifier.height(20.dp))
        NeonVisualizer(magnitudes = magnitudes, isPlaying = playerState.isPlaying, style = playerState.visualizerStyle, animatedAccentColor = if (playerState.isKaraokeModeActive) NeonPink else animatedAccentColor)
        Spacer(modifier = Modifier.height(12.dp))

        // Slider
        val currentSliderValue = if (isDraggingSlider) dragSliderValue else progress
        Slider(value = currentSliderValue, onValueChange = { isDraggingSlider = true; dragSliderValue = it }, onValueChangeFinished = { isDraggingSlider = false; onSeekTo((dragSliderValue * playerState.durationMs).toLong()) }, colors = SliderDefaults.colors(thumbColor = Color.White, activeTrackColor = if (playerState.isKaraokeModeActive) NeonPink else animatedAccentColor))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text(formattedPosition, color = TextSecondary); Text(playerState.durationFormatted, color = TextSecondary) }

        Spacer(modifier = Modifier.height(20.dp))

        // Controls
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
        
        // Tools
        Row(modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp).clip(RoundedCornerShape(20.dp)).background(Color.White.copy(alpha = 0.08f)).border(0.5.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(20.dp)).padding(16.dp), horizontalArrangement = Arrangement.SpaceAround) {
            ToolItem(Icons.Default.Timer, if (playerState.sleepTimerMinutesLeft != null) "${playerState.sleepTimerMinutesLeft}m" else "Timer", playerState.sleepTimerMinutesLeft != null, animatedAccentColor, onSelectSleepTimer)
            ToolItem(Icons.Default.Equalizer, "EQ", true, animatedAccentColor, onSelectEqualizer)
            ToolItem(Icons.Default.Speed, "${playerState.playbackSpeed}x", playerState.playbackSpeed != 1.0f, animatedAccentColor, onSelectPlaybackSpeed)
            ToolItem(Icons.Default.Tune, "Norm", playerState.isNormalizationEnabled, animatedAccentColor, onToggleNormalization)
            ToolItem(Icons.Default.QueueMusic, "Cola", false, animatedAccentColor, onOpenQueue)
        }
    }
}

@Composable
fun LandscapePlayerContent(
    playerState: PlayerUiState,
    currentSong: Song,
    animatedAccentColor: Color,
    magnitudes: FloatArray,
    progress: Float,
    formattedPosition: String,
    onDismiss: () -> Unit,
    onTogglePlayPause: () -> Unit,
    onSeekTo: (Long) -> Unit,
    onSkipNext: () -> Unit,
    onSkipPrevious: () -> Unit,
    onToggleShuffle: () -> Unit,
    onCycleRepeat: () -> Unit,
    onToggleFavorite: () -> Unit,
    onOpenQueue: () -> Unit,
    onToggleKaraokeMode: (Boolean) -> Unit,
    onSetVolume: (Float) -> Unit,
    onToggleNormalization: () -> Unit,
    onSelectEqualizer: () -> Unit,
    onSelectSleepTimer: () -> Unit,
    onSelectPlaybackSpeed: () -> Unit,
    onShowLyrics: () -> Unit,
    onSetPitch: (Int) -> Unit,
    onToggleVocalReduction: (Boolean) -> Unit,
    onSetVocalStrength: (Float) -> Unit
) {
    var isDraggingSlider by remember { mutableStateOf(false) }
    var dragSliderValue by remember { mutableFloatStateOf(0f) }

    Row(modifier = Modifier.fillMaxSize().padding(24.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.weight(1f).fillMaxHeight(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(modifier = Modifier.size(240.dp).shadow(30.dp, RoundedCornerShape(20.dp), spotColor = animatedAccentColor).clip(RoundedCornerShape(20.dp)).border(1.dp, animatedAccentColor.copy(alpha = 0.2f), RoundedCornerShape(20.dp))) {
                    if (!currentSong.artworkUri.isNullOrBlank()) AsyncImage(model = currentSong.artworkUri, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                    else Icon(Icons.Default.MusicNote, null, tint = animatedAccentColor, modifier = Modifier.size(80.dp).align(Alignment.Center))
                }
                Spacer(modifier = Modifier.height(20.dp))
                Text(currentSong.title, style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold), color = TextPrimary, maxLines = 1, textAlign = TextAlign.Center)
                Text(currentSong.artist, style = MaterialTheme.typography.bodyLarge, color = TextSecondary, maxLines = 1, textAlign = TextAlign.Center)
            }
        }

        Column(modifier = Modifier.weight(1.2f).fillMaxHeight().padding(start = 24.dp), verticalArrangement = Arrangement.Center) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onDismiss) { Icon(Icons.Default.KeyboardArrowDown, null, tint = TextPrimary, modifier = Modifier.size(32.dp)) }
                NeonVisualizer(magnitudes = magnitudes, isPlaying = playerState.isPlaying, style = playerState.visualizerStyle, animatedAccentColor = animatedAccentColor, modifier = Modifier.weight(1f).padding(horizontal = 16.dp))
                IconButton(onClick = onShowLyrics) { Icon(Icons.Default.MusicNote, null, tint = TextPrimary) }
            }

            Spacer(modifier = Modifier.height(24.dp))
            val currentSliderValue = if (isDraggingSlider) dragSliderValue else progress
            Slider(value = currentSliderValue, onValueChange = { isDraggingSlider = true; dragSliderValue = it }, onValueChangeFinished = { isDraggingSlider = false; onSeekTo((dragSliderValue * playerState.durationMs).toLong()) }, colors = SliderDefaults.colors(thumbColor = Color.White, activeTrackColor = animatedAccentColor))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text(formattedPosition, color = TextSecondary, fontSize = 12.sp); Text(playerState.durationFormatted, color = TextSecondary, fontSize = 12.sp) }

            Spacer(modifier = Modifier.height(24.dp))
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceEvenly) {
                IconButton(onClick = onToggleShuffle) { Icon(Icons.Default.Shuffle, null, tint = if (playerState.shuffleMode) animatedAccentColor else TextTertiary) }
                IconButton(onClick = onSkipPrevious) { Icon(Icons.Default.SkipPrevious, null, tint = TextPrimary, modifier = Modifier.size(44.dp)) }
                Surface(onClick = onTogglePlayPause, shape = CircleShape, color = Color.White, modifier = Modifier.size(72.dp)) {
                    Box(contentAlignment = Alignment.Center) {
                        if (playerState.isBuffering) CircularProgressIndicator(color = animatedAccentColor, modifier = Modifier.size(40.dp))
                        else Icon(if (playerState.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow, null, tint = Color.Black, modifier = Modifier.size(40.dp))
                    }
                }
                IconButton(onClick = onSkipNext) { Icon(Icons.Default.SkipNext, null, tint = TextPrimary, modifier = Modifier.size(44.dp)) }
                IconButton(onClick = onCycleRepeat) { Icon(if (playerState.repeatMode == DomainRepeatMode.ONE) Icons.Default.RepeatOne else Icons.Default.Repeat, null, tint = if (playerState.repeatMode != DomainRepeatMode.OFF) animatedAccentColor else TextTertiary) }
            }

            Spacer(modifier = Modifier.height(32.dp))
            Row(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Color.White.copy(alpha = 0.05f)).padding(12.dp), horizontalArrangement = Arrangement.SpaceAround) {
                IconButton(onClick = onSelectEqualizer) { Icon(Icons.Default.Equalizer, null, tint = animatedAccentColor) }
                IconButton(onClick = onSelectPlaybackSpeed) { Icon(Icons.Default.Speed, null, tint = if (playerState.playbackSpeed != 1.0f) animatedAccentColor else TextTertiary) }
                IconButton(onClick = onToggleFavorite) { Icon(if (currentSong.isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder, null, tint = if (currentSong.isFavorite) NeonPink else TextTertiary) }
                IconButton(onClick = onOpenQueue) { Icon(Icons.Default.QueueMusic, null, tint = TextPrimary) }
            }
        }
    }
}

@Composable
fun KaraokeControlPanel(playerState: PlayerUiState, onSetPitch: (Int) -> Unit, onToggleVocalReduction: (Boolean) -> Unit, onSetVocalStrength: (Float) -> Unit) {
    Column(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(NeonPink.copy(alpha = 0.12f)).border(1.dp, NeonPink.copy(alpha = 0.2f), RoundedCornerShape(20.dp)).padding(16.dp)) {
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
fun LyricsDisplay(lyrics: Lyrics?, currentPositionMs: Long, animatedAccentColor: Color) {
    var isLoadingTimeout by remember { mutableStateOf(false) }
    val latencyCompensation = 200L
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
        Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp, vertical = 16.dp)) {
            Text(text = lyrics.plainText ?: "No hay letras.", style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold, lineHeight = 36.sp, textAlign = TextAlign.Center), color = TextPrimary, modifier = Modifier.fillMaxWidth())
        }
    } else {
        val listState = rememberLazyListState()
        val currentLineIndex = lyrics.lines.indexOfLast { it.timeMs <= compensatedPosition }.coerceAtLeast(0)

        LaunchedEffect(currentLineIndex) { if (currentLineIndex >= 0) { listState.animateScrollToItem(currentLineIndex, -400) } }

        Box(modifier = Modifier.fillMaxSize().graphicsLayer { alpha = 0.99f }.drawWithContent {
            drawContent()
            drawRect(Brush.verticalGradient(listOf(Color.Transparent, Color.Black, Color.Black, Color.Transparent), startY = 0f, endY = size.height), blendMode = androidx.compose.ui.graphics.BlendMode.DstIn)
        }) {
            LazyColumn(state = listState, modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(vertical = 250.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                itemsIndexed(lyrics.lines) { index, line ->
                    val isPast = index < currentLineIndex
                    val isCurrent = index == currentLineIndex
                    val isUpcoming = index == currentLineIndex + 1
                    val timeToNext = line.timeMs - compensatedPosition
                    val isVeryClose = isUpcoming && timeToNext < 600L
                    val scale by animateFloatAsState(targetValue = if (isCurrent) 1.15f else 1f, animationSpec = tween(400, easing = FastOutSlowInEasing))
                    val targetAlpha = when { isCurrent -> 1f; isVeryClose -> 0.7f; isPast -> 0.25f; else -> 0.4f }
                    val alpha by animateFloatAsState(targetValue = targetAlpha, animationSpec = tween(400))
                    Text(text = line.text, style = MaterialTheme.typography.headlineMedium.copy(fontWeight = if (isCurrent) FontWeight.Black else FontWeight.Bold, fontSize = if (isCurrent) 28.sp else 24.sp, lineHeight = 38.sp, textAlign = TextAlign.Center), color = (if (isCurrent) Color.White else TextPrimary).copy(alpha = alpha), modifier = Modifier.padding(vertical = 12.dp, horizontal = 32.dp).fillMaxWidth().graphicsLayer { scaleX = scale; scaleY = scale })
                }
            }
        }
    }
}

@Composable
fun NeonVisualizer(magnitudes: FloatArray, isPlaying: Boolean, style: VisualizerStyle, animatedAccentColor: Color, modifier: Modifier = Modifier) {
    Box(modifier = modifier.height(60.dp), contentAlignment = Alignment.Center) {
        when (style) {
            VisualizerStyle.RING -> {
                androidx.compose.foundation.Canvas(modifier = Modifier.size(60.dp)) {
                    val center = androidx.compose.ui.geometry.Offset(size.width / 2, size.height / 2)
                    val radius = size.minDimension / 2 - 4.dp.toPx()
                    magnitudes.forEachIndexed { index, mag ->
                        val angle = (index.toFloat() / magnitudes.size.toFloat()) * 2 * PI.toFloat()
                        val height = if (isPlaying) mag * 12.dp.toPx() else 2.dp.toPx()
                        val start = androidx.compose.ui.geometry.Offset(center.x + radius * cos(angle), center.y + radius * sin(angle))
                        val end = androidx.compose.ui.geometry.Offset(center.x + (radius + height) * cos(angle), center.y + (radius + height) * sin(angle))
                        drawLine(color = animatedAccentColor, start = start, end = end, strokeWidth = 2.dp.toPx(), cap = StrokeCap.Round)
                    }
                }
            }
            VisualizerStyle.WAVE -> {
                androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
                    val path = Path()
                    val width = size.width
                    val height = size.height
                    val centerY = height / 2
                    path.moveTo(0f, centerY)
                    magnitudes.forEachIndexed { index, mag ->
                        val x = (index.toFloat() / (magnitudes.size - 1)) * width
                        val y = centerY + (if (isPlaying) (mag - 0.5f) * height else 0f)
                        path.lineTo(x, y)
                    }
                    drawPath(path = path, color = animatedAccentColor, style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round))
                }
            }
            VisualizerStyle.MIRROR -> {
                Row(modifier = Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(2.dp), verticalAlignment = Alignment.CenterVertically) {
                    magnitudes.forEach { mag ->
                        val h = if (isPlaying) (mag * 0.8f).coerceIn(0.1f, 1f) else 0.1f
                        Box(modifier = Modifier.weight(1f).fillMaxHeight(h).clip(CircleShape).background(Brush.verticalGradient(listOf(Color.Transparent, animatedAccentColor, Color.Transparent))))
                    }
                }
            }
            else -> {
                Row(modifier = Modifier.fillMaxWidth().height(48.dp), horizontalArrangement = Arrangement.spacedBy(3.dp), verticalAlignment = Alignment.Bottom) {
                    magnitudes.forEach { mag ->
                        val heightFactor = if (isPlaying) mag else 0.1f
                        when (style) {
                            VisualizerStyle.BARS -> Box(modifier = Modifier.weight(1f).fillMaxHeight(heightFactor).clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp)).background(Brush.verticalGradient(listOf(animatedAccentColor, animatedAccentColor.copy(alpha = 0.4f), Color.Transparent))))
                            VisualizerStyle.SYMMETRIC -> Box(modifier = Modifier.weight(1f).fillMaxHeight(heightFactor).align(Alignment.CenterVertically).clip(RoundedCornerShape(4.dp)).background(animatedAccentColor))
                            VisualizerStyle.DOTS -> Box(modifier = Modifier.weight(1f).size(if (isPlaying) (mag * 24).dp else 4.dp).clip(CircleShape).background(animatedAccentColor).align(Alignment.CenterVertically))
                            VisualizerStyle.PIXELS -> Column(modifier = Modifier.weight(1f).fillMaxHeight(), verticalArrangement = Arrangement.Bottom, horizontalAlignment = Alignment.CenterHorizontally) {
                                val numBlocks = (heightFactor * 8).toInt().coerceAtLeast(1)
                                repeat(numBlocks) { Box(modifier = Modifier.fillMaxWidth().height(4.dp).padding(vertical = 1.dp).background(animatedAccentColor)) }
                            }
                            else -> {}
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PlaybackSpeedDialog(currentSpeed: Float, onDismiss: () -> Unit, onSelect: (Float) -> Unit) {
    AlertDialog(onDismissRequest = onDismiss, title = { Text("Velocidad de reproducción") }, text = {
        Column { listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f).forEach { speed -> TextItem("${speed}x", currentSpeed == speed) { onSelect(speed) } } }
    }, confirmButton = { TextButton(onClick = onDismiss) { Text("Cerrar") } }, containerColor = ObsidianSurfaceVariant)
}

@Composable
fun SleepTimerDialog(currentMinutes: Int?, onDismiss: () -> Unit, onSelect: (Int?) -> Unit) {
    var customValue by remember { mutableStateOf("") }
    AlertDialog(onDismissRequest = onDismiss, title = { Text("Apagado automático") }, text = {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Selecciona un tiempo o ingresa uno personalizado:", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(15, 30, 45, 60).forEach { mins ->
                    Surface(onClick = { onSelect(mins) }, shape = RoundedCornerShape(8.dp), color = if (currentMinutes == mins) NeonCyan.copy(alpha = 0.2f) else Color.White.copy(alpha = 0.05f), border = androidx.compose.foundation.BorderStroke(1.dp, if (currentMinutes == mins) NeonCyan else Color.Transparent), modifier = Modifier.weight(1f)) {
                        Box(modifier = Modifier.padding(vertical = 8.dp), contentAlignment = Alignment.Center) { Text("${mins}m", color = if (currentMinutes == mins) NeonCyan else Color.White, fontSize = 12.sp) }
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(value = customValue, onValueChange = { if (it.all { char -> char.isDigit() } && it.length <= 3) customValue = it }, label = { Text("Minutos personalizados") }, placeholder = { Text("Ej: 120") }, modifier = Modifier.fillMaxWidth(), singleLine = true, keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number), colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = NeonCyan, unfocusedBorderColor = Color.White.copy(alpha = 0.2f)))
            TextItem("Desactivar temporizador", currentMinutes == null) { onSelect(null) }
        }
    }, confirmButton = { Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) { TextButton(onClick = onDismiss) { Text("Cancelar", color = TextSecondary) }; if (customValue.isNotEmpty()) { TextButton(onClick = { onSelect(customValue.toIntOrNull()) }) { Text("Iniciar", color = NeonCyan, fontWeight = FontWeight.Bold) } } } }, containerColor = ObsidianSurfaceVariant)
}

@Composable
fun TextItem(label: String, isSelected: Boolean, onClick: () -> Unit) { Text(label, color = if (isSelected) Color.White else TextPrimary, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal, modifier = Modifier.fillMaxWidth().clickable { onClick() }.padding(12.dp)) }

@Composable
fun VolumeIndicator(volume: Float, accentColor: Color) {
    Box(modifier = Modifier.size(100.dp).clip(CircleShape).background(Color.Black.copy(alpha = 0.6f)).border(2.dp, accentColor.copy(alpha = 0.5f), CircleShape), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(imageVector = when { volume > 0.6f -> Icons.Default.VolumeUp; volume > 0.1f -> Icons.Default.VolumeDown; else -> Icons.Default.VolumeMute }, contentDescription = null, tint = Color.White, modifier = Modifier.size(32.dp))
            Text(text = "${(volume * 100).toInt()}%", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = Color.White)
        }
    }
}

@Composable
fun AmbientAura(magnitudes: FloatArray, accentColor: Color, isPlaying: Boolean, style: AmbientAuraStyle = AmbientAuraStyle.MINIMAL_EDGE, intensity: Float = 0.4f, weight: Float = 0.8f) {
    val bassIntensity = if (isPlaying) { (magnitudes.take(4).average().toFloat() * 2.5f).coerceIn(0f, 1f) } else 0f
    val baseAlpha = intensity.coerceIn(0.05f, 0.9f)
    val pulseStrength = (bassIntensity * 0.4f * intensity).coerceIn(0f, 0.5f)
    val auraAlpha by animateFloatAsState(targetValue = if (isPlaying) baseAlpha + pulseStrength else baseAlpha * 0.8f, animationSpec = spring(stiffness = Spring.StiffnessLow))
    val density = LocalDensity.current
    val coreThickness = with(density) { (weight * 1.dp.toPx()).coerceAtLeast(0.5f) }
    Box(modifier = Modifier.fillMaxSize()) {
        when (style) {
            AmbientAuraStyle.MINIMAL_EDGE -> {
                Box(modifier = Modifier.fillMaxSize().blur(12.dp).drawBehind { drawRect(color = accentColor.copy(alpha = auraAlpha * 0.5f), style = Stroke(width = 16.dp.toPx()), blendMode = androidx.compose.ui.graphics.BlendMode.Screen) })
                Box(modifier = Modifier.fillMaxSize().drawBehind { drawRect(color = accentColor.copy(alpha = auraAlpha), style = Stroke(width = coreThickness), blendMode = androidx.compose.ui.graphics.BlendMode.Plus) })
            }
            AmbientAuraStyle.SOFT_CORNERS -> {
                Box(modifier = Modifier.fillMaxSize().drawBehind {
                    val cornerSize = 100.dp.toPx()
                    val brush = Brush.radialGradient(colors = listOf(accentColor.copy(alpha = auraAlpha), Color.Transparent), radius = cornerSize)
                    drawCircle(brush, radius = cornerSize, center = androidx.compose.ui.geometry.Offset(0f, 0f))
                    drawCircle(brush, radius = cornerSize, center = androidx.compose.ui.geometry.Offset(size.width, 0f))
                    drawCircle(brush, radius = cornerSize, center = androidx.compose.ui.geometry.Offset(0f, size.height))
                    drawCircle(brush, radius = cornerSize, center = androidx.compose.ui.geometry.Offset(size.width, size.height))
                }.blur(20.dp))
            }
            AmbientAuraStyle.GRADIENT_FLOW -> {
                val infiniteTransition = rememberInfiniteTransition(label = "gradient_flow")
                val offset by infiniteTransition.animateFloat(initialValue = 0f, targetValue = 1000f, animationSpec = infiniteRepeatable(animation = tween(10000, easing = LinearEasing), repeatMode = androidx.compose.animation.core.RepeatMode.Restart))
                Box(modifier = Modifier.fillMaxSize().drawBehind {
                    val brush = Brush.linearGradient(colors = listOf(accentColor.copy(alpha = auraAlpha * 0.2f), accentColor.copy(alpha = auraAlpha), accentColor.copy(alpha = auraAlpha * 0.2f)), start = androidx.compose.ui.geometry.Offset(offset, 0f), end = androidx.compose.ui.geometry.Offset(offset + size.width, size.height), tileMode = androidx.compose.ui.graphics.TileMode.Mirror)
                    drawRect(brush = brush, style = Stroke(width = coreThickness), blendMode = androidx.compose.ui.graphics.BlendMode.Plus)
                })
            }
        }
    }
}

@Composable
fun AnimatedParticleBackground(accentColor: Color) {
    val infiniteTransition = rememberInfiniteTransition(label = "particles")
    val particles = remember { List(15) { (0..100).random() } }
    Box(modifier = Modifier.fillMaxSize()) {
        particles.forEachIndexed { index, startPos ->
            val duration = remember { (4000..8000).random() }
            val delay = remember { (0..2000).random() }
            val yPos by infiniteTransition.animateFloat(initialValue = 1.1f, targetValue = -0.1f, animationSpec = infiniteRepeatable(animation = tween(duration, delayMillis = delay, easing = LinearEasing), repeatMode = AnimRepeatMode.Restart))
            val xOffset by infiniteTransition.animateFloat(initialValue = -20f, targetValue = 20f, animationSpec = infiniteRepeatable(animation = tween(duration / 2, easing = LinearEasing), repeatMode = AnimRepeatMode.Reverse))
            Box(modifier = Modifier.fillMaxSize().graphicsLayer { translationY = yPos * size.height; translationX = (startPos.toFloat() / 100f) * size.width + xOffset; alpha = 0.15f }) {
                Box(modifier = Modifier.size(remember { (4..12).random().dp }).clip(CircleShape).background(Brush.radialGradient(listOf(accentColor, Color.Transparent))))
            }
        }
    }
}
