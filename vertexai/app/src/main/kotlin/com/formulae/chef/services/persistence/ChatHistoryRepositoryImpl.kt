package com.formulae.chef.services.persistence

import android.util.Log
import com.google.firebase.database.FirebaseDatabase
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.tasks.await

class ChatHistoryRepositoryImpl(
    override val uid: String,
    private val database: FirebaseDatabase = FirebaseInstance.database
) : ChatHistoryRepository {
    private val _chatHistoryKey = "users/$uid/chat_history"

    override fun saveNewEntries(newEntries: List<Content>) {
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

    override suspend fun loadChatHistoryLastTwentyEntries(): List<Content> {
        return suspendCancellableCoroutine { continuation ->
            database.getReference(_chatHistoryKey).get()
                .addOnSuccessListener { dataSnapshot ->
                    val contentList = dataSnapshot.children.mapNotNull { child ->
                        child.getValue(Content::class.java)
                    }.takeLast(20)
                    continuation.resume(contentList)
                }.addOnFailureListener { exception ->
                    Log.d("ChatHistoryRealtimeDatabasePersistence", "Error getting data", exception)
                    continuation.resumeWithException(exception)
                }
        }
    }

    override suspend fun loadAllEntries(): List<Pair<String, Content>> {
        return suspendCancellableCoroutine { continuation ->
            database.getReference(_chatHistoryKey).get()
                .addOnSuccessListener { dataSnapshot ->
                    val entries = dataSnapshot.children.mapNotNull { child ->
                        val content = child.getValue(Content::class.java) ?: return@mapNotNull null
                        val key = child.key ?: return@mapNotNull null
                        Pair(key, content)
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
}
