package com.example.melody.ui.library

import android.Manifest
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.automirrored.filled.PlaylistPlay
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Lyrics
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.RemoveCircleOutline
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.melody.domain.model.AlbumGroup
import com.example.melody.domain.model.ArtistGroup
import com.example.melody.domain.model.FolderGroup
import com.example.melody.domain.model.GroupSortOption
import com.example.melody.domain.model.LibraryTab
import com.example.melody.domain.model.Playlist
import com.example.melody.domain.model.SmartPlaylistType
import com.example.melody.domain.model.Song
import com.example.melody.domain.model.SortOption
import com.example.melody.ui.theme.LarkBackgroundDark
import com.example.melody.ui.theme.LarkBlueAccent
import com.example.melody.ui.theme.LarkSurfaceDark
import com.example.melody.ui.theme.LarkSurfaceElevated
import com.example.melody.ui.theme.LarkTextSecondary
import com.example.melody.ui.viewmodel.PlayerUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    uiState: PlayerUiState,
    onPermissionResult: (Boolean) -> Unit,
    onRefresh: () -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onSortOptionChange: (SortOption) -> Unit,
    onToggleSortDirection: () -> Unit = {},
    onGroupSortOptionChange: (GroupSortOption) -> Unit = {},
    onToggleGroupSortDirection: () -> Unit = {},
    onSelectTab: (LibraryTab) -> Unit,
    onSongClick: (Song, List<Song>) -> Unit,
    onPlayNext: (Song) -> Unit = {},
    onToggleFavorite: (Long) -> Unit,
    onHideSong: (Long) -> Unit = {},
    onUnhideSong: (Long) -> Unit = {},
    onRestoreAllHiddenSongs: () -> Unit = {},
    onAddSongToPlaylist: (Long, Long) -> Unit,
    onCreatePlaylistAndAddSong: (String, Long) -> Unit,
    onCreatePlaylist: (String) -> Unit = {},
    onDeletePlaylist: (Long) -> Unit = {},
    onRemoveSongFromPlaylist: (Long, Long) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val permissionToRequest = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        Manifest.permission.READ_MEDIA_AUDIO
    } else {
        Manifest.permission.READ_EXTERNAL_STORAGE
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        onPermissionResult(isGranted)
    }

    LaunchedEffect(Unit) {
        if (!uiState.hasPermission) {
            permissionLauncher.launch(permissionToRequest)
        }
    }

    var isSearchVisible by rememberSaveable { mutableStateOf(false) }
    var showSortBottomSheet by rememberSaveable { mutableStateOf(false) }
    var showSettingsDialog by rememberSaveable { mutableStateOf(false) }

    var songTargetForBottomSheet by remember { mutableStateOf<Song?>(null) }
    var songTargetPlaylistContextId by remember { mutableStateOf<Long?>(null) }
    var songTargetForPlaylist by remember { mutableStateOf<Song?>(null) }
    var songTargetForInfo by remember { mutableStateOf<Song?>(null) }
    var showCreatePlaylistDialog by rememberSaveable { mutableStateOf(false) }

    // Estados de selección para vistas de detalle (Carpetas, Artistas, Álbumes, Playlists y Smart Playlists)
    var selectedFolderPath by rememberSaveable { mutableStateOf<String?>(null) }
    var selectedArtistName by rememberSaveable { mutableStateOf<String?>(null) }
    var selectedAlbumName by rememberSaveable { mutableStateOf<String?>(null) }
    var selectedPlaylistId by rememberSaveable { mutableStateOf<Long?>(null) }
    var selectedSmartPlaylistId by rememberSaveable { mutableStateOf<String?>(null) }

    val activeTab = uiState.selectedLibraryTab

    val isGroupRootView = (activeTab == LibraryTab.FOLDERS && selectedFolderPath == null) ||
        (activeTab == LibraryTab.ARTISTS && selectedArtistName == null) ||
        (activeTab == LibraryTab.ALBUMS && selectedAlbumName == null)

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(LarkBackgroundDark)
    ) {
        // Cabecera compacta estilo MainHeadView de Lark Player
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 6.dp, top = 10.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = "Música",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "(${uiState.filteredSongs.size})",
                    style = MaterialTheme.typography.bodyMedium,
                    color = LarkTextSecondary,
                    modifier = Modifier.padding(bottom = 3.dp)
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = {
                        isSearchVisible = !isSearchVisible
                        if (!isSearchVisible && uiState.searchQuery.isNotEmpty()) {
                            onSearchQueryChange("")
                        }
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Buscar",
                        tint = if (isSearchVisible || uiState.searchQuery.isNotEmpty()) {
                            LarkBlueAccent
                        } else {
                            Color.White
                        }
                    )
                }

                IconButton(onClick = { showSortBottomSheet = true }) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Sort,
                        contentDescription = "Ordenar",
                        tint = Color.White
                    )
                }

                IconButton(onClick = onRefresh) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Actualizar biblioteca",
                        tint = Color.White
                    )
                }

                Box {
                    IconButton(onClick = { showSettingsDialog = true }) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Ajustes y canciones ocultas",
                            tint = if (uiState.hiddenSongs.isNotEmpty()) LarkBlueAccent else Color.White
                        )
                    }
                    if (uiState.hiddenSongs.isNotEmpty()) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(top = 8.dp, end = 8.dp)
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(LarkBlueAccent)
                        )
                    }
                }
            }
        }

        // Barra de búsqueda desplegable animada
        AnimatedVisibility(
            visible = isSearchVisible,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            OutlinedTextField(
                value = uiState.searchQuery,
                onValueChange = onSearchQueryChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                placeholder = {
                    Text(
                        text = "Buscar canciones, artistas, álbumes o carpetas...",
                        color = LarkTextSecondary
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        tint = LarkBlueAccent
                    )
                },
                trailingIcon = {
                    IconButton(
                        onClick = {
                            if (uiState.searchQuery.isNotEmpty()) {
                                onSearchQueryChange("")
                            } else {
                                isSearchVisible = false
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Clear,
                            contentDescription = "Cerrar búsqueda",
                            tint = LarkTextSecondary
                        )
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(24.dp)
            )
        }

        // Sub-navegación horizontal por Chips estilo MainFragment de Lark Player:
        // Canciones | Playlists | Carpetas | Artistas | Álbumes
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(LibraryTab.entries) { tab ->
                val selected = tab == activeTab
                Surface(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .clickable {
                            selectedFolderPath = null
                            selectedArtistName = null
                            selectedAlbumName = null
                            selectedPlaylistId = null
                            selectedSmartPlaylistId = null
                            onSelectTab(tab)
                        },
                    shape = RoundedCornerShape(20.dp),
                    color = if (selected) LarkBlueAccent else LarkSurfaceDark
                ) {
                    Text(
                        text = tab.label,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                        color = if (selected) Color(0xFF06131C) else LarkTextSecondary,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
            }
        }

        // Contenido principal según estado y Chip seleccionado
        when {
            !uiState.hasPermission -> {
                PermissionRequiredState(
                    onRequestPermission = { permissionLauncher.launch(permissionToRequest) }
                )
            }

            uiState.isLoading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = LarkBlueAccent)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Escaneando biblioteca musical...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = LarkTextSecondary
                        )
                    }
                }
            }

            uiState.allSongs.isEmpty() && activeTab != LibraryTab.PLAYLISTS -> {
                EmptyLibraryState(
                    isSearching = false,
                    onRefresh = onRefresh
                )
            }

            else -> {
                when (activeTab) {
                    LibraryTab.SONGS -> {
                        if (uiState.filteredSongs.isEmpty()) {
                            EmptyLibraryState(
                                isSearching = uiState.searchQuery.isNotBlank(),
                                onRefresh = onRefresh
                            )
                        } else {
                            SongsListContent(
                                songs = uiState.filteredSongs,
                                favoriteIds = uiState.favoriteSongIds,
                                currentSong = uiState.currentSong,
                                isPlaying = uiState.isPlaying,
                                onSongClick = { song -> onSongClick(song, uiState.filteredSongs) },
                                onPlayAll = {
                                    if (uiState.filteredSongs.isNotEmpty()) {
                                        onSongClick(uiState.filteredSongs.first(), uiState.filteredSongs)
                                    }
                                },
                                onToggleFavorite = onToggleFavorite,
                                onOpenSongMenu = { song ->
                                    songTargetPlaylistContextId = null
                                    songTargetForBottomSheet = song
                                }
                            )
                        }
                    }

                    LibraryTab.FOLDERS -> {
                        val activeFolder = uiState.folders.find { it.folderPath == selectedFolderPath }
                        if (activeFolder != null) {
                            BackHandler { selectedFolderPath = null }
                            GroupSongsDetailView(
                                title = activeFolder.folderName,
                                subtitle = "${activeFolder.songCount} canciones • ${activeFolder.folderPath}",
                                songs = activeFolder.songs,
                                favoriteIds = uiState.favoriteSongIds,
                                currentSong = uiState.currentSong,
                                isPlaying = uiState.isPlaying,
                                onBack = { selectedFolderPath = null },
                                onPlayAll = {
                                    if (activeFolder.songs.isNotEmpty()) {
                                        onSongClick(activeFolder.songs.first(), activeFolder.songs)
                                    }
                                },
                                onSongClick = { song -> onSongClick(song, activeFolder.songs) },
                                onToggleFavorite = onToggleFavorite,
                                onOpenSongMenu = { song ->
                                    songTargetPlaylistContextId = null
                                    songTargetForBottomSheet = song
                                }
                            )
                        } else {
                            FoldersListContent(
                                folders = uiState.folders,
                                onFolderClick = { folder -> selectedFolderPath = folder.folderPath },
                                onPlayFolder = { folder ->
                                    if (folder.songs.isNotEmpty()) {
                                        onSongClick(folder.songs.first(), folder.songs)
                                    }
                                }
                            )
                        }
                    }

                    LibraryTab.ARTISTS -> {
                        val activeArtist = uiState.artists.find { it.artistName == selectedArtistName }
                        if (activeArtist != null) {
                            BackHandler { selectedArtistName = null }
                            GroupSongsDetailView(
                                title = activeArtist.artistName,
                                subtitle = "${activeArtist.songCount} canciones",
                                songs = activeArtist.songs,
                                favoriteIds = uiState.favoriteSongIds,
                                currentSong = uiState.currentSong,
                                isPlaying = uiState.isPlaying,
                                onBack = { selectedArtistName = null },
                                onPlayAll = {
                                    if (activeArtist.songs.isNotEmpty()) {
                                        onSongClick(activeArtist.songs.first(), activeArtist.songs)
                                    }
                                },
                                onSongClick = { song -> onSongClick(song, activeArtist.songs) },
                                onToggleFavorite = onToggleFavorite,
                                onOpenSongMenu = { song ->
                                    songTargetPlaylistContextId = null
                                    songTargetForBottomSheet = song
                                }
                            )
                        } else {
                            ArtistsListContent(
                                artists = uiState.artists,
                                onArtistClick = { artist -> selectedArtistName = artist.artistName },
                                onPlayArtist = { artist ->
                                    if (artist.songs.isNotEmpty()) {
                                        onSongClick(artist.songs.first(), artist.songs)
                                    }
                                }
                            )
                        }
                    }

                    LibraryTab.ALBUMS -> {
                        val activeAlbum = uiState.albums.find { it.albumName == selectedAlbumName }
                        if (activeAlbum != null) {
                            BackHandler { selectedAlbumName = null }
                            GroupSongsDetailView(
                                title = activeAlbum.albumName,
                                subtitle = "${activeAlbum.artistName} • ${activeAlbum.songCount} canciones",
                                songs = activeAlbum.songs,
                                favoriteIds = uiState.favoriteSongIds,
                                currentSong = uiState.currentSong,
                                isPlaying = uiState.isPlaying,
                                onBack = { selectedAlbumName = null },
                                onPlayAll = {
                                    if (activeAlbum.songs.isNotEmpty()) {
                                        onSongClick(activeAlbum.songs.first(), activeAlbum.songs)
                                    }
                                },
                                onSongClick = { song -> onSongClick(song, activeAlbum.songs) },
                                onToggleFavorite = onToggleFavorite,
                                onOpenSongMenu = { song ->
                                    songTargetPlaylistContextId = null
                                    songTargetForBottomSheet = song
                                }
                            )
                        } else {
                            AlbumsListContent(
                                albums = uiState.albums,
                                onAlbumClick = { album -> selectedAlbumName = album.albumName },
                                onPlayAlbum = { album ->
                                    if (album.songs.isNotEmpty()) {
                                        onSongClick(album.songs.first(), album.songs)
                                    }
                                }
                            )
                        }
                    }

                    LibraryTab.PLAYLISTS -> {
                        val smartType = SmartPlaylistType.entries.find { it.id == selectedSmartPlaylistId }
                        val activePlaylist = uiState.playlists.find { it.id == selectedPlaylistId }

                        when {
                            smartType != null -> {
                                BackHandler { selectedSmartPlaylistId = null }
                                val smartSongs = when (smartType) {
                                    SmartPlaylistType.FAVORITES -> uiState.favoriteSongs
                                    SmartPlaylistType.MOST_PLAYED -> uiState.mostPlayedSongs
                                    SmartPlaylistType.RECENTLY_PLAYED -> uiState.recentlyPlayedSongs
                                    SmartPlaylistType.WITH_LYRICS -> uiState.songsWithLyrics
                                }
                                GroupSongsDetailView(
                                    title = smartType.title,
                                    subtitle = "${smartSongs.size} canciones • ${smartType.subtitle}",
                                    songs = smartSongs,
                                    favoriteIds = uiState.favoriteSongIds,
                                    currentSong = uiState.currentSong,
                                    isPlaying = uiState.isPlaying,
                                    onBack = { selectedSmartPlaylistId = null },
                                    onPlayAll = {
                                        if (smartSongs.isNotEmpty()) {
                                            onSongClick(smartSongs.first(), smartSongs)
                                        }
                                    },
                                    onSongClick = { song -> onSongClick(song, smartSongs) },
                                    onToggleFavorite = onToggleFavorite,
                                    onOpenSongMenu = { song ->
                                        songTargetPlaylistContextId = null
                                        songTargetForBottomSheet = song
                                    }
                                )
                            }

                            activePlaylist != null -> {
                                BackHandler { selectedPlaylistId = null }
                                GroupSongsDetailView(
                                    title = activePlaylist.name,
                                    subtitle = "${activePlaylist.songs.size} canciones en la playlist",
                                    songs = activePlaylist.songs,
                                    favoriteIds = uiState.favoriteSongIds,
                                    currentSong = uiState.currentSong,
                                    isPlaying = uiState.isPlaying,
                                    onBack = { selectedPlaylistId = null },
                                    onPlayAll = {
                                        if (activePlaylist.songs.isNotEmpty()) {
                                            onSongClick(activePlaylist.songs.first(), activePlaylist.songs)
                                        }
                                    },
                                    onSongClick = { song -> onSongClick(song, activePlaylist.songs) },
                                    onToggleFavorite = onToggleFavorite,
                                    onOpenSongMenu = { song ->
                                        songTargetPlaylistContextId = activePlaylist.id
                                        songTargetForBottomSheet = song
                                    }
                                )
                            }

                            else -> {
                                PlaylistsTabContent(
                                    favoriteSongs = uiState.favoriteSongs,
                                    mostPlayedSongs = uiState.mostPlayedSongs,
                                    recentlyPlayedSongs = uiState.recentlyPlayedSongs,
                                    songsWithLyrics = uiState.songsWithLyrics,
                                    playlists = uiState.playlists,
                                    onSmartPlaylistClick = { type -> selectedSmartPlaylistId = type.id },
                                    onPlaySmartPlaylist = { songs ->
                                        if (songs.isNotEmpty()) {
                                            onSongClick(songs.first(), songs)
                                        }
                                    },
                                    onCreatePlaylistClick = { showCreatePlaylistDialog = true },
                                    onPlaylistClick = { playlist -> selectedPlaylistId = playlist.id },
                                    onPlayPlaylist = { playlist ->
                                        if (playlist.songs.isNotEmpty()) {
                                            onSongClick(playlist.songs.first(), playlist.songs)
                                        }
                                    },
                                    onDeletePlaylist = onDeletePlaylist
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // SortBottomSheet contextual inspirado en SortBottomSheetFragment de Lark Player
    if (showSortBottomSheet) {
        SortBottomSheet(
            isGroupRootView = isGroupRootView,
            activeTab = activeTab,
            songSortOption = uiState.sortOption,
            songSortAscending = uiState.sortAscending,
            groupSortOption = uiState.groupSortOption,
            groupSortAscending = uiState.groupSortAscending,
            onDismiss = { showSortBottomSheet = false },
            onSelectSongSort = { option ->
                onSortOptionChange(option)
                showSortBottomSheet = false
            },
            onToggleSongSortDirection = onToggleSortDirection,
            onSelectGroupSort = { option ->
                onGroupSortOptionChange(option)
                showSortBottomSheet = false
            },
            onToggleGroupSortDirection = onToggleGroupSortDirection
        )
    }

    // Menú contextual ⋮ estilo BottomSheetFragment (o.vx7) de Lark Player
    songTargetForBottomSheet?.let { song ->
        val isFav = uiState.favoriteSongIds.contains(song.id)
        val playlistCtxId = songTargetPlaylistContextId
        SongActionsBottomSheet(
            song = song,
            isFavorite = isFav,
            playCount = uiState.playbackStats[song.id]?.playCount ?: 0,
            onDismiss = { songTargetForBottomSheet = null },
            onToggleFavorite = { onToggleFavorite(song.id) },
            onPlayNext = {
                onPlayNext(song)
                Toast.makeText(
                    context,
                    "Se reproducirá a continuación: ${song.title}",
                    Toast.LENGTH_SHORT
                ).show()
                songTargetForBottomSheet = null
            },
            onAddToPlaylist = {
                songTargetForBottomSheet = null
                songTargetForPlaylist = song
            },
            onShareSong = {
                songTargetForBottomSheet = null
                shareSongAudio(context, song)
            },
            onShowSongInfo = {
                songTargetForBottomSheet = null
                songTargetForInfo = song
            },
            onHideSong = {
                onHideSong(song.id)
                Toast.makeText(
                    context,
                    "Canción ocultada (puedes restaurarla en el icono de Ajustes)",
                    Toast.LENGTH_SHORT
                ).show()
                songTargetForBottomSheet = null
            },
            onRemoveFromPlaylist = playlistCtxId?.let { plId ->
                {
                    onRemoveSongFromPlaylist(plId, song.id)
                    songTargetForBottomSheet = null
                }
            }
        )
    }

    songTargetForPlaylist?.let { targetSong ->
        AddToPlaylistDialog(
            song = targetSong,
            playlists = uiState.playlists,
            onDismiss = { songTargetForPlaylist = null },
            onSelectPlaylist = { playlistId ->
                onAddSongToPlaylist(playlistId, targetSong.id)
                songTargetForPlaylist = null
            },
            onCreateNewPlaylist = { newName ->
                onCreatePlaylistAndAddSong(newName, targetSong.id)
                songTargetForPlaylist = null
            }
        )
    }

    songTargetForInfo?.let { infoSong ->
        val stats = uiState.playbackStats[infoSong.id]
        SongInfoDialog(
            song = infoSong,
            playCount = stats?.playCount ?: 0,
            onDismiss = { songTargetForInfo = null }
        )
    }

    if (showSettingsDialog) {
        HiddenSongsSettingsDialog(
            hiddenSongs = uiState.hiddenSongs,
            totalSongsCount = uiState.allSongs.size,
            onUnhideSong = onUnhideSong,
            onRestoreAll = onRestoreAllHiddenSongs,
            onDismiss = { showSettingsDialog = false }
        )
    }

    if (showCreatePlaylistDialog) {
        var newName by rememberSaveable { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showCreatePlaylistDialog = false },
            containerColor = LarkSurfaceElevated,
            title = { Text("Nueva lista de reproducción", color = Color.White) },
            text = {
                OutlinedTextField(
                    value = newName,
                    onValueChange = { newName = it },
                    placeholder = { Text("Nombre de la lista") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onCreatePlaylist(newName.trim())
                        showCreatePlaylistDialog = false
                    },
                    enabled = newName.isNotBlank()
                ) {
                    Text("Crear")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreatePlaylistDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }
}

fun shareSongAudio(context: Context, song: Song) {
    try {
        val uri = Uri.parse(song.contentUri)
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "audio/*"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_TITLE, song.title)
            putExtra(Intent.EXTRA_TEXT, "${song.title} - ${song.artist}")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(
            Intent.createChooser(shareIntent, "Compartir ${song.title}")
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    } catch (e: Exception) {
        Toast.makeText(context, "No se pudo compartir el archivo de audio", Toast.LENGTH_SHORT).show()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SortBottomSheet(
    isGroupRootView: Boolean,
    activeTab: LibraryTab,
    songSortOption: SortOption,
    songSortAscending: Boolean,
    groupSortOption: GroupSortOption,
    groupSortAscending: Boolean,
    onDismiss: () -> Unit,
    onSelectSongSort: (SortOption) -> Unit,
    onToggleSongSortDirection: () -> Unit,
    onSelectGroupSort: (GroupSortOption) -> Unit,
    onToggleGroupSortDirection: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = LarkSurfaceElevated
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
                .padding(bottom = 24.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isGroupRootView) {
                        "Ordenar ${activeTab.label.lowercase()}"
                    } else {
                        "Ordenar canciones"
                    },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                val isAscending = if (isGroupRootView) groupSortAscending else songSortAscending
                Surface(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .clickable {
                            if (isGroupRootView) onToggleGroupSortDirection()
                            else onToggleSongSortDirection()
                        },
                    shape = RoundedCornerShape(16.dp),
                    color = LarkSurfaceDark
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (isAscending) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward,
                            contentDescription = null,
                            tint = LarkBlueAccent,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isAscending) "Ascendente" else "Descendente",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = LarkBlueAccent
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = LarkSurfaceDark)
            Spacer(modifier = Modifier.height(6.dp))

            if (isGroupRootView) {
                GroupSortOption.entries.forEach { option ->
                    val selected = option == groupSortOption
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onSelectGroupSort(option) }
                            .padding(vertical = 14.dp, horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
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
            } else {
                SortOption.entries.forEach { option ->
                    val selected = option == songSortOption
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onSelectSongSort(option) }
                            .padding(vertical = 14.dp, horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
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
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SongActionsBottomSheet(
    song: Song,
    isFavorite: Boolean,
    playCount: Int,
    onDismiss: () -> Unit,
    onToggleFavorite: () -> Unit,
    onPlayNext: () -> Unit,
    onAddToPlaylist: () -> Unit,
    onShareSong: () -> Unit,
    onShowSongInfo: () -> Unit,
    onHideSong: () -> Unit,
    onRemoveFromPlaylist: (() -> Unit)? = null
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = LarkSurfaceElevated
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp)
        ) {
            // Cabecera tipo SheetHeaderBean de BottomSheetFragment en Lark Player
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                SongArtwork(
                    albumArtUri = song.albumArtUri,
                    modifier = Modifier.size(52.dp)
                )
                Spacer(modifier = Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = song.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "${song.artist} • ${song.album}" + if (playCount > 0) " • $playCount repr." else "",
                        style = MaterialTheme.typography.bodySmall,
                        color = LarkTextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                IconButton(onClick = onToggleFavorite) {
                    Icon(
                        imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "Favorito",
                        tint = if (isFavorite) MaterialTheme.colorScheme.error else LarkTextSecondary
                    )
                }
                IconButton(onClick = onShowSongInfo) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "Información de la canción",
                        tint = LarkBlueAccent
                    )
                }
            }

            HorizontalDivider(
                color = LarkSurfaceDark,
                modifier = Modifier.padding(vertical = 4.dp)
            )

            BottomSheetActionRow(
                icon = Icons.AutoMirrored.Filled.PlaylistPlay,
                label = "Reproducir siguiente",
                onClick = onPlayNext
            )

            BottomSheetActionRow(
                icon = Icons.AutoMirrored.Filled.PlaylistAdd,
                label = "Añadir a lista de reproducción",
                onClick = onAddToPlaylist
            )

            BottomSheetActionRow(
                icon = Icons.Default.Share,
                label = "Compartir canción",
                onClick = onShareSong
            )

            HorizontalDivider(
                color = LarkSurfaceDark,
                modifier = Modifier.padding(vertical = 6.dp, horizontal = 20.dp)
            )

            BottomSheetActionRow(
                icon = Icons.Default.VisibilityOff,
                label = "Ocultar de la biblioteca",
                onClick = onHideSong
            )

            if (onRemoveFromPlaylist != null) {
                BottomSheetActionRow(
                    icon = Icons.Default.RemoveCircleOutline,
                    label = "Quitar de esta playlist",
                    tint = MaterialTheme.colorScheme.error,
                    onClick = onRemoveFromPlaylist
                )
            }
        }
    }
}

@Composable
private fun BottomSheetActionRow(
    icon: ImageVector,
    label: String,
    tint: Color = Color.White,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 22.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (tint == Color.White) LarkBlueAccent else tint,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = tint,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun SongsListContent(
    songs: List<Song>,
    favoriteIds: Set<Long>,
    currentSong: Song?,
    isPlaying: Boolean,
    onSongClick: (Song) -> Unit,
    onPlayAll: () -> Unit,
    onToggleFavorite: (Long) -> Unit,
    onOpenSongMenu: (Song) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        // Fila de acción rápida estilo Lark Player ("Reproducir todo")
        item(key = "play_all_header") {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onPlayAll)
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(LarkBlueAccent),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Shuffle,
                        contentDescription = null,
                        tint = Color(0xFF06131C),
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Reproducir todo",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "(${songs.size})",
                    style = MaterialTheme.typography.bodyMedium,
                    color = LarkTextSecondary
                )
            }
        }

        items(
            items = songs,
            key = { it.id }
        ) { song ->
            val isFavorite = favoriteIds.contains(song.id)
            val isCurrentSong = currentSong?.id == song.id
            SongRowItem(
                song = song,
                isFavorite = isFavorite,
                isCurrentSong = isCurrentSong,
                isPlaying = isPlaying && isCurrentSong,
                onClick = { onSongClick(song) },
                onToggleFavorite = { onToggleFavorite(song.id) },
                onOpenMoreMenu = { onOpenSongMenu(song) }
            )
        }
    }
}

@Composable
private fun FoldersListContent(
    folders: List<FolderGroup>,
    onFolderClick: (FolderGroup) -> Unit,
    onPlayFolder: (FolderGroup) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        items(
            items = folders,
            key = { it.folderPath }
        ) { folder ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onFolderClick(folder) }
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(10.dp)),
                    color = LarkSurfaceDark
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Folder,
                            contentDescription = null,
                            tint = LarkBlueAccent,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = folder.folderName,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "${folder.songCount} canciones • ${folder.folderPath}",
                        style = MaterialTheme.typography.bodySmall,
                        color = LarkTextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                IconButton(onClick = { onPlayFolder(folder) }) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Reproducir carpeta",
                        tint = LarkBlueAccent
                    )
                }
            }
        }
    }
}

