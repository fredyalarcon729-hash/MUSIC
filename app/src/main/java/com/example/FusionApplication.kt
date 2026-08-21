package com.example

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import com.example.core.auth.AuthManager
import com.example.core.source.ConfigManager
import com.example.core.source.ExternalServicesManager
import com.example.core.source.LocalMusicSourceProvider
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

        createNotificationChannel()

        configManager = ConfigManager(this)
        database = FusionDatabase.getInstance(this)
        localProvider = LocalMusicSourceProvider(this)
        val resolver = YouTubeAudioResolver(configManager.getYouTubeApiKey())
        youTubeProvider = YouTubeMusicSourceProvider(resolver)
        downloadManager = YouTubeDownloadManager(this, database.musicDao(), resolver)

        servicesManager = ExternalServicesManager(this, localProvider).apply {
            registerProvider(youTubeProvider)
        }

        musicRepository = MusicRepository(
            localProvider = localProvider,
            musicDao = database.musicDao(),
            downloadManager = downloadManager,
            youtubeProvider = youTubeProvider
        )
        authManager = AuthManager(this)
        playerManager = FusionPlayerManager.getInstance(this)

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
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Notificación de control de reproducción de música"
                setShowBadge(false)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }
}
