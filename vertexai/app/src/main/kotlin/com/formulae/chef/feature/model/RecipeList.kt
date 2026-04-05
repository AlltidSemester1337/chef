package com.formulae.chef.feature.model

data class RecipeList(
    var id: String? = null,
    var name: String = "",
    var recipeIds: List<String> = emptyList()
)
