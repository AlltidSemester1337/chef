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

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.vertexai.GenerativeModel
import com.google.firebase.vertexai.type.asTextOrNull
import com.google.firebase.vertexai.type.content
import com.google.firebase.vertexai.type.Content
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import android.util.Log
import com.formulae.chef.services.persistence.ChatHistoryRealtimeDatabasePersistence

class ChatViewModel(
    generativeModel: GenerativeModel
) : ViewModel() {
    private val _persistenceImpl = ChatHistoryRealtimeDatabasePersistence()
    private val _chatHistory: MutableStateFlow<List<Content>> = MutableStateFlow(emptyList())
    private val chat = generativeModel.startChat(
        history = _chatHistory.value
    )
    private val _uiState: MutableStateFlow<ChatUiState> =
        MutableStateFlow(
            ChatUiState()
        )

    val uiState: StateFlow<ChatUiState> =
        _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            _chatHistory.value = initializeChatHistory()
        }
        viewModelScope.launch {
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
                    //_persistenceImpl.saveChatHistory(_chatHistory.value)
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
}