@Composable
private fun ArtistsListContent(
    artists: List<ArtistGroup>,
    onArtistClick: (ArtistGroup) -> Unit,
    onPlayArtist: (ArtistGroup) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        items(
            items = artists,
            key = { it.artistName }
        ) { artist ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onArtistClick(artist) }
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape),
                    color = LarkSurfaceDark
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            tint = LarkBlueAccent,
                            modifier = Modifier.size(26.dp)
                        )
                        if (!artist.coverArtUri.isNullOrBlank()) {
                            AsyncImage(
                                model = artist.coverArtUri,
                                contentDescription = artist.artistName,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = artist.artistName,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "${artist.songCount} canciones",
                        style = MaterialTheme.typography.bodySmall,
                        color = LarkTextSecondary
                    )
                }

                IconButton(onClick = { onPlayArtist(artist) }) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Reproducir artista",
                        tint = LarkBlueAccent
                    )
                }
            }
        }
    }
}

@Composable
private fun AlbumsListContent(
    albums: List<AlbumGroup>,
    onAlbumClick: (AlbumGroup) -> Unit,
    onPlayAlbum: (AlbumGroup) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        items(
            items = albums,
            key = { it.albumName }
        ) { album ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onAlbumClick(album) }
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                SongArtwork(
                    albumArtUri = album.albumArtUri,
                    modifier = Modifier.size(52.dp)
                )

                Spacer(modifier = Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = album.albumName,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "${album.artistName} • ${album.songCount} canciones",
                        style = MaterialTheme.typography.bodySmall,
                        color = LarkTextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                IconButton(onClick = { onPlayAlbum(album) }) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Reproducir álbum",
                        tint = LarkBlueAccent
                    )
                }
            }
        }
    }
}

