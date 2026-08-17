package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.local.dao.MusicDao
import com.example.data.local.entity.DownloadedSongEntity
import com.example.data.local.entity.FavoriteEntity
import com.example.data.local.entity.HistoryEntity
import com.example.data.local.entity.PlaylistEntity
import com.example.data.local.entity.PlaylistItemEntity

@Database(
    entities = [
        FavoriteEntity::class,
        PlaylistEntity::class,
        PlaylistItemEntity::class,
        HistoryEntity::class,
        DownloadedSongEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class FusionDatabase : RoomDatabase() {

    abstract fun musicDao(): MusicDao

    companion object {
        @Volatile
        private var INSTANCE: FusionDatabase? = null

        fun getInstance(context: Context): FusionDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    FusionDatabase::class.java,
                    "fusion_music_db"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}
