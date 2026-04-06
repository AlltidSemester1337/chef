package com.formulae.chef.services.persistence

import android.util.Log
import com.formulae.chef.feature.model.RecipeVariant
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.tasks.await

private const val RECIPE_VARIANTS_KEY = "recipe_variants"

class RecipeVariantRepositoryImpl(
    private val database: FirebaseDatabase = FirebaseInstance.database
) : RecipeVariantRepository {

    override suspend fun loadVariantsForRecipe(recipeId: String): List<RecipeVariant> {
        return database.getReference(RECIPE_VARIANTS_KEY)
            .child(recipeId)
            .get()
            .await()
            .children
            .mapNotNull { snapshot ->
                snapshot.getValue(RecipeVariant::class.java)
                    ?.copy(isPinned = snapshot.child("isPinned").getValue(Boolean::class.java) ?: false)
            }
    }

    override suspend fun saveVariant(recipeId: String, variant: RecipeVariant): String {
        val ref = database.getReference(RECIPE_VARIANTS_KEY).child(recipeId)
        val newRef = ref.push()
        val id = newRef.key ?: ""
        newRef.setValue(variant.copy(id = id)).await()
        Log.d("FirebaseDB", "Variant $id saved successfully")
        return id
    }

    override fun updateVariantIsPinned(recipeId: String, variantId: String, isPinned: Boolean) {
        database.getReference(RECIPE_VARIANTS_KEY)
            .child(recipeId)
            .child(variantId)
            .updateChildren(mapOf("isPinned" to isPinned))
            .addOnFailureListener { e ->
                Log.e("FirebaseDB", "Failed to update isPinned for variant $variantId", e)
            }
    }

    override fun deleteVariant(recipeId: String, variantId: String) {
        database.getReference(RECIPE_VARIANTS_KEY)
            .child(recipeId)
            .child(variantId)
            .removeValue()
            .addOnSuccessListener {
                Log.d("FirebaseDB", "Variant $variantId deleted successfully")
            }
            .addOnFailureListener { e ->
                Log.e("FirebaseDB", "Failed to delete variant $variantId", e)
            }
    }
}
