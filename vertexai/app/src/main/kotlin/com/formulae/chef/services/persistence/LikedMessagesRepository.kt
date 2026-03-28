package com.formulae.chef.services.persistence

import com.formulae.chef.feature.model.LikedMessage

interface LikedMessagesRepository {
    suspend fun loadLikedMessages(): List<Pair<String, LikedMessage>>
    fun saveLikedMessage(text: String)
    fun deleteMessages(ids: List<String>)
}
