package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.animation.core.RepeatMode as AnimRepeatMode
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.core.model.Song
import com.example.ui.FusionMainViewModel
import com.example.ui.components.FusionBottomNav
import com.example.ui.components.FusionNavDestination
import com.example.ui.components.MiniPlayer
import com.example.ui.navigation.FusionNavHost
import com.example.ui.player.AddToPlaylistDialog
import com.example.ui.player.CreatePlaylistDialog
import com.example.ui.player.FullPlayerSheet
import com.example.ui.player.QueueSheet
import com.example.ui.theme.FusionMusicTheme
import com.example.ui.viewmodels.SearchViewModel
import com.example.ui.viewmodels.SettingsViewModel
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {

    private val mainViewModel: FusionMainViewModel by viewModels()
    private val searchViewModel: SearchViewModel by viewModels()
    private val settingsViewModel: SettingsViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val themeMode by settingsViewModel.themeMode.collectAsState()
            val isDarkTheme = when (themeMode) {
                "light" -> false
                "dark" -> true
                "system" -> androidx.compose.foundation.isSystemInDarkTheme()
                else -> true
            }

            FusionMusicTheme(darkTheme = isDarkTheme) {
                MainAppContent(
                    mainViewModel = mainViewModel,
                    searchViewModel = searchViewModel,
                    settingsViewModel = settingsViewModel
                )
            }
        }
    }
}

@Composable
fun MainAppContent(
    mainViewModel: FusionMainViewModel,
    searchViewModel: SearchViewModel,
    settingsViewModel: SettingsViewModel
) {
    val context = LocalContext.current
    val navController = rememberNavController()

    // Request permissions dynamically
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions.values.any { it }
        if (granted) {
            mainViewModel.rescanLocalLibrary()
        }
    }

    LaunchedEffect(Unit) {
        val permissionsToRequest = mutableListOf<String>()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.READ_MEDIA_AUDIO)
            }
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS)
            }
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.RECORD_AUDIO)
            }
        } else {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.READ_EXTERNAL_STORAGE)
            }
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.RECORD_AUDIO)
            }
        }

        if (permissionsToRequest.isNotEmpty()) {
            permissionLauncher.launch(permissionsToRequest.toTypedArray())
        }
    }

    // State Collection
    val playerState by mainViewModel.playerUiState.collectAsState()
    val playlists by mainViewModel.playlists.collectAsState()
    val accentColor by mainViewModel.accentColor.collectAsState()
    val playbackPosition by mainViewModel.playbackPositionMs.collectAsState()

    // Handle Shutdown Event
    LaunchedEffect(playerState.isShuttingDown) {
        if (playerState.isShuttingDown) {
            delay(3000)
            (context as? android.app.Activity)?.finish()
        }
    }

    // Listen for UI events
    LaunchedEffect(Unit) {
        mainViewModel.eventFlow.collect { message ->
            android.widget.Toast.makeText(context, message, android.widget.Toast.LENGTH_LONG).show()
        }
    }

    // UI state for sheets/dialogs
    var isFullPlayerExpanded by remember { mutableStateOf(false) }
    var isQueueSheetExpanded by remember { mutableStateOf(false) }
    var songForPlaylistSelection by remember { mutableStateOf<Song?>(null) }
    var showCreatePlaylistDialog by remember { mutableStateOf(false) }

    // Navigation state
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val showBottomBar = FusionNavDestination.values().any { it.route == currentRoute }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            if (showBottomBar) {
                Column(modifier = Modifier.navigationBarsPadding()) {
                    MiniPlayer(
                        playerState = playerState,
                        progressProvider = { playerState.progress },
                        onExpandPlayer = { isFullPlayerExpanded = true },
                        onTogglePlayPause = { mainViewModel.togglePlayPause() },
                        onSkipNext = { mainViewModel.skipToNext() },
                        onSkipPrevious = { mainViewModel.skipToPrevious() }
                    )

                    FusionBottomNav(
                        currentDestination = FusionNavDestination.values().find { it.route == currentRoute } ?: FusionNavDestination.HOME,
                        onNavigateTo = { destination ->
                            navController.navigate(destination.route) {
                                popUpTo(navController.graph.startDestinationId) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(innerPadding)
        ) {
            FusionNavHost(
                navController = navController,
                mainViewModel = mainViewModel,
                searchViewModel = searchViewModel,
                settingsViewModel = settingsViewModel,
                onSongForPlaylist = { songForPlaylistSelection = it },
                onOpenCreatePlaylist = { showCreatePlaylistDialog = true }
            )

            // Floating Power Dial
            if (showBottomBar) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 10.dp, end = 16.dp)
                        .statusBarsPadding()
                ) {
                    NeonPowerButton(
                        isOn = !playerState.isShuttingDown,
                        accentColor = accentColor,
                        onClick = { mainViewModel.shutdownApp() }
                    )
                }
            }
        }

        if (playerState.isShuttingDown) {
            ShutdownOverlay(accentColor = accentColor)
        }
    }

    // Full Player Modal Bottom Sheet
    if (isFullPlayerExpanded && (playerState.currentSong != null)) {
        FullPlayerSheet(
            playerState = playerState,
            accentColor = accentColor,
            positionMsProvider = { playbackPosition },
            onDismiss = { isFullPlayerExpanded = false },
            onTogglePlayPause = { mainViewModel.togglePlayPause() },
            onSeekTo = { mainViewModel.seekTo(it) },
            onSkipNext = { mainViewModel.skipToNext() },
            onSkipPrevious = { mainViewModel.skipToPrevious() },
            onToggleShuffle = { mainViewModel.toggleShuffle() },
            onCycleRepeat = { mainViewModel.cycleRepeatMode() },
            onToggleFavorite = { playerState.currentSong?.let { mainViewModel.toggleFavorite(it) } },
            onOpenQueue = { isQueueSheetExpanded = true },
            onSelectSleepTimer = { mainViewModel.setSleepTimer(it) },
            onSelectEqualizer = { mainViewModel.setEqualizerPreset(it) },
            onSetBandLevel = { band, level -> mainViewModel.setBandLevel(band, level) },
            onSetBassBoost = { strength -> mainViewModel.setBassBoost(strength) },
            onSetVirtualizer = { strength -> mainViewModel.setVirtualizer(strength) },
            onToggleKaraokeMode = { mainViewModel.setKaraokeModeActive(it) },
            onSetPitch = { mainViewModel.setPitchSemitones(it) },
            onToggleVocalReduction = { mainViewModel.setVocalReductionEnabled(it) },
            onSetVocalStrength = { mainViewModel.setVocalReductionStrength(it) },
            onSetPlaybackSpeed = { mainViewModel.setPlaybackSpeed(it) },
            onToggleNormalization = { mainViewModel.toggleNormalization() },
            onSetVolume = { mainViewModel.setVolume(it) }
        )
    }

    // Queue Sheet
    if (isQueueSheetExpanded) {
        QueueSheet(
            playerState = playerState,
            onDismiss = { isQueueSheetExpanded = false },
            onPlaySongAt = { song, idx -> mainViewModel.playSong(song, playerState.queue, idx) },
            onRemoveFromQueue = { mainViewModel.removeFromQueue(it) },
            onClearQueue = { mainViewModel.clearQueue() }
        )
    }

    // Add to Playlist Dialog
    songForPlaylistSelection?.let { song ->
        AddToPlaylistDialog(
            song = song,
            playlists = playlists,
            onDismiss = { songForPlaylistSelection = null },
            onSelectPlaylist = { playlist ->
                mainViewModel.addSongToPlaylist(playlist.id, song.id)
                songForPlaylistSelection = null
            },
            onOpenCreateDialog = { showCreatePlaylistDialog = true }
        )
    }

    // Create Playlist Dialog
    if (showCreatePlaylistDialog) {
        CreatePlaylistDialog(
            onDismiss = { showCreatePlaylistDialog = false },
            onCreate = { name, desc ->
                mainViewModel.createPlaylist(name, desc)
                showCreatePlaylistDialog = false
            }
        )
    }
}

