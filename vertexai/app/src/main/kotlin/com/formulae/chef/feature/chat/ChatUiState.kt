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
        val index = _messages.indexOfLast { it.isPending }
        if (index != -1) {
            _messages[index] = _messages[index].copy(isPending = false)
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

    fun updateRecipeImage(messageId: String, recipeId: String, imageUrl: String) {
        val index = _messages.indexOfFirst { it.id == messageId }
        if (index != -1) {
            val msg = _messages[index]
            val newRecipes = msg.recipes.map { r ->
                if (r.id == recipeId) r.copyOf(imageUrl = imageUrl) else r
            }
            _messages[index] = msg.copy(recipes = newRecipes)
        }
    }

    fun markRecipeImageFailed(messageId: String, recipeId: String) {
        val index = _messages.indexOfFirst { it.id == messageId }
        if (index != -1) {
            val msg = _messages[index]
            _messages[index] = msg.copy(failedImageRecipeIds = msg.failedImageRecipeIds + recipeId)
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

    fun updateMessageLiked(messageId: String, isLiked: Boolean) {
        val index = _messages.indexOfFirst { it.id == messageId }
        if (index != -1) {
            _messages[index] = _messages[index].copy(isLiked = isLiked)
        }
    }
}
