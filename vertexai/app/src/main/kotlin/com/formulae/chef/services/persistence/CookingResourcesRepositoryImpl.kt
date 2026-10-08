package com.formulae.chef.services.persistence

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await

class CookingResourcesRepositoryImpl(
    private val uid: String,
    private val firestore: FirebaseFirestore = FirebaseInstance.firestore
) : CookingResourcesRepository {
    private val userRef get() = firestore.userDoc(uid)

    override suspend fun load(): CachedCookingResources? {
        return userRef.get().await().get(FirestorePaths.COOKING_RESOURCES, CachedCookingResources::class.java)
    }

    // Merge: a plain set() would wipe betaInteractionCount, which the rules reject.
    override suspend fun save(cached: CachedCookingResources) {
        userRef.set(mapOf(FirestorePaths.COOKING_RESOURCES to cached), SetOptions.merge()).await()
        Log.d("CookingResourcesRepo", "Cooking resources saved successfully")
    }
}
