package com.formulae.chef.services.persistence

import android.util.Log
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.tasks.await

class CookingResourcesRepositoryImpl(
    private val uid: String,
    private val database: FirebaseDatabase = FirebaseInstance.database
) : CookingResourcesRepository {
    private val ref get() = database.getReference("users/$uid/cooking_resources")

    override suspend fun load(): CachedCookingResources? {
        return ref.get().await().getValue(CachedCookingResources::class.java)
    }

    override suspend fun save(cached: CachedCookingResources) {
        ref.setValue(cached).await()
        Log.d("CookingResourcesRepo", "Cooking resources saved successfully")
    }
}
