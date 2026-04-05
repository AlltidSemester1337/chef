package com.formulae.chef.services.voice

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.datasource.ByteArrayDataSource
import androidx.media3.datasource.DataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.ConcatenatingMediaSource
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Plays TTS audio via ExoPlayer with gapless sentence-chunked streaming.
 * [playChunked] accepts a [Flow] of synthesized audio chunks and feeds them to a
 * [ConcatenatingMediaSource] as they arrive, so playback of the first sentence begins
 * immediately while subsequent sentences are still being synthesized.
 */
@Suppress("DEPRECATION") // ConcatenatingMediaSource deprecated in favour of player playlist API; migration deferred
class AudioPlayer(private val context: Context) {

    private val _isSpeaking = MutableStateFlow(false)
    val isSpeaking: StateFlow<Boolean> = _isSpeaking.asStateFlow()

    private val _speakingMessageId = MutableStateFlow<String?>(null)
    val speakingMessageId: StateFlow<String?> = _speakingMessageId.asStateFlow()

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var playbackJob: Job? = null
    private var player: ExoPlayer? = null

    /**
     * Starts gapless chunked playback. Each [ByteArray] emitted by [audioFlow] is appended
     * to the player's playlist as it arrives. Cancels any in-progress playback first.
     */
    fun playChunked(audioFlow: Flow<ByteArray>, messageId: String) {
        stop()

        playbackJob = scope.launch {
            val concatenatingSource = ConcatenatingMediaSource()
            val newPlayer = ExoPlayer.Builder(context).build()
            player = newPlayer
            _isSpeaking.value = true
            _speakingMessageId.value = messageId

            newPlayer.setMediaSource(concatenatingSource)
            newPlayer.playWhenReady = true

            var flowDone = false

            newPlayer.addListener(object : Player.Listener {
                override fun onPlaybackStateChanged(state: Int) {
                    if (state == Player.STATE_ENDED && flowDone) {
                        _isSpeaking.value = false
                        _speakingMessageId.value = null
                    }
                }

                override fun onPlayerError(error: PlaybackException) {
                    Log.e("AudioPlayer", "ExoPlayer error", error)
                    _isSpeaking.value = false
                    _speakingMessageId.value = null
                }
            })

            try {
                var chunkIndex = 0
                audioFlow.collect { bytes ->
                    val idx = chunkIndex
                    concatenatingSource.addMediaSource(createMediaSource(bytes))
                    chunkIndex++

                    when {
                        idx == 0 -> newPlayer.prepare()
                        newPlayer.playbackState == Player.STATE_ENDED -> {
                            // Race: player exhausted all sources before this chunk arrived.
                            // Seek to the new item to resume gapless playback.
                            newPlayer.seekTo(idx, 0L)
                        }
                    }
                }
                flowDone = true

                // If the player already finished all sources by the time the flow completed,
                // the STATE_ENDED listener won't fire again — clean up here.
                if (newPlayer.playbackState == Player.STATE_ENDED) {
                    _isSpeaking.value = false
                    _speakingMessageId.value = null
                }
            } catch (e: Exception) {
                Log.e("AudioPlayer", "Chunked playback error", e)
                _isSpeaking.value = false
                _speakingMessageId.value = null
            }
        }
    }

    fun stop() {
        playbackJob?.cancel()
        playbackJob = null
        player?.let {
            try {
                it.stop()
                it.release()
            } catch (e: Exception) {
                Log.w("AudioPlayer", "Error stopping player", e)
            }
        }
        player = null
        _isSpeaking.value = false
        _speakingMessageId.value = null
    }

    fun release() {
        stop()
        scope.cancel()
    }

    private fun createMediaSource(bytes: ByteArray): ProgressiveMediaSource =
        ProgressiveMediaSource.Factory(DataSource.Factory { ByteArrayDataSource(bytes) })
            .createMediaSource(MediaItem.fromUri(Uri.EMPTY))
}
