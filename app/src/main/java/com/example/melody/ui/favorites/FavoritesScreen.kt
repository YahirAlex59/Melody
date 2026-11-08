package com.example.melody.ui.favorites

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.HeartBroken
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.melody.domain.model.Song
import com.example.melody.ui.library.SongRowItem
import com.example.melody.ui.theme.LarkBackgroundDark
import com.example.melody.ui.theme.LarkBlueAccent
import com.example.melody.ui.theme.LarkSurfaceElevated
import com.example.melody.ui.theme.LarkTextSecondary
import com.example.melody.ui.viewmodel.PlayerUiState

@Composable
fun FavoritesScreen(
    uiState: PlayerUiState,
    onSongClick: (Song, List<Song>) -> Unit,
    onToggleFavorite: (Long) -> Unit,
    onAddSongToPlaylist: (Long, Long) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier
) {
    var songForInfo by remember { mutableStateOf<Song?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(LarkBackgroundDark)
    ) {
        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Favoritos",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = "${uiState.favoriteSongs.size} canciones favoritas",
                    style = MaterialTheme.typography.bodySmall,
                    color = LarkTextSecondary
                )
            }

            if (uiState.favoriteSongs.isNotEmpty()) {
                Button(
                    onClick = {
                        onSongClick(uiState.favoriteSongs.first(), uiState.favoriteSongs)
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = LarkBlueAccent,
                        contentColor = Color(0xFF06131C)
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Reproducir todo", fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (uiState.favoriteSongs.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.HeartBroken,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = LarkTextSecondary
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Aún no tienes favoritos",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Marca canciones con el corazón en la biblioteca para acceder rápido a ellas aquí.",
                        style = MaterialTheme.typography.bodyMedium,
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
                    items = uiState.favoriteSongs,
                    key = { it.id }
                ) { song ->
                    val isCurrentSong = uiState.currentSong?.id == song.id
                    SongRowItem(
                        song = song,
                        isFavorite = true,
                        isCurrentSong = isCurrentSong,
                        isPlaying = uiState.isPlaying && isCurrentSong,
                        onClick = { onSongClick(song, uiState.favoriteSongs) },
                        onToggleFavorite = { onToggleFavorite(song.id) },
                        onOpenAddToPlaylist = {
                            val firstPlaylist = uiState.playlists.firstOrNull()
                            if (firstPlaylist != null) {
                                onAddSongToPlaylist(firstPlaylist.id, song.id)
                            }
                        },
                        onShowSongInfo = { songForInfo = song }
                    )
                }
            }
        }
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
