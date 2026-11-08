package com.example.melody.domain.model

data class Playlist(
    val id: Long,
    val name: String,
    val createdAt: Long,
    val songIds: List<Long> = emptyList(),
    val songs: List<Song> = emptyList()
)
