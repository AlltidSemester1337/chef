package com.formulae.chef.services.persistence

import android.util.Log
import com.formulae.chef.feature.model.Recipe
import com.formulae.chef.feature.model.RecipeOfTheMonth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await

class RecipeRepositoryImpl(
    private val firestore: FirebaseFirestore = FirebaseInstance.firestore
) : RecipeRepository {
    private val recipes get() = firestore.collection(FirestorePaths.RECIPES)

    override fun saveRecipe(recipe: Recipe) {
        val documentRef = recipe.id?.let { recipes.document(it) } ?: recipes.document()
        val recipeWithId = recipe.copy(id = documentRef.id)

        documentRef.set(recipeWithId).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                Log.d("Firestore", "Recipe ${documentRef.id} saved successfully!")
            } else {
                Log.e("Firestore", "Failed to add new recipe: ", task.exception)
            }
        }
    }

    override suspend fun loadUserRecipes(uid: String): List<Recipe> {
        return recipes.whereEqualTo("uid", uid)
            .get()
            .await()
            .toObjects(Recipe::class.java)
            .sortedByDescending { it.updatedAtTimestamp }
    }

    // Newest first, sorted client-side to avoid a composite index. The collection screen shows this order.
    override suspend fun loadAllRecipes(): List<Recipe> {
        return recipes.get().await().toObjects(Recipe::class.java).sortedByDescending { it.updatedAtTimestamp }
    }

    override fun removeRecipe(recipeId: String) {
        recipes.document(recipeId).delete()
            .addOnSuccessListener {
                Log.d("Firestore", "Recipe $recipeId deleted successfully")
            }
            .addOnFailureListener { e ->
                Log.e("Firestore", "Error deleting recipe", e)
            }
    }

    // Orphaned = uid field absent (not null): Recipe.uid is a non-null String, so toObject() would
    // throw on an explicit null.
    override fun removeRecipeUid(recipeId: String) {
        recipes.document(recipeId).update("uid", FieldValue.delete())
            .addOnSuccessListener {
                Log.d("Firestore", "Recipe $recipeId updated successfully")
            }
            .addOnFailureListener { e ->
                Log.e("Firestore", "Error updating recipe", e)
            }
    }

    override suspend fun getRecipeById(recipeId: String): Recipe? {
        return recipes.document(recipeId).get().await().toObject(Recipe::class.java)
    }

    override suspend fun getLatestRecipeOfTheMonth(): RecipeOfTheMonth? {
        return firestore.collection(FirestorePaths.RECIPE_OF_THE_MONTH)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(1)
            .get()
            .await()
            .documents
            .firstOrNull()
            ?.let { doc -> doc.toObject(RecipeOfTheMonth::class.java)?.apply { id = doc.id } }
    }
}
