package com.formulae.chef.services.persistence

import android.util.Log
import com.formulae.chef.feature.model.LikedMessage
import com.google.firebase.firestore.FirebaseFirestore
import java.time.ZoneOffset
import java.time.ZonedDateTime
import kotlinx.coroutines.tasks.await

class LikedMessagesRepositoryImpl(
    uid: String,
    firestore: FirebaseFirestore = FirebaseInstance.firestore
) : LikedMessagesRepository {
    private val likedMessages = firestore.userCollection(uid, FirestorePaths.LIKED_MESSAGES)

    override suspend fun loadLikedMessages(): List<Pair<String, LikedMessage>> {
        return try {
            likedMessages.get().await().documents.mapNotNull { doc ->
                doc.toObject(LikedMessage::class.java)?.let { doc.id to it }
            }.sortedBy { (_, message) -> message.likedAtTimestamp }
        } catch (e: Exception) {
            Log.e("LikedMessages", "Failed to load liked messages", e)
            emptyList()
        }
    }

    override fun saveLikedMessage(text: String): String? {
        val entry = LikedMessage(
            text = text,
            likedAt = ZonedDateTime.now(ZoneOffset.UTC).toString()
        )
        val ref = likedMessages.document()
        ref.set(entry)
            .addOnFailureListener { e ->
                Log.e("LikedMessages", "Failed to save liked message", e)
            }
        return ref.id
    }

    override fun deleteMessages(ids: List<String>) {
        ids.forEach { id ->
            likedMessages.document(id).delete()
                .addOnFailureListener { e ->
                    Log.e("LikedMessages", "Failed to delete liked message $id", e)
                }
        }
    }
}
