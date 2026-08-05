/*
 * Copyright 2023 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.formulae.chef.feature.chat.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.rememberAsyncImagePainter
import com.formulae.chef.GenerativeViewModelFactory
import com.formulae.chef.R
import com.formulae.chef.feature.chat.ChatViewModel
import com.formulae.chef.feature.collection.ui.DetailRoute
import com.formulae.chef.feature.model.Recipe
import com.formulae.chef.services.voice.sanitizeMarkdown
import com.formulae.chef.ui.components.ChefTopBar
import com.formulae.chef.ui.theme.AppTypography
import com.formulae.chef.ui.theme.BackgroundColor
import com.formulae.chef.ui.theme.Terracotta100
import com.formulae.chef.ui.theme.Terracotta200
import com.formulae.chef.ui.theme.Terracotta600
import com.formulae.chef.ui.theme.TextPrimary
import com.formulae.chef.ui.theme.TextSecondary
import kotlinx.coroutines.launch

@Composable
internal fun ChatRoute(
    chatViewModel: ChatViewModel = viewModel(factory = GenerativeViewModelFactory)
) {
    val selectedRecipe by chatViewModel.selectedRecipeFromChat.collectAsState()

    DisposableEffect(Unit) {
        onDispose { chatViewModel.onNavigateAway() }
    }

    var showIngredients by rememberSaveable(selectedRecipe?.id) { mutableStateOf(true) }

    if (selectedRecipe != null) {
        DetailRoute(
            recipe = selectedRecipe!!,
            onBack = { chatViewModel.clearSelectedRecipe() },
            showIngredients = showIngredients,
            onTabChanged = { showIngredients = it }
        )
    } else {
        ChatContent(chatViewModel)
    }
}

@Composable
private fun ChatContent(chatViewModel: ChatViewModel) {
    val chatUiState by chatViewModel.uiState.collectAsState()
    val isLoading by chatViewModel.isLoading.collectAsState()
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    val messageCount = chatUiState.messages.size

    val lastNonPendingModelMessage = chatUiState.messages.lastOrNull {
        it.participant == Participant.MODEL && !it.isPending && it.text.isNotBlank()
    }
    val voice = rememberVoiceController(
        onSendMessage = { text ->
            chatViewModel.sendMessage(text)
            coroutineScope.launch { listState.scrollToItem(0) }
        },
        lastNonPendingModelMessage = lastNonPendingModelMessage
    )

    LaunchedEffect(messageCount) {
        if (messageCount > 0) {
            listState.animateScrollToItem(0)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .imePadding()
    ) {
        ChefTopBar(
            title = "Chat with Chef",
            navigationIcon = {
                Image(
                    painter = painterResource(R.drawable.logo),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                )
            }
        )

        if (isLoading) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            ChatList(
                chatMessages = chatUiState.messages,
                listState = listState,
                onStarClicked = chatViewModel::onRecipeStarred,
                onLikeClicked = chatViewModel::onMessageLiked,
                onRecipeClick = chatViewModel::onRecipeSelectedFromChat,
                onRecipeStarredFromGrid = { messageId, recipe ->
                    chatViewModel.onRecipeStarredFromGrid(messageId, recipe)
                },
                speakingMessageId = voice.speakingMessageId,
                onSpeakClicked = voice.onSpeakClicked,
                modifier = Modifier.weight(1f)
            )
        }

        MessageInput(
            onSendMessage = { inputText ->
                chatViewModel.sendMessage(inputText)
            },
            resetScroll = {
                coroutineScope.launch { listState.scrollToItem(0) }
            },
            isRecording = voice.isRecording,
            onStartRecording = voice.onStartRecording
        )
    }
}

@Composable
fun ChatList(
    chatMessages: List<ChatMessage>,
    listState: LazyListState,
    onStarClicked: (ChatMessage) -> Unit,
    onLikeClicked: (ChatMessage) -> Unit,
    onRecipeClick: (Recipe) -> Unit,
    onRecipeStarredFromGrid: (String, Recipe) -> Unit,
    speakingMessageId: String? = null,
    onSpeakClicked: ((ChatMessage) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        reverseLayout = true,
        state = listState,
        modifier = modifier.fillMaxWidth()
    ) {
        items(chatMessages.reversed(), key = { it.id }) { message ->
            ChatBubbleItem(
                chatMessage = message,
                onStarClicked = onStarClicked,
                onLikeClicked = onLikeClicked,
                onRecipeClick = onRecipeClick,
                onRecipeStarredFromGrid = onRecipeStarredFromGrid,
                onSpeakClicked = onSpeakClicked,
                isSpeakingThisMessage = speakingMessageId == message.id
            )
        }
    }
}

@Composable
fun ChatBubbleItem(
    chatMessage: ChatMessage,
    onStarClicked: (ChatMessage) -> Unit,
    onLikeClicked: (ChatMessage) -> Unit,
    onRecipeClick: (Recipe) -> Unit,
    onRecipeStarredFromGrid: (String, Recipe) -> Unit,
    onSpeakClicked: ((ChatMessage) -> Unit)? = null,
    isSpeakingThisMessage: Boolean = false
) {
    val isModelMessage = chatMessage.participant == Participant.MODEL ||
        chatMessage.participant == Participant.ERROR

    val horizontalAlignment = if (isModelMessage) Alignment.Start else Alignment.End

    Column(
        horizontalAlignment = horizontalAlignment,
        modifier = Modifier
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .fillMaxWidth()
    ) {
        when {
            chatMessage.recipes.isNotEmpty() -> {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (chatMessage.text.isNotBlank()) {
                        Text(
                            text = chatMessage.text.sanitizeMarkdown(),
                            style = AppTypography.bodyLarge.copy(color = TextPrimary),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    chatMessage.recipes.forEach { recipe ->
                        ChatRecipeCard(
                            recipe = recipe,
                            isStarred = recipe.id in chatMessage.starredRecipeIds,
                            isImageFailed = recipe.id != null &&
                                recipe.id in chatMessage.failedImageRecipeIds,
                            onClick = { onRecipeClick(recipe) },
                            onStarClick = { onRecipeStarredFromGrid(chatMessage.id, recipe) }
                        )
                    }
                }
            }

            chatMessage.participant == Participant.USER -> {
                Box(
                    modifier = Modifier
                        .background(Terracotta200, RoundedCornerShape(8.dp))
                        .padding(8.dp)
                ) {
                    Text(
                        text = chatMessage.text,
                        style = AppTypography.bodyLarge.copy(color = TextPrimary)
                    )
                }
            }

            chatMessage.participant == Participant.ERROR -> {
                Box(
                    modifier = Modifier
                        .background(
                            MaterialTheme.colorScheme.errorContainer,
                            RoundedCornerShape(8.dp)
                        )
                        .padding(8.dp)
                        .fillMaxWidth()
                ) {
                    Text(
                        text = chatMessage.text,
                        style = AppTypography.bodyLarge.copy(
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    )
                }
            }

            else -> {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (chatMessage.isPending) {
                        CircularProgressIndicator(
                            modifier = Modifier
                                .size(16.dp)
                                .padding(end = 8.dp),
                            strokeWidth = 2.dp
                        )
                    }
                    Text(
                        text = chatMessage.text.sanitizeMarkdown(),
                        style = AppTypography.bodyLarge.copy(color = TextPrimary),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                if (!chatMessage.isPending) {
                    Row {
                        IconButton(
                            onClick = { onLikeClicked(chatMessage) },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ThumbUp,
                                contentDescription = if (chatMessage.isLiked) {
                                    "Liked"
                                } else {
                                    "Like response"
                                },
                                tint = if (chatMessage.isLiked) Terracotta600 else TextSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        if (onSpeakClicked != null && chatMessage.text.isNotBlank()) {
                            IconButton(
                                onClick = { onSpeakClicked(chatMessage) },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                                    contentDescription = if (isSpeakingThisMessage) {
                                        "Stop reading"
                                    } else {
                                        "Read response aloud"
                                    },
                                    tint = if (isSpeakingThisMessage) {
                                        Terracotta600
                                    } else {
                                        TextSecondary
                                    },
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ChatRecipeCard(
    recipe: Recipe,
    isStarred: Boolean,
    isImageFailed: Boolean,
    onClick: () -> Unit,
    onStarClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(104.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(Terracotta100)
            .clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .width(140.dp)
                .fillMaxHeight()
                .background(Color.White)
        ) {
            when {
                recipe.imageUrl != null -> {
                    Image(
                        painter = rememberAsyncImagePainter(recipe.imageUrl),
                        contentDescription = recipe.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                isImageFailed -> {
                    Text(
                        text = "Image unavailable",
                        style = AppTypography.bodySmall.copy(color = TextSecondary),
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(8.dp)
                    )
                }
                else -> {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .size(24.dp)
                            .align(Alignment.Center),
                        strokeWidth = 2.dp,
                        color = Terracotta600
                    )
                }
            }
            Box(
                modifier = Modifier
                    .padding(8.dp)
                    .size(32.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(BackgroundColor)
                    .align(Alignment.TopStart)
                    .clickable(onClick = onStarClick),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isStarred) {
                        Icons.Default.Bookmark
                    } else {
                        Icons.Outlined.BookmarkBorder
                    },
                    contentDescription = if (isStarred) {
                        "Remove from collection"
                    } else {
                        "Save to collection"
                    },
                    tint = Terracotta600,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .padding(12.dp)
        ) {
            Text(
                text = recipe.title ?: "",
                style = AppTypography.labelMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontStyle = FontStyle.Italic,
                    color = TextPrimary
                ),
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.weight(1f))
            // TODO: ratings and save count (no model support yet)
        }
    }
}

@Composable
fun MessageInput(
    onSendMessage: (String) -> Unit,
    resetScroll: () -> Unit = {},
    isRecording: Boolean = false,
    onStartRecording: () -> Unit = {}
) {
    var userMessage by rememberSaveable { mutableStateOf("") }
    val keyboardController = LocalSoftwareKeyboardController.current

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color.White,
        shape = RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp),
        shadowElevation = 4.dp
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 8.dp, vertical = 10.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(BackgroundColor)
                    .pointerInput(Unit) {
                        detectTapGestures(onLongPress = { onStartRecording() })
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isRecording) Icons.Default.MicOff else Icons.Default.Mic,
                    contentDescription = if (isRecording) "Recording…" else "Hold to speak",
                    tint = if (isRecording) Terracotta600 else TextSecondary,
                    modifier = Modifier.size(20.dp)
                )
            }

            OutlinedTextField(
                value = userMessage,
                placeholder = {
                    Text(
                        text = stringResource(R.string.chat_label),
                        style = AppTypography.bodyLarge.copy(color = TextSecondary)
                    )
                },
                onValueChange = { userMessage = it },
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Sentences
                ),
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 8.dp)
            )

            IconButton(
                onClick = {
                    if (userMessage.isNotBlank()) {
                        onSendMessage(userMessage)
                        userMessage = ""
                        resetScroll()
                        keyboardController?.hide()
                    }
                }
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.Send,
                    contentDescription = stringResource(R.string.action_send),
                    tint = if (userMessage.isNotBlank()) Terracotta600 else TextSecondary
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewChatList() {
    ChatList(
        chatMessages = listOf(
            ChatMessage(text = "Can you give me a recipe for coq au vin?"),
            ChatMessage(
                text = "Beef Rendang (Indonesian Beef Curry)\\n\\n" +
                    "This recipe delivers a rich and flavorful Indonesian beef curry.",
                participant = Participant.MODEL
            )
        ),
        listState = rememberLazyListState(),
        onStarClicked = {},
        onLikeClicked = {},
        onRecipeClick = {},
        onRecipeStarredFromGrid = { _, _ -> }
    )
}

@Preview(showBackground = true)
@Composable
fun PreviewMessageInput() {
    MessageInput(onSendMessage = {})
}

@Preview(showBackground = true, name = "Recipe Cards – 2 suggestions")
@Composable
fun PreviewRecipeCards() {
    val recipes = listOf(
        Recipe(id = "1", title = "West African Peanut Stew", imageUrl = null),
        Recipe(id = "2", title = "Pasta Carbonara", imageUrl = null)
    )
    ChatBubbleItem(
        chatMessage = ChatMessage(
            text = "Here are some ideas:",
            participant = Participant.MODEL,
            recipes = recipes,
            starredRecipeIds = setOf("2")
        ),
        onStarClicked = {},
        onLikeClicked = {},
        onRecipeClick = {},
        onRecipeStarredFromGrid = { _, _ -> }
    )
}
