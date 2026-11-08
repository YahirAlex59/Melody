package com.example.melody.domain.model

import java.util.Locale

data class Song(
    val id: Long,
    val title: String,
    val artist: String,
    val album: String,
    val duration: Long,
    val contentUri: String,
    val albumArtUri: String?,
    val dateAdded: Long,
    val filePath: String = "",
    val folderName: String = "Música",
    val folderPath: String = "",
    val fileSize: Long = 0L,
    val hasLyrics: Boolean = false,
    val lrcFilePath: String? = null
) {
    val formattedDuration: String
        get() {
            val totalSeconds = (duration / 1000).coerceAtLeast(0)
            val minutes = totalSeconds / 60
            val seconds = totalSeconds % 60
            return "%02d:%02d".format(minutes, seconds)
        }

    val formattedFileSize: String
        get() {
            if (fileSize <= 0L) return "Desconocido"
            val kb = fileSize / 1024.0
            val mb = kb / 1024.0
            return if (mb >= 1.0) {
                String.format(Locale.US, "%.2f MB", mb)
            } else {
                String.format(Locale.US, "%.0f KB", kb)
            }
        }
}
