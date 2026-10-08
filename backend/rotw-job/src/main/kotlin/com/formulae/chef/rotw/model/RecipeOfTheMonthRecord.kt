package com.formulae.chef.rotw.model

/**
 * Record written to `recipe_of_the_month/{autoId}` in Firestore (`createdAt` ISO-8601, stored as a Timestamp).
 */
data class RecipeOfTheMonthRecord(
    val recipeId: String,
    val recipeTitle: String,
    val videoUrl: String,
    val monthOf: String,
    val createdAt: String
)
