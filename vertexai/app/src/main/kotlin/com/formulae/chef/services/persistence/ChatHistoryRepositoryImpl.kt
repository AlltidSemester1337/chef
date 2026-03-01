package com.formulae.chef.services.persistence

import android.util.Log
<<<<<<< HEAD
import com.google.firebase.database.FirebaseDatabase
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.tasks.await
=======
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.suspendCancellableCoroutine
>>>>>>> 30785eb0 (WIP)

data class Content(
    var role: String = "user", // Default value, must be mutable (var)
    var parts: List<Part> = emptyList() // Default empty list, must be mutable
)

data class Part(
    var text: String = ""
)

class ChatHistoryRepositoryImpl(
    override val uid: String,
    private val database: FirebaseDatabase = FirebaseInstance.database
) : ChatHistoryRepository {
    private val _chatHistoryKey = "users/$uid/chat_history"

    override fun saveNewEntries(newEntries: List<com.google.firebase.vertexai.type.Content>) {
        val reference = database.getReference(_chatHistoryKey)
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
            database.getReference(_chatHistoryKey).get()
                .addOnSuccessListener { dataSnapshot ->
                    val contentList = dataSnapshot.children.mapNotNull { child ->
                        child.getValue(Content::class.java)
                    }.takeLast(20).map { content ->
                        com.google.firebase.vertexai.type.Content(
                            content.role,
                            content.parts.map { part -> com.google.firebase.vertexai.type.TextPart(part.text) }
                        )
                    }
                    continuation.resume(contentList)
                }.addOnFailureListener { exception ->
                    Log.d("ChatHistoryRealtimeDatabasePersistence", "Error getting data", exception)
                    continuation.resumeWithException(exception)
                }
        }
    }
<<<<<<< HEAD

    override suspend fun loadAllEntries(): List<Pair<String, com.google.firebase.vertexai.type.Content>> {
        return suspendCancellableCoroutine { continuation ->
            database.getReference(_chatHistoryKey).get()
                .addOnSuccessListener { dataSnapshot ->
                    val entries = dataSnapshot.children.mapNotNull { child ->
                        val content = child.getValue(Content::class.java) ?: return@mapNotNull null
                        val key = child.key ?: return@mapNotNull null
                        Pair(
                            key,
                            com.google.firebase.vertexai.type.Content(
                                content.role,
                                content.parts.map { part -> com.google.firebase.vertexai.type.TextPart(part.text) }
                            )
                        )
                    }
                    continuation.resume(entries)
                }
                .addOnFailureListener { exception ->
                    Log.d("ChatHistoryRealtimeDatabasePersistence", "Error getting all data", exception)
                    continuation.resumeWithException(exception)
                }
        }
    }

    override suspend fun deleteEntries(pushIds: List<String>) {
        val reference = database.getReference(_chatHistoryKey)
        for (pushId in pushIds) {
            reference.child(pushId).removeValue().await()
        }
    }
=======
>>>>>>> 30785eb0 (WIP)
}
