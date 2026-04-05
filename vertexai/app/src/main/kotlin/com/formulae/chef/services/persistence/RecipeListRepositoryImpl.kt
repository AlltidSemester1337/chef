package com.formulae.chef.services.persistence

import android.util.Log
import com.formulae.chef.feature.model.RecipeList
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.tasks.await

class RecipeListRepositoryImpl(
    private val database: FirebaseDatabase = FirebaseInstance.database
) : RecipeListRepository {

    private fun listsRef(uid: String) = database.getReference("users/$uid/lists")

    override suspend fun loadUserLists(uid: String): List<RecipeList> {
        return listsRef(uid).get().await().children.mapNotNull { snapshot ->
            snapshot.getValue(RecipeList::class.java)?.also { list ->
                if (list.id == null) list.id = snapshot.key
            }
        }
    }

    override fun createList(uid: String, name: String) {
        val ref = listsRef(uid)
        val newRef = ref.push()
        val list = RecipeList(id = newRef.key, name = name)
        newRef.setValue(list)
            .addOnSuccessListener { Log.d("RecipeListRepo", "List '${list.name}' created") }
            .addOnFailureListener { e -> Log.e("RecipeListRepo", "Failed to create list", e) }
    }

    override fun deleteList(uid: String, listId: String) {
        listsRef(uid).child(listId).removeValue()
            .addOnSuccessListener { Log.d("RecipeListRepo", "List $listId deleted") }
            .addOnFailureListener { e -> Log.e("RecipeListRepo", "Failed to delete list", e) }
    }

    override fun addRecipeToList(uid: String, listId: String, recipeId: String) {
        val listRef = listsRef(uid).child(listId)
        listRef.get().addOnSuccessListener { snapshot ->
            val list = snapshot.getValue(RecipeList::class.java) ?: return@addOnSuccessListener
            if (!list.recipeIds.contains(recipeId)) {
                val updated = list.recipeIds + recipeId
                listRef.child("recipeIds").setValue(updated)
                    .addOnFailureListener { e -> Log.e("RecipeListRepo", "Failed to add recipe to list", e) }
            }
        }.addOnFailureListener { e -> Log.e("RecipeListRepo", "Failed to read list for add", e) }
    }

    override fun removeRecipeFromList(uid: String, listId: String, recipeId: String) {
        val listRef = listsRef(uid).child(listId)
        listRef.get().addOnSuccessListener { snapshot ->
            val list = snapshot.getValue(RecipeList::class.java) ?: return@addOnSuccessListener
            val updated = list.recipeIds.filter { it != recipeId }
            listRef.child("recipeIds").setValue(updated)
                .addOnFailureListener { e -> Log.e("RecipeListRepo", "Failed to remove recipe from list", e) }
        }.addOnFailureListener { e -> Log.e("RecipeListRepo", "Failed to read list for remove", e) }
    }
}
