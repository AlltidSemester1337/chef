package com.formulae.chef.services.persistence

import android.util.Log
import com.formulae.chef.feature.model.RecipeList
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

private const val RECIPE_IDS = "recipeIds"
private const val CREATED_AT = "createdAt"

class RecipeListRepositoryImpl(
    private val firestore: FirebaseFirestore = FirebaseInstance.firestore
) : RecipeListRepository {

    private fun lists(uid: String) = firestore.userCollection(uid, FirestorePaths.LISTS)

    override suspend fun loadUserLists(uid: String): List<RecipeList> {
        // Ordered by creation (createdAt is storage-only; RecipeList has no such field).
        return lists(uid).get().await().documents
            .sortedBy { it.getTimestamp(CREATED_AT) }
            .mapNotNull { doc ->
                doc.toObject(RecipeList::class.java)?.also { list ->
                    if (list.id == null) list.id = doc.id
                }
            }
    }

    override fun createList(uid: String, name: String): RecipeList {
        val newRef = lists(uid).document()
        val list = RecipeList(id = newRef.id, name = name)
        val data =
            mapOf("id" to list.id, "name" to list.name, RECIPE_IDS to list.recipeIds, CREATED_AT to Timestamp.now())
        newRef.set(data)
            .addOnSuccessListener { Log.d("RecipeListRepo", "List '${list.name}' created") }
            .addOnFailureListener { e -> Log.e("RecipeListRepo", "Failed to create list", e) }
        return list
    }

    override fun deleteList(uid: String, listId: String) {
        lists(uid).document(listId).delete()
            .addOnSuccessListener { Log.d("RecipeListRepo", "List $listId deleted") }
            .addOnFailureListener { e -> Log.e("RecipeListRepo", "Failed to delete list", e) }
    }

    // arrayUnion/arrayRemove are atomic server-side and skip duplicates, so no read-modify-write.
    override fun addRecipeToList(uid: String, listId: String, recipeId: String) {
        lists(uid).document(listId).update(RECIPE_IDS, FieldValue.arrayUnion(recipeId))
            .addOnFailureListener { e -> Log.e("RecipeListRepo", "Failed to add recipe to list", e) }
    }

    override fun removeRecipeFromList(uid: String, listId: String, recipeId: String) {
        lists(uid).document(listId).update(RECIPE_IDS, FieldValue.arrayRemove(recipeId))
            .addOnFailureListener { e -> Log.e("RecipeListRepo", "Failed to remove recipe from list", e) }
    }
}
