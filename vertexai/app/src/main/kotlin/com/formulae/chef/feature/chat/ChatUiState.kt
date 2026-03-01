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

import androidx.compose.runtime.toMutableStateList
import com.formulae.chef.feature.chat.ui.ChatMessage
import com.formulae.chef.feature.model.Recipe

class ChatUiState(
    messages: List<ChatMessage> = emptyList()
) {
    private val _messages: MutableList<ChatMessage> = messages.toMutableStateList()
    val messages: List<ChatMessage> = _messages

    fun addMessage(msg: ChatMessage) {
        _messages.add(msg)
    }

    fun replaceLastPendingMessage() {
        val lastMessage = _messages.lastOrNull()
        lastMessage?.let {
            val newMessage = lastMessage.apply { isPending = false }
            _messages.removeAt(_messages.lastIndex)
            _messages.add(newMessage)
        }
    }

    fun updateStarredMessage(msg: ChatMessage, isStarred: Boolean) {
        val index = _messages.indexOfFirst { it.id == msg.id }
        if (index != -1) {
            _messages[index] = msg.copy(isStarred = isStarred)
        }
    }

    fun updateMessageRecipes(messageId: String, recipes: List<Recipe>) {
        val index = _messages.indexOfFirst { it.id == messageId }
        if (index != -1) {
            _messages[index] = _messages[index].copy(recipes = recipes)
        }
    }

    fun updateRecipeStarred(messageId: String, recipeId: String, isStarred: Boolean) {
        val index = _messages.indexOfFirst { it.id == messageId }
        if (index != -1) {
            val msg = _messages[index]
            val newStarredIds = if (isStarred) msg.starredRecipeIds + recipeId else msg.starredRecipeIds - recipeId
            _messages[index] = msg.copy(starredRecipeIds = newStarredIds)
        }
    }
}
