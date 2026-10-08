package com.formulae.chef.rotw.service

import com.formulae.chef.rotw.model.IngredientData
import com.formulae.chef.rotw.model.RecipeData
import com.formulae.chef.rotw.model.RecipeOfTheMonthRecord
import com.google.api.core.ApiFuture
import com.google.auth.oauth2.GoogleCredentials
import com.google.cloud.Timestamp
import com.google.cloud.firestore.Firestore
import com.google.cloud.storage.Acl
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.cloud.FirestoreClient
import com.google.firebase.cloud.StorageClient
import java.io.ByteArrayInputStream
import java.time.Instant
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.slf4j.LoggerFactory

private val logger = LoggerFactory.getLogger(FirebaseAdminService::class.java)

private const val RECIPES = "recipes"
private const val RECIPE_OF_THE_MONTH = "recipe_of_the_month"
private const val VIDEO_HISTORY = "video_generation_history"
private const val STORAGE_VIDEO_PATH = "videos/rotw"

/**
 * Firebase Admin SDK client for all Firestore and Storage operations performed by the rotw-job.
 * Schema: `.ai/firestore-schema.md`. The Admin SDK bypasses `firestore.rules`.
 *
 * Credentials come from ADC: the attached service account on Cloud Run, or
 * `gcloud auth application-default login` locally. Set FIRESTORE_EMULATOR_HOST to use the emulator.
 */
class FirebaseAdminService(
    private val firestore: Firestore,
    private val storageBucket: String
) {

    suspend fun loadFavouriteRecipes(): List<RecipeData> {
        val snapshot = firestore.collection(RECIPES).whereEqualTo("isFavourite", true).get().await()
        return snapshot.documents.mapNotNull { doc -> recipeDataFrom(doc.id, doc.data) }
    }

    suspend fun loadSelectedRecipeIds(): Set<String> =
        firestore.collection(VIDEO_HISTORY).get().await().documents.map { it.id }.toSet()

    suspend fun uploadVideo(videoBytes: ByteArray, monthOf: String, recipeId: String): String {
        val bucket = StorageClient.getInstance().bucket()
        // Keyed by recipeId, not just monthOf, so a repeated/retried run within the same
        // month can't overwrite an earlier run's video out from under its recipe record.
        val blobName = "$STORAGE_VIDEO_PATH/$monthOf-$recipeId.mp4"
        val blob = bucket.create(blobName, ByteArrayInputStream(videoBytes), "video/mp4")
        blob.createAcl(Acl.of(Acl.User.ofAllUsers(), Acl.Role.READER))
        val downloadUrl = "https://storage.googleapis.com/$storageBucket/$blobName"
        logger.info("Video uploaded to $downloadUrl")
        return downloadUrl
    }

    suspend fun writeRecipeOfTheMonth(record: RecipeOfTheMonthRecord) {
        val ref = firestore.collection(RECIPE_OF_THE_MONTH).document()
        ref.set(recipeOfTheMonthData(record)).await()
        logger.info("RecipeOfTheMonth record written: ${ref.id}")
    }

    suspend fun markRecipeSelected(recipeId: String) {
        firestore.collection(VIDEO_HISTORY).document(recipeId).set(mapOf("selectedAt" to Timestamp.now())).await()
    }

    suspend fun updateRecipeVideoUrl(recipeId: String, videoUrl: String) {
        firestore.collection(RECIPES).document(recipeId).update("videoUrl", videoUrl).await()
        logger.info("Updated videoUrl on recipe $recipeId")
    }

    companion object {
        /**
         * Required env vars: FIREBASE_STORAGE_BUCKET (e.g. project-id.firebasestorage.app).
         * Project and credentials come from ADC.
         */
        fun fromEnvironment(): FirebaseAdminService {
            val storageBucket = System.getenv("FIREBASE_STORAGE_BUCKET")
                ?: error("FIREBASE_STORAGE_BUCKET env var not set")
            if (FirebaseApp.getApps().isEmpty()) {
                val options = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.getApplicationDefault())
                    .setStorageBucket(storageBucket)
                    .build()
                FirebaseApp.initializeApp(options)
            }
            return FirebaseAdminService(FirestoreClient.getFirestore(), storageBucket)
        }
    }
}

/** Maps a `recipes/{id}` document to [RecipeData]; null when the title is missing. Pure — unit-tested. */
internal fun recipeDataFrom(id: String, data: Map<String, Any?>): RecipeData? {
    val title = data["title"] as? String ?: return null
    val ingredients = (data["ingredients"] as? List<*>).orEmpty().mapNotNull { item ->
        val ingredient = item as? Map<*, *> ?: return@mapNotNull null
        IngredientData(
            name = ingredient["name"] as? String,
            quantity = ingredient["quantity"] as? String,
            unit = ingredient["unit"] as? String
        )
    }
    return RecipeData(id = id, title = title, ingredients = ingredients, isFavourite = data["isFavourite"] == true)
}

/** `recipe_of_the_month` document data; `createdAt` stored as a Firestore Timestamp. Pure — unit-tested. */
internal fun recipeOfTheMonthData(record: RecipeOfTheMonthRecord): Map<String, Any> {
    val createdAt = Instant.parse(record.createdAt)
    return mapOf(
        "recipeId" to record.recipeId,
        "recipeTitle" to record.recipeTitle,
        "videoUrl" to record.videoUrl,
        "monthOf" to record.monthOf,
        "createdAt" to Timestamp.ofTimeSecondsAndNanos(createdAt.epochSecond, createdAt.nano)
    )
}

private suspend fun <T> ApiFuture<T>.await(): T = withContext(Dispatchers.IO) { get() }
