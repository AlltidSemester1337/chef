package com.formulae.chef.services.persistence

import com.formulae.chef.feature.model.RecipeVariant

interface RecipeVariantRepository {
    suspend fun loadVariantsForRecipe(recipeId: String): List<RecipeVariant>
    suspend fun saveVariant(recipeId: String, variant: RecipeVariant): String
    fun updateVariantIsPinned(recipeId: String, variantId: String, isPinned: Boolean)
    fun deleteVariant(recipeId: String, variantId: String)
}
