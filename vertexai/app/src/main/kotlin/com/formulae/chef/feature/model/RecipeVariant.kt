package com.formulae.chef.feature.model

import com.formulae.chef.services.persistence.FirestoreTime
import com.google.firebase.Timestamp
import com.google.firebase.firestore.Exclude
import com.google.firebase.firestore.PropertyName

data class RecipeVariant(
    var id: String? = null,
    var label: String = "",
    @get:Exclude
    @set:Exclude
    var createdAt: String = "",
    @get:PropertyName("isPinned")
    @set:PropertyName("isPinned")
    var isPinned: Boolean = false,
    var title: String = "",
    var summary: String = "",
    var servings: String? = "",
    var prepTime: String? = null,
    var cookingTime: String? = null,
    var nutrientsPerServing: List<Nutrient>? = listOf(),
    var ingredients: List<Ingredient> = listOf(),
    @get:PropertyName("difficulty")
    @set:PropertyName("difficulty")
    var difficulty: Difficulty? = Difficulty.EASY,
    var instructions: List<String> = listOf(),
    var tipsAndTricks: String? = null
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
