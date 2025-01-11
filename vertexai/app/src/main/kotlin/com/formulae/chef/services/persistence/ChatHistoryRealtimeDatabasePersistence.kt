package com.formulae.chef.services.persistence

import android.util.Log
import com.formulae.chef.BuildConfig
import com.formulae.chef.util.json.ContentInstanceCreator
import com.formulae.chef.util.json.PartInstanceCreator
import com.google.firebase.FirebaseApp
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import com.google.firebase.vertexai.type.Content
import com.google.firebase.vertexai.type.Part
import com.google.gson.GsonBuilder
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

private val gson = GsonBuilder()
    .registerTypeAdapter(Content::class.java, ContentInstanceCreator())
    .registerTypeAdapter(Part::class.java, PartInstanceCreator())
    .create()

private const val CHAT_HISTORY_KEY = "chatHistory"

class ChatHistoryRealtimeDatabasePersistence : ChatHistoryPersistence {
    private val _database =
        Firebase.database(
            FirebaseApp.getInstance(),
            BuildConfig.firebaseDbUrl
        )

    override fun saveNewEntries(newEntries: List<Content>) {
        val reference = _database.getReference(CHAT_HISTORY_KEY)
        for (entry in newEntries) {
            val newEntriesChildren = gson.toJson(entry)
            reference.push().setValue(newEntriesChildren).addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Log.d("FirebaseDB", "entry saved successfully!")
                } else {
                    Log.e("FirebaseDB", "Failed to add new entry: ", task.exception)
                }
            }
        }
    }

    override suspend fun loadChatHistory(): List<Content> {
        return suspendCancellableCoroutine { continuation ->
            _database.getReference(CHAT_HISTORY_KEY).get().addOnSuccessListener { dataSnapshot ->
                val contentList = dataSnapshot.children.mapNotNull { child ->
                    child.getValue(String::class.java)?.let { gson.fromJson(it, Content::class.java) }
                }
                continuation.resume(contentList)
            }.addOnFailureListener { exception ->
                Log.d("ChatHistoryRealtimeDatabasePersistence", "Error getting data", exception)
                continuation.resumeWithException(exception)
            }
        }
    }
}