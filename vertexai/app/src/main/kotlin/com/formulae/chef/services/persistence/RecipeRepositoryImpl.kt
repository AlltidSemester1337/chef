package com.formulae.chef.services.persistence

import android.util.Log
import com.formulae.chef.feature.model.Recipe
import com.google.gson.Gson
import kotlinx.coroutines.tasks.await

private const val FAVOURITES_KEY = "recipes"

class RecipeRepositoryImpl : RecipeRepository {
    private val _database = FirebaseInstance.database
    private val recipesRef = _database.getReference(FAVOURITES_KEY)

    override fun saveRecipe(recipe: Recipe) {
        val reference = _database.getReference(FAVOURITES_KEY)
        val newDocumentRef = reference.push()
        val recipeWithId = recipe.copy(id = newDocumentRef.key)

        newDocumentRef.setValue(recipeWithId).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                Log.d("FirebaseDB", "Recipe $newDocumentRef saved successfully!")
            } else {
                Log.e("FirebaseDB", "Failed to add new recipe: ", task.exception)
            }
        }
    }

    override suspend fun loadUserRecipes(uid: String): List<Recipe> {
        return recipesRef.orderByChild("uid")
            .equalTo(uid)
            .get()
            .await()
            .children
            .mapNotNull { snapshot -> snapshot.getValue(Recipe::class.java) }
    }

    override suspend fun loadAllRecipes(): List<Recipe> {
        return recipesRef.get().await().children.mapNotNull { snapshot ->
            snapshot.getValue(Recipe::class.java)
        }
    }

    override fun removeRecipe(recipeId: String) {
        val reference = _database.getReference(FAVOURITES_KEY)

        reference.child(recipeId).removeValue()
            .addOnSuccessListener {
                Log.d("FirebaseDB", "Recipe $recipeId deleted successfully")
            }
            .addOnFailureListener { e ->
                Log.e("FirebaseDB", "Error deleting recipe", e)
            }
    }
}
