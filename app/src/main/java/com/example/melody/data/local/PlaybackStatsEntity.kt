package com.example.melody.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "playback_stats")
data class PlaybackStatsEntity(
    @PrimaryKey
    val songId: Long,
    val playCount: Int = 0,
    val lastPlayedAt: Long = 0L,
    val isHidden: Boolean = false
)
