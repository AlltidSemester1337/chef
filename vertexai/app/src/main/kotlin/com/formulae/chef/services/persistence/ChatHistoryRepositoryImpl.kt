package com.formulae.chef.services.persistence

import android.util.Log


import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException


data class Content(
    var role: String = "user", // Default value, must be mutable (var)
    var parts: List<Part> = emptyList() // Default empty list, must be mutable
)

data class Part(
    var text: String = ""
)

class ChatHistoryRepositoryImpl(override val uid: String) : ChatHistoryRepository {
    private val _chatHistoryKey = "users/$uid/chat_history"
    private val _database = FirebaseInstance.database

    override fun saveNewEntries(newEntries: List<com.google.firebase.vertexai.type.Content>) {
        val reference = _database.getReference(_chatHistoryKey)
        for (entry in newEntries) {
            reference.push().setValue(entry).addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Log.d("FirebaseDB", "entry saved successfully!")
                } else {
                    Log.e("FirebaseDB", "Failed to add new entry: ", task.exception)
                }
            }
        }
    }

    override suspend fun loadChatHistoryLastTwentyEntries(): List<com.google.firebase.vertexai.type.Content> {
        return suspendCancellableCoroutine { continuation ->
            _database.getReference(_chatHistoryKey).get()
                .addOnSuccessListener { dataSnapshot ->
                    val contentList = dataSnapshot.children.mapNotNull { child ->
                        child.getValue(Content::class.java)
                    }.takeLast(20).map { content ->
                        com.google.firebase.vertexai.type.Content(
                            content.role,
                            content.parts.map { part -> com.google.firebase.vertexai.type.TextPart(part.text) })
                    }
                    continuation.resume(contentList)
                }.addOnFailureListener { exception ->
                    Log.d("ChatHistoryRealtimeDatabasePersistence", "Error getting data", exception)
                    continuation.resumeWithException(exception)
                }
        }
    }
}