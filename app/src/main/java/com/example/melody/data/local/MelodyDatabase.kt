package com.example.melody.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        FavoriteSongEntity::class,
        PlaylistEntity::class,
        PlaylistSongCrossRef::class,
        PlaybackStatsEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class MelodyDatabase : RoomDatabase() {

    abstract fun favoriteSongDao(): FavoriteSongDao
    abstract fun playlistDao(): PlaylistDao
    abstract fun playbackStatsDao(): PlaybackStatsDao

    companion object {
        @Volatile
        private var INSTANCE: MelodyDatabase? = null

        fun getDatabase(context: Context): MelodyDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    MelodyDatabase::class.java,
                    "melody_database"
                )
                    .fallbackToDestructiveMigration(true)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
