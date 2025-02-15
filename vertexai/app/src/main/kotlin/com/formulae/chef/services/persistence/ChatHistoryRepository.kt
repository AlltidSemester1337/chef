package com.formulae.chef.services.persistence

import com.google.firebase.vertexai.type.Content

interface ChatHistoryRepository {
    fun saveNewEntries(newEntries: List<Content>)
    suspend fun loadChatHistoryLastTwentyEntries(): List<Content>
}