@file:Suppress("ktlint:standard:filename")

package com.formulae.chef.feature.chat.ui

import android.Manifest
import android.content.pm.PackageManager
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.formulae.chef.BuildConfig
import com.formulae.chef.services.voice.AudioPlayer
import com.formulae.chef.services.voice.GcpTextToSpeechService
import com.formulae.chef.services.voice.SpeechInputManager
import com.formulae.chef.services.voice.splitIntoSentences
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.flow

data class VoiceControllerState(
    val isRecording: Boolean,
    val speakingMessageId: String?,
    val onStartRecording: () -> Unit,
    val onSpeakClicked: (ChatMessage) -> Unit
)

@Composable
fun rememberVoiceController(
    onSendMessage: (String) -> Unit,
    lastNonPendingModelMessage: ChatMessage?
): VoiceControllerState {
    val context = LocalContext.current

    val speechManager = remember { SpeechInputManager(context) }
    val ttsService = remember { GcpTextToSpeechService(BuildConfig.gcpTtsApiKey) }
    val audioPlayer = remember { AudioPlayer(context) }

    DisposableEffect(Unit) {
        onDispose {
            speechManager.destroy()
            audioPlayer.release()
        }
    }

    val isRecording by speechManager.isListening.collectAsState()
    val transcript by speechManager.transcript.collectAsState()
    val speechError by speechManager.error.collectAsState()
    val speakingMessageId by audioPlayer.speakingMessageId.collectAsState()

    var pendingVoiceTts by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            speechManager.startListening()
        } else {
            Toast.makeText(context, "Microphone permission is required for voice input", Toast.LENGTH_SHORT).show()
        }
    }

    LaunchedEffect(transcript) {
        val text = transcript ?: return@LaunchedEffect
        onSendMessage(text)
        pendingVoiceTts = true
        speechManager.clearTranscript()
    }

    LaunchedEffect(speechError) {
        val error = speechError ?: return@LaunchedEffect
        Toast.makeText(context, error, Toast.LENGTH_SHORT).show()
        speechManager.clearError()
    }

    LaunchedEffect(lastNonPendingModelMessage?.id) {
        if (pendingVoiceTts && lastNonPendingModelMessage != null) {
            pendingVoiceTts = false
            val text = lastNonPendingModelMessage.text
            val messageId = lastNonPendingModelMessage.id
            if (text.isNotBlank()) {
                val sentences = text.splitIntoSentences()
                val audioFlow = flow {
                    for (sentence in sentences) {
                        try {
                            emit(ttsService.synthesize(sentence))
                        } catch (e: CancellationException) {
                            throw e
                        } catch (e: Exception) {
                            Log.e("VoiceController", "TTS auto-play failed for sentence", e)
                            Toast.makeText(context, "Voice playback failed", Toast.LENGTH_SHORT).show()
                            return@flow
                        }
                    }
                }
                audioPlayer.playChunked(audioFlow, messageId)
            }
        }
    }

    return VoiceControllerState(
        isRecording = isRecording,
        speakingMessageId = speakingMessageId,
        onStartRecording = {
            val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
                PackageManager.PERMISSION_GRANTED
            if (granted) {
                speechManager.startListening()
            } else {
                permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            }
        },
        onSpeakClicked = { msg ->
            if (speakingMessageId == msg.id) {
                audioPlayer.stop()
            } else {
                val sentences = msg.text.splitIntoSentences()
                val audioFlow = flow {
                    for (sentence in sentences) {
                        try {
                            emit(ttsService.synthesize(sentence))
                        } catch (e: CancellationException) {
                            throw e
                        } catch (e: Exception) {
                            Log.e("VoiceController", "TTS speak-on-demand failed for sentence", e)
                            Toast.makeText(context, "Voice playback failed", Toast.LENGTH_SHORT).show()
                            return@flow
                        }
                    }
                }
                audioPlayer.playChunked(audioFlow, msg.id)
            }
        }
    )
}
