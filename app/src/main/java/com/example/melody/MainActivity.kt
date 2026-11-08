package com.example.melody

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.melody.ui.library.LibraryScreen
import com.example.melody.ui.player.ExpandedPlayerSheet
import com.example.melody.ui.player.MiniPlayer
import com.example.melody.ui.theme.LarkBackgroundDark
import com.example.melody.ui.theme.MelodyTheme
import com.example.melody.ui.viewmodel.PlayerViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MelodyTheme {
                MelodyApp()
            }
        }
    }
}

@Composable
fun MelodyApp(
    playerViewModel: PlayerViewModel = viewModel()
) {
    val uiState by playerViewModel.uiState.collectAsStateWithLifecycle()
    var isPlayerSheetExpanded by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = LarkBackgroundDark,
        bottomBar = {
            uiState.currentSong?.let { activeSong ->
                Box(
                    modifier = Modifier
                        .background(LarkBackgroundDark)
                        .navigationBarsPadding()
                ) {
                    MiniPlayer(
                        currentSong = activeSong,
                        isPlaying = uiState.isPlaying,
                        repeatMode = uiState.repeatMode,
                        currentPosition = uiState.currentPosition,
                        duration = uiState.duration,
                        onTogglePlayPause = playerViewModel::togglePlayPause,
                        onCycleRepeat = playerViewModel::cycleRepeatMode,
                        onSkipNext = playerViewModel::skipToNext,
                        onExpand = { isPlayerSheetExpanded = true }
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            LibraryScreen(
                uiState = uiState,
                onPermissionResult = playerViewModel::onPermissionResult,
                onRefresh = playerViewModel::refreshLibrary,
                onSearchQueryChange = playerViewModel::updateSearchQuery,
                onSortOptionChange = { option -> playerViewModel.updateSortOption(option) },
                onToggleSortDirection = playerViewModel::toggleSortDirection,
                onGroupSortOptionChange = { option -> playerViewModel.updateGroupSortOption(option) },
                onToggleGroupSortDirection = playerViewModel::toggleGroupSortDirection,
                onSelectTab = playerViewModel::selectLibraryTab,
                onSongClick = playerViewModel::playSongFromQueue,
                onPlayNext = playerViewModel::playNext,
                onToggleFavorite = playerViewModel::toggleFavorite,
                onHideSong = playerViewModel::hideSong,
                onUnhideSong = playerViewModel::unhideSong,
                onRestoreAllHiddenSongs = playerViewModel::restoreAllHiddenSongs,
                onAddSongToPlaylist = playerViewModel::addSongToPlaylist,
                onCreatePlaylistAndAddSong = playerViewModel::createPlaylistAndAddSong,
                onCreatePlaylist = playerViewModel::createPlaylist,
                onDeletePlaylist = playerViewModel::deletePlaylist,
                onRemoveSongFromPlaylist = playerViewModel::removeSongFromPlaylist
            )
        }
    }

    if (isPlayerSheetExpanded && uiState.currentSong != null) {
        ExpandedPlayerSheet(
            uiState = uiState,
            onDismiss = { isPlayerSheetExpanded = false },
            onTogglePlayPause = playerViewModel::togglePlayPause,
            onSeekTo = playerViewModel::seekTo,
            onSkipNext = playerViewModel::skipToNext,
            onSkipPrevious = playerViewModel::skipToPrevious,
            onToggleShuffle = playerViewModel::toggleShuffle,
            onCycleRepeat = playerViewModel::cycleRepeatMode,
            onToggleFavorite = playerViewModel::toggleFavorite,
            onSelectQueueIndex = playerViewModel::playQueueItemAt,
            onSetPlaybackSpeed = playerViewModel::setPlaybackSpeed,
            onSetSleepTimer = playerViewModel::setSleepTimer,
            onSetEqualizerEnabled = playerViewModel::setEqualizerEnabled,
            onSelectEqualizerPreset = playerViewModel::selectEqualizerPreset,
            onSetEqualizerBandLevel = playerViewModel::setEqualizerBandLevel,
            onSetBassBoostStrength = playerViewModel::setBassBoostStrength
        )
    }
}