@Composable
private fun PlaylistsTabContent(
    favoriteSongs: List<Song>,
    mostPlayedSongs: List<Song>,
    recentlyPlayedSongs: List<Song>,
    songsWithLyrics: List<Song>,
    playlists: List<Playlist>,
    onSmartPlaylistClick: (SmartPlaylistType) -> Unit,
    onPlaySmartPlaylist: (List<Song>) -> Unit,
    onCreatePlaylistClick: () -> Unit,
    onPlaylistClick: (Playlist) -> Unit,
    onPlayPlaylist: (Playlist) -> Unit,
    onDeletePlaylist: (Long) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        item(key = "smart_playlists_section_header") {
            Text(
                text = "Listas inteligentes",
                style = MaterialTheme.typography.labelLarge,
                color = LarkTextSecondary,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
            )
        }

        // 1. Favoritos (LikedPlaylistFragment)
        item(key = "smart_favorites") {
            SmartPlaylistRow(
                title = SmartPlaylistType.FAVORITES.title,
                subtitle = "${favoriteSongs.size} canciones",
                icon = Icons.Default.Favorite,
                iconTint = Color(0xFFFF5252),
                coverUri = favoriteSongs.firstOrNull()?.albumArtUri,
                onClick = { onSmartPlaylistClick(SmartPlaylistType.FAVORITES) },
                onPlay = { onPlaySmartPlaylist(favoriteSongs) },
                canPlay = favoriteSongs.isNotEmpty()
            )
        }

        // 2. Más reproducidas (OfficialPlaylistFragment - mostly)
        item(key = "smart_most_played") {
            SmartPlaylistRow(
                title = SmartPlaylistType.MOST_PLAYED.title,
                subtitle = "${mostPlayedSongs.size} canciones",
                icon = Icons.Default.LocalFireDepartment,
                iconTint = Color(0xFFFF9800),
                coverUri = mostPlayedSongs.firstOrNull()?.albumArtUri,
                onClick = { onSmartPlaylistClick(SmartPlaylistType.MOST_PLAYED) },
                onPlay = { onPlaySmartPlaylist(mostPlayedSongs) },
                canPlay = mostPlayedSongs.isNotEmpty()
            )
        }

        // 3. Recién reproducidas / Historial (OfficialPlaylistFragment - recently)
        item(key = "smart_recently_played") {
            SmartPlaylistRow(
                title = SmartPlaylistType.RECENTLY_PLAYED.title,
                subtitle = "${recentlyPlayedSongs.size} canciones",
                icon = Icons.Default.History,
                iconTint = LarkBlueAccent,
                coverUri = recentlyPlayedSongs.firstOrNull()?.albumArtUri,
                onClick = { onSmartPlaylistClick(SmartPlaylistType.RECENTLY_PLAYED) },
                onPlay = { onPlaySmartPlaylist(recentlyPlayedSongs) },
                canPlay = recentlyPlayedSongs.isNotEmpty()
            )
        }

        // 4. Canciones con letras (LyricsPlaylistFragment)
        item(key = "smart_with_lyrics") {
            SmartPlaylistRow(
                title = SmartPlaylistType.WITH_LYRICS.title,
                subtitle = "${songsWithLyrics.size} canciones",
                icon = Icons.Default.Lyrics,
                iconTint = Color(0xFF25D366),
                coverUri = songsWithLyrics.firstOrNull()?.albumArtUri,
                onClick = { onSmartPlaylistClick(SmartPlaylistType.WITH_LYRICS) },
                onPlay = { onPlaySmartPlaylist(songsWithLyrics) },
                canPlay = songsWithLyrics.isNotEmpty()
            )
        }

        item(key = "user_playlists_divider") {
            HorizontalDivider(
                color = LarkSurfaceDark,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
        }

        // Botón "+ Nueva lista de reproducción" (AddPlaylistHolder)
        item(key = "create_playlist_row") {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onCreatePlaylistClick)
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(10.dp)),
                    color = LarkSurfaceDark
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            tint = LarkBlueAccent,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(14.dp))
                Text(
                    text = "+ Nueva lista de reproducción",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = LarkBlueAccent
                )
            }
        }

        items(
            items = playlists,
            key = { it.id }
        ) { playlist ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onPlaylistClick(playlist) }
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val cover = playlist.songs.firstOrNull()?.albumArtUri
                if (cover != null) {
                    SongArtwork(
                        albumArtUri = cover,
                        modifier = Modifier.size(48.dp)
                    )
                } else {
                    Surface(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(10.dp)),
                        color = LarkSurfaceDark
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
                        style = MaterialTheme.typography.bodyLarge,
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
                    IconButton(onClick = { onPlayPlaylist(playlist) }) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Reproducir playlist",
                            tint = LarkBlueAccent
                        )
                    }
                }

                IconButton(onClick = { onDeletePlaylist(playlist.id) }) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Eliminar playlist",
                        tint = LarkTextSecondary
                    )
                }
            }
        }
    }
}

