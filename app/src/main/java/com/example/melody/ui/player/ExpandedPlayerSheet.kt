package com.example.melody.ui.player

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Lyrics
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.media3.common.Player
import coil.compose.AsyncImage
import com.example.melody.domain.model.EqualizerPreset
import com.example.melody.domain.model.EqualizerUiState
import com.example.melody.domain.model.LyricLine
import com.example.melody.domain.model.SleepTimerOption
import com.example.melody.ui.library.SongArtwork
import com.example.melody.ui.library.shareSongAudio
import com.example.melody.ui.theme.LarkBackgroundDark
import com.example.melody.ui.theme.LarkBlueAccent
import com.example.melody.ui.theme.LarkSurfaceDark
import com.example.melody.ui.theme.LarkSurfaceElevated
import com.example.melody.ui.theme.LarkTextSecondary
import com.example.melody.ui.viewmodel.PlayerUiState
import java.util.Locale

private enum class ExpandedPlayerViewMode(val label: String) {
    ARTWORK("Carátula"),
    LYRICS("Letras"),
    EQUALIZER("Efectos"),
    QUEUE("Cola")
}

private val PLAYBACK_SPEED_OPTIONS = listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpandedPlayerSheet(
    uiState: PlayerUiState,
    onDismiss: () -> Unit,
    onTogglePlayPause: () -> Unit,
    onSeekTo: (Long) -> Unit,
    onSkipNext: () -> Unit,
    onSkipPrevious: () -> Unit,
    onToggleShuffle: () -> Unit,
    onCycleRepeat: () -> Unit,
    onToggleFavorite: (Long) -> Unit,
    onSelectQueueIndex: (Int) -> Unit,
    onSetPlaybackSpeed: (Float) -> Unit,
    onSetSleepTimer: (SleepTimerOption) -> Unit,
    onSetEqualizerEnabled: (Boolean) -> Unit,
    onSelectEqualizerPreset: (EqualizerPreset) -> Unit,
    onSetEqualizerBandLevel: (Int, Int) -> Unit,
    onSetBassBoostStrength: (Int) -> Unit
) {
    val currentSong = uiState.currentSong ?: return
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var viewMode by remember { mutableStateOf(ExpandedPlayerViewMode.ARTWORK) }
    var isDraggingSlider by remember { mutableStateOf(false) }
    var sliderDragPosition by remember { mutableFloatStateOf(0f) }

    var showSpeedDialog by remember { mutableStateOf(false) }
    var showSleepTimerDialog by remember { mutableStateOf(false) }

    val totalDuration = uiState.duration.coerceAtLeast(currentSong.duration).coerceAtLeast(1L)
    val effectivePosition = if (isDraggingSlider) {
        sliderDragPosition.toLong()
    } else {
        uiState.currentPosition.coerceIn(0L, totalDuration)
    }

    val isFavorite = uiState.favoriteSongIds.contains(currentSong.id)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = LarkBackgroundDark
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Barra superior con selector de modo: Carátula | Letras | Efectos | Cola
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = "Minimizar reproductor",
                        tint = Color.White,
                        modifier = Modifier.size(30.dp)
                    )
                }

                // Selector tipo píldora (Carátula / Letras / Efectos / Cola)
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(LarkSurfaceDark)
                        .padding(3.dp),
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    ExpandedPlayerViewMode.entries.forEach { mode ->
                        val selected = viewMode == mode
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .background(if (selected) LarkBlueAccent else Color.Transparent)
                                .clickable { viewMode = mode }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = mode.label,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                color = if (selected) Color(0xFF06131C) else LarkTextSecondary
                            )
                        }
                    }
                }

                IconButton(
                    onClick = {
                        viewMode = if (viewMode == ExpandedPlayerViewMode.QUEUE) {
                            ExpandedPlayerViewMode.ARTWORK
                        } else {
                            ExpandedPlayerViewMode.QUEUE
                        }
                    }
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.QueueMusic,
                        contentDescription = "Ver cola actual",
                        tint = if (viewMode == ExpandedPlayerViewMode.QUEUE) {
                            LarkBlueAccent
                        } else {
                            Color.White
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            when (viewMode) {
                ExpandedPlayerViewMode.QUEUE -> {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                    ) {
                        Text(
                            text = "${uiState.currentQueue.size} pistas en la cola",
                            style = MaterialTheme.typography.labelLarge,
                            color = LarkTextSecondary,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )

                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(bottom = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            itemsIndexed(
                                items = uiState.currentQueue,
                                key = { index, song -> "${song.id}_$index" }
                            ) { index, song ->
                                val isCurrent = song.id == currentSong.id
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(
                                            if (isCurrent) LarkSurfaceElevated else LarkSurfaceDark
                                        )
                                        .clickable { onSelectQueueIndex(index) }
                                        .padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    SongArtwork(
                                        albumArtUri = song.albumArtUri,
                                        showEqualizerOverlay = isCurrent,
                                        isPlaying = uiState.isPlaying && isCurrent,
                                        modifier = Modifier.size(44.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = song.title,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium,
                                            color = if (isCurrent) LarkBlueAccent else Color.White,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = song.artist,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = LarkTextSecondary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    if (isCurrent) {
                                        Icon(
                                            imageVector = Icons.Default.Equalizer,
                                            contentDescription = null,
                                            tint = LarkBlueAccent,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    } else {
                                        Text(
                                            text = song.formattedDuration,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = LarkTextSecondary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                ExpandedPlayerViewMode.LYRICS -> {
                    LyricsPanelView(
                        songTitle = currentSong.title,
                        artistName = currentSong.artist,
                        lyrics = uiState.currentLyrics,
                        currentPositionMs = effectivePosition,
                        onSeekTo = onSeekTo,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                    )
                }

                ExpandedPlayerViewMode.EQUALIZER -> {
                    EqualizerPanelView(
                        equalizerState = uiState.equalizerState,
                        onSetEnabled = onSetEqualizerEnabled,
                        onSelectPreset = onSelectEqualizerPreset,
                        onSetBandLevel = onSetEqualizerBandLevel,
                        onSetBassBoost = onSetBassBoostStrength,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                    )
                }

                ExpandedPlayerViewMode.ARTWORK -> {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth(0.78f)
                                .aspectRatio(1f)
                                .clip(RoundedCornerShape(24.dp))
                                .clickable {
                                    if (uiState.currentLyrics.isNotEmpty() || currentSong.hasLyrics) {
                                        viewMode = ExpandedPlayerViewMode.LYRICS
                                    }
                                },
                            color = LarkSurfaceDark,
                            tonalElevation = 8.dp,
                            shadowElevation = 8.dp
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Album,
                                    contentDescription = null,
                                    tint = LarkTextSecondary.copy(alpha = 0.4f),
                                    modifier = Modifier.size(96.dp)
                                )
                                if (!currentSong.albumArtUri.isNullOrBlank()) {
                                    AsyncImage(
                                        model = currentSong.albumArtUri,
                                        contentDescription = "Carátula del álbum",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = currentSong.title,
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "${currentSong.artist} • ${currentSong.album}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = LarkTextSecondary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            IconButton(onClick = { viewMode = ExpandedPlayerViewMode.LYRICS }) {
                                Icon(
                                    imageVector = Icons.Default.Lyrics,
                                    contentDescription = "Ver letras",
                                    tint = if (currentSong.hasLyrics) LarkBlueAccent else LarkTextSecondary
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Fila de acciones rápidas de Lark Player (Favorito, Ecualizador, Velocidad, Temporizador, Compartir)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(LarkSurfaceDark)
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 1. Favorito
                IconButton(onClick = { onToggleFavorite(currentSong.id) }) {
                    Icon(
                        imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "Favorito",
                        tint = if (isFavorite) MaterialTheme.colorScheme.error else LarkTextSecondary
                    )
                }

                // 2. Ecualizador (MotionEqFragment)
                IconButton(
                    onClick = {
                        viewMode = if (viewMode == ExpandedPlayerViewMode.EQUALIZER) {
                            ExpandedPlayerViewMode.ARTWORK
                        } else {
                            ExpandedPlayerViewMode.EQUALIZER
                        }
                    }
                ) {
                    val eqHighlighted =
                        uiState.equalizerState.enabled || viewMode == ExpandedPlayerViewMode.EQUALIZER
                    Icon(
                        imageVector = Icons.Default.Equalizer,
                        contentDescription = "Ecualizador y efectos",
                        tint = if (eqHighlighted) LarkBlueAccent else LarkTextSecondary
                    )
                }

                // 3. Velocidad de reproducción (0.5x - 2.0x)
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(
                            if (uiState.playbackSpeed != 1.0f) {
                                LarkBlueAccent.copy(alpha = 0.18f)
                            } else {
                                LarkSurfaceElevated
                            }
                        )
                        .clickable { showSpeedDialog = true }
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = formatPlaybackSpeed(uiState.playbackSpeed),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (uiState.playbackSpeed != 1.0f) LarkBlueAccent else Color.White
                    )
                }

                // 4. Temporizador de apagado (Sleep Timer)
                val sleepActive = uiState.sleepTimerOption != SleepTimerOption.OFF
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { showSleepTimerDialog = true }
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Timer,
                        contentDescription = "Temporizador de apagado",
                        tint = if (sleepActive) LarkBlueAccent else LarkTextSecondary,
                        modifier = Modifier.size(22.dp)
                    )
                    if (sleepActive) {
                        Spacer(modifier = Modifier.width(4.dp))
                        val timerLabel = if (uiState.sleepTimerOption == SleepTimerOption.END_OF_TRACK) {
                            "Fin"
                        } else {
                            formatMillis(uiState.sleepTimerRemainingMs)
                        }
                        Text(
                            text = timerLabel,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = LarkBlueAccent
                        )
                    }
                }

                // 5. Compartir pista
                IconButton(onClick = { shareSongAudio(context, currentSong) }) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = "Compartir canción",
                        tint = LarkTextSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Barra de progreso interactiva (Slider seekTo)
            Column(modifier = Modifier.fillMaxWidth()) {
                Slider(
                    value = effectivePosition.toFloat(),
                    onValueChange = { newValue ->
                        isDraggingSlider = true
                        sliderDragPosition = newValue
                    },
                    onValueChangeFinished = {
                        onSeekTo(sliderDragPosition.toLong())
                        isDraggingSlider = false
                    },
                    valueRange = 0f..totalDuration.toFloat(),
                    colors = SliderDefaults.colors(
                        thumbColor = LarkBlueAccent,
                        activeTrackColor = LarkBlueAccent,
                        inactiveTrackColor = LarkSurfaceElevated
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = formatMillis(effectivePosition),
                        style = MaterialTheme.typography.labelMedium,
                        color = LarkTextSecondary
                    )
                    Text(
                        text = formatMillis(totalDuration),
                        style = MaterialTheme.typography.labelMedium,
                        color = LarkTextSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Controles principales de reproducción
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 20.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onToggleShuffle) {
                    Icon(
                        imageVector = Icons.Default.Shuffle,
                        contentDescription = "Modo aleatorio",
                        tint = if (uiState.shuffleModeEnabled) {
                            LarkBlueAccent
                        } else {
                            LarkTextSecondary
                        },
                        modifier = Modifier.size(26.dp)
                    )
                }

                IconButton(
                    onClick = onSkipPrevious,
                    modifier = Modifier.size(52.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.SkipPrevious,
                        contentDescription = "Anterior",
                        tint = Color.White,
                        modifier = Modifier.size(36.dp)
                    )
                }

                FilledIconButton(
                    onClick = onTogglePlayPause,
                    modifier = Modifier.size(68.dp),
                    shape = CircleShape,
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = LarkBlueAccent,
                        contentColor = Color(0xFF06131C)
                    )
                ) {
                    Icon(
                        imageVector = if (uiState.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (uiState.isPlaying) "Pausar" else "Reproducir",
                        modifier = Modifier.size(36.dp)
                    )
                }

                IconButton(
                    onClick = onSkipNext,
                    modifier = Modifier.size(52.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.SkipNext,
                        contentDescription = "Siguiente",
                        tint = Color.White,
                        modifier = Modifier.size(36.dp)
                    )
                }

                IconButton(onClick = onCycleRepeat) {
                    val repeatIcon = when (uiState.repeatMode) {
                        Player.REPEAT_MODE_ONE -> Icons.Default.RepeatOne
                        else -> Icons.Default.Repeat
                    }
                    val isRepeatActive = uiState.repeatMode != Player.REPEAT_MODE_OFF
                    Icon(
                        imageVector = repeatIcon,
                        contentDescription = "Modo de repetición",
                        tint = if (isRepeatActive) {
                            LarkBlueAccent
                        } else {
                            LarkTextSecondary
                        },
                        modifier = Modifier.size(26.dp)
                    )
                }
            }
        }
    }

    if (showSpeedDialog) {
        PlaybackSpeedDialog(
            currentSpeed = uiState.playbackSpeed,
            onSelectSpeed = { speed ->
                onSetPlaybackSpeed(speed)
                showSpeedDialog = false
            },
            onDismiss = { showSpeedDialog = false }
        )
    }

    if (showSleepTimerDialog) {
        SleepTimerDialog(
            currentOption = uiState.sleepTimerOption,
            remainingMs = uiState.sleepTimerRemainingMs,
            onSelectOption = { option ->
                onSetSleepTimer(option)
                showSleepTimerDialog = false
            },
            onDismiss = { showSleepTimerDialog = false }
        )
    }
}

@Composable
private fun EqualizerPanelView(
    equalizerState: EqualizerUiState,
    onSetEnabled: (Boolean) -> Unit,
    onSelectPreset: (EqualizerPreset) -> Unit,
    onSetBandLevel: (Int, Int) -> Unit,
    onSetBassBoost: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(LarkSurfaceDark)
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Ecualizador y Efectos",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = if (equalizerState.enabled) {
                        "Preajuste: ${equalizerState.preset.label}"
                    } else {
                        "Desactivado (activa para ajustar bandas)"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = if (equalizerState.enabled) LarkBlueAccent else LarkTextSecondary
                )
            }

            Switch(
                checked = equalizerState.enabled,
                onCheckedChange = onSetEnabled,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color(0xFF06131C),
                    checkedTrackColor = LarkBlueAccent,
                    uncheckedThumbColor = LarkTextSecondary,
                    uncheckedTrackColor = LarkSurfaceElevated
                )
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(EqualizerPreset.entries) { preset ->
                val selected = equalizerState.preset == preset && equalizerState.enabled
                FilterChip(
                    selected = selected,
                    onClick = { onSelectPreset(preset) },
                    label = {
                        Text(
                            text = preset.label,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        containerColor = LarkSurfaceElevated,
                        labelColor = LarkTextSecondary,
                        selectedContainerColor = LarkBlueAccent,
                        selectedLabelColor = Color(0xFF06131C)
                    ),
                    border = null
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            equalizerState.bands.forEach { band ->
                val minLevel = band.minLevelMillibels.toFloat()
                val maxLevel = band.maxLevelMillibels.toFloat().coerceAtLeast(minLevel + 100f)
                val dbValue = band.levelMillibels / 100
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(LarkSurfaceElevated.copy(alpha = 0.65f))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = band.formattedFrequency,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        )
                        Text(
                            text = String.format(Locale.US, "%+d dB", dbValue),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (equalizerState.enabled) LarkBlueAccent else LarkTextSecondary
                        )
                    }

                    Slider(
                        value = band.levelMillibels.toFloat().coerceIn(minLevel, maxLevel),
                        onValueChange = { newValue ->
                            onSetBandLevel(band.index, newValue.toInt())
                        },
                        valueRange = minLevel..maxLevel,
                        enabled = equalizerState.enabled,
                        colors = SliderDefaults.colors(
                            thumbColor = LarkBlueAccent,
                            activeTrackColor = LarkBlueAccent,
                            inactiveTrackColor = LarkSurfaceDark
                        )
                    )
                }
            }

            // Refuerzo de graves (Bass Boost)
            val bassPercent = (equalizerState.bassBoostStrength / 10).coerceIn(0, 100)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(LarkSurfaceElevated.copy(alpha = 0.85f))
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Refuerzo de graves (Bass Boost)",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                    Text(
                        text = "$bassPercent%",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (equalizerState.enabled) LarkBlueAccent else LarkTextSecondary
                    )
                }

                Slider(
                    value = equalizerState.bassBoostStrength.toFloat().coerceIn(0f, 1000f),
                    onValueChange = { newValue ->
                        onSetBassBoost(newValue.toInt())
                    },
                    valueRange = 0f..1000f,
                    enabled = equalizerState.enabled,
                    colors = SliderDefaults.colors(
                        thumbColor = LarkBlueAccent,
                        activeTrackColor = LarkBlueAccent,
                        inactiveTrackColor = LarkSurfaceDark
                    )
                )
            }
        }
    }
}

@Composable
private fun PlaybackSpeedDialog(
    currentSpeed: Float,
    onSelectSpeed: (Float) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = LarkSurfaceDark,
        icon = {
            Icon(
                imageVector = Icons.Default.Speed,
                contentDescription = null,
                tint = LarkBlueAccent
            )
        },
        title = {
            Text(
                text = "Velocidad de reproducción",
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                PLAYBACK_SPEED_OPTIONS.forEach { speed ->
                    val selected = kotlin.math.abs(currentSpeed - speed) < 0.01f
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (selected) LarkSurfaceElevated else Color.Transparent)
                            .clickable { onSelectSpeed(speed) }
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (speed == 1.0f) "1.0x (Normal)" else formatPlaybackSpeed(speed),
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                            color = if (selected) LarkBlueAccent else Color.White
                        )
                        if (selected) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = LarkBlueAccent
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cerrar", color = LarkBlueAccent)
            }
        }
    )
}

