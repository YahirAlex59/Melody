package com.example.melody.ui.viewmodel

import android.Manifest
import android.app.Application
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import com.example.melody.data.local.MediaStoreScanner
import com.example.melody.data.local.MelodyDatabase
import com.example.melody.data.local.PlaybackStatsEntity
import com.example.melody.data.repository.MusicRepository
import com.example.melody.domain.model.AlbumGroup
import com.example.melody.domain.model.ArtistGroup
import com.example.melody.domain.model.EqualizerPreset
import com.example.melody.domain.model.EqualizerUiState
import com.example.melody.domain.model.FolderGroup
import com.example.melody.domain.model.GroupSortOption
import com.example.melody.domain.model.LibraryTab
import com.example.melody.domain.model.LyricLine
import com.example.melody.domain.model.Playlist
import com.example.melody.domain.model.SleepTimerOption
import com.example.melody.domain.model.Song
import com.example.melody.domain.model.SortOption
import com.example.melody.playback.AudioEffectsManager
import com.example.melody.playback.PlayerControllerManager
import com.example.melody.playback.PlayerControllerManager.Companion.toSongOrNull
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.Locale

data class PlayerUiState(
    val allSongs: List<Song> = emptyList(),
    val filteredSongs: List<Song> = emptyList(),
    val hiddenSongs: List<Song> = emptyList(),
    val folders: List<FolderGroup> = emptyList(),
    val artists: List<ArtistGroup> = emptyList(),
    val albums: List<AlbumGroup> = emptyList(),
    val selectedLibraryTab: LibraryTab = LibraryTab.SONGS,
    val favoriteSongIds: Set<Long> = emptySet(),
    val favoriteSongs: List<Song> = emptyList(),
    val mostPlayedSongs: List<Song> = emptyList(),
    val recentlyPlayedSongs: List<Song> = emptyList(),
    val songsWithLyrics: List<Song> = emptyList(),
    val playbackStats: Map<Long, PlaybackStatsEntity> = emptyMap(),
    val playlists: List<Playlist> = emptyList(),
    val searchQuery: String = "",
    val sortOption: SortOption = SortOption.DATE_ADDED,
    val sortAscending: Boolean = false,
    val groupSortOption: GroupSortOption = GroupSortOption.SONG_COUNT,
    val groupSortAscending: Boolean = false,
    val hasPermission: Boolean = false,
    val isLoading: Boolean = false,
    val currentQueue: List<Song> = emptyList(),
    val currentSong: Song? = null,
    val currentLyrics: List<LyricLine> = emptyList(),
    val isPlaying: Boolean = false,
    val shuffleModeEnabled: Boolean = false,
    val repeatMode: Int = Player.REPEAT_MODE_OFF,
    val playbackSpeed: Float = 1.0f,
    val sleepTimerOption: SleepTimerOption = SleepTimerOption.OFF,
    val sleepTimerRemainingMs: Long = 0L,
    val equalizerState: EqualizerUiState = EqualizerUiState(),
    val currentPosition: Long = 0L,
    val duration: Long = 0L
)

class PlayerViewModel(application: Application) : AndroidViewModel(application) {

    private val database = MelodyDatabase.getDatabase(application)
    private val repository = MusicRepository(
        mediaStoreScanner = MediaStoreScanner(application),
        favoriteSongDao = database.favoriteSongDao(),
        playlistDao = database.playlistDao(),
        playbackStatsDao = database.playbackStatsDao()
    )
    private val controllerManager = PlayerControllerManager(application)

    private val _uiState = MutableStateFlow(PlayerUiState(hasPermission = checkAudioPermission()))
    val uiState: StateFlow<PlayerUiState> = _uiState.asStateFlow()

    private var positionTrackerJob: Job? = null
    private var sleepTimerJob: Job? = null
    private var loadedLyricsSongId: Long? = null
    private var lastRecordedPlayedSongId: Long? = null

