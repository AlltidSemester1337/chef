package com.formulae.chef.services.persistence

import com.formulae.chef.feature.collection.Recipe

interface RecipeRepository {
    fun saveRecipe(recipe: Recipe)
    suspend fun loadRecipes(): List<Recipe>
}