package com.example.melody.playback

import android.app.PendingIntent
import android.content.Intent
import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.example.melody.MainActivity
import com.example.melody.domain.model.EqualizerBand
import com.example.melody.domain.model.EqualizerPreset
import com.example.melody.domain.model.EqualizerUiState
import com.example.melody.domain.model.defaultEqualizerBands
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

object AudioEffectsManager {
    private var equalizer: Equalizer? = null
    private var bassBoost: BassBoost? = null
    private var currentSessionId: Int = C.AUDIO_SESSION_ID_UNSET

    private val _state = MutableStateFlow(EqualizerUiState())
    val state: StateFlow<EqualizerUiState> = _state.asStateFlow()

    @Synchronized
    fun bindToAudioSession(audioSessionId: Int) {
        if (audioSessionId == C.AUDIO_SESSION_ID_UNSET || audioSessionId <= 0) return
        if (audioSessionId == currentSessionId && equalizer != null) return

        releaseEffectsInternal()
        currentSessionId = audioSessionId

        try {
            val eq = Equalizer(0, audioSessionId).apply {
                enabled = _state.value.enabled
            }
            equalizer = eq

            val numBands = eq.numberOfBands.toInt().coerceAtLeast(1)
            val levelRange = eq.bandLevelRange
            val minLevel = levelRange.getOrNull(0)?.toInt() ?: -1500
            val maxLevel = levelRange.getOrNull(1)?.toInt() ?: 1500

            val currentBands = _state.value.bands
            val hardwareBands = (0 until numBands).map { idx ->
                val centerFreqMilliHz = eq.getCenterFreq(idx.toShort())
                val centerFreqHz = (centerFreqMilliHz / 1000).coerceAtLeast(1)
                val desiredLevel = currentBands.getOrNull(idx)?.levelMillibels
                    ?.coerceIn(minLevel, maxLevel) ?: 0
                eq.setBandLevel(idx.toShort(), desiredLevel.toShort())
                EqualizerBand(
                    index = idx,
                    centerFreqHz = centerFreqHz,
                    levelMillibels = desiredLevel,
                    minLevelMillibels = minLevel,
                    maxLevelMillibels = maxLevel
                )
            }

            _state.update { it.copy(bands = hardwareBands) }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        try {
            val bb = BassBoost(0, audioSessionId).apply {
                enabled = _state.value.enabled && _state.value.bassBoostStrength > 0
                if (strengthSupported) {
                    setStrength(_state.value.bassBoostStrength.coerceIn(0, 1000).toShort())
                }
            }
            bassBoost = bb
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    @Synchronized
    fun setEnabled(enabled: Boolean) {
        _state.update { it.copy(enabled = enabled) }
        try {
            equalizer?.enabled = enabled
            bassBoost?.enabled = enabled && _state.value.bassBoostStrength > 0
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    @Synchronized
    fun selectPreset(preset: EqualizerPreset) {
        val currentBands = _state.value.bands.ifEmpty { defaultEqualizerBands() }
        val updatedBands = currentBands.mapIndexed { index, band ->
            val target = preset.bandLevelsMillibels.getOrElse(index) { 0 }
                .coerceIn(band.minLevelMillibels, band.maxLevelMillibels)
            try {
                equalizer?.setBandLevel(index.toShort(), target.toShort())
            } catch (e: Exception) {
                e.printStackTrace()
            }
            band.copy(levelMillibels = target)
        }
        if (!_state.value.enabled) {
            setEnabled(true)
        }
        _state.update {
            it.copy(
                enabled = true,
                preset = preset,
                bands = updatedBands
            )
        }
    }

    @Synchronized
    fun setBandLevel(bandIndex: Int, levelMillibels: Int) {
        val currentBands = _state.value.bands.ifEmpty { defaultEqualizerBands() }
        val updatedBands = currentBands.map { band ->
            if (band.index == bandIndex) {
                val clamped = levelMillibels.coerceIn(band.minLevelMillibels, band.maxLevelMillibels)
                try {
                    equalizer?.setBandLevel(bandIndex.toShort(), clamped.toShort())
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                band.copy(levelMillibels = clamped)
            } else {
                band
            }
        }
        if (!_state.value.enabled) {
            setEnabled(true)
        }
        _state.update {
            it.copy(
                enabled = true,
                preset = EqualizerPreset.CUSTOM,
                bands = updatedBands
            )
        }
    }

    @Synchronized
    fun setBassBoostStrength(strength: Int) {
        val clamped = strength.coerceIn(0, 1000)
        if (!_state.value.enabled && clamped > 0) {
            setEnabled(true)
        }
        _state.update { it.copy(bassBoostStrength = clamped) }
        try {
            bassBoost?.let { bb ->
                bb.enabled = _state.value.enabled && clamped > 0
                if (bb.strengthSupported) {
                    bb.setStrength(clamped.toShort())
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    @Synchronized
    fun release() {
        releaseEffectsInternal()
        currentSessionId = C.AUDIO_SESSION_ID_UNSET
    }

    private fun releaseEffectsInternal() {
        try {
            equalizer?.release()
        } catch (_: Exception) {
        }
        equalizer = null

        try {
            bassBoost?.release()
        } catch (_: Exception) {
        }
        bassBoost = null
    }
}

class AudioPlaybackService : MediaSessionService() {

    private var mediaSession: MediaSession? = null

    @UnstableApi
    override fun onCreate() {
        super.onCreate()

        val audioAttributes = AudioAttributes.Builder()
            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
            .setUsage(C.USAGE_MEDIA)
            .build()

        val player = ExoPlayer.Builder(this)
            .setAudioAttributes(audioAttributes, /* handleAudioFocus = */ true)
            .setHandleAudioBecomingNoisy(true)
            .setWakeMode(C.WAKE_MODE_LOCAL)
            .build()

        AudioEffectsManager.bindToAudioSession(player.audioSessionId)
        player.addListener(object : Player.Listener {
            override fun onAudioSessionIdChanged(audioSessionId: Int) {
                AudioEffectsManager.bindToAudioSession(audioSessionId)
            }
        })

        val sessionActivityIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val sessionActivityPendingIntent = PendingIntent.getActivity(
            this,
            0,
            sessionActivityIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        mediaSession = MediaSession.Builder(this, player)
            .setSessionActivity(sessionActivityPendingIntent)
            .build()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return mediaSession
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        val player = mediaSession?.player
        if (player == null || !player.playWhenReady || player.mediaItemCount == 0) {
            stopSelf()
        }
    }

    override fun onDestroy() {
        AudioEffectsManager.release()
        mediaSession?.run {
            player.release()
            release()
            mediaSession = null
        }
        super.onDestroy()
    }
}
