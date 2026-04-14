package com.formulae.chef.feature.model

data class RecipeOfTheMonth(
    var id: String? = null,
    var recipeId: String = "",
    var recipeTitle: String = "",
    var videoUrl: String = "",
    var monthOf: String = "",
    var createdAt: String = ""
)
