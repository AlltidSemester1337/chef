package com.formulae.chef.services.persistence

data class Content(
    var role: String = "user", // Default value, must be mutable (var)
    var parts: List<Part> = emptyList() // Default empty list, must be mutable
)

data class Part(
    var text: String = ""
)

interface ChatHistoryRepository {
    val uid: String

    fun saveNewEntries(newEntries: List<Content>)

    suspend fun loadChatHistoryLastTwentyEntries(): List<Content>

    suspend fun loadAllEntries(): List<Pair<String, Content>>

    suspend fun deleteEntries(pushIds: List<String>)
}
