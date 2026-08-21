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
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
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
            FusionMusicTheme {
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
    val songs by viewModel.songs.collectAsState()
    val downloadedSongs by viewModel.downloadedSongs.collectAsState()
    val totalStorageBytes by viewModel.totalDownloadedStorageBytes.collectAsState()
    val downloadStates by viewModel.downloadStates.collectAsState()
    val isSearchingYouTube by viewModel.isSearchingYouTube.collectAsState()

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
    val onRescan: () -> Unit = remember(viewModel) { { viewModel.rescanLocalLibrary() } }

    Scaffold(
        containerColor = ObsidianDark,
        bottomBar = {
            if (!isViewingServices) {
                Column(modifier = Modifier.navigationBarsPadding()) {
                    // Mini Player (Only visible if a song is loaded)
                    MiniPlayer(
                        playerState = playerState,
                        onExpandPlayer = { isFullPlayerExpanded = true },
                        onTogglePlayPause = { viewModel.togglePlayPause() },
                        onSkipNext = { viewModel.skipToNext() }
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
                .background(ObsidianDark)
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
                    transitionSpec = { fadeIn() togetherWith fadeOut() },
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
                            isSearchingYouTube = isSearchingYouTube,
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
                            onRescan = onRescan,
                            onSelectEqualizer = { viewModel.setEqualizerPreset(it) },
                            onSelectSleepTimer = { viewModel.setSleepTimer(it) },
                            onToggleSkipSilence = { viewModel.setSkipSilenceEnabled(it) },
                            onNavigateToServices = { isViewingServices = true }
                        )
                    }
                }
            }
        }
    }

    // Full Player Modal Bottom Sheet
    if (isFullPlayerExpanded && (playerState.currentSong != null)) {
        FullPlayerSheet(
            playerState = playerState,
            accentColor = accentColor,
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
            onToggleKaraokeMode = { viewModel.setKaraokeModeActive(it) },
            onSetPitch = { viewModel.setPitchSemitones(it) },
            onToggleVocalReduction = { viewModel.setVocalReductionEnabled(it) },
            onSetVocalStrength = { viewModel.setVocalReductionStrength(it) }
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
