package com.example.audio

import android.media.AudioAttributes
import android.media.MediaPlayer
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File

data class AudioPlaybackState(
    val activeTrackId: Long? = null,
    val isPlaying: Boolean = false,
    val isLooping: Boolean = true,
    val currentPositionMs: Int = 0,
    val durationMs: Int = 0,
    val errorMessage: String? = null
)

class FocusAudioPlayer {

    private val playerScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var mediaPlayer: MediaPlayer? = null
    private var progressJob: Job? = null

    private val _playbackState = MutableStateFlow(AudioPlaybackState())
    val playbackState: StateFlow<AudioPlaybackState> = _playbackState.asStateFlow()

    fun togglePlay(trackId: Long, filePath: String) {
        val current = _playbackState.value
        if (current.activeTrackId == trackId && mediaPlayer != null) {
            if (current.isPlaying) {
                mediaPlayer?.pause()
                _playbackState.update { it.copy(isPlaying = false) }
            } else {
                mediaPlayer?.start()
                _playbackState.update { it.copy(isPlaying = true) }
            }
            return
        }
        playTrack(trackId, filePath)
    }

    fun playTrack(trackId: Long, filePath: String) {
        releasePlayer()
        val file = File(filePath)
        if (!file.exists()) {
            _playbackState.update {
                it.copy(
                    activeTrackId = null,
                    isPlaying = false,
                    errorMessage = "Audio file not found on device."
                )
            }
            return
        }

        try {
            val mp = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                isLooping = _playbackState.value.isLooping
                setDataSource(file.absolutePath)
                setOnPreparedListener { prepared ->
                    prepared.start()
                    _playbackState.update {
                        it.copy(
                            activeTrackId = trackId,
                            isPlaying = true,
                            durationMs = prepared.duration.coerceAtLeast(0),
                            currentPositionMs = 0,
                            errorMessage = null
                        )
                    }
                    startProgressTicker()
                }
                setOnCompletionListener {
                    if (!it.isLooping) {
                        _playbackState.update { state ->
                            state.copy(isPlaying = false, currentPositionMs = state.durationMs)
                        }
                    }
                }
                setOnErrorListener { _, what, extra ->
                    Log.e(TAG, "MediaPlayer error what=$what extra=$extra")
                    _playbackState.update {
                        it.copy(
                            isPlaying = false,
                            errorMessage = "Audio playback error ($what)."
                        )
                    }
                    true
                }
                prepareAsync()
            }
            mediaPlayer = mp
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize MediaPlayer", e)
            _playbackState.update {
                it.copy(
                    isPlaying = false,
                    errorMessage = e.localizedMessage ?: "Unable to play audio."
                )
            }
        }
    }

    fun toggleLooping() {
        val nextLoop = !_playbackState.value.isLooping
        mediaPlayer?.isLooping = nextLoop
        _playbackState.update { it.copy(isLooping = nextLoop) }
    }

    fun seekTo(positionMs: Int) {
        mediaPlayer?.let { mp ->
            mp.seekTo(positionMs)
            _playbackState.update { it.copy(currentPositionMs = positionMs) }
        }
    }

    fun stopAndRelease() {
        releasePlayer()
        _playbackState.value = AudioPlaybackState(isLooping = _playbackState.value.isLooping)
    }

    private fun startProgressTicker() {
        progressJob?.cancel()
        progressJob = playerScope.launch {
            while (isActive) {
                val mp = mediaPlayer
                if (mp != null && _playbackState.value.isPlaying) {
                    runCatching {
                        _playbackState.update {
                            it.copy(
                                currentPositionMs = mp.currentPosition.coerceAtLeast(0),
                                durationMs = mp.duration.coerceAtLeast(it.durationMs)
                            )
                        }
                    }
                }
                delay(350L)
            }
        }
    }

    private fun releasePlayer() {
        progressJob?.cancel()
        progressJob = null
        runCatching { mediaPlayer?.stop() }
        runCatching { mediaPlayer?.release() }
        mediaPlayer = null
    }

    companion object {
        private const val TAG = "FocusAudioPlayer"
    }
}
