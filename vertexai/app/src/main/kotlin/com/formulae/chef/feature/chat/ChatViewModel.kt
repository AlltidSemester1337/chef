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

package com.formulae.chef.feature.chat

import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.formulae.chef.feature.collection.Recipe
import com.formulae.chef.services.persistence.ChatHistoryRepositoryImpl
import com.google.firebase.vertexai.Chat
import com.google.firebase.vertexai.GenerativeModel
import com.google.firebase.vertexai.type.Content
import com.google.firebase.vertexai.type.asTextOrNull
import com.google.firebase.vertexai.type.content
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ChatViewModel(
    generativeModel: GenerativeModel
) : ViewModel() {
    private val _persistenceImpl = ChatHistoryRepositoryImpl()
    private val _chatHistory: MutableStateFlow<List<Content>> = MutableStateFlow(emptyList())
    private val _uiState: MutableStateFlow<ChatUiState> =
        MutableStateFlow(
            ChatUiState()
        )

    val uiState: StateFlow<ChatUiState> =
        _uiState.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private lateinit var chat: Chat

    init {
        viewModelScope.launch {
            _isLoading.value = true
            _chatHistory.value = initializeChatHistory()
            chat = generativeModel.startChat(
                history = _chatHistory.value
            )
            _isLoading.value = false
            _chatHistory.collect { history -> updateUiStateMessages(history) }
        }
    }

    private suspend fun initializeChatHistory(): List<Content> {
        var result: List<Content> = emptyList()

        try {
            result = _persistenceImpl.loadChatHistory()
        } catch (e: Exception) {
            Log.e("FirebaseDB", "Error fetching chat history", e)
        }

        return result
    }

    private fun updateUiStateMessages(history: List<Content>) {
        _uiState.value = ChatUiState(
            history.map { content ->
                ChatMessage(
                    text = content.parts.first().asTextOrNull() ?: "",
                    participant = if (content.role == "user") Participant.USER else Participant.MODEL,
                    isPending = false
                )
            }
        )
    }

    fun sendMessage(userMessage: String) {
        val newUserContent = content(role = "user") { text(userMessage) }
        _uiState.value.addMessage(
            ChatMessage(
                text = userMessage,
                participant = Participant.USER,
                isPending = true
            )
        )

        viewModelScope.launch {
            try {
                val response = chat.sendMessage(userMessage)

                _uiState.value.replaceLastPendingMessage()

                response.text?.let { modelResponse ->
                    val newModelContent = content(role = "model") { text(modelResponse) }
                    _chatHistory.value += newUserContent
                    _chatHistory.value += newModelContent
                    updateUiStateMessages(_chatHistory.value)
                    _persistenceImpl.saveNewEntries(listOf(newUserContent, newModelContent))
                }
            } catch (e: Exception) {
                _uiState.value.replaceLastPendingMessage()
                _uiState.value.addMessage(
                    ChatMessage(
                        text = e.localizedMessage,
                        participant = Participant.ERROR
                    )
                )
            }
        }
    }

    fun onRecipeStarred(message: ChatMessage) {
        if (message.isStarred) {
            TODO() //deleteRecipe(message)
        } else {
            saveRecipe(message)
        }
    }

    fun saveRecipe(message: ChatMessage) {
        viewModelScope.launch {
            try {
                // TODO To perform migration, ask for a specific algorith to be used for transformation. After that update prompt to provide more structured model response and revise this logic.
                val recipeData = mapOf(
                    "title" to "Untitled Recipe",
                    "description" to "Saved from chat",
                    "content" to message.text
                )
                Log.d("CHEF", "CHEF:::::RECIPE_SAVE")

                // Save to Firebase
                //Firebase.database.getReference("recipes").push().setValue(recipeData)

                // Update UI
                val updatedMessage = message.copy(isStarred = true)
                // TODO Fix
                //_uiState.value = _uiState.value.copy(
                //    messages = _uiState.value.messages.map {
                //        if (it.id == message.id) updatedMessage else it
                //    }
                //)

                // Show toast
                //Toast.makeText(
                //    context,
                //    "Recipe saved successfully!",
                //    Toast.LENGTH_SHORT
                //).show()
            } catch (e: Exception) {
                Log.e("FirebaseSave", "Failed to save recipe", e)
                //Toast.makeText(
                //    context,
                //    "Failed to save recipe: ${e.localizedMessage}",
                //    Toast.LENGTH_SHORT
                //.show()
            }
        }
    }
}
