package com.formulae.chef.rotw.model

/**
 * Record written to `recipe_of_the_month/{pushId}` in Firebase RTDB.
 */
data class RecipeOfTheMonthRecord(
    val recipeId: String,
    val recipeTitle: String,
    val videoUrl: String,
    val monthOf: String,
    val createdAt: String
)
