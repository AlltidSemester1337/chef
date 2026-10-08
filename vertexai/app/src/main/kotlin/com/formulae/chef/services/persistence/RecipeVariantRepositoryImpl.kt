package com.formulae.chef.services.persistence

import android.util.Log
import com.formulae.chef.feature.model.RecipeVariant
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class RecipeVariantRepositoryImpl(
    private val firestore: FirebaseFirestore = FirebaseInstance.firestore
) : RecipeVariantRepository {

    private fun variants(recipeId: String) =
        firestore.collection(FirestorePaths.RECIPES).document(recipeId).collection(FirestorePaths.VARIANTS)

    override suspend fun loadVariantsForRecipe(recipeId: String): List<RecipeVariant> {
        return variants(recipeId).get().await().toObjects(RecipeVariant::class.java)
    }

    override suspend fun saveVariant(recipeId: String, variant: RecipeVariant): String {
        val newRef = variants(recipeId).document()
        newRef.set(variant.copy(id = newRef.id)).await()
        Log.d("Firestore", "Variant ${newRef.id} saved successfully")
        return newRef.id
    }

    override fun updateVariantIsPinned(recipeId: String, variantId: String, isPinned: Boolean) {
        variants(recipeId).document(variantId)
            .update("isPinned", isPinned)
            .addOnFailureListener { e ->
                Log.e("Firestore", "Failed to update isPinned for variant $variantId", e)
            }
    }

    override fun deleteVariant(recipeId: String, variantId: String) {
        variants(recipeId).document(variantId)
            .delete()
            .addOnSuccessListener {
                Log.d("Firestore", "Variant $variantId deleted successfully")
            }
            .addOnFailureListener { e ->
                Log.e("Firestore", "Failed to delete variant $variantId", e)
            }
    }
}