@Composable
private fun SmartPlaylistRow(
    title: String,
    subtitle: String,
    icon: ImageVector,
    iconTint: Color,
    coverUri: String?,
    onClick: () -> Unit,
    onPlay: () -> Unit,
    canPlay: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(10.dp)),
            color = LarkSurfaceDark
        ) {
            Box(contentAlignment = Alignment.Center) {
                if (!coverUri.isNullOrBlank()) {
                    AsyncImage(
                        model = coverUri,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.45f))
                    )
                }
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = LarkTextSecondary
            )
        }

        if (canPlay) {
            IconButton(onClick = onPlay) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "Reproducir $title",
                    tint = LarkBlueAccent
                )
            }
        }
    }
}

@Composable
private fun GroupSongsDetailView(
    title: String,
    subtitle: String,
    songs: List<Song>,
    favoriteIds: Set<Long>,
    currentSong: Song?,
    isPlaying: Boolean,
    onBack: () -> Unit,
    onPlayAll: () -> Unit,
    onSongClick: (Song) -> Unit,
    onToggleFavorite: (Long) -> Unit,
    onOpenSongMenu: (Song) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(LarkSurfaceDark)
                .padding(horizontal = 8.dp, vertical = 10.dp),
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
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = LarkTextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            if (songs.isNotEmpty()) {
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
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Reproducir", fontWeight = FontWeight.Bold)
                }
            }
        }

        if (songs.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No hay canciones en esta lista",
                    color = LarkTextSecondary
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                items(
                    items = songs,
                    key = { it.id }
                ) { song ->
                    val isFavorite = favoriteIds.contains(song.id)
                    val isCurrentSong = currentSong?.id == song.id
                    SongRowItem(
                        song = song,
                        isFavorite = isFavorite,
                        isCurrentSong = isCurrentSong,
                        isPlaying = isPlaying && isCurrentSong,
                        onClick = { onSongClick(song) },
                        onToggleFavorite = { onToggleFavorite(song.id) },
                        onOpenMoreMenu = { onOpenSongMenu(song) }
                    )
                }
            }
        }
    }
}

