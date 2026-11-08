package com.example.melody.playback

import android.content.ComponentName
import android.content.Context
import android.net.Uri
import android.os.Bundle
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.example.melody.domain.model.Song
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.guava.await

class PlayerControllerManager(private val context: Context) {

    private var controllerFuture: ListenableFuture<MediaController>? = null
    var controller: MediaController? = null
        private set

    private val listeners = mutableListOf<Player.Listener>()

    suspend fun connect(listener: Player.Listener): MediaController? {
        if (!listeners.contains(listener)) {
            listeners.add(listener)
        }
        controller?.let { existing ->
            existing.removeListener(listener)
            existing.addListener(listener)
            return existing
        }

        val sessionToken = SessionToken(
            context.applicationContext,
            ComponentName(context.applicationContext, AudioPlaybackService::class.java)
        )

        val future = controllerFuture ?: MediaController.Builder(
            context.applicationContext,
            sessionToken
        ).buildAsync().also {
            controllerFuture = it
        }

        return try {
            val mediaController = future.await()
            controller = mediaController
            listeners.forEach { l ->
                mediaController.removeListener(l)
                mediaController.addListener(l)
            }
            mediaController
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun playQueue(songs: List<Song>, startIndex: Int = 0) {
        val mediaController = controller ?: return
        if (songs.isEmpty()) return

        val safeIndex = startIndex.coerceIn(0, songs.lastIndex)
        val mediaItems = songs.map { it.toMediaItem() }

        mediaController.setMediaItems(mediaItems, safeIndex, 0L)
        mediaController.prepare()
        mediaController.play()
    }

    /**
     * Inserta la canción inmediatamente después de la pista en reproducción actual
     * (currentMediaItemIndex + 1), o la reproduce de inmediato si la cola está vacía.
     */
    fun playNext(song: Song) {
        val mediaController = controller ?: return
        if (mediaController.mediaItemCount == 0) {
            playQueue(listOf(song), 0)
            return
        }
        val insertIndex = (mediaController.currentMediaItemIndex + 1)
            .coerceIn(0, mediaController.mediaItemCount)
        mediaController.addMediaItem(insertIndex, song.toMediaItem())
        if (mediaController.playbackState == Player.STATE_IDLE) {
            mediaController.prepare()
        }
    }

    fun togglePlayPause() {
        val mediaController = controller ?: return
        if (mediaController.isPlaying) {
            mediaController.pause()
        } else {
            if (mediaController.playbackState == Player.STATE_ENDED) {
                mediaController.seekTo(0L)
            }
            mediaController.play()
        }
    }

    fun pause() {
        controller?.pause()
    }

    fun setPlaybackSpeed(speed: Float) {
        val mediaController = controller ?: return
        mediaController.setPlaybackSpeed(speed.coerceIn(0.5f, 2.0f))
    }

    fun seekTo(positionMs: Long) {
        val mediaController = controller ?: return
        val duration = mediaController.duration.coerceAtLeast(0L)
        val target = if (duration > 0L) positionMs.coerceIn(0L, duration) else positionMs.coerceAtLeast(0L)
        mediaController.seekTo(target)
    }

    fun skipToNext() {
        val mediaController = controller ?: return
        if (mediaController.hasNextMediaItem()) {
            mediaController.seekToNextMediaItem()
        } else if (mediaController.mediaItemCount > 0) {
            mediaController.seekTo(0, 0L)
        }
    }

    fun skipToPrevious() {
        val mediaController = controller ?: return
        if (mediaController.currentPosition > 3000L) {
            mediaController.seekTo(0L)
        } else if (mediaController.hasPreviousMediaItem()) {
            mediaController.seekToPreviousMediaItem()
        } else if (mediaController.mediaItemCount > 0) {
            mediaController.seekTo(0, 0L)
        }
    }

    fun skipToQueueIndex(index: Int) {
        val mediaController = controller ?: return
        if (index in 0 until mediaController.mediaItemCount) {
            mediaController.seekTo(index, 0L)
            mediaController.play()
        }
    }

    fun toggleShuffle() {
        val mediaController = controller ?: return
        mediaController.shuffleModeEnabled = !mediaController.shuffleModeEnabled
    }

    fun cycleRepeatMode() {
        val mediaController = controller ?: return
        mediaController.repeatMode = when (mediaController.repeatMode) {
            Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
            Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
            else -> Player.REPEAT_MODE_OFF
        }
    }

    fun release() {
        controller?.let { mediaController ->
            listeners.forEach { mediaController.removeListener(it) }
        }
        listeners.clear()
        controllerFuture?.let { MediaController.releaseFuture(it) }
        controllerFuture = null
        controller = null
    }

    companion object {
        private const val EXTRA_SONG_DURATION = "extra_song_duration"
        private const val EXTRA_DATE_ADDED = "extra_date_added"
        private const val EXTRA_CONTENT_URI = "extra_content_uri"
        private const val EXTRA_FILE_PATH = "extra_file_path"
        private const val EXTRA_FOLDER_NAME = "extra_folder_name"
        private const val EXTRA_FOLDER_PATH = "extra_folder_path"
        private const val EXTRA_FILE_SIZE = "extra_file_size"
        private const val EXTRA_HAS_LYRICS = "extra_has_lyrics"
        private const val EXTRA_LRC_FILE_PATH = "extra_lrc_file_path"

        fun Song.toMediaItem(): MediaItem {
            val extras = Bundle().apply {
                putLong(EXTRA_SONG_DURATION, duration)
                putLong(EXTRA_DATE_ADDED, dateAdded)
                putString(EXTRA_CONTENT_URI, contentUri)
                putString(EXTRA_FILE_PATH, filePath)
                putString(EXTRA_FOLDER_NAME, folderName)
                putString(EXTRA_FOLDER_PATH, folderPath)
                putLong(EXTRA_FILE_SIZE, fileSize)
                putBoolean(EXTRA_HAS_LYRICS, hasLyrics)
                putString(EXTRA_LRC_FILE_PATH, lrcFilePath)
            }
            val metadata = MediaMetadata.Builder()
                .setTitle(title)
                .setArtist(artist)
                .setAlbumTitle(album)
                .setArtworkUri(albumArtUri?.let { Uri.parse(it) })
                .setExtras(extras)
                .build()

            return MediaItem.Builder()
                .setMediaId(id.toString())
                .setUri(Uri.parse(contentUri))
                .setMediaMetadata(metadata)
                .build()
        }

        fun MediaItem.toSongOrNull(): Song? {
            val id = mediaId.toLongOrNull() ?: return null
            val meta = mediaMetadata
            val extras = meta.extras
            val contentUri = localConfiguration?.uri?.toString()
                ?: extras?.getString(EXTRA_CONTENT_URI)
                ?: return null

            return Song(
                id = id,
                title = meta.title?.toString() ?: "Canción desconocida",
                artist = meta.artist?.toString() ?: "Artista desconocido",
                album = meta.albumTitle?.toString() ?: "Álbum desconocido",
                duration = extras?.getLong(EXTRA_SONG_DURATION) ?: 0L,
                contentUri = contentUri,
                albumArtUri = meta.artworkUri?.toString(),
                dateAdded = extras?.getLong(EXTRA_DATE_ADDED) ?: 0L,
                filePath = extras?.getString(EXTRA_FILE_PATH).orEmpty(),
                folderName = extras?.getString(EXTRA_FOLDER_NAME) ?: "Música",
                folderPath = extras?.getString(EXTRA_FOLDER_PATH).orEmpty(),
                fileSize = extras?.getLong(EXTRA_FILE_SIZE) ?: 0L,
                hasLyrics = extras?.getBoolean(EXTRA_HAS_LYRICS) ?: false,
                lrcFilePath = extras?.getString(EXTRA_LRC_FILE_PATH)
            )
        }
    }
}
