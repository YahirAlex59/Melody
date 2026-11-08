package com.example.melody.data.repository

import com.example.melody.data.local.FavoriteSongDao
import com.example.melody.data.local.FavoriteSongEntity
import com.example.melody.data.local.MediaStoreScanner
import com.example.melody.data.local.PlaybackStatsDao
import com.example.melody.data.local.PlaybackStatsEntity
import com.example.melody.data.local.PlaylistDao
import com.example.melody.data.local.PlaylistEntity
import com.example.melody.data.local.PlaylistSongCrossRef
import com.example.melody.domain.model.LyricLine
import com.example.melody.domain.model.Playlist
import com.example.melody.domain.model.Song
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

class MusicRepository(
    private val mediaStoreScanner: MediaStoreScanner,
    private val favoriteSongDao: FavoriteSongDao,
    private val playlistDao: PlaylistDao,
    private val playbackStatsDao: PlaybackStatsDao
) {

    val favoriteSongIdsFlow: Flow<Set<Long>> = favoriteSongDao.getAllFavorites()
        .map { entities -> entities.map { it.songId }.toSet() }

    val playbackStatsMapFlow: Flow<Map<Long, PlaybackStatsEntity>> = playbackStatsDao.getAllStats()
        .map { list -> list.associateBy { it.songId } }

    val playlistsMetadataFlow: Flow<List<Playlist>> = combine(
        playlistDao.getAllPlaylists(),
        playlistDao.getAllPlaylistSongCrossRefs()
    ) { playlistEntities, crossRefs ->
        val crossRefsByPlaylist = crossRefs.groupBy { it.playlistId }
        playlistEntities.map { entity ->
            val songIds = crossRefsByPlaylist[entity.playlistId]
                ?.map { it.songId }
                ?: emptyList()
            Playlist(
                id = entity.playlistId,
                name = entity.name,
                createdAt = entity.createdAt,
                songIds = songIds
            )
        }
    }

    suspend fun scanDeviceSongs(): List<Song> {
        return mediaStoreScanner.scanSongs()
    }

    suspend fun readLyricsForSong(song: Song): List<LyricLine> {
        return mediaStoreScanner.readLyricsForSong(song)
    }

    suspend fun toggleFavorite(songId: Long) {
        if (favoriteSongDao.isFavorite(songId)) {
            favoriteSongDao.deleteFavoriteById(songId)
        } else {
            favoriteSongDao.insertFavorite(FavoriteSongEntity(songId = songId))
        }
    }

    suspend fun recordSongPlayed(songId: Long) {
        val existing = playbackStatsDao.getStatsForSong(songId)
        val updated = if (existing != null) {
            existing.copy(
                playCount = existing.playCount + 1,
                lastPlayedAt = System.currentTimeMillis()
            )
        } else {
            PlaybackStatsEntity(
                songId = songId,
                playCount = 1,
                lastPlayedAt = System.currentTimeMillis(),
                isHidden = false
            )
        }
        playbackStatsDao.upsertStats(updated)
    }

    suspend fun setSongHidden(songId: Long, isHidden: Boolean) {
        val existing = playbackStatsDao.getStatsForSong(songId)
        val updated = existing?.copy(isHidden = isHidden)
            ?: PlaybackStatsEntity(
                songId = songId,
                playCount = 0,
                lastPlayedAt = 0L,
                isHidden = isHidden
            )
        playbackStatsDao.upsertStats(updated)
    }

    suspend fun restoreAllHiddenSongs() {
        playbackStatsDao.restoreAllHiddenSongs()
    }

    suspend fun createPlaylist(name: String): Long {
        val cleanName = name.trim()
        if (cleanName.isEmpty()) return -1L
        return playlistDao.insertPlaylist(PlaylistEntity(name = cleanName))
    }

    suspend fun deletePlaylist(playlistId: Long) {
        playlistDao.deletePlaylistById(playlistId)
    }

    suspend fun addSongToPlaylist(playlistId: Long, songId: Long) {
        playlistDao.insertPlaylistSongCrossRef(
            PlaylistSongCrossRef(playlistId = playlistId, songId = songId)
        )
    }

    suspend fun removeSongFromPlaylist(playlistId: Long, songId: Long) {
        playlistDao.removeSongFromPlaylist(playlistId = playlistId, songId = songId)
    }
}