@Composable
fun SongRowItem(
    song: Song,
    isFavorite: Boolean,
    isCurrentSong: Boolean,
    isPlaying: Boolean,
    onClick: () -> Unit,
    onToggleFavorite: () -> Unit,
    onOpenAddToPlaylist: () -> Unit = {},
    onShowSongInfo: () -> Unit = {},
    onRemoveFromPlaylist: (() -> Unit)? = null,
    onOpenMoreMenu: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(start = 16.dp, end = 6.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        SongArtwork(
            albumArtUri = song.albumArtUri,
            showEqualizerOverlay = isCurrentSong,
            isPlaying = isPlaying,
            modifier = Modifier.size(48.dp)
        )

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = song.title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (isCurrentSong) FontWeight.Bold else FontWeight.Normal,
                color = if (isCurrentSong) LarkBlueAccent else Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(3.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                if (song.hasLyrics) {
                    Box(
                        modifier = Modifier
                            .padding(end = 6.dp)
                            .border(
                                width = 1.dp,
                                color = LarkBlueAccent,
                                shape = RoundedCornerShape(4.dp)
                            )
                            .padding(horizontal = 4.dp, vertical = 1.dp)
                    ) {
                        Text(
                            text = "LETRAS",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = LarkBlueAccent,
                            lineHeight = 10.sp
                        )
                    }
                }

                Text(
                    text = "${song.artist} • ${song.album}",
                    style = MaterialTheme.typography.bodySmall,
                    color = LarkTextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        IconButton(onClick = onToggleFavorite) {
            Icon(
                imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                contentDescription = if (isFavorite) "Quitar de favoritos" else "Agregar a favoritos",
                tint = if (isFavorite) MaterialTheme.colorScheme.error else LarkTextSecondary,
                modifier = Modifier.size(20.dp)
            )
        }

        IconButton(
            onClick = {
                if (onOpenMoreMenu != null) {
                    onOpenMoreMenu()
                } else {
                    onShowSongInfo()
                }
            }
        ) {
            Icon(
                imageVector = Icons.Default.MoreVert,
                contentDescription = "Más opciones",
                tint = LarkTextSecondary
            )
        }
    }
}

@Composable
fun SongArtwork(
    albumArtUri: String?,
    modifier: Modifier = Modifier,
    showEqualizerOverlay: Boolean = false,
    isPlaying: Boolean = false
) {
    Surface(
        modifier = modifier.clip(RoundedCornerShape(8.dp)),
        color = LarkSurfaceDark
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = Icons.Default.Album,
                contentDescription = null,
                tint = LarkTextSecondary.copy(alpha = 0.45f),
                modifier = Modifier.size(24.dp)
            )
            if (!albumArtUri.isNullOrBlank()) {
                AsyncImage(
                    model = albumArtUri,
                    contentDescription = "Carátula del álbum",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }

            if (showEqualizerOverlay) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.55f)),
                    contentAlignment = Alignment.Center
                ) {
                    EqualizerBarsOverlay(isPlaying = isPlaying)
                }
            }
        }
    }
}

