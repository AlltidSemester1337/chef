package com.formulae.chef.services.persistence

import com.formulae.chef.util.json.ContentInstanceCreator
import com.formulae.chef.util.json.PartInstanceCreator
import com.google.common.reflect.TypeToken
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import com.google.firebase.vertexai.type.Content
import com.google.firebase.vertexai.type.Part
import com.google.gson.GsonBuilder
import android.util.Log
import com.google.firebase.FirebaseApp
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

private val gson = GsonBuilder()
    .registerTypeAdapter(Content::class.java, ContentInstanceCreator())
    .registerTypeAdapter(Part::class.java, PartInstanceCreator())
    .create()

private const val CHAT_HISTORY_KEY = "chatHistory"

// TODO: Test and perform migration if successful, then release 1.1
class ChatHistoryRealtimeDatabasePersistence : ChatHistoryPersistence {
    private val _database =
        // TODO: Protect url? Move to config file?
        Firebase.database(
            FirebaseApp.getInstance(),
            "https://idyllic-bloom-425307-r6-default-rtdb.europe-west1.firebasedatabase.app/"
        )

    // TODO: Change to saving/insert/write just a single message instead of entire history? see https://firebase.google.com/docs/database/android/read-and-write
    override fun saveChatHistory(history: List<Content>) {
        val historyJson = gson.toJson(history)
        _database.getReference(CHAT_HISTORY_KEY).setValue(historyJson).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                Log.d("FirebaseDB", "historyJson saved successfully!")
            } else {
                Log.e("FirebaseDB", "Failed to save historyJson: ", task.exception)
            }
        }
    }

    override suspend fun loadChatHistory(): List<Content> {
        return suspendCancellableCoroutine { continuation ->
            _database.getReference(CHAT_HISTORY_KEY).get().addOnSuccessListener { dataSnapshot ->
                val historyJson = dataSnapshot.getValue(String::class.java)
                if (historyJson != null) {
                    val type = object : TypeToken<List<Content>>() {}.type
                    val contentList: List<Content> = gson.fromJson(historyJson, type)
                    continuation.resume(contentList)
                } else {
                    continuation.resume(emptyList())
                }
            }.addOnFailureListener { exception ->
                Log.d("ChatHistoryRealtimeDatabasePersistence", "Error getting data", exception)
                continuation.resumeWithException(exception)
            }
        }
    }
}