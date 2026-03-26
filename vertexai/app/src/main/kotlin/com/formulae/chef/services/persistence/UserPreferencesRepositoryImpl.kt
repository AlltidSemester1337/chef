package com.formulae.chef.services.persistence

import android.util.Log
import com.formulae.chef.feature.model.UserPreferences
import com.google.firebase.database.FirebaseDatabase
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.tasks.await

class UserPreferencesRepositoryImpl(
    override val uid: String,
    private val database: FirebaseDatabase = FirebaseInstance.database
) : UserPreferencesRepository {
    private val _prefsKey = "users/$uid/preferences"

    override suspend fun loadPreferences(): UserPreferences? {
        return suspendCancellableCoroutine { continuation ->
            database.getReference(_prefsKey).get()
                .addOnSuccessListener { dataSnapshot ->
                    continuation.resume(dataSnapshot.getValue(UserPreferences::class.java))
                }
                .addOnFailureListener { exception ->
                    Log.e("UserPreferencesRepo", "Error loading preferences", exception)
                    continuation.resumeWithException(exception)
                }
        }
    }

    override suspend fun savePreferences(preferences: UserPreferences) {
        database.getReference(_prefsKey).setValue(preferences).await()
        Log.d("UserPreferencesRepo", "Preferences saved successfully")
    }
}
