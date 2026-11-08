package com.example.melody.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface PlaybackStatsDao {

    @Query("SELECT * FROM playback_stats")
    fun getAllStats(): Flow<List<PlaybackStatsEntity>>

    @Query("SELECT * FROM playback_stats WHERE songId = :songId LIMIT 1")
    suspend fun getStatsForSong(songId: Long): PlaybackStatsEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertStats(entity: PlaybackStatsEntity)

    @Query("UPDATE playback_stats SET isHidden = 0 WHERE isHidden = 1")
    suspend fun restoreAllHiddenSongs()
}
