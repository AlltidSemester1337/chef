package com.formulae.chef.rotw.model

/**
 * Lightweight recipe representation for backend use.
 * Built from a Firestore `recipes/{id}` document (see `recipeDataFrom`).
 */
data class RecipeData(
    var id: String = "",
    var title: String = "",
    var ingredients: List<IngredientData> = emptyList(),
    var isFavourite: Boolean = false
)

data class IngredientData(
    var name: String? = "",
    var quantity: String? = "",
    var unit: String? = ""
)
