package com.formulae.chef.services.persistence

import com.formulae.chef.feature.model.LikedMessage

interface LikedMessagesRepository {
    suspend fun loadLikedMessages(): List<Pair<String, LikedMessage>>

    /** Persists [text] as liked and returns the new entry's key (null if none could be allocated). */
    fun saveLikedMessage(text: String): String?
    fun deleteMessages(ids: List<String>)
}
