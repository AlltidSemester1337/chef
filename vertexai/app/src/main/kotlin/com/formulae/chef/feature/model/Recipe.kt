package com.formulae.chef.feature.model

import com.google.firebase.database.PropertyName

data class Recipe(
    var id: String? = null,
    var uid: String = "",
    var title: String = "",
    var summary: String = "",
    var servings: String? = "",
    var prepTime: String? = null,
    var cookingTime: String? = null,
    var nutrientsPerServing: List<Nutrient>? = listOf(),
    var ingredients: List<Ingredient> = listOf(),
    var difficulty: Difficulty? = Difficulty.EASY,
    var instructions: List<String> = listOf(),
    var tipsAndTricks: String? = null,

    var imageUrl: String? = null,
    var updatedAt: String = "",
    @get:PropertyName("isFavourite")
    @set:PropertyName("isFavourite")
    var isFavourite: Boolean = false,
    var copyId: String? = null
)

// TODO: Migrate! //var ingredients: String = "",
data class Ingredient(
    var name: String? = "",
    var quantity: String? = "",
    var unit: String? = ""
)

data class Nutrient(
    var name: String? = "",
    var quantity: String? = "",
    var unit: String? = ""
)

enum class Difficulty {
    EASY, MEDIUM, HARD
}
