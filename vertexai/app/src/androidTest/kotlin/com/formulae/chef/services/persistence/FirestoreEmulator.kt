package com.formulae.chef.services.persistence

import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.MemoryCacheSettings
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

/**
 * Shared Firestore + Auth emulator wiring for persistence integration tests.
 *
 * Prerequisites:
 *   cd firestore-tools && npx firebase emulators:start --only firestore,auth --project <project_id>
 * where <project_id> is the one in google-services.json (the emulators reject other project IDs;
 * nothing reaches production), and an Android *emulator* (10.0.2.2 = host loopback).
 *
 * The emulator enforces `firestore.rules`, so every test signs in as fresh anonymous users and
 * writes only data those users are allowed to write. Fresh UIDs also isolate tests from each other,
 * so no cleanup is needed (emulator data is discarded on shutdown).
 */
object FirestoreEmulator {
    private const val HOST = "10.0.2.2"

    val firestore: FirebaseFirestore by lazy {
        FirebaseFirestore.getInstance().apply {
            useEmulator(HOST, 8080)
            firestoreSettings = FirebaseFirestoreSettings.Builder()
                .setLocalCacheSettings(MemoryCacheSettings.newBuilder().build())
                .build()
        }
    }

    private val auth: FirebaseAuth by lazy {
        FirebaseAuth.getInstance().apply { useEmulator(HOST, 9099) }
    }

    /** Signs out and in again anonymously; returns the new UID (now `request.auth.uid` in rules). */
    suspend fun signInAsNewUser(): String {
        firestore // make sure Firestore is wired to the emulator before the first auth change
        auth.signOut()
        return auth.signInAnonymously().await().user!!.uid
    }

    /**
     * Writes a document through the emulator's REST API with the `owner` token, which bypasses the
     * rules — for seeding data the rules would reject (e.g. documents written before validation).
     * [fieldsJson] is a Firestore REST `fields` object.
     */
    suspend fun seedBypassingRules(collection: String, documentId: String, fieldsJson: String) =
        withContext(Dispatchers.IO) {
            val projectId = FirebaseApp.getInstance().options.projectId
            val url = URL(
                "http://$HOST:8080/v1/projects/$projectId/databases/(default)/documents/$collection" +
                    "?documentId=$documentId"
            )
            val connection = url.openConnection() as HttpURLConnection
            try {
                connection.requestMethod = "POST"
                connection.doOutput = true
                connection.setRequestProperty("Authorization", "Bearer owner")
                connection.setRequestProperty("Content-Type", "application/json")
                connection.outputStream.use { it.write("{\"fields\":$fieldsJson}".toByteArray()) }
                val status = connection.responseCode
                check(status == 200) { "Seeding $collection/$documentId failed: HTTP $status" }
            } finally {
                connection.disconnect()
            }
        }

    /** Polls [block] until [condition] holds, to wait for fire-and-forget repository writes. */
    suspend fun <T> awaitCondition(block: suspend () -> T, condition: (T) -> Boolean): T {
        repeat(20) {
            val value = block()
            if (condition(value)) return value
            delay(200)
        }
        return block()
    }
}
