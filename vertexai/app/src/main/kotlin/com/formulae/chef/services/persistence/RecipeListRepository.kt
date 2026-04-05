package com.formulae.chef.services.persistence

import com.formulae.chef.feature.model.RecipeList

interface RecipeListRepository {
    suspend fun loadUserLists(uid: String): List<RecipeList>
    fun createList(uid: String, name: String): RecipeList
    fun deleteList(uid: String, listId: String)
    fun addRecipeToList(uid: String, listId: String, recipeId: String)
    fun removeRecipeFromList(uid: String, listId: String, recipeId: String)
}
