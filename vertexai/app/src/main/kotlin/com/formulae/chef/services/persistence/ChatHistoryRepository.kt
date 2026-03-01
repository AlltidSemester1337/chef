package com.formulae.chef.services.persistence

import com.google.firebase.vertexai.type.Content

interface ChatHistoryRepository {
    val uid: String

    fun saveNewEntries(newEntries: List<Content>)

    suspend fun loadChatHistoryLastTwentyEntries(): List<Content>
<<<<<<< HEAD

    suspend fun loadAllEntries(): List<Pair<String, Content>>

    suspend fun deleteEntries(pushIds: List<String>)
=======
>>>>>>> 30785eb0 (WIP)
}
