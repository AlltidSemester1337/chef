package com.formulae.chef.feature.model

import com.formulae.chef.services.persistence.FirestoreTime
import com.google.firebase.Timestamp
import com.google.firebase.firestore.Exclude
import com.google.firebase.firestore.PropertyName

data class RecipeOfTheMonth(
    var id: String? = null,
    var recipeId: String = "",
    var recipeTitle: String = "",
    var videoUrl: String = "",
    var monthOf: String = "",
    @get:Exclude
    @set:Exclude
    var createdAt: String = ""
) {
    /** Firestore mapping of [createdAt], stored as a Timestamp. */
    @get:PropertyName("createdAt")
    @set:PropertyName("createdAt")
    var createdAtTimestamp: Timestamp?
        get() = FirestoreTime.toTimestamp(createdAt)
        set(value) {
            createdAt = FirestoreTime.toIso(value)
        }
}
