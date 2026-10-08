package com.formulae.chef.services.persistence

import android.util.Log
import com.formulae.chef.feature.model.UserPreferences
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await

class UserPreferencesRepositoryImpl(
    override val uid: String,
    private val firestore: FirebaseFirestore = FirebaseInstance.firestore
) : UserPreferencesRepository {
    private val userRef get() = firestore.userDoc(uid)

    override suspend fun loadPreferences(): UserPreferences? {
        return try {
            userRef.get().await().get(FirestorePaths.PREFERENCES, UserPreferences::class.java)
        } catch (e: Exception) {
            Log.e("UserPreferencesRepo", "Error loading preferences", e)
            throw e
        }
    }

    // Merge: a plain set() would wipe betaInteractionCount, which the rules reject.
    override suspend fun savePreferences(preferences: UserPreferences) {
        userRef.set(mapOf(FirestorePaths.PREFERENCES to preferences), SetOptions.merge()).await()
        Log.d("UserPreferencesRepo", "Preferences saved successfully")
    }
}
