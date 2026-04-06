package com.formulae.chef.feature.model

import com.google.firebase.database.PropertyName

data class RecipeVariant(
    var id: String? = null,
    var label: String = "",
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
)
