package com.example.melody.ui.playlists

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.melody.domain.model.Playlist
import com.example.melody.domain.model.Song
import com.example.melody.ui.library.SongArtwork
import com.example.melody.ui.library.SongRowItem
import com.example.melody.ui.theme.LarkBackgroundDark
import com.example.melody.ui.theme.LarkBlueAccent
import com.example.melody.ui.theme.LarkSurfaceDark
import com.example.melody.ui.theme.LarkSurfaceElevated
import com.example.melody.ui.theme.LarkTextSecondary
import com.example.melody.ui.viewmodel.PlayerUiState

@Composable
fun PlaylistsScreen(
    uiState: PlayerUiState,
    onCreatePlaylist: (String) -> Unit,
    onDeletePlaylist: (Long) -> Unit,
    onRemoveSongFromPlaylist: (Long, Long) -> Unit,
    onSongClick: (Song, List<Song>) -> Unit,
    onToggleFavorite: (Long) -> Unit = {},
    modifier: Modifier = Modifier
) {
    var selectedPlaylistId by rememberSaveable { mutableStateOf<Long?>(null) }
    var showCreateDialog by rememberSaveable { mutableStateOf(false) }
    var songForInfo by remember { mutableStateOf<Song?>(null) }

    val selectedPlaylist = remember(uiState.playlists, selectedPlaylistId) {
        uiState.playlists.find { it.id == selectedPlaylistId }
    }

    if (selectedPlaylist != null) {
        BackHandler {
            selectedPlaylistId = null
        }
        PlaylistDetailContent(
            playlist = selectedPlaylist,
            favoriteIds = uiState.favoriteSongIds,
            currentSong = uiState.currentSong,
            isPlaying = uiState.isPlaying,
            onBack = { selectedPlaylistId = null },
            onPlayAll = {
                if (selectedPlaylist.songs.isNotEmpty()) {
                    onSongClick(selectedPlaylist.songs.first(), selectedPlaylist.songs)
                }
            },
            onSongClick = { song ->
                onSongClick(song, selectedPlaylist.songs)
            },
            onToggleFavorite = onToggleFavorite,
            onShowSongInfo = { songForInfo = it },
            onRemoveSong = { songId ->
                onRemoveSongFromPlaylist(selectedPlaylist.id, songId)
            },
            modifier = modifier
        )
    } else {
        Column(
            modifier = modifier
                .fillMaxSize()
                .background(LarkBackgroundDark)
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Playlists",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "${uiState.playlists.size} listas guardadas",
                        style = MaterialTheme.typography.bodySmall,
                        color = LarkTextSecondary
                    )
                }

                Button(
                    onClick = { showCreateDialog = true },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = LarkBlueAccent,
                        contentColor = Color(0xFF06131C)
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Nueva lista", fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (uiState.playlists.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.QueueMusic,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = LarkTextSecondary
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "No hay listas creadas",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Crea una lista para organizar tus canciones favoritas.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = LarkTextSecondary,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(
                        items = uiState.playlists,
                        key = { it.id }
                    ) { playlist ->
                        PlaylistRowCard(
                            playlist = playlist,
                            onClick = { selectedPlaylistId = playlist.id },
                            onPlay = {
                                if (playlist.songs.isNotEmpty()) {
                                    onSongClick(playlist.songs.first(), playlist.songs)
                                }
                            },
                            onDelete = { onDeletePlaylist(playlist.id) }
                        )
                    }
                }
            }
        }
    }

    if (showCreateDialog) {
        CreatePlaylistDialog(
            onDismiss = { showCreateDialog = false },
            onCreate = { name ->
                onCreatePlaylist(name)
                showCreateDialog = false
            }
        )
    }

    songForInfo?.let { infoSong ->
        AlertDialog(
            onDismissRequest = { songForInfo = null },
            containerColor = LarkSurfaceElevated,
            title = {
                Text(
                    text = "Información de la canción",
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Título: ${infoSong.title}", color = Color.White)
                    Text("Artista: ${infoSong.artist}", color = LarkTextSecondary)
                    Text("Álbum: ${infoSong.album}", color = LarkTextSecondary)
                    Text("Duración: ${infoSong.formattedDuration}", color = LarkTextSecondary)
                    Text("Tamaño: ${infoSong.formattedFileSize}", color = LarkTextSecondary)
                    Text("Ruta: ${infoSong.filePath.ifEmpty { infoSong.contentUri }}", color = LarkTextSecondary)
                }
            },
            confirmButton = {
                TextButton(onClick = { songForInfo = null }) {
                    Text("Cerrar", color = LarkBlueAccent)
                }
            }
        )
    }
}

