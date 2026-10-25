package com.formulae.chef.services.storage

import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.storageMetadata
import java.util.UUID
import kotlinx.coroutines.tasks.await

/**
 * Recipe images in Firebase Storage. `recipes/{uuid}.jpg` with content type `image/jpeg` is the only
 * shape storage.rules lets clients create, and the returned URL is the form firestore.rules accept
 * for `Recipe.imageUrl`.
 */
class RecipeImageStorage(private val storage: FirebaseStorage) {

    /** Uploads [jpeg] under a new name and returns its public download URL. */
    suspend fun upload(jpeg: ByteArray): String {
        val ref = storage.reference.child("recipes/${UUID.randomUUID()}.jpg")
        ref.putBytes(jpeg, storageMetadata { contentType = "image/jpeg" }).await()
        val downloadUrl = ref.downloadUrl.await()
        // Without the token: recipe images are publicly readable.
        return "https://" + downloadUrl.host + downloadUrl.encodedPath + "?alt=media"
    }

    companion object {
        fun forProject(projectId: String?): RecipeImageStorage =
            RecipeImageStorage(FirebaseStorage.getInstance("gs://$projectId.firebasestorage.app"))
    }
}
