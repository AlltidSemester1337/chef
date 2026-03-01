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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
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
import kotlinx.coroutines.launch

@Composable
internal fun ChatRoute(
    chatViewModel: ChatViewModel = viewModel(factory = GenerativeViewModelFactory)
) {
    val selectedRecipe by chatViewModel.selectedRecipeFromChat.collectAsState()

    if (selectedRecipe != null) {
        DetailRoute(
            recipe = selectedRecipe!!,
            onBack = { chatViewModel.clearSelectedRecipe() }
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

    Scaffold(
        bottomBar = {
            MessageInput(
                onSendMessage = { inputText ->
                    chatViewModel.sendMessage(inputText)
                },
                resetScroll = {
                    coroutineScope.launch { listState.scrollToItem(0) }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
            } else {
                ChatList(
                    chatMessages = chatUiState.messages,
                    listState = listState,
                    onStarClicked = chatViewModel::onRecipeStarred,
                    onRecipeClick = chatViewModel::onRecipeSelectedFromChat,
                    onRecipeStarredFromGrid = { messageId, recipe ->
                        chatViewModel.onRecipeStarredFromGrid(messageId, recipe)
                    }
                )
            }
        }
    }
}

@Composable
fun ChatList(
    chatMessages: List<ChatMessage>,
    listState: LazyListState,
    onStarClicked: (ChatMessage) -> Unit,
    onRecipeClick: (Recipe) -> Unit,
    onRecipeStarredFromGrid: (String, Recipe) -> Unit
) {
    LazyColumn(
        reverseLayout = true,
        state = listState,
        modifier = Modifier.fillMaxSize()
    ) {
        items(chatMessages.reversed(), key = { it.id }) { message ->
            ChatBubbleItem(
                chatMessage = message,
                onStarClicked = onStarClicked,
                onRecipeClick = onRecipeClick,
                onRecipeStarredFromGrid = onRecipeStarredFromGrid
            )
        }
    }
}

@Composable
fun ChatBubbleItem(
    chatMessage: ChatMessage,
    onStarClicked: (ChatMessage) -> Unit,
    onRecipeClick: (Recipe) -> Unit,
    onRecipeStarredFromGrid: (String, Recipe) -> Unit
) {
    val isModelMessage = chatMessage.participant == Participant.MODEL ||
            chatMessage.participant == Participant.ERROR

    val horizontalAlignment = if (isModelMessage) Alignment.Start else Alignment.End

    Column(
        horizontalAlignment = horizontalAlignment,
        modifier = Modifier
            .padding(horizontal = 8.dp, vertical = 4.dp)
            .fillMaxWidth()
    ) {
        Text(
            text = chatMessage.participant.name,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(bottom = 4.dp)
        )

        if (chatMessage.recipes.isNotEmpty()) {
            RecipeGrid(
                recipes = chatMessage.recipes,
                starredRecipeIds = chatMessage.starredRecipeIds,
                onRecipeClick = onRecipeClick,
                onStarClick = { recipe -> onRecipeStarredFromGrid(chatMessage.id, recipe) }
            )
        } else {
            val backgroundColor = when (chatMessage.participant) {
                Participant.MODEL -> MaterialTheme.colorScheme.primaryContainer
                Participant.USER -> MaterialTheme.colorScheme.tertiaryContainer
                Participant.ERROR -> MaterialTheme.colorScheme.errorContainer
            }

            val bubbleShape = if (isModelMessage) {
                RoundedCornerShape(4.dp, 20.dp, 20.dp, 20.dp)
            } else {
                RoundedCornerShape(20.dp, 4.dp, 20.dp, 20.dp)
            }

            Row {
                if (chatMessage.isPending) {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .align(Alignment.CenterVertically)
                            .padding(all = 8.dp)
                    )
                }
                Column {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = backgroundColor),
                        shape = bubbleShape,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                modifier = Modifier.fillMaxWidth(),
                                text = chatMessage.text,
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                    if (chatMessage.participant == Participant.MODEL) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = backgroundColor),
                            shape = bubbleShape,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            IconButton(
                                onClick = { onStarClicked(chatMessage) },
                                modifier = Modifier.align(Alignment.End)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Star,
                                    contentDescription = if (chatMessage.isStarred) "Remove recipe from collection" else "Save recipe to collection",
                                    tint = if (chatMessage.isStarred) Color.Yellow else Color.Gray
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
private fun RecipeGrid(
    recipes: List<Recipe>,
    starredRecipeIds: Set<String>,
    onRecipeClick: (Recipe) -> Unit,
    onStarClick: (Recipe) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        recipes.chunked(3).forEach { rowRecipes ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                rowRecipes.forEach { recipe ->
                    RecipeSuggestionCard(
                        recipe = recipe,
                        isStarred = recipe.id in starredRecipeIds,
                        onRecipeClick = { onRecipeClick(recipe) },
                        onStarClick = { onStarClick(recipe) },
                        modifier = Modifier.weight(1f)
                    )
                }
                repeat(3 - rowRecipes.size) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun RecipeSuggestionCard(
    recipe: Recipe,
    isStarred: Boolean,
    onRecipeClick: () -> Unit,
    onStarClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.clickable { onRecipeClick() }
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp),
                contentAlignment = Alignment.Center
            ) {
                if (recipe.imageUrl != null) {
                    Image(
                        painter = rememberAsyncImagePainter(recipe.imageUrl),
                        contentDescription = recipe.title,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    CircularProgressIndicator()
                }
            }
            Text(
                text = recipe.title,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)
            )
            IconButton(
                onClick = onStarClick,
                modifier = Modifier.align(Alignment.End)
            ) {
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = if (isStarred) "Remove from collection" else "Save to collection",
                    tint = if (isStarred) Color.Yellow else Color.Gray
                )
            }
        }
    }
}

@Composable
fun MessageInput(
    onSendMessage: (String) -> Unit,
    resetScroll: () -> Unit = {}
) {
    var userMessage by rememberSaveable { mutableStateOf("") }
    ElevatedCard(
        modifier = Modifier
            .padding(bottom = 40.dp)
            .fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth()
        ) {
            OutlinedTextField(
                value = userMessage,
                label = { Text(stringResource(R.string.chat_label)) },
                onValueChange = { userMessage = it },
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Sentences
                ),
                modifier = Modifier
                    .align(Alignment.CenterVertically)
                    .fillMaxWidth()
                    .weight(0.85f)
            )
            IconButton(
                onClick = {
                    if (userMessage.isNotBlank()) {
                        onSendMessage(userMessage)
                        userMessage = ""
                        resetScroll()
                    }
                },
                modifier = Modifier
                    .padding(start = 16.dp)
                    .align(Alignment.CenterVertically)
                    .fillMaxWidth()
                    .weight(0.15f)
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.Send,
                    contentDescription = stringResource(R.string.action_send),
                    modifier = Modifier
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
                text = "Beef Rendang (Indonesian Beef Curry)\\n\\nThis recipe delivers a rich and flavorful Indonesian beef curry.",
                participant = Participant.MODEL
            ),
        ),
        listState = rememberLazyListState(),
        onStarClicked = {},
        onRecipeClick = {},
        onRecipeStarredFromGrid = { _, _ -> }
    )
}

@Preview(showBackground = true)
@Composable
fun PreviewMessageInput() {
    MessageInput(onSendMessage = {})
}

@Preview(showBackground = true, name = "Recipe Grid – 5 suggestions")
@Composable
fun PreviewRecipeGrid() {
    val recipes = listOf(
        Recipe(id = "1", title = "West African Peanut Stew", imageUrl = null),
        Recipe(id = "2", title = "Pasta Carbonara", imageUrl = null),
        Recipe(id = "3", title = "Chicken Tikka Masala", imageUrl = null),
        Recipe(id = "4", title = "Beef Rendang", imageUrl = null),
        Recipe(id = "5", title = "Coq au Vin", imageUrl = null),
    )
    ChatBubbleItem(
        chatMessage = ChatMessage(
            participant = Participant.MODEL,
            recipes = recipes,
            starredRecipeIds = setOf("2")
        ),
        onStarClicked = {},
        onRecipeClick = {},
        onRecipeStarredFromGrid = { _, _ -> }
    )
}
