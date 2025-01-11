package com.formulae.chef.services.persistence

import com.formulae.chef.feature.collection.Recipe

interface RecipeRepository {
    suspend fun saveRecipe(recipe: Recipe)
    suspend fun loadRecipes(): List<Recipe>
}