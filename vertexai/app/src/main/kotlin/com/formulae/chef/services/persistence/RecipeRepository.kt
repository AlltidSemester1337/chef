package com.formulae.chef.services.persistence

import com.formulae.chef.feature.model.Recipe
import com.formulae.chef.feature.model.RecipeOfTheMonth

interface RecipeRepository {
    fun saveRecipe(recipe: Recipe)
    suspend fun loadUserRecipes(uid: String): List<Recipe>
    suspend fun loadAllRecipes(): List<Recipe>
    fun removeRecipe(recipeId: String)
    fun removeRecipeUid(recipeId: String)
    suspend fun getRecipeById(recipeId: String): Recipe?
    suspend fun getLatestRecipeOfTheMonth(): RecipeOfTheMonth?
}
