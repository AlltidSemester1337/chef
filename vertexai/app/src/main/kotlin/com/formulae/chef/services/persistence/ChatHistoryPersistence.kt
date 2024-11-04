package com.formulae.chef.services.persistence

import com.google.firebase.vertexai.type.Content

interface ChatHistoryPersistence {
    fun saveNewEntries(newEntries: List<Content>)
    suspend fun loadChatHistory(): List<Content>
}