@Composable
fun NeonPowerButton(isOn: Boolean, accentColor: Color, onClick: () -> Unit) {
    val glowColor = if (isOn) accentColor else Color.DarkGray
    val infiniteTransition = rememberInfiniteTransition(label = "power_glow")
    
    val shadowBlur by infiniteTransition.animateFloat(
        initialValue = 6f,
        targetValue = 12f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearOutSlowInEasing),
            repeatMode = AnimRepeatMode.Reverse
        ),
        label = "shadow_blur"
    )

    Surface(
        onClick = onClick,
        enabled = isOn,
        color = Color.Transparent,
        shape = CircleShape,
        modifier = Modifier
            .size(38.dp)
            .shadow(
                elevation = if (isOn) shadowBlur.dp else 0.dp,
                shape = CircleShape,
                spotColor = glowColor,
                ambientColor = glowColor
            )
            .border(1.5.dp, glowColor.copy(alpha = 0.6f), CircleShape)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(glowColor.copy(alpha = 0.25f), Color.Transparent)
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.PowerSettingsNew,
                contentDescription = "Apagar aplicación",
                tint = if (isOn) Color.White else Color.Gray,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
fun ShutdownOverlay(accentColor: Color) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.95f))
            .clickable(enabled = false) {},
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Default.PowerSettingsNew,
                contentDescription = null,
                tint = accentColor,
                modifier = Modifier.size(100.dp)
            )
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "FUSION MUSIC",
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Black, letterSpacing = 2.sp),
                color = Color.White
            )
            Text(
                text = "CERRANDO SESIÓN...",
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                color = accentColor.copy(alpha = 0.8f)
            )
            Spacer(modifier = Modifier.height(60.dp))
            CircularProgressIndicator(
                modifier = Modifier.size(40.dp),
                color = accentColor,
                strokeWidth = 3.dp
            )
        }
    }
}
