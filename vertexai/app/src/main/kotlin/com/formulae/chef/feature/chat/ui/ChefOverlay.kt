package com.formulae.chef.feature.chat.ui

import android.Manifest
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.formulae.chef.BuildConfig
import com.formulae.chef.feature.chat.OverlayChatViewModel
import com.formulae.chef.feature.model.Recipe
import com.formulae.chef.services.voice.AudioPlayer
import com.formulae.chef.services.voice.GcpTextToSpeechService
import com.formulae.chef.services.voice.SpeechInputManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChefOverlay(
    viewModel: OverlayChatViewModel,
    recipe: Recipe?,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val uiState by viewModel.uiState.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    val messageCount = uiState.messages.size
    val context = LocalContext.current

    val speechManager = remember { SpeechInputManager(context) }
    val ttsService = remember { GcpTextToSpeechService(BuildConfig.gcpTtsApiKey) }
    val audioPlayer = remember { AudioPlayer() }

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

    LaunchedEffect(Unit) {
        if (recipe != null) {
            viewModel.initWithRecipeContext(recipe)
        }
    }

    LaunchedEffect(transcript) {
        val text = transcript ?: return@LaunchedEffect
        viewModel.sendMessage(text)
        pendingVoiceTts = true
        speechManager.clearTranscript()
        coroutineScope.launch { listState.scrollToItem(0) }
    }

    LaunchedEffect(speechError) {
        val error = speechError ?: return@LaunchedEffect
        Toast.makeText(context, error, Toast.LENGTH_SHORT).show()
        speechManager.clearError()
    }

    val lastNonPendingModelMessage = uiState.messages.lastOrNull {
        it.participant == Participant.MODEL && !it.isPending && it.text.isNotBlank()
    }
    LaunchedEffect(lastNonPendingModelMessage?.id) {
        if (pendingVoiceTts && lastNonPendingModelMessage != null) {
            pendingVoiceTts = false
            val text = lastNonPendingModelMessage.text
            val messageId = lastNonPendingModelMessage.id
            coroutineScope.launch {
                try {
                    val audioBytes = withContext(Dispatchers.IO) { ttsService.synthesize(text) }
                    audioPlayer.play(audioBytes, messageId)
                } catch (e: Exception) {
                    Toast.makeText(context, "Voice playback failed", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    LaunchedEffect(messageCount) {
        if (messageCount > 0) listState.animateScrollToItem(0)
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        modifier = Modifier.fillMaxHeight(0.75f)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            if (isLoading && uiState.messages.isEmpty()) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
            } else {
                LazyColumn(
                    reverseLayout = true,
                    state = listState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    items(uiState.messages.reversed(), key = { it.id }) { message ->
                        OverlayChatBubble(
                            message = message,
                            speakingMessageId = speakingMessageId,
                            onSpeakClicked = { msg ->
                                coroutineScope.launch {
                                    try {
                                        if (speakingMessageId == msg.id) {
                                            audioPlayer.stop()
                                        } else {
                                            val audioBytes = withContext(Dispatchers.IO) {
                                                ttsService.synthesize(msg.text)
                                            }
                                            audioPlayer.play(audioBytes, msg.id)
                                        }
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "Voice playback failed", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                        )
                    }
                }
            }

            OverlayMessageInput(
                onSendMessage = { text ->
                    viewModel.sendMessage(text)
                    coroutineScope.launch { listState.scrollToItem(0) }
                },
                isRecording = isRecording,
                onStartRecording = {
                    permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                }
            )
        }
    }
}

@Composable
private fun OverlayChatBubble(
    message: ChatMessage,
    speakingMessageId: String? = null,
    onSpeakClicked: ((ChatMessage) -> Unit)? = null
) {
    val isModelMessage = message.participant == Participant.MODEL ||
        message.participant == Participant.ERROR
    val horizontalAlignment = if (isModelMessage) Alignment.Start else Alignment.End

    Column(
        horizontalAlignment = horizontalAlignment,
        modifier = Modifier
            .padding(horizontal = 8.dp, vertical = 4.dp)
            .fillMaxWidth()
    ) {
        val label = when (message.participant) {
            Participant.USER -> "You"
            Participant.MODEL -> "Chef"
            Participant.ERROR -> "Error"
        }
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(bottom = 4.dp)
        )

        val backgroundColor = when (message.participant) {
            Participant.MODEL -> MaterialTheme.colorScheme.primaryContainer
            Participant.USER -> MaterialTheme.colorScheme.tertiaryContainer
            Participant.ERROR -> MaterialTheme.colorScheme.errorContainer
        }
        val bubbleShape = if (isModelMessage) {
            RoundedCornerShape(4.dp, 20.dp, 20.dp, 20.dp)
        } else {
            RoundedCornerShape(20.dp, 4.dp, 20.dp, 20.dp)
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            if (message.isPending) {
                CircularProgressIndicator(
                    modifier = Modifier
                        .align(Alignment.CenterVertically)
                        .padding(all = 8.dp)
                )
            }
            Card(
                colors = CardDefaults.cardColors(containerColor = backgroundColor),
                shape = bubbleShape,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(text = message.text, modifier = Modifier.fillMaxWidth())
                }
                if (isModelMessage && onSpeakClicked != null && message.text.isNotBlank()) {
                    Row(modifier = Modifier.fillMaxWidth()) {
                        IconButton(onClick = { onSpeakClicked(message) }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                                contentDescription = if (speakingMessageId == message.id) {
                                    "Stop reading"
                                } else {
                                    "Read response aloud"
                                },
                                tint = if (speakingMessageId == message.id) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    Color.Gray
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun OverlayMessageInput(
    onSendMessage: (String) -> Unit,
    isRecording: Boolean = false,
    onStartRecording: () -> Unit = {}
) {
    var userMessage by rememberSaveable { mutableStateOf("") }
    val keyboardController = LocalSoftwareKeyboardController.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            modifier = Modifier
                .pointerInput(Unit) {
                    detectTapGestures(onLongPress = { onStartRecording() })
                },
            onClick = {}
        ) {
            Icon(
                imageVector = if (isRecording) Icons.Default.MicOff else Icons.Default.Mic,
                contentDescription = if (isRecording) "Recording…" else "Hold to speak",
                tint = if (isRecording) MaterialTheme.colorScheme.error else Color.Gray
            )
        }
        OutlinedTextField(
            value = userMessage,
            onValueChange = { userMessage = it },
            label = { Text("Ask Chef…") },
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
            modifier = Modifier.weight(1f)
        )
        IconButton(
            onClick = {
                if (userMessage.isNotBlank()) {
                    onSendMessage(userMessage)
                    userMessage = ""
                    keyboardController?.hide()
                }
            }
        ) {
            Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send")
        }
    }
}
