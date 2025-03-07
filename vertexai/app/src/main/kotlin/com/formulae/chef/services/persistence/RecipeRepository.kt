package com.formulae.chef.services.persistence

import com.formulae.chef.feature.model.Recipe

interface RecipeRepository {
    fun saveRecipe(recipe: Recipe)
    suspend fun loadUserRecipes(uid: String): List<Recipe>
    suspend fun loadAllRecipes(): List<Recipe>
    fun removeRecipe(recipeId: String)
}