@Composable
private fun SleepTimerDialog(
    currentOption: SleepTimerOption,
    remainingMs: Long,
    onSelectOption: (SleepTimerOption) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = LarkSurfaceDark,
        icon = {
            Icon(
                imageVector = Icons.Default.Timer,
                contentDescription = null,
                tint = LarkBlueAccent
            )
        },
        title = {
            Text(
                text = "Temporizador de apagado",
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                if (currentOption != SleepTimerOption.OFF) {
                    val activeStatus = if (currentOption == SleepTimerOption.END_OF_TRACK) {
                        "Se pausará al terminar la canción actual"
                    } else {
                        "Tiempo restante: ${formatMillis(remainingMs)}"
                    }
                    Text(
                        text = activeStatus,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = LarkBlueAccent,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                }

                SleepTimerOption.entries.forEach { option ->
                    val selected = currentOption == option
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (selected) LarkSurfaceElevated else Color.Transparent)
                            .clickable { onSelectOption(option) }
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = option.label,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                            color = if (selected) LarkBlueAccent else Color.White
                        )
                        if (selected) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = LarkBlueAccent
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cerrar", color = LarkBlueAccent)
            }
        }
    )
}

@Composable
private fun LyricsPanelView(
    songTitle: String,
    artistName: String,
    lyrics: List<LyricLine>,
    currentPositionMs: Long,
    onSeekTo: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()

    val activeIndex = remember(lyrics, currentPositionMs) {
        if (lyrics.isEmpty()) -1
        else {
            val idx = lyrics.indexOfLast { line ->
                val time = line.timeMs
                time != null && time <= currentPositionMs
            }
            if (idx >= 0) idx else 0
        }
    }

    LaunchedEffect(activeIndex) {
        if (activeIndex >= 0 && activeIndex < lyrics.size) {
            listState.animateScrollToItem(
                index = (activeIndex - 2).coerceAtLeast(0)
            )
        }
    }

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(LarkSurfaceDark)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = songTitle,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = artistName,
            style = MaterialTheme.typography.bodySmall,
            color = LarkTextSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Spacer(modifier = Modifier.height(12.dp))

        if (lyrics.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Lyrics,
                        contentDescription = null,
                        tint = LarkTextSecondary,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "No se encontró archivo .lrc para esta canción",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Coloca un archivo .lrc con el mismo nombre de la canción en su carpeta para ver las letras sincronizadas aquí.",
                        style = MaterialTheme.typography.bodySmall,
                        color = LarkTextSecondary,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(vertical = 48.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                itemsIndexed(lyrics) { index, line ->
                    val isCurrentLine = index == activeIndex
                    Text(
                        text = line.text,
                        style = if (isCurrentLine) {
                            MaterialTheme.typography.titleMedium
                        } else {
                            MaterialTheme.typography.bodyMedium
                        },
                        fontWeight = if (isCurrentLine) FontWeight.Bold else FontWeight.Normal,
                        color = if (isCurrentLine) LarkBlueAccent else LarkTextSecondary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable(enabled = line.timeMs != null) {
                                line.timeMs?.let { onSeekTo(it) }
                            }
                            .padding(vertical = 4.dp, horizontal = 8.dp)
                    )
                }
            }
        }
    }
}

private fun formatPlaybackSpeed(speed: Float): String {
    return if (speed % 1.0f == 0f) {
        String.format(Locale.US, "%.1fx", speed)
    } else {
        String.format(Locale.US, "%.2fx", speed).trimEnd('0')
    }
}

private fun formatMillis(millis: Long): String {
    val totalSeconds = (millis / 1000).coerceAtLeast(0L)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%02d:%02d".format(minutes, seconds)
}
