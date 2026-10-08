package com.formulae.chef.services.persistence

import com.formulae.chef.feature.model.CookingResource
import com.google.firebase.Timestamp
import com.google.firebase.firestore.Exclude
import com.google.firebase.firestore.PropertyName

data class CachedCookingResources(
    var resources: List<CookingResource> = emptyList(),
    @get:Exclude
    @set:Exclude
    var updatedAt: String = ""
) {
    /** Firestore mapping of [updatedAt], stored as a Timestamp. */
    @get:PropertyName("updatedAt")
    @set:PropertyName("updatedAt")
    var updatedAtTimestamp: Timestamp?
        get() = FirestoreTime.toTimestamp(updatedAt)
        set(value) {
            updatedAt = FirestoreTime.toIso(value)
        }
}

interface CookingResourcesRepository {
    suspend fun load(): CachedCookingResources?
    suspend fun save(cached: CachedCookingResources)
}
