package com.formulae.chef.services.persistence

import android.util.Log
import com.google.firebase.database.FirebaseDatabase
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.tasks.await

class CookingResourcesRepositoryImpl(
    private val uid: String,
    private val database: FirebaseDatabase = FirebaseInstance.database
) : CookingResourcesRepository {
    private val ref get() = database.getReference("users/$uid/cooking_resources")

    override suspend fun load(): CachedCookingResources? {
        return suspendCancellableCoroutine { continuation ->
            ref.get()
                .addOnSuccessListener { snapshot ->
                    continuation.resume(snapshot.getValue(CachedCookingResources::class.java))
                }
                .addOnFailureListener { exception ->
                    Log.e("CookingResourcesRepo", "Error loading cooking resources", exception)
                    continuation.resumeWithException(exception)
                }
        }
    }

    override suspend fun save(cached: CachedCookingResources) {
        ref.setValue(cached).await()
        Log.d("CookingResourcesRepo", "Cooking resources saved successfully")
    }
}
