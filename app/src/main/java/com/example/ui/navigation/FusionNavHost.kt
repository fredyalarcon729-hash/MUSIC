package com.example.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.core.model.Song
import com.example.ui.FusionMainViewModel
import com.example.ui.components.FusionNavDestination
import com.example.ui.screens.*
import com.example.ui.viewmodels.SearchViewModel
import com.example.ui.viewmodels.SettingsViewModel

@Composable
fun FusionNavHost(
    navController: NavHostController,
    mainViewModel: FusionMainViewModel,
    searchViewModel: SearchViewModel,
    settingsViewModel: SettingsViewModel,
    onSongForPlaylist: (Song) -> Unit,
    onOpenCreatePlaylist: () -> Unit,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = FusionNavDestination.HOME.route,
        modifier = modifier
    ) {
        composable(FusionNavDestination.HOME.route) {
            val songs by mainViewModel.songs.collectAsState()
            val albums by mainViewModel.albums.collectAsState()
            val favoriteSongs by mainViewModel.favoriteSongs.collectAsState()
            val playerState by mainViewModel.playerUiState.collectAsState()
            val isScanning by mainViewModel.isScanning.collectAsState()

            HomeScreen(
                songs = songs,
                albums = albums,
                favoriteSongs = favoriteSongs,
                playerState = playerState,
                isScanning = isScanning,
                onPlaySong = { song, queue -> mainViewModel.playSong(song, queue) },
                onPlayAll = { list, shuffle -> mainViewModel.playAll(list, shuffle) },
                onToggleFavorite = { mainViewModel.toggleFavorite(it) },
                onPlayNext = { mainViewModel.playNext(it) },
                onAddToQueue = { mainViewModel.addToQueue(it) },
                onAddToPlaylist = onSongForPlaylist,
                onRescan = { mainViewModel.rescanLocalLibrary() },
                onOpenAlbum = { navController.navigate(FusionNavDestination.LIBRARY.route) },
                onNavigateToServices = { navController.navigate("services") }
            )
        }

        composable(FusionNavDestination.LIBRARY.route) {
            val songs by mainViewModel.songs.collectAsState()
            val downloadedSongs by mainViewModel.downloadedSongs.collectAsState()
            val totalStorageBytes by mainViewModel.totalDownloadedStorageBytes.collectAsState()
            val albums by mainViewModel.albums.collectAsState()
            val artists by mainViewModel.artists.collectAsState()
            val playerState by mainViewModel.playerUiState.collectAsState()
            val downloadStates by mainViewModel.downloadStates.collectAsState()

            LibraryScreen(
                songs = songs,
                downloadedSongs = downloadedSongs,
                totalStorageBytes = totalStorageBytes,
                albums = albums,
                artists = artists,
                playerState = playerState,
                downloadStates = downloadStates,
                onPlaySong = { song, queue -> mainViewModel.playSong(song, queue) },
                onPlayAll = { list, shuffle -> mainViewModel.playAll(list, shuffle) },
                onToggleFavorite = { mainViewModel.toggleFavorite(it) },
                onPlayNext = { mainViewModel.playNext(it) },
                onAddToQueue = { mainViewModel.addToQueue(it) },
                onAddToPlaylist = onSongForPlaylist,
                onDeleteDownload = { mainViewModel.deleteDownload(it) },
                onNavigateToSearch = { navController.navigate(FusionNavDestination.SEARCH.route) }
            )
        }

        composable(FusionNavDestination.SEARCH.route) {
            val query by searchViewModel.searchQuery.collectAsState()
            val selectedFilter by searchViewModel.searchFilter.collectAsState()
            val searchResults by searchViewModel.searchResults.collectAsState()
            val searchHistory by searchViewModel.searchHistory.collectAsState()
            val playerState by mainViewModel.playerUiState.collectAsState()
            val allSongs by mainViewModel.songs.collectAsState()
            val downloadStates by mainViewModel.downloadStates.collectAsState()

            SearchScreen(
                query = query,
                selectedFilter = selectedFilter,
                searchResults = searchResults,
                playerState = playerState,
                allSongs = allSongs,
                downloadStates = downloadStates,
                searchHistory = searchHistory,
                onQueryChanged = { searchViewModel.updateSearchQuery(it) },
                onFilterChanged = { searchViewModel.updateSearchFilter(it) },
                onPlaySong = { song, queue -> mainViewModel.playSong(song, queue) },
                onToggleFavorite = { mainViewModel.toggleFavorite(it) },
                onPlayNext = { mainViewModel.playNext(it) },
                onAddToQueue = { mainViewModel.addToQueue(it) },
                onAddToPlaylist = onSongForPlaylist,
                onClearHistory = { searchViewModel.clearSearchHistory() },
                onDownloadSong = { mainViewModel.startDownload(it) },
                onCancelDownload = { mainViewModel.cancelDownload(it) },
                onDeleteDownload = { mainViewModel.deleteDownload(it) }
            )
        }

        composable(FusionNavDestination.STATS.route) {
            val totalTimeMs by mainViewModel.totalListeningTimeMs.collectAsState()
            val topArtists by mainViewModel.topArtists.collectAsState()
            val hourlyActivity by mainViewModel.hourlyActivity.collectAsState()

            StatsScreen(
                totalTimeMs = totalTimeMs,
                topArtists = topArtists,
                hourlyActivity = hourlyActivity
            )
        }

        composable(FusionNavDestination.PLAYLISTS.route) {
            val playlists by mainViewModel.playlists.collectAsState()
            val favoriteSongs by mainViewModel.favoriteSongs.collectAsState()
            val playerState by mainViewModel.playerUiState.collectAsState()

            PlaylistsScreen(
                playlists = playlists,
                favoriteSongs = favoriteSongs,
                playerState = playerState,
                onOpenCreateDialog = onOpenCreatePlaylist,
                onDeletePlaylist = { mainViewModel.deletePlaylist(it) },
                onPlaySong = { song, queue -> mainViewModel.playSong(song, queue) },
                onPlayAll = { list, shuffle -> mainViewModel.playAll(list, shuffle) },
                onToggleFavorite = { mainViewModel.toggleFavorite(it) },
                onPlayNext = { mainViewModel.playNext(it) },
                onAddToQueue = { mainViewModel.addToQueue(it) },
                onAddToPlaylist = onSongForPlaylist,
                onRemoveFromPlaylist = { pId, sId -> mainViewModel.removeSongFromPlaylist(pId, sId) },
                getSongsForPlaylist = { mainViewModel.getSongsForPlaylist(it) }
            )
        }

        composable(FusionNavDestination.SETTINGS.route) {
            val playerState by mainViewModel.playerUiState.collectAsState()
            val isScanning by mainViewModel.isScanning.collectAsState()
            val themeMode by settingsViewModel.themeMode.collectAsState()
            val visualizerStyle by settingsViewModel.visualizerStyle.collectAsState()
            val customAccentColor by settingsViewModel.customAccentColor.collectAsState()
            val filterVoiceNotes by settingsViewModel.filterVoiceNotes.collectAsState()
            val filterDuplicates by settingsViewModel.filterDuplicates.collectAsState()
            val isAmbientAuraEnabled by settingsViewModel.isAmbientAuraEnabled.collectAsState()
            val ambientAuraStyle by settingsViewModel.ambientAuraStyle.collectAsState()
            val ambientAuraIntensity by settingsViewModel.ambientAuraIntensity.collectAsState()
            val ambientAuraWeight by settingsViewModel.ambientAuraWeight.collectAsState()

            SettingsScreen(
                playerState = playerState,
                isScanning = isScanning,
                themeMode = themeMode,
                visualizerStyle = visualizerStyle,
                customAccentColor = customAccentColor,
                filterVoiceNotes = filterVoiceNotes,
                filterDuplicates = filterDuplicates,
                isAmbientAuraEnabled = isAmbientAuraEnabled,
                ambientAuraStyle = ambientAuraStyle,
                ambientAuraIntensity = ambientAuraIntensity,
                ambientAuraWeight = ambientAuraWeight,
                onRescan = { mainViewModel.rescanLocalLibrary() },
                onSelectEqualizer = { mainViewModel.setEqualizerPreset(it) },
                onSetBandLevel = { band, level -> mainViewModel.setBandLevel(band, level) },
                onSetBassBoost = { strength -> mainViewModel.setBassBoost(strength) },
                onSetVirtualizer = { strength -> mainViewModel.setVirtualizer(strength) },
                onSelectSleepTimer = { mainViewModel.setSleepTimer(it) },
                onToggleSkipSilence = { mainViewModel.setSkipSilenceEnabled(it) },
                onToggleFilterVoiceNotes = { settingsViewModel.setFilterVoiceNotesEnabled(it) },
                onToggleFilterDuplicates = { settingsViewModel.setFilterDuplicatesEnabled(it) },
                onToggleAmbientAura = { settingsViewModel.setAmbientAuraEnabled(it) },
                onSetAmbientAuraStyle = { settingsViewModel.updateAmbientAuraStyle(it) },
                onSetAmbientAuraIntensity = { settingsViewModel.updateAmbientAuraIntensity(it) },
                onSetAmbientAuraWeight = { settingsViewModel.updateAmbientAuraWeight(it) },
                onSetVisualizerStyle = { settingsViewModel.updateVisualizerStyle(it) },
                onSetCustomAccentColor = {
                    settingsViewModel.updateCustomAccentColor(it)
                    mainViewModel.updateThemeForCurrentSong()
                },
                onSetThemeMode = { settingsViewModel.updateThemeMode(it) },
                onNavigateToServices = { navController.navigate("services") }
            )
        }

        composable("services") {
            val servicesState by settingsViewModel.servicesState.collectAsState(initial = emptyList())
            val youtubeApiKey by settingsViewModel.youtubeApiKey.collectAsState()
            val googleClientId by settingsViewModel.googleClientId.collectAsState()
            val userSession by mainViewModel.userSession.collectAsState()

            ServicesScreen(
                services = servicesState,
                youtubeApiKey = youtubeApiKey,
                googleClientId = googleClientId,
                userSession = userSession,
                onUpdateYouTubeApiKey = { settingsViewModel.updateYouTubeApiKey(it) },
                onUpdateGoogleClientId = { settingsViewModel.updateGoogleClientId(it) },
                onToggleService = { source, enabled -> settingsViewModel.toggleExternalService(source, enabled) },
                onSignIn = { ctx, clientId -> mainViewModel.signInWithGoogle(ctx, clientId ?: settingsViewModel.googleClientId.value) },
                onSignOut = { mainViewModel.signOut() },
                onGetDiagnosticInfo = { mainViewModel.getDiagnosticInfo() },
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
