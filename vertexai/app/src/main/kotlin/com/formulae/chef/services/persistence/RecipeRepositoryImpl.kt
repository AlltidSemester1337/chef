package com.formulae.chef.services.persistence

import android.util.Log
import com.formulae.chef.feature.model.Recipe
import com.google.gson.Gson
import kotlinx.coroutines.tasks.await

private const val FAVOURITES_KEY = "favourites"

class RecipeRepositoryImpl : RecipeRepository {
    private val _database = FirebaseInstance.database
    private val recipesRef = _database.getReference(FAVOURITES_KEY)
    private val gson = Gson()

    override fun saveRecipe(recipe: Recipe) {
        val reference = _database.getReference(FAVOURITES_KEY)
        val newEntriesChildren = gson.toJson(recipe)
        reference.push().setValue(newEntriesChildren).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                Log.d("FirebaseDB", "Recipe saved successfully!")
            } else {
                Log.e("FirebaseDB", "Failed to add new recipe: ", task.exception)
            }
        }
    }

    override suspend fun loadRecipes(): List<Recipe> {
        return recipesRef.get().await().children.mapNotNull { snapshot ->
            snapshot.getValue(String::class.java)?.let { gson.fromJson(it, Recipe::class.java) }
        }
    }
}
