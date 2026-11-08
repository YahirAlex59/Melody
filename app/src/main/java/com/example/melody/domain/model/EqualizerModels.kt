package com.example.melody.domain.model

enum class SmartPlaylistType(
    val id: String,
    val title: String,
    val subtitle: String
) {
    FAVORITES("favorites", "Favoritos", "Canciones que te gustan"),
    MOST_PLAYED("most_played", "Más reproducidas", "Tus pistas más escuchadas"),
    RECENTLY_PLAYED("recently_played", "Recién reproducidas", "Historial de reproducción"),
    WITH_LYRICS("with_lyrics", "Canciones con letras", "Pistas con letras .lrc")
}

enum class SleepTimerOption(
    val label: String,
    val minutes: Int
) {
    OFF("Desactivado", 0),
    MIN_15("15 minutos", 15),
    MIN_30("30 minutos", 30),
    MIN_45("45 minutos", 45),
    MIN_60("60 minutos", 60),
    END_OF_TRACK("Al terminar la canción actual", -1)
}

enum class EqualizerPreset(
    val label: String,
    val bandLevelsMillibels: List<Int>
) {
    NORMAL("Normal", listOf(0, 0, 0, 0, 0)),
    ROCK("Rock", listOf(500, 300, -100, 300, 500)),
    POP("Pop", listOf(-100, 250, 500, 200, -150)),
    CLASSICAL("Clásica", listOf(450, 200, -200, 300, 450)),
    JAZZ("Jazz", listOf(350, 150, -150, 250, 400)),
    ELECTRONIC("Electrónica", listOf(550, 350, 0, 250, 550)),
    CUSTOM("Personalizado", listOf(0, 0, 0, 0, 0))
}

data class EqualizerBand(
    val index: Int,
    val centerFreqHz: Int,
    val levelMillibels: Int,
    val minLevelMillibels: Int = -1500,
    val maxLevelMillibels: Int = 1500
) {
    val formattedFrequency: String
        get() = if (centerFreqHz >= 1000) {
            val khz = centerFreqHz / 1000f
            if (centerFreqHz % 1000 == 0) "${centerFreqHz / 1000} kHz" else "%.1f kHz".format(khz)
        } else {
            "$centerFreqHz Hz"
        }
}

data class EqualizerUiState(
    val enabled: Boolean = false,
    val preset: EqualizerPreset = EqualizerPreset.NORMAL,
    val bands: List<EqualizerBand> = defaultEqualizerBands(),
    val bassBoostStrength: Int = 0
)

fun defaultEqualizerBands(): List<EqualizerBand> = listOf(
    EqualizerBand(index = 0, centerFreqHz = 60, levelMillibels = 0),
    EqualizerBand(index = 1, centerFreqHz = 230, levelMillibels = 0),
    EqualizerBand(index = 2, centerFreqHz = 910, levelMillibels = 0),
    EqualizerBand(index = 3, centerFreqHz = 3600, levelMillibels = 0),
    EqualizerBand(index = 4, centerFreqHz = 14000, levelMillibels = 0)
)
