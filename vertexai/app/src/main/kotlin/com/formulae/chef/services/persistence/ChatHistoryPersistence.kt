package com.formulae.chef.services.persistence

import com.google.firebase.vertexai.type.Content

interface ChatHistoryPersistence {
    fun saveChatHistory(history: List<Content>)
    suspend fun loadChatHistory(): List<Content>
}