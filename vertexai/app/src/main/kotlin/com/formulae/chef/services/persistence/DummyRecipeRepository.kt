package com.formulae.chef.services.persistence

import com.formulae.chef.feature.collection.Recipe

class DummyRecipeRepository {
    // Simulate fetching recipes with a static list
    fun getDummyRecipes(): List<Recipe> {
        return listOf(
            Recipe(
                id = "1",
                title = "Spaghetti Bolognese",
                ingredients = listOf("Spaghetti", "Minced meat", "Tomato sauce", "Onion", "Garlic"),
                instructions = "Cook the spaghetti. Prepare the sauce. Mix and serve."
            ),
            Recipe(
                id = "2",
                title = "Pancakes",
                ingredients = listOf("Flour", "Eggs", "Milk", "Sugar"),
                instructions = "Mix ingredients. Fry on a hot pan. Serve with syrup."
            )
        )
    }
}
