package com.formulae.chef.services.persistence

import android.util.Log
import com.formulae.chef.feature.model.Recipe
import com.formulae.chef.feature.model.RecipeOfTheMonth
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.tasks.await

private const val RECIPES_KEY = "recipes"
private const val RECIPE_OF_THE_MONTH_KEY = "recipe_of_the_month"

class RecipeRepositoryImpl(
    private val database: FirebaseDatabase = FirebaseInstance.database
) : RecipeRepository {
    private val recipesRef = database.getReference(RECIPES_KEY)

    override fun saveRecipe(recipe: Recipe) {
        val reference = database.getReference(RECIPES_KEY)
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
            .mapNotNull { snapshot ->
                snapshot.getValue(Recipe::class.java)
                    ?.copy(isFavourite = snapshot.child("isFavourite").getValue(Boolean::class.java) ?: false)
            }
    }

    override suspend fun loadAllRecipes(): List<Recipe> {
        return recipesRef.get().await().children.mapNotNull { snapshot ->
            snapshot.getValue(Recipe::class.java)
                ?.copy(isFavourite = snapshot.child("isFavourite").getValue(Boolean::class.java) ?: false)
        }
    }

    override fun removeRecipe(recipeId: String) {
        val reference = database.getReference(RECIPES_KEY)

        reference.child(recipeId).removeValue()
            .addOnSuccessListener {
                Log.d("FirebaseDB", "Recipe $recipeId deleted successfully")
            }
            .addOnFailureListener { e ->
                Log.e("FirebaseDB", "Error deleting recipe", e)
            }
    }

    override fun removeRecipeUid(recipeId: String) {
        val reference = database.getReference(RECIPES_KEY)

        reference.child(recipeId).updateChildren(mapOf("uid" to null))
            .addOnSuccessListener {
                Log.d("FirebaseDB", "Recipe $recipeId updated successfully")
            }
            .addOnFailureListener { e ->
                Log.e("FirebaseDB", "Error updating recipe", e)
            }
    }

    override suspend fun getRecipeById(recipeId: String): Recipe? {
        val snapshot = recipesRef.child(recipeId).get().await()
        return snapshot.getValue(Recipe::class.java)
            ?.copy(isFavourite = snapshot.child("isFavourite").getValue(Boolean::class.java) ?: false)
    }

    override suspend fun getLatestRecipeOfTheMonth(): RecipeOfTheMonth? {
        return database.getReference(RECIPE_OF_THE_MONTH_KEY)
            .limitToLast(1)
            .get()
            .await()
            .children
            .firstOrNull()
            ?.getValue(RecipeOfTheMonth::class.java)
    }
}
