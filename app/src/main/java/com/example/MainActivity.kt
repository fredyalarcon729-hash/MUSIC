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
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.animation.core.RepeatMode as AnimRepeatMode
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material3.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import kotlinx.coroutines.delay
import com.example.core.model.Song
import com.example.ui.FusionMainViewModel
import com.example.ui.components.FusionBottomNav
import com.example.ui.components.FusionNavDestination
import com.example.ui.components.MiniPlayer
import com.example.ui.player.AddToPlaylistDialog
import com.example.ui.player.CreatePlaylistDialog
import com.example.ui.player.FullPlayerSheet
import com.example.ui.player.QueueSheet
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.LibraryScreen
import com.example.ui.screens.PlaylistsScreen
import com.example.ui.screens.SearchScreen
import com.example.ui.screens.ServicesScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.StatsScreen
import com.example.ui.theme.FusionMusicTheme
import com.example.ui.theme.ObsidianDark

class MainActivity : ComponentActivity() {

    private val viewModel: FusionMainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val themeMode by viewModel.themeMode.collectAsState()
            val isDarkTheme = when (themeMode) {
                "light" -> false
                "dark" -> true
                "system" -> androidx.compose.foundation.isSystemInDarkTheme()
                else -> true // Default to Dark
            }

            FusionMusicTheme(darkTheme = isDarkTheme) {
                MainAppContent(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun MainAppContent(viewModel: FusionMainViewModel) {
    val context = LocalContext.current

    // Request permissions dynamically
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions.values.any { it }
        if (granted) {
            viewModel.rescanLocalLibrary()
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
    val themeMode by viewModel.themeMode.collectAsState()
    val visualizerStyle by viewModel.visualizerStyle.collectAsState()
    val customAccentColor by viewModel.customAccentColor.collectAsState()
    val filterVoiceNotes by viewModel.filterVoiceNotes.collectAsState()
    val filterDuplicates by viewModel.filterDuplicates.collectAsState()
    val isDarkTheme = when (themeMode) {
        "light" -> false
        "dark" -> true
        else -> androidx.compose.foundation.isSystemInDarkTheme()
    }

    val songs by viewModel.songs.collectAsState()
    val downloadedSongs by viewModel.downloadedSongs.collectAsState()
    val totalStorageBytes by viewModel.totalDownloadedStorageBytes.collectAsState()
    val downloadStates by viewModel.downloadStates.collectAsState()

    val albums by viewModel.albums.collectAsState()
    val artists by viewModel.artists.collectAsState()
    val favoriteSongs by viewModel.favoriteSongs.collectAsState()
    val playlists by viewModel.playlists.collectAsState()
    val playerState by viewModel.playerUiState.collectAsState()
    val isScanning by viewModel.isScanning.collectAsState()
    val servicesState by viewModel.servicesState.collectAsState()
    val youtubeApiKey by viewModel.youtubeApiKey.collectAsState()
    val googleClientId by viewModel.googleClientId.collectAsState()
    val accentColor by viewModel.accentColor.collectAsState()
    val playbackPosition by viewModel.playbackPositionMs.collectAsState()

    // Handle Shutdown Event
    LaunchedEffect(playerState.isShuttingDown) {
        if (playerState.isShuttingDown) {
            delay(3000) // Give time for the creative animation
            (context as? android.app.Activity)?.finish()
        }
    }

    val searchQuery by viewModel.searchQuery.collectAsState()
    val searchFilter by viewModel.searchFilter.collectAsState()
    val searchResults by viewModel.searchResults.collectAsState()
    val searchHistory by viewModel.searchHistory.collectAsState()

    val totalTimeMs by viewModel.totalListeningTimeMs.collectAsState()
    val topArtists by viewModel.topArtists.collectAsState()
    val hourlyActivity by viewModel.hourlyActivity.collectAsState()
    val userSession by viewModel.userSession.collectAsState()

    // Navigation & Sheet Dialog States
    var currentDestination by remember { mutableStateOf(FusionNavDestination.HOME) }
    var isViewingServices by remember { mutableStateOf(value = false) }

    // Listen for UI events from ViewModel
    LaunchedEffect(Unit) {
        viewModel.eventFlow.collect { message ->
            android.widget.Toast.makeText(context, message, android.widget.Toast.LENGTH_LONG).show()
        }
    }

    var isFullPlayerExpanded by remember { mutableStateOf(value = false) }
    var isQueueSheetExpanded by remember { mutableStateOf(value = false) }

    var songForPlaylistSelection by remember { mutableStateOf<Song?>(null) }
    var showCreatePlaylistDialog by remember { mutableStateOf(value = false) }

    // Optimization: Memoize common callbacks to prevent unnecessary recompositions
    val onPlaySong: (Song, List<Song>) -> Unit = remember(viewModel) { { song, queue -> viewModel.playSong(song, queue) } }
    val onToggleFavorite: (Song) -> Unit = remember(viewModel) { { song -> viewModel.toggleFavorite(song) } }
    val onPlayNext: (Song) -> Unit = remember(viewModel) { { song -> viewModel.playNext(song) } }
    val onAddToQueue: (Song) -> Unit = remember(viewModel) { { song -> viewModel.addToQueue(song) } }
    val onPlayAll: (List<Song>, Boolean) -> Unit = remember(viewModel) { { list, shuffle -> viewModel.playAll(list, shuffle) } }
    val onSkipPrevious: () -> Unit = remember(viewModel) { { viewModel.skipToPrevious() } }
    val onRescan: () -> Unit = remember(viewModel) { { viewModel.rescanLocalLibrary() } }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            if (!isViewingServices) {
                Column(modifier = Modifier.navigationBarsPadding()) {
                    // Mini Player (Only visible if a song is loaded)
                    MiniPlayer(
                        playerState = playerState,
                        progressProvider = { playerState.progress }, // This will still recompose MiniPlayer on position change because it's accessing playerState.progress
                        onExpandPlayer = { isFullPlayerExpanded = true },
                        onTogglePlayPause = { viewModel.togglePlayPause() },
                        onSkipNext = { viewModel.skipToNext() },
                        onSkipPrevious = onSkipPrevious
                    )

                    // Persistent Material 3 Bottom Nav Bar
                    FusionBottomNav(
                        currentDestination = currentDestination,
                        onNavigateTo = { currentDestination = it }
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
            if (isViewingServices) {
                ServicesScreen(
                    services = servicesState,
                    youtubeApiKey = youtubeApiKey,
                    googleClientId = googleClientId,
                    userSession = userSession,
                    onUpdateYouTubeApiKey = { viewModel.updateYouTubeApiKey(it) },
                    onUpdateGoogleClientId = { viewModel.updateGoogleClientId(it) },
                    onToggleService = { source, enabled -> viewModel.toggleExternalService(source, enabled) },
                    onSignIn = { ctx, clientId -> viewModel.signInWithGoogle(ctx, clientId) },
                    onSignOut = { viewModel.signOut() },
                    onGetDiagnosticInfo = { viewModel.getDiagnosticInfo() },
                    onNavigateBack = { isViewingServices = false }
                )
            } else {
                AnimatedContent(
                    targetState = currentDestination,
                    transitionSpec = {
                        val direction = if (targetState.ordinal > initialState.ordinal) 1 else -1
                        (slideInHorizontally { width -> direction * width } + fadeIn())
                            .togetherWith(slideOutHorizontally { width -> -direction * width } + fadeOut())
                    },
                    label = "screen_transition"
                ) { target ->
                    when (target) {
                        FusionNavDestination.HOME -> HomeScreen(
                            songs = songs,
                            albums = albums,
                            favoriteSongs = favoriteSongs,
                            playerState = playerState,
                            isScanning = isScanning,
                            onPlaySong = onPlaySong,
                            onPlayAll = onPlayAll,
                            onToggleFavorite = onToggleFavorite,
                            onPlayNext = onPlayNext,
                            onAddToQueue = onAddToQueue,
                            onAddToPlaylist = { songForPlaylistSelection = it },
                            onRescan = onRescan,
                            onOpenAlbum = { _ ->
                                currentDestination = FusionNavDestination.LIBRARY
                            },
                            onNavigateToServices = { isViewingServices = true }
                        )

                        FusionNavDestination.LIBRARY -> LibraryScreen(
                            songs = songs,
                            downloadedSongs = downloadedSongs,
                            totalStorageBytes = totalStorageBytes,
                            albums = albums,
                            artists = artists,
                            playerState = playerState,
                            downloadStates = downloadStates,
                            onPlaySong = onPlaySong,
                            onPlayAll = onPlayAll,
                            onToggleFavorite = onToggleFavorite,
                            onPlayNext = onPlayNext,
                            onAddToQueue = onAddToQueue,
                            onAddToPlaylist = { songForPlaylistSelection = it },
                            onDeleteDownload = { viewModel.deleteDownload(it) },
                            onNavigateToSearch = { currentDestination = FusionNavDestination.SEARCH }
                        )

                        FusionNavDestination.SEARCH -> SearchScreen(
                            query = searchQuery,
                            selectedFilter = searchFilter,
                            searchResults = searchResults,
                            playerState = playerState,
                            allSongs = songs,
                            downloadStates = downloadStates,
                            searchHistory = searchHistory,
                            onQueryChanged = { viewModel.updateSearchQuery(it) },
                            onFilterChanged = { viewModel.updateSearchFilter(it) },
                            onPlaySong = onPlaySong,
                            onToggleFavorite = onToggleFavorite,
                            onPlayNext = onPlayNext,
                            onAddToQueue = onAddToQueue,
                            onAddToPlaylist = { songForPlaylistSelection = it },
                            onClearHistory = { viewModel.clearSearchHistory() },
                            onDownloadSong = { viewModel.startDownload(it) },
                            onCancelDownload = { viewModel.cancelDownload(it) },
                            onDeleteDownload = { viewModel.deleteDownload(it) }
                        )

                        FusionNavDestination.STATS -> StatsScreen(
                            totalTimeMs = totalTimeMs,
                            topArtists = topArtists,
                            hourlyActivity = hourlyActivity
                        )

                        FusionNavDestination.PLAYLISTS -> PlaylistsScreen(
                            playlists = playlists,
                            favoriteSongs = favoriteSongs,
                            playerState = playerState,
                            onOpenCreateDialog = { showCreatePlaylistDialog = true },
                            onDeletePlaylist = { viewModel.deletePlaylist(it) },
                            onPlaySong = onPlaySong,
                            onPlayAll = onPlayAll,
                            onToggleFavorite = onToggleFavorite,
                            onPlayNext = onPlayNext,
                            onAddToQueue = onAddToQueue,
                            onAddToPlaylist = { songForPlaylistSelection = it },
                            onRemoveFromPlaylist = { pId, sId -> viewModel.removeSongFromPlaylist(pId, sId) },
                            getSongsForPlaylist = { viewModel.getSongsForPlaylist(it) }
                        )

                        FusionNavDestination.SETTINGS -> SettingsScreen(
                            playerState = playerState,
                            isScanning = isScanning,
                            themeMode = themeMode,
                            visualizerStyle = visualizerStyle,
                            customAccentColor = customAccentColor,
                            filterVoiceNotes = filterVoiceNotes,
                            filterDuplicates = filterDuplicates,
                            onRescan = onRescan,
                            onSelectEqualizer = { viewModel.setEqualizerPreset(it) },
                            onSetBandLevel = { band, level -> viewModel.setBandLevel(band, level) },
                            onSetBassBoost = { strength -> viewModel.setBassBoost(strength) },
                            onSetVirtualizer = { strength -> viewModel.setVirtualizer(strength) },
                            onSelectSleepTimer = { viewModel.setSleepTimer(it) },
                            onToggleSkipSilence = { viewModel.setSkipSilenceEnabled(it) },
                            onToggleFilterVoiceNotes = { viewModel.setFilterVoiceNotesEnabled(it) },
                            onToggleFilterDuplicates = { viewModel.setFilterDuplicatesEnabled(it) },
                            onSetVisualizerStyle = { viewModel.updateVisualizerStyle(it) },
                            onSetCustomAccentColor = { viewModel.updateCustomAccentColor(it) },
                            onSetThemeMode = { viewModel.updateThemeMode(it) },
                            onNavigateToServices = { isViewingServices = true }
                        )
                    }
                }
            }

            // Floating Power Dial
            if (!isViewingServices) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 12.dp, end = 16.dp)
                        .statusBarsPadding()
                ) {
                    NeonPowerButton(isOn = !playerState.isShuttingDown, accentColor = accentColor)
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
            onTogglePlayPause = { viewModel.togglePlayPause() },
            onSeekTo = { viewModel.seekTo(it) },
            onSkipNext = { viewModel.skipToNext() },
            onSkipPrevious = { viewModel.skipToPrevious() },
            onToggleShuffle = { viewModel.toggleShuffle() },
            onCycleRepeat = { viewModel.cycleRepeatMode() },
            onToggleFavorite = { playerState.currentSong?.let { viewModel.toggleFavorite(it) } },
            onOpenQueue = { isQueueSheetExpanded = true },
            onSelectSleepTimer = { viewModel.setSleepTimer(it) },
            onSelectEqualizer = { viewModel.setEqualizerPreset(it) },
            onSetBandLevel = { band, level -> viewModel.setBandLevel(band, level) },
            onSetBassBoost = { strength -> viewModel.setBassBoost(strength) },
            onSetVirtualizer = { strength -> viewModel.setVirtualizer(strength) },
            onToggleKaraokeMode = { viewModel.setKaraokeModeActive(it) },
            onSetPitch = { viewModel.setPitchSemitones(it) },
            onToggleVocalReduction = { viewModel.setVocalReductionEnabled(it) },
            onSetVocalStrength = { viewModel.setVocalReductionStrength(it) },
            onSetPlaybackSpeed = { viewModel.setPlaybackSpeed(it) },
            onToggleNormalization = { viewModel.toggleNormalization() },
            onSetVolume = { viewModel.setVolume(it) }
        )
    }

    // Queue Sheet
    if (isQueueSheetExpanded) {
        QueueSheet(
            playerState = playerState,
            onDismiss = { isQueueSheetExpanded = false },
            onPlaySongAt = { song, idx -> viewModel.playSong(song, playerState.queue, idx) },
            onRemoveFromQueue = { viewModel.removeFromQueue(it) },
            onClearQueue = { viewModel.clearQueue() }
        )
    }

    // Add to Playlist Dialog
    songForPlaylistSelection?.let { song ->
        AddToPlaylistDialog(
            song = song,
            playlists = playlists,
            onDismiss = { songForPlaylistSelection = null },
            onSelectPlaylist = { playlist ->
                viewModel.addSongToPlaylist(playlist.id, song.id)
                songForPlaylistSelection = null
            },
            onOpenCreateDialog = {
                showCreatePlaylistDialog = true
            }
        )
    }

    // Create Playlist Dialog
    if (showCreatePlaylistDialog) {
        CreatePlaylistDialog(
            onDismiss = { showCreatePlaylistDialog = false },
            onCreate = { name, desc ->
                viewModel.createPlaylist(name, desc)
                showCreatePlaylistDialog = false
            }
        )
    }
}

@Composable
fun NeonPowerButton(isOn: Boolean, accentColor: Color) {
    val glowColor = if (isOn) accentColor else Color.DarkGray
    val infiniteTransition = rememberInfiniteTransition(label = "power_glow")
    
    val shadowBlur by infiniteTransition.animateFloat(
        initialValue = 8f,
        targetValue = 16f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearOutSlowInEasing),
            repeatMode = AnimRepeatMode.Reverse
        ),
        label = "shadow_blur"
    )

    Box(
        modifier = Modifier
            .size(42.dp)
            .shadow(
                elevation = if (isOn) shadowBlur.dp else 0.dp,
                shape = CircleShape,
                spotColor = glowColor,
                ambientColor = glowColor
            )
            .clip(CircleShape)
            .background(
                Brush.radialGradient(
                    colors = listOf(glowColor.copy(alpha = 0.4f), glowColor.copy(alpha = 0.1f))
                )
            )
            .border(2.dp, glowColor.copy(alpha = 0.8f), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        // Inner circle "Button"
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .background(if (isOn) Color.White else Color.Black)
                .border(1.dp, glowColor, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.PowerSettingsNew,
                contentDescription = null,
                tint = if (isOn) accentColor else Color.Gray,
                modifier = Modifier.size(16.dp)
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
            .clickable(enabled = false) {}, // Intercept clicks
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
