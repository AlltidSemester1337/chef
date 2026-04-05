package com.formulae.chef.services.voice

import android.content.Context
import android.util.Log
import androidx.core.net.toUri
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import java.io.File
import kotlinx.coroutines.CancellationException
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
import kotlinx.coroutines.withContext

/**
 * Plays TTS audio via ExoPlayer with gapless sentence-chunked streaming.
 * [playChunked] accepts a [Flow] of synthesized MP3 chunks. Each chunk is written to a
 * temp file and appended to the player's playlist as it arrives, so playback of the first
 * sentence begins immediately while subsequent sentences are still being synthesized.
 * Temp files are deleted once playback finishes or is stopped.
 */
class AudioPlayer(private val context: Context) {

    private val _isSpeaking = MutableStateFlow(false)
    val isSpeaking: StateFlow<Boolean> = _isSpeaking.asStateFlow()

    private val _speakingMessageId = MutableStateFlow<String?>(null)
    val speakingMessageId: StateFlow<String?> = _speakingMessageId.asStateFlow()

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var playbackJob: Job? = null
    private var player: ExoPlayer? = null
    private val tmpFiles = mutableListOf<File>()

    private val ttsAudioAttributes = AudioAttributes.Builder()
        .setContentType(C.AUDIO_CONTENT_TYPE_SPEECH)
        .setUsage(C.USAGE_MEDIA)
        .build()

    /**
     * Starts chunked playback. Each [ByteArray] emitted by [audioFlow] is written to a
     * temp file and added to the player's playlist. Cancels any in-progress playback first.
     */
    fun playChunked(audioFlow: Flow<ByteArray>, messageId: String) {
        stop()

        playbackJob = scope.launch {
            val newPlayer = ExoPlayer.Builder(context).build()
            newPlayer.setAudioAttributes(ttsAudioAttributes, true)
            player = newPlayer
            _isSpeaking.value = true
            _speakingMessageId.value = messageId
            newPlayer.playWhenReady = true

            var flowDone = false

            newPlayer.addListener(object : Player.Listener {
                override fun onPlaybackStateChanged(state: Int) {
                    Log.d("AudioPlayer", "State → ${playerStateName(state)}, flowDone=$flowDone")
                    if (state == Player.STATE_ENDED && flowDone) {
                        deleteTmpFiles()
                        _isSpeaking.value = false
                        _speakingMessageId.value = null
                    }
                }

                override fun onPlayerError(error: PlaybackException) {
                    Log.e("AudioPlayer", "ExoPlayer error: ${error.errorCodeName}", error)
                    deleteTmpFiles()
                    _isSpeaking.value = false
                    _speakingMessageId.value = null
                }

                override fun onIsPlayingChanged(isPlaying: Boolean) {
                    Log.d("AudioPlayer", "isPlaying=$isPlaying")
                }
            })

            try {
                var chunkIndex = 0
                audioFlow.collect { bytes ->
                    val idx = chunkIndex
                    val tmpFile = withContext(Dispatchers.IO) {
                        File.createTempFile("tts_chunk_$idx", ".mp3", context.cacheDir)
                            .also { it.writeBytes(bytes) }
                    }
                    tmpFiles.add(tmpFile)
                    Log.d("AudioPlayer", "Chunk $idx: ${bytes.size} bytes → ${tmpFile.name}")

                    newPlayer.addMediaItem(MediaItem.fromUri(tmpFile.toUri()))
                    chunkIndex++

                    when (newPlayer.playbackState) {
                        Player.STATE_IDLE -> {
                            Log.d("AudioPlayer", "First chunk — calling prepare()")
                            newPlayer.prepare()
                        }
                        Player.STATE_ENDED -> {
                            // Race: player exhausted earlier items before this chunk arrived.
                            Log.d("AudioPlayer", "Resuming from STATE_ENDED at item $idx")
                            newPlayer.seekTo(newPlayer.mediaItemCount - 1, 0L)
                        }
                        else -> Unit
                    }
                }
                flowDone = true
                val state = playerStateName(newPlayer.playbackState)
                Log.d("AudioPlayer", "Flow complete, ${newPlayer.mediaItemCount} items, state=$state")

                if (newPlayer.playbackState == Player.STATE_ENDED) {
                    deleteTmpFiles()
                    _isSpeaking.value = false
                    _speakingMessageId.value = null
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e("AudioPlayer", "Chunked playback error", e)
                deleteTmpFiles()
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
        deleteTmpFiles()
        _isSpeaking.value = false
        _speakingMessageId.value = null
    }

    fun release() {
        stop()
        scope.cancel()
    }

    private fun deleteTmpFiles() {
        tmpFiles.forEach { it.delete() }
        tmpFiles.clear()
    }

    private fun playerStateName(state: Int) = when (state) {
        Player.STATE_IDLE -> "IDLE"
        Player.STATE_BUFFERING -> "BUFFERING"
        Player.STATE_READY -> "READY"
        Player.STATE_ENDED -> "ENDED"
        else -> "UNKNOWN($state)"
    }
}