    private val playerListener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            syncPlayerState()
            if (isPlaying) {
                startPositionTracker()
                _uiState.value.currentSong?.let { recordPlayIfNeeded(it.id) }
            } else {
                stopPositionTracker()
            }
        }

        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            if (_uiState.value.sleepTimerOption == SleepTimerOption.END_OF_TRACK &&
                reason == Player.MEDIA_ITEM_TRANSITION_REASON_AUTO
            ) {
                controllerManager.pause()
                _uiState.update {
                    it.copy(
                        sleepTimerOption = SleepTimerOption.OFF,
                        sleepTimerRemainingMs = 0L
                    )
                }
            }
            syncPlayerState()
            val newSongId = mediaItem?.mediaId?.toLongOrNull()
            if (newSongId != null && (controllerManager.controller?.isPlaying == true || reason == Player.MEDIA_ITEM_TRANSITION_REASON_AUTO)) {
                recordPlayIfNeeded(newSongId)
            }
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            if (playbackState == Player.STATE_ENDED &&
                _uiState.value.sleepTimerOption == SleepTimerOption.END_OF_TRACK
            ) {
                controllerManager.pause()
                _uiState.update {
                    it.copy(
                        sleepTimerOption = SleepTimerOption.OFF,
                        sleepTimerRemainingMs = 0L
                    )
                }
            }
            syncPlayerState()
        }

        override fun onPlaybackParametersChanged(playbackParameters: PlaybackParameters) {
            _uiState.update { it.copy(playbackSpeed = playbackParameters.speed) }
        }

        override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
            syncPlayerState()
        }

        override fun onRepeatModeChanged(repeatMode: Int) {
            syncPlayerState()
        }

        override fun onEvents(player: Player, events: Player.Events) {
            syncPlayerState()
        }
    }

    init {
        observeFavorites()
        observePlaybackStats()
        observePlaylists()
        observeEqualizer()
        connectPlayerController()
        if (_uiState.value.hasPermission) {
            refreshLibrary()
        }
    }

    private fun checkAudioPermission(): Boolean {
        val context = getApplication<Application>()
        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_AUDIO
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }
        return ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
    }

    private fun connectPlayerController() {
        viewModelScope.launch {
            val controller = controllerManager.connect(playerListener)
            if (controller != null) {
                syncPlayerState()
                if (controller.isPlaying) {
                    startPositionTracker()
                }
            }
        }
    }

    private fun observeFavorites() {
        viewModelScope.launch {
            repository.favoriteSongIdsFlow.collect { favoriteIds ->
                _uiState.update { state ->
                    recomputeDerivedLibraryState(state.copy(favoriteSongIds = favoriteIds))
                }
            }
        }
    }

    private fun observePlaybackStats() {
        viewModelScope.launch {
            repository.playbackStatsMapFlow.collect { statsMap ->
                _uiState.update { state ->
                    recomputeDerivedLibraryState(state.copy(playbackStats = statsMap))
                }
            }
        }
    }

    private fun observePlaylists() {
        viewModelScope.launch {
            repository.playlistsMetadataFlow.collect { rawPlaylists ->
                _uiState.update { state ->
                    recomputeDerivedLibraryState(state.copy(playlists = rawPlaylists))
                }
            }
        }
    }

    private fun observeEqualizer() {
        viewModelScope.launch {
            AudioEffectsManager.state.collect { eqState ->
                _uiState.update { it.copy(equalizerState = eqState) }
            }
        }
    }

    private fun recordPlayIfNeeded(songId: Long) {
        if (lastRecordedPlayedSongId == songId) return
        lastRecordedPlayedSongId = songId
        viewModelScope.launch {
            repository.recordSongPlayed(songId)
        }
    }

    fun selectLibraryTab(tab: LibraryTab) {
        _uiState.update { it.copy(selectedLibraryTab = tab) }
    }

    fun onPermissionResult(granted: Boolean) {
        val actualGranted = granted || checkAudioPermission()
        _uiState.update { it.copy(hasPermission = actualGranted) }
        if (actualGranted) {
            refreshLibrary()
        }
    }

    fun refreshLibrary() {
        if (!checkAudioPermission()) {
            _uiState.update { it.copy(hasPermission = false, isLoading = false) }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(hasPermission = true, isLoading = true) }
            val scannedSongs = repository.scanDeviceSongs()
            _uiState.update { state ->
                recomputeDerivedLibraryState(
                    state.copy(
                        allSongs = scannedSongs,
                        isLoading = false
                    )
                )
            }
            syncPlayerState()
        }
    }

    fun updateSearchQuery(query: String) {
        _uiState.update { state ->
            recomputeDerivedLibraryState(state.copy(searchQuery = query))
        }
    }

    fun updateSortOption(sortOption: SortOption, ascending: Boolean = defaultAscendingFor(sortOption)) {
        _uiState.update { state ->
            recomputeDerivedLibraryState(
                state.copy(
                    sortOption = sortOption,
                    sortAscending = ascending
                )
            )
        }
    }

    fun toggleSortDirection() {
        _uiState.update { state ->
            recomputeDerivedLibraryState(state.copy(sortAscending = !state.sortAscending))
        }
    }

    fun updateGroupSortOption(
        groupSortOption: GroupSortOption,
        ascending: Boolean = defaultAscendingForGroup(groupSortOption)
    ) {
        _uiState.update { state ->
            recomputeDerivedLibraryState(
                state.copy(
                    groupSortOption = groupSortOption,
                    groupSortAscending = ascending
                )
            )
        }
    }

    fun toggleGroupSortDirection() {
        _uiState.update { state ->
            recomputeDerivedLibraryState(state.copy(groupSortAscending = !state.groupSortAscending))
        }
    }

    private fun defaultAscendingFor(option: SortOption): Boolean = when (option) {
        SortOption.TITLE, SortOption.ARTIST -> true
        SortOption.DATE_ADDED, SortOption.MOST_PLAYED, SortOption.DURATION -> false
    }

    private fun defaultAscendingForGroup(option: GroupSortOption): Boolean = when (option) {
        GroupSortOption.NAME -> true
        GroupSortOption.SONG_COUNT, GroupSortOption.NEWEST -> false
    }

    private fun recomputeDerivedLibraryState(state: PlayerUiState): PlayerUiState {
        val stats = state.playbackStats
        val visibleSongs = state.allSongs.filter { song ->
            stats[song.id]?.isHidden != true
        }
        val hiddenSongs = state.allSongs.filter { song ->
            stats[song.id]?.isHidden == true
        }

        val visibleSongsMap = visibleSongs.associateBy { it.id }

        val filtered = applyFilterAndSort(
            songs = visibleSongs,
            query = state.searchQuery,
            sortOption = state.sortOption,
            ascending = state.sortAscending,
            stats = stats
        )

        val favSongs = state.favoriteSongIds.mapNotNull { visibleSongsMap[it] }
        val mostPlayed = visibleSongs
            .filter { (stats[it.id]?.playCount ?: 0) > 0 }
            .sortedWith(
                compareByDescending<Song> { stats[it.id]?.playCount ?: 0 }
                    .thenByDescending { stats[it.id]?.lastPlayedAt ?: 0L }
            )
        val recentlyPlayed = visibleSongs
            .filter { (stats[it.id]?.lastPlayedAt ?: 0L) > 0L }
            .sortedByDescending { stats[it.id]?.lastPlayedAt ?: 0L }
        val withLyrics = visibleSongs
            .filter { it.hasLyrics }
            .sortedBy { it.title.lowercase(Locale.getDefault()) }

        val hydratedPlaylists = state.playlists.map { playlist ->
            playlist.copy(songs = playlist.songIds.mapNotNull { visibleSongsMap[it] })
        }

        return state.copy(
            filteredSongs = filtered,
            hiddenSongs = hiddenSongs,
            folders = buildFolderGroups(filtered, state.groupSortOption, state.groupSortAscending),
            artists = buildArtistGroups(filtered, state.groupSortOption, state.groupSortAscending),
            albums = buildAlbumGroups(filtered, state.groupSortOption, state.groupSortAscending),
            favoriteSongs = favSongs,
            mostPlayedSongs = mostPlayed,
            recentlyPlayedSongs = recentlyPlayed,
            songsWithLyrics = withLyrics,
            playlists = hydratedPlaylists
        )
    }

    private fun applyFilterAndSort(
        songs: List<Song>,
        query: String,
        sortOption: SortOption,
        ascending: Boolean,
        stats: Map<Long, PlaybackStatsEntity>
    ): List<Song> {
        val trimmed = query.trim()
        val matching = if (trimmed.isEmpty()) {
            songs
        } else {
            songs.filter { song ->
                song.title.contains(trimmed, ignoreCase = true) ||
                    song.artist.contains(trimmed, ignoreCase = true) ||
                    song.album.contains(trimmed, ignoreCase = true) ||
                    song.folderName.contains(trimmed, ignoreCase = true)
            }
        }

        val comparator: Comparator<Song> = when (sortOption) {
            SortOption.TITLE -> compareBy<Song> { it.title.lowercase(Locale.getDefault()) }
            SortOption.ARTIST -> compareBy<Song> { it.artist.lowercase(Locale.getDefault()) }
                .thenBy { it.title.lowercase(Locale.getDefault()) }
            SortOption.DURATION -> compareBy<Song> { it.duration }
                .thenBy { it.title.lowercase(Locale.getDefault()) }
            SortOption.DATE_ADDED -> compareBy<Song> { it.dateAdded }
                .thenBy { it.title.lowercase(Locale.getDefault()) }
            SortOption.MOST_PLAYED -> compareBy<Song> { stats[it.id]?.playCount ?: 0 }
                .thenBy { stats[it.id]?.lastPlayedAt ?: 0L }
        }

        return if (ascending) {
            matching.sortedWith(comparator)
        } else {
            matching.sortedWith(comparator.reversed())
        }
    }

    private fun buildFolderGroups(
        songs: List<Song>,
        sortOption: GroupSortOption,
        ascending: Boolean
    ): List<FolderGroup> {
        val map = LinkedHashMap<String, MutableList<Song>>()
        for (song in songs) {
            val key = song.folderPath.ifEmpty { "/storage/emulated/0/Music" }
                .lowercase(Locale.ENGLISH)
            map.getOrPut(key) { mutableListOf() }.add(song)
        }
        val groups = map.values.map { groupSongs ->
            val first = groupSongs.first()
            FolderGroup(
                folderName = first.folderName,
                folderPath = first.folderPath,
                songs = groupSongs
            )
        }
        val comparator: Comparator<FolderGroup> = when (sortOption) {
            GroupSortOption.NAME -> compareBy<FolderGroup> { it.folderName.lowercase(Locale.getDefault()) }
            GroupSortOption.SONG_COUNT -> compareBy<FolderGroup> { it.songCount }
                .thenBy { it.folderName.lowercase(Locale.getDefault()) }
            GroupSortOption.NEWEST -> compareBy<FolderGroup> { group ->
                group.songs.maxOfOrNull { it.dateAdded } ?: 0L
            }
        }
        return if (ascending) groups.sortedWith(comparator) else groups.sortedWith(comparator.reversed())
    }

    private fun buildArtistGroups(
        songs: List<Song>,
        sortOption: GroupSortOption,
        ascending: Boolean
    ): List<ArtistGroup> {
        val groups = songs.groupBy { it.artist }
            .map { (artistName, artistSongs) ->
                ArtistGroup(
                    artistName = artistName,
                    songs = artistSongs
                )
            }
        val comparator: Comparator<ArtistGroup> = when (sortOption) {
            GroupSortOption.NAME -> compareBy<ArtistGroup> { it.artistName.lowercase(Locale.getDefault()) }
            GroupSortOption.SONG_COUNT -> compareBy<ArtistGroup> { it.songCount }
                .thenBy { it.artistName.lowercase(Locale.getDefault()) }
            GroupSortOption.NEWEST -> compareBy<ArtistGroup> { group ->
                group.songs.maxOfOrNull { it.dateAdded } ?: 0L
            }
        }
        return if (ascending) groups.sortedWith(comparator) else groups.sortedWith(comparator.reversed())
    }

    private fun buildAlbumGroups(
        songs: List<Song>,
        sortOption: GroupSortOption,
        ascending: Boolean
    ): List<AlbumGroup> {
        val groups = songs.groupBy { it.album }
            .map { (albumName, albumSongs) ->
                val first = albumSongs.first()
                val cover = albumSongs.firstOrNull { !it.albumArtUri.isNullOrBlank() }?.albumArtUri
                AlbumGroup(
                    albumName = albumName,
                    artistName = first.artist,
                    albumArtUri = cover,
                    songs = albumSongs
                )
            }
        val comparator: Comparator<AlbumGroup> = when (sortOption) {
            GroupSortOption.NAME -> compareBy<AlbumGroup> { it.albumName.lowercase(Locale.getDefault()) }
            GroupSortOption.SONG_COUNT -> compareBy<AlbumGroup> { it.songCount }
                .thenBy { it.albumName.lowercase(Locale.getDefault()) }
            GroupSortOption.NEWEST -> compareBy<AlbumGroup> { group ->
                group.songs.maxOfOrNull { it.dateAdded } ?: 0L
            }
        }
        return if (ascending) groups.sortedWith(comparator) else groups.sortedWith(comparator.reversed())
    }

    private fun loadLyricsForCurrentSongIfNeeded(song: Song?) {
        if (song == null) {
            loadedLyricsSongId = null
            _uiState.update { it.copy(currentLyrics = emptyList()) }
            return
        }
        if (loadedLyricsSongId == song.id) return
        loadedLyricsSongId = song.id
        viewModelScope.launch {
            val lyrics = repository.readLyricsForSong(song)
            if (loadedLyricsSongId == song.id) {
                _uiState.update { it.copy(currentLyrics = lyrics) }
            }
        }
    }

    fun playSongFromQueue(song: Song, queue: List<Song>) {
        val activeQueue = queue.ifEmpty { listOf(song) }
        val startIndex = activeQueue.indexOfFirst { it.id == song.id }.coerceAtLeast(0)
        val targetSong = activeQueue[startIndex]
        _uiState.update {
            it.copy(
                currentQueue = activeQueue,
                currentSong = targetSong,
                duration = targetSong.duration,
                currentPosition = 0L
            )
        }
        lastRecordedPlayedSongId = null
        recordPlayIfNeeded(targetSong.id)
        loadLyricsForCurrentSongIfNeeded(targetSong)
        controllerManager.playQueue(activeQueue, startIndex)
    }

    fun playNext(song: Song) {
        controllerManager.playNext(song)
        syncPlayerState()
    }

    fun playQueueItemAt(index: Int) {
        controllerManager.skipToQueueIndex(index)
    }

    fun togglePlayPause() {
        val controller = controllerManager.controller
        if (controller != null && controller.mediaItemCount == 0) {
            val defaultQueue = _uiState.value.filteredSongs.ifEmpty { _uiState.value.allSongs }
            if (defaultQueue.isNotEmpty()) {
                playSongFromQueue(defaultQueue.first(), defaultQueue)
                return
            }
        }
        controllerManager.togglePlayPause()
    }

    fun seekTo(positionMs: Long) {
        _uiState.update { it.copy(currentPosition = positionMs.coerceAtLeast(0L)) }
        controllerManager.seekTo(positionMs)
    }

    fun skipToNext() {
        controllerManager.skipToNext()
    }

    fun skipToPrevious() {
        controllerManager.skipToPrevious()
    }

    fun toggleShuffle() {
        controllerManager.toggleShuffle()
    }

    fun cycleRepeatMode() {
        controllerManager.cycleRepeatMode()
    }

    fun setPlaybackSpeed(speed: Float) {
        val clamped = speed.coerceIn(0.5f, 2.0f)
        controllerManager.setPlaybackSpeed(clamped)
        _uiState.update { it.copy(playbackSpeed = clamped) }
    }

    fun setSleepTimer(option: SleepTimerOption) {
        sleepTimerJob?.cancel()
        sleepTimerJob = null

        when {
            option == SleepTimerOption.OFF -> {
                _uiState.update {
                    it.copy(
                        sleepTimerOption = SleepTimerOption.OFF,
                        sleepTimerRemainingMs = 0L
                    )
                }
            }

            option == SleepTimerOption.END_OF_TRACK -> {
                val remaining = (_uiState.value.duration - _uiState.value.currentPosition).coerceAtLeast(0L)
                _uiState.update {
                    it.copy(
                        sleepTimerOption = SleepTimerOption.END_OF_TRACK,
                        sleepTimerRemainingMs = remaining
                    )
                }
            }

            option.minutes > 0 -> {
                val totalMs = option.minutes * 60_000L
                _uiState.update {
                    it.copy(
                        sleepTimerOption = option,
                        sleepTimerRemainingMs = totalMs
                    )
                }
                sleepTimerJob = viewModelScope.launch {
                    var remaining = totalMs
                    while (isActive && remaining > 0L) {
                        delay(1000L)
                        remaining = (remaining - 1000L).coerceAtLeast(0L)
                        _uiState.update { it.copy(sleepTimerRemainingMs = remaining) }
                    }
                    if (remaining <= 0L) {
                        controllerManager.pause()
                        _uiState.update {
                            it.copy(
                                sleepTimerOption = SleepTimerOption.OFF,
                                sleepTimerRemainingMs = 0L
                            )
                        }
                    }
                }
            }
        }
    }

    fun setEqualizerEnabled(enabled: Boolean) {
        AudioEffectsManager.setEnabled(enabled)
    }

    fun selectEqualizerPreset(preset: EqualizerPreset) {
        AudioEffectsManager.selectPreset(preset)
    }

    fun setEqualizerBandLevel(bandIndex: Int, levelMillibels: Int) {
        AudioEffectsManager.setBandLevel(bandIndex, levelMillibels)
    }

    fun setBassBoostStrength(strength: Int) {
        AudioEffectsManager.setBassBoostStrength(strength)
    }

    fun toggleFavorite(songId: Long) {
        viewModelScope.launch {
            repository.toggleFavorite(songId)
        }
    }

    fun hideSong(songId: Long) {
        viewModelScope.launch {
            repository.setSongHidden(songId, true)
        }
    }

    fun unhideSong(songId: Long) {
        viewModelScope.launch {
            repository.setSongHidden(songId, false)
        }
    }

    fun restoreAllHiddenSongs() {
        viewModelScope.launch {
            repository.restoreAllHiddenSongs()
        }
    }

    fun createPlaylist(name: String) {
        viewModelScope.launch {
            repository.createPlaylist(name)
        }
    }

    fun createPlaylistAndAddSong(name: String, songId: Long) {
        viewModelScope.launch {
            val playlistId = repository.createPlaylist(name)
            if (playlistId > 0L) {
                repository.addSongToPlaylist(playlistId, songId)
            }
        }
    }

    fun deletePlaylist(playlistId: Long) {
        viewModelScope.launch {
            repository.deletePlaylist(playlistId)
        }
    }

    fun addSongToPlaylist(playlistId: Long, songId: Long) {
        viewModelScope.launch {
            repository.addSongToPlaylist(playlistId, songId)
        }
    }

    fun removeSongFromPlaylist(playlistId: Long, songId: Long) {
        viewModelScope.launch {
            repository.removeSongFromPlaylist(playlistId, songId)
        }
    }

    private fun syncPlayerState() {
        val controller = controllerManager.controller ?: return
        val songsById = _uiState.value.allSongs.associateBy { it.id }

        val currentMediaItem = controller.currentMediaItem
        val resolvedCurrentSong = currentMediaItem?.let { item ->
            val id = item.mediaId.toLongOrNull()
            (id?.let { songsById[it] }) ?: item.toSongOrNull()
        }

        val queueItems = buildList {
            for (i in 0 until controller.mediaItemCount) {
                val item = controller.getMediaItemAt(i)
                val id = item.mediaId.toLongOrNull()
                val song = (id?.let { songsById[it] }) ?: item.toSongOrNull()
                if (song != null) add(song)
            }
        }

        val playerDuration = controller.duration.takeIf { it > 0L }
            ?: resolvedCurrentSong?.duration
            ?: 0L

        val pos = controller.currentPosition.coerceIn(0L, playerDuration.coerceAtLeast(0L))
        val effectiveSong = resolvedCurrentSong ?: _uiState.value.currentSong

        _uiState.update { state ->
            val endOfTrackRemaining = if (state.sleepTimerOption == SleepTimerOption.END_OF_TRACK) {
                (playerDuration - pos).coerceAtLeast(0L)
            } else {
                state.sleepTimerRemainingMs
            }
            state.copy(
                currentSong = effectiveSong,
                currentQueue = if (queueItems.isNotEmpty()) queueItems else state.currentQueue,
                isPlaying = controller.isPlaying,
                shuffleModeEnabled = controller.shuffleModeEnabled,
                repeatMode = controller.repeatMode,
                playbackSpeed = controller.playbackParameters.speed,
                sleepTimerRemainingMs = endOfTrackRemaining,
                currentPosition = pos,
                duration = playerDuration
            )
        }
        loadLyricsForCurrentSongIfNeeded(effectiveSong)
    }

    private fun startPositionTracker() {
        if (positionTrackerJob?.isActive == true) return
        positionTrackerJob = viewModelScope.launch {
            while (isActive) {
                val controller = controllerManager.controller
                if (controller == null || !controller.isPlaying) {
                    break
                }
                val currentSongDuration = _uiState.value.currentSong?.duration ?: 0L
                val totalDuration = controller.duration.takeIf { it > 0L } ?: currentSongDuration
                val currentPos = controller.currentPosition.coerceIn(
                    0L,
                    totalDuration.coerceAtLeast(0L)
                )
                _uiState.update { state ->
                    val endOfTrackRemaining = if (state.sleepTimerOption == SleepTimerOption.END_OF_TRACK) {
                        (totalDuration - currentPos).coerceAtLeast(0L)
                    } else {
                        state.sleepTimerRemainingMs
                    }
                    state.copy(
                        currentPosition = currentPos,
                        duration = totalDuration,
                        sleepTimerRemainingMs = endOfTrackRemaining,
                        isPlaying = true
                    )
                }
                delay(250L)
            }
        }
    }

    private fun stopPositionTracker() {
        positionTrackerJob?.cancel()
        positionTrackerJob = null
    }

    override fun onCleared() {
        stopPositionTracker()
        sleepTimerJob?.cancel()
        controllerManager.release()
        super.onCleared()
    }
}
