package com.formulae.chef.services.voice

import android.content.Context
import android.util.Log
import android.widget.Toast
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * Builds a cold [Flow] that synthesizes each sentence via [ttsService] and emits the resulting
 * MP3 bytes. Cancellation is propagated cleanly; other errors are logged and end the stream.
 */
fun buildTtsFlow(
    sentences: List<String>,
    ttsService: GcpTextToSpeechService,
    context: Context,
    logTag: String = "TTS"
): Flow<ByteArray> = flow {
    for (sentence in sentences) {
        try {
            emit(ttsService.synthesize(sentence))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(logTag, "TTS synthesis failed", e)
            Toast.makeText(context, "Voice playback failed", Toast.LENGTH_SHORT).show()
            return@flow
        }
    }
}