@Composable
private fun EqualizerBarsOverlay(
    isPlaying: Boolean
) {
    val transition = rememberInfiniteTransition(label = "eq_bars")
    val bar1 by transition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 420, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bar1"
    )
    val bar2 by transition.animateFloat(
        initialValue = 0.95f,
        targetValue = 0.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 360, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bar2"
    )
    val bar3 by transition.animateFloat(
        initialValue = 0.45f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bar3"
    )

    val heights = if (isPlaying) listOf(bar1, bar2, bar3) else listOf(0.4f, 0.7f, 0.35f)

    Row(
        modifier = Modifier
            .width(18.dp)
            .height(18.dp),
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        heights.forEach { fraction ->
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(fraction.coerceIn(0.2f, 1f))
                    .clip(RoundedCornerShape(2.dp))
                    .background(LarkBlueAccent)
            )
        }
    }
}

@Composable
private fun SongInfoDialog(
    song: Song,
    playCount: Int = 0,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = LarkSurfaceElevated,
        title = {
            Text(
                text = "Información de la canción",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                InfoRow(label = "Título", value = song.title)
                InfoRow(label = "Artista", value = song.artist)
                InfoRow(label = "Álbum", value = song.album)
                InfoRow(label = "Duración", value = song.formattedDuration)
                InfoRow(label = "Tamaño", value = song.formattedFileSize)
                InfoRow(label = "Reproducciones", value = "$playCount veces")
                InfoRow(label = "Carpeta", value = song.folderName)
                InfoRow(
                    label = "Letras (.lrc)",
                    value = if (song.hasLyrics) "Disponibles" else "No detectadas"
                )
                InfoRow(
                    label = "Ruta de archivo",
                    value = song.filePath.ifEmpty { song.contentUri }
                )
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
private fun HiddenSongsSettingsDialog(
    hiddenSongs: List<Song>,
    totalSongsCount: Int,
    onUnhideSong: (Long) -> Unit,
    onRestoreAll: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = LarkSurfaceElevated,
        title = {
            Text(
                text = "Ajustes de biblioteca",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Total escaneado en dispositivo: $totalSongsCount pistas",
                    style = MaterialTheme.typography.bodyMedium,
                    color = LarkTextSecondary
                )
                Text(
                    text = "Canciones ocultas: ${hiddenSongs.size}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )

                if (hiddenSongs.isEmpty()) {
                    Text(
                        text = "No tienes canciones ocultas actualmente. Puedes ocultar cualquier pista desde su menú de tres puntos (⋮).",
                        style = MaterialTheme.typography.bodySmall,
                        color = LarkTextSecondary
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(
                            items = hiddenSongs,
                            key = { it.id }
                        ) { hiddenSong ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(LarkSurfaceDark)
                                    .padding(horizontal = 10.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = hiddenSong.title,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = Color.White,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = hiddenSong.artist,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = LarkTextSecondary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                IconButton(onClick = { onUnhideSong(hiddenSong.id) }) {
                                    Icon(
                                        imageVector = Icons.Default.Restore,
                                        contentDescription = "Restaurar canción",
                                        tint = LarkBlueAccent
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (hiddenSongs.isNotEmpty()) {
                Button(
                    onClick = {
                        onRestoreAll()
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = LarkBlueAccent,
                        contentColor = Color(0xFF06131C)
                    )
                ) {
                    Text("Restaurar todas", fontWeight = FontWeight.Bold)
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cerrar", color = LarkBlueAccent)
            }
        }
    )
}

@Composable
private fun InfoRow(label: String, value: String) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = LarkTextSecondary
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White
        )
    }
}

@Composable
private fun PermissionRequiredState(
    onRequestPermission: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(24.dp)
        ) {
            Icon(
                imageVector = Icons.Default.FolderOpen,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = LarkBlueAccent
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Permiso de audio requerido",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Melody necesita acceso a tus archivos de audio locales para mostrar y reproducir tu biblioteca musical.",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = LarkTextSecondary
            )
            Spacer(modifier = Modifier.height(20.dp))
            Button(
                onClick = onRequestPermission,
                colors = ButtonDefaults.buttonColors(
                    containerColor = LarkBlueAccent,
                    contentColor = Color(0xFF06131C)
                )
            ) {
                Text("Conceder permiso", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun EmptyLibraryState(
    isSearching: Boolean,
    onRefresh: () -> Unit
) {
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
                modifier = Modifier.size(64.dp),
                tint = LarkTextSecondary
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = if (isSearching) "Sin coincidencias" else "No se encontraron canciones",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = if (isSearching) {
                    "Intenta con otro término de búsqueda."
                } else {
                    "Agrega archivos de música a tu dispositivo y pulsa actualizar."
                },
                style = MaterialTheme.typography.bodyMedium,
                color = LarkTextSecondary,
                textAlign = TextAlign.Center
            )
            if (!isSearching) {
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = onRefresh,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = LarkBlueAccent,
                        contentColor = Color(0xFF06131C)
                    )
                ) {
                    Icon(imageVector = Icons.Default.Refresh, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Volver a escanear", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun AddToPlaylistDialog(
    song: Song,
    playlists: List<Playlist>,
    onDismiss: () -> Unit,
    onSelectPlaylist: (Long) -> Unit,
    onCreateNewPlaylist: (String) -> Unit
) {
    var newPlaylistName by rememberSaveable { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = LarkSurfaceElevated,
        title = {
            Text(
                text = "Agregar a lista",
                style = MaterialTheme.typography.titleLarge,
                color = Color.White
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = song.title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = LarkBlueAccent,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = newPlaylistName,
                        onValueChange = { newPlaylistName = it },
                        placeholder = { Text("Nueva lista...") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(
                        onClick = {
                            if (newPlaylistName.isNotBlank()) {
                                onCreateNewPlaylist(newPlaylistName.trim())
                            }
                        },
                        enabled = newPlaylistName.isNotBlank()
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Crear y agregar",
                            tint = LarkBlueAccent
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (playlists.isEmpty()) {
                    Text(
                        text = "Aún no tienes listas guardadas. Escribe un nombre arriba para crear la primera.",
                        style = MaterialTheme.typography.bodySmall,
                        color = LarkTextSecondary
                    )
                } else {
                    Text(
                        text = "Seleccionar lista existente:",
                        style = MaterialTheme.typography.labelMedium,
                        color = LarkTextSecondary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(160.dp)
                    ) {
                        items(
                            items = playlists,
                            key = { it.id }
                        ) { playlist ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { onSelectPlaylist(playlist.id) }
                                    .padding(vertical = 10.dp, horizontal = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.QueueMusic,
                                    contentDescription = null,
                                    tint = LarkBlueAccent
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = playlist.name,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium,
                                        color = Color.White
                                    )
                                    Text(
                                        text = "${playlist.songs.size} canciones",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = LarkTextSecondary
                                    )
                                }
                            }
                            HorizontalDivider(color = LarkSurfaceDark)
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
