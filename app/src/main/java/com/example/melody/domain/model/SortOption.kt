package com.example.melody.domain.model

enum class SortOption(val label: String) {
    DATE_ADDED("Más recientes"),
    MOST_PLAYED("Más reproducidas"),
    TITLE("A - Z"),
    ARTIST("Artista"),
    DURATION("Mayor duración")
}

enum class GroupSortOption(val label: String) {
    SONG_COUNT("Mayor cantidad de canciones"),
    NAME("A - Z"),
    NEWEST("Más recientes")
}
