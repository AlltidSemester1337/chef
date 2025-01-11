package com.formulae.chef.services.persistence

import com.formulae.chef.feature.collection.Recipe
import kotlinx.coroutines.tasks.await

class RecipeRepositoryImpl : RecipeRepository {
    private val _database = FirebaseInstance.database
    private val recipesRef = _database.getReference("recipes")

    override suspend fun saveRecipe(recipe: Recipe) {
        val key = recipe.id ?: recipesRef.push().key
        key?.let {
            recipesRef.child(it).setValue(recipe.copy(id = it)).await()
        }
    }

    override suspend fun loadRecipes(): List<Recipe> {
        return recipesRef.get().await().children.mapNotNull { snapshot ->
            snapshot.getValue(Recipe::class.java)
        }
    }
}
