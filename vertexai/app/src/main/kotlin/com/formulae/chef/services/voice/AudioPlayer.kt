package com.formulae.chef.services.voice

import android.media.MediaDataSource
import android.media.MediaPlayer
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class AudioPlayer {

    private val _isSpeaking = MutableStateFlow(false)
    val isSpeaking: StateFlow<Boolean> = _isSpeaking.asStateFlow()

    private val _speakingMessageId = MutableStateFlow<String?>(null)
    val speakingMessageId: StateFlow<String?> = _speakingMessageId.asStateFlow()

    private var player: MediaPlayer? = null

    fun play(audioBytes: ByteArray, messageId: String) {
        stop()
        try {
            player = MediaPlayer().apply {
                setDataSource(ByteArrayMediaDataSource(audioBytes))
                setOnPreparedListener {
                    _isSpeaking.value = true
                    _speakingMessageId.value = messageId
                    start()
                }
                setOnCompletionListener {
                    _isSpeaking.value = false
                    _speakingMessageId.value = null
                    it.release()
                    if (player == it) player = null
                }
                setOnErrorListener { _, what, extra ->
                    Log.e("AudioPlayer", "MediaPlayer error: what=$what extra=$extra")
                    _isSpeaking.value = false
                    _speakingMessageId.value = null
                    false
                }
                prepareAsync()
            }
        } catch (e: Exception) {
            Log.e("AudioPlayer", "Failed to play audio", e)
            _isSpeaking.value = false
            _speakingMessageId.value = null
        }
    }

    fun stop() {
        player?.let {
            try {
                if (it.isPlaying) it.stop()
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
    }

    private class ByteArrayMediaDataSource(private val data: ByteArray) : MediaDataSource() {
        override fun readAt(position: Long, buffer: ByteArray, offset: Int, size: Int): Int {
            if (position >= data.size) return -1
            val available = data.size - position.toInt()
            val bytesToRead = minOf(size, available)
            System.arraycopy(data, position.toInt(), buffer, offset, bytesToRead)
            return bytesToRead
        }

        override fun getSize(): Long = data.size.toLong()

        override fun close() {}
    }
}
