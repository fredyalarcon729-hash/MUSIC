package com.example

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import com.example.core.auth.AuthManager
import com.example.core.source.ConfigManager
import com.example.core.source.ExternalServicesManager
import com.example.core.source.LocalMusicSourceProvider
import com.example.core.source.deezer.DeezerMusicSourceProvider
import com.example.core.source.firebase.FirebaseMusicSourceProvider
import com.example.core.source.youtube.YouTubeAudioResolver
import com.example.core.source.youtube.YouTubeDownloadManager
import com.example.core.source.youtube.YouTubeMusicSourceProvider
import com.example.data.local.FusionDatabase
import com.example.data.repository.MusicRepository
import com.example.player.FusionPlayerManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class FusionApplication : Application() {

    lateinit var database: FusionDatabase
        private set

    lateinit var localProvider: LocalMusicSourceProvider
        private set

    lateinit var youTubeProvider: YouTubeMusicSourceProvider
        private set

    lateinit var deezerProvider: DeezerMusicSourceProvider
        private set

    lateinit var firebaseProvider: FirebaseMusicSourceProvider
        private set

    lateinit var configManager: ConfigManager
        private set

    lateinit var downloadManager: YouTubeDownloadManager
        private set

    lateinit var servicesManager: ExternalServicesManager
        private set

    lateinit var musicRepository: MusicRepository
        private set

    lateinit var authManager: AuthManager
        private set

    lateinit var playerManager: FusionPlayerManager
        private set

    override fun onCreate() {
        super.onCreate()

        // Initialize Firebase early to prevent service access crashes
        try {
            com.google.firebase.FirebaseApp.initializeApp(this)
        } catch (e: Exception) {
            android.util.Log.e("FusionApplication", "Failed to initialize Firebase: ${e.message}")
        }

        createNotificationChannel()

        configManager = ConfigManager(this)
        database = FusionDatabase.getInstance(this)
        localProvider = LocalMusicSourceProvider(this)
        val resolver = YouTubeAudioResolver(this, configManager.getYouTubeApiKey(), null, configManager.getRapidApiKey())
        youTubeProvider = YouTubeMusicSourceProvider(resolver)
        deezerProvider = DeezerMusicSourceProvider()
        firebaseProvider = FirebaseMusicSourceProvider()
        downloadManager = YouTubeDownloadManager(this, database.musicDao(), resolver)

        servicesManager = ExternalServicesManager(this, localProvider).apply {
            registerProvider(youTubeProvider)
            registerProvider(deezerProvider)
            registerProvider(firebaseProvider)
        }

        musicRepository = MusicRepository(
            localProvider = localProvider,
            musicDao = database.musicDao(),
            downloadManager = downloadManager,
            youtubeProvider = youTubeProvider,
            deezerProvider = deezerProvider,
            firebaseProvider = firebaseProvider,
            configManager = configManager
        )
        authManager = AuthManager(this)
        playerManager = FusionPlayerManager.getInstance(this)
        playerManager.restorePlaybackState()

        playerManager.setOnSongCompletedCallback { song ->
            CoroutineScope(Dispatchers.IO).launch {
                musicRepository.recordPlayedSong(song.id, song.durationMs)
            }
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "fusion_playback_channel",
                "Reproducción Fusion Music",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notificación de control de reproducción de música"
                setShowBadge(false)
                lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }
}
