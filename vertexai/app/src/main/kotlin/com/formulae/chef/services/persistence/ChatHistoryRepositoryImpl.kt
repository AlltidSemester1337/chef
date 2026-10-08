package com.formulae.chef.services.persistence

import android.util.Log
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import java.time.Instant
import kotlinx.coroutines.tasks.await

private const val CREATED_AT = "createdAt"

class ChatHistoryRepositoryImpl(
    override val uid: String,
    private val firestore: FirebaseFirestore = FirebaseInstance.firestore
) : ChatHistoryRepository {
    private val chatHistory get() = firestore.userCollection(uid, FirestorePaths.CHAT_HISTORY)

    // Firestore IDs are random, so order comes from createdAt. Entries saved together get
    // microsecond-spaced times to keep their relative order.
    override fun saveNewEntries(newEntries: List<Content>) {
        val now = Instant.now()
        newEntries.forEachIndexed { index, entry ->
            val createdAt = now.plusNanos(index * 1_000L)
            val data = mapOf(
                "role" to entry.role,
                "parts" to entry.parts.map { mapOf("text" to it.text) },
                CREATED_AT to Timestamp(createdAt.epochSecond, createdAt.nano)
            )
            chatHistory.document().set(data).addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Log.d("Firestore", "entry saved successfully!")
                } else {
                    Log.e("Firestore", "Failed to add new entry: ", task.exception)
                }
            }
        }
    }

    override suspend fun loadChatHistoryLastTwentyEntries(): List<Content> {
        return try {
            chatHistory.orderBy(CREATED_AT, Query.Direction.ASCENDING)
                .limitToLast(20)
                .get()
                .await()
                .documents
                .mapNotNull { it.toObject(Content::class.java) }
        } catch (e: Exception) {
            Log.d("ChatHistoryRepository", "Error getting data", e)
            throw e
        }
    }

    override suspend fun loadAllEntries(): List<Pair<String, Content>> {
        return try {
            chatHistory.orderBy(CREATED_AT, Query.Direction.ASCENDING)
                .get()
                .await()
                .documents
                .mapNotNull { doc -> doc.toObject(Content::class.java)?.let { doc.id to it } }
        } catch (e: Exception) {
            Log.d("ChatHistoryRepository", "Error getting all data", e)
            throw e
        }
    }

    override suspend fun deleteEntries(pushIds: List<String>) {
        pushIds.chunked(MAX_BATCH_WRITES).forEach { ids ->
            val batch = firestore.batch()
            ids.forEach { batch.delete(chatHistory.document(it)) }
            batch.commit().await()
        }
    }

    private companion object {
        const val MAX_BATCH_WRITES = 500
    }
}
