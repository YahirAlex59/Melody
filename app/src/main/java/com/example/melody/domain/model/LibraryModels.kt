package com.example.melody.domain.model

enum class LibraryTab(val label: String) {
    SONGS("Canciones"),
    PLAYLISTS("Playlists"),
    FOLDERS("Carpetas"),
    ARTISTS("Artistas"),
    ALBUMS("Álbumes")
}

data class FolderGroup(
    val folderName: String,
    val folderPath: String,
    val songs: List<Song>
) {
    val songCount: Int get() = songs.size
}

data class ArtistGroup(
    val artistName: String,
    val songs: List<Song>
) {
    val songCount: Int get() = songs.size
    val coverArtUri: String? get() = songs.firstOrNull { !it.albumArtUri.isNullOrBlank() }?.albumArtUri
}

data class AlbumGroup(
    val albumName: String,
    val artistName: String,
    val albumArtUri: String?,
    val songs: List<Song>
) {
    val songCount: Int get() = songs.size
}

data class LyricLine(
    val timeMs: Long?,
    val text: String
)
