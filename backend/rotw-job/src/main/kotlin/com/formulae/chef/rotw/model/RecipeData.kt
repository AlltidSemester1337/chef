package com.formulae.chef.rotw.model

/**
 * Lightweight recipe representation for backend use.
 * Maps to the Firebase RTDB `recipes/{id}` node.
 * Uses mutable var fields with default values for Firebase Admin SDK deserialization.
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
