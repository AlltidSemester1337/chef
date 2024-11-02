package com.formulae.chef.services.persistence

import android.content.Context
import com.google.firebase.vertexai.type.Content

interface ChatHistoryPersistence {
    fun saveChatHistory(history: List<Content>)
    fun loadChatHistory(): List<Content>
}