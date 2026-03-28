package com.formulae.chef.services.persistence

import android.util.Log
import com.formulae.chef.feature.model.LikedMessage
import java.time.ZoneOffset
import java.time.ZonedDateTime
import kotlinx.coroutines.tasks.await

private const val LIKED_MESSAGES_KEY = "liked_messages"

class LikedMessagesRepositoryImpl(uid: String) : LikedMessagesRepository {
    private val likedMessagesRef = FirebaseInstance.database
        .getReference("users/$uid/$LIKED_MESSAGES_KEY")

    override suspend fun loadLikedMessages(): List<Pair<String, LikedMessage>> {
        return try {
            likedMessagesRef.get().await().children.mapNotNull { snapshot ->
                val msg = snapshot.getValue(LikedMessage::class.java) ?: return@mapNotNull null
                Pair(snapshot.key ?: return@mapNotNull null, msg)
            }
        } catch (e: Exception) {
            Log.e("LikedMessages", "Failed to load liked messages", e)
            emptyList()
        }
    }

    override fun saveLikedMessage(text: String) {
        val entry = LikedMessage(
            text = text,
            likedAt = ZonedDateTime.now(ZoneOffset.UTC).toString()
        )
        likedMessagesRef.push().setValue(entry)
            .addOnFailureListener { e ->
                Log.e("LikedMessages", "Failed to save liked message", e)
            }
    }

    override fun deleteMessages(ids: List<String>) {
        ids.forEach { id ->
            likedMessagesRef.child(id).removeValue()
                .addOnFailureListener { e ->
                    Log.e("LikedMessages", "Failed to delete liked message $id", e)
                }
        }
    }
}