@Composable
private fun PlaylistRowCard(
    playlist: Playlist,
    onClick: () -> Unit,
    onPlay: () -> Unit,
    onDelete: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
        color = LarkSurfaceDark,
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val coverArt = playlist.songs.firstOrNull()?.albumArtUri
            if (coverArt != null) {
                SongArtwork(
                    albumArtUri = coverArt,
                    modifier = Modifier.size(52.dp)
                )
            } else {
                Surface(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(RoundedCornerShape(10.dp)),
                    color = LarkSurfaceElevated
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.QueueMusic,
                            contentDescription = null,
                            tint = LarkBlueAccent,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = playlist.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${playlist.songs.size} canciones",
                    style = MaterialTheme.typography.bodySmall,
                    color = LarkTextSecondary
                )
            }

            if (playlist.songs.isNotEmpty()) {
                IconButton(onClick = onPlay) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Reproducir lista",
                        tint = LarkBlueAccent
                    )
                }
            }

            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Eliminar lista",
                    tint = LarkTextSecondary
                )
            }
        }
    }
}

@Composable
private fun PlaylistDetailContent(
    playlist: Playlist,
    favoriteIds: Set<Long>,
    currentSong: Song?,
    isPlaying: Boolean,
    onBack: () -> Unit,
    onPlayAll: () -> Unit,
    onSongClick: (Song) -> Unit,
    onToggleFavorite: (Long) -> Unit,
    onShowSongInfo: (Song) -> Unit,
    onRemoveSong: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(LarkBackgroundDark)
    ) {
        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Volver",
                        tint = Color.White
                    )
                }
                Column {
                    Text(
                        text = playlist.name,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${playlist.songs.size} canciones en la lista",
                        style = MaterialTheme.typography.bodySmall,
                        color = LarkTextSecondary
                    )
                }
            }

            if (playlist.songs.isNotEmpty()) {
                Button(
                    onClick = onPlayAll,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = LarkBlueAccent,
                        contentColor = Color(0xFF06131C)
                    ),
                    modifier = Modifier.padding(end = 8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Reproducir", fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (playlist.songs.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.MusicNote,
                        contentDescription = null,
                        modifier = Modifier.size(56.dp),
                        tint = LarkTextSecondary
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Esta lista está vacía",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Agrega canciones desde la pestaña Biblioteca usando el menú de cada canción.",
                        style = MaterialTheme.typography.bodySmall,
                        color = LarkTextSecondary,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                items(
                    items = playlist.songs,
                    key = { it.id }
                ) { song ->
                    val isCurrentSong = currentSong?.id == song.id
                    SongRowItem(
                        song = song,
                        isFavorite = favoriteIds.contains(song.id),
                        isCurrentSong = isCurrentSong,
                        isPlaying = isPlaying && isCurrentSong,
                        onClick = { onSongClick(song) },
                        onToggleFavorite = { onToggleFavorite(song.id) },
                        onOpenAddToPlaylist = {},
                        onShowSongInfo = { onShowSongInfo(song) },
                        onRemoveFromPlaylist = { onRemoveSong(song.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun CreatePlaylistDialog(
    onDismiss: () -> Unit,
    onCreate: (String) -> Unit
) {
    var name by rememberSaveable { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = LarkSurfaceElevated,
        title = { Text("Nueva lista de reproducción", color = Color.White) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                placeholder = { Text("Nombre de la lista") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            Button(
                onClick = { onCreate(name.trim()) },
                enabled = name.isNotBlank()
            ) {
                Text("Crear")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}
