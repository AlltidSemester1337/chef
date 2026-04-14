package com.formulae.chef.rotw.service

import com.formulae.chef.rotw.model.IngredientData
import com.formulae.chef.rotw.model.RecipeData
import com.formulae.chef.rotw.model.RecipeOfTheMonthRecord
import com.google.auth.oauth2.GoogleCredentials
import com.google.cloud.storage.Acl
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.cloud.StorageClient
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.io.ByteArrayInputStream
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.suspendCancellableCoroutine
import org.slf4j.LoggerFactory

private val logger = LoggerFactory.getLogger(FirebaseAdminService::class.java)

private const val RECIPES_KEY = "recipes"
private const val RECIPE_OF_THE_MONTH_KEY = "recipe_of_the_month"
private const val VIDEO_HISTORY_KEY = "video_generation_history"
private const val STORAGE_VIDEO_PATH = "videos/rotw"

/**
 * Firebase Admin SDK client for all RTDB and Storage operations performed by the rotw-job.
 *
 * Required env vars:
 * - GOOGLE_APPLICATION_CREDENTIALS — path to service account JSON
 * - FIREBASE_DB_URL — Firebase RTDB URL
 * - FIREBASE_STORAGE_BUCKET — Firebase Storage bucket name (e.g. project-id.appspot.com)
 */
class FirebaseAdminService {
    private val database: FirebaseDatabase
    private val storageBucket: String

    init {
        val dbUrl = System.getenv("FIREBASE_DB_URL")
            ?: error("FIREBASE_DB_URL env var not set")
        storageBucket = System.getenv("FIREBASE_STORAGE_BUCKET")
            ?: error("FIREBASE_STORAGE_BUCKET env var not set")

        val options = FirebaseOptions.builder()
            .setCredentials(GoogleCredentials.getApplicationDefault())
            .setDatabaseUrl(dbUrl)
            .setStorageBucket(storageBucket)
            .build()

        if (FirebaseApp.getApps().isEmpty()) {
            FirebaseApp.initializeApp(options)
        }

        database = FirebaseDatabase.getInstance()
    }

    suspend fun loadFavouriteRecipes(): List<RecipeData> {
        val snapshot = database.getReference(RECIPES_KEY).awaitGet()
        return snapshot.children.mapNotNull { child ->
            val isFavourite = child.child("isFavourite").getValue(Boolean::class.java) ?: false
            if (!isFavourite) return@mapNotNull null
            val id = child.key ?: return@mapNotNull null
            val title = child.child("title").getValue(String::class.java) ?: return@mapNotNull null
            val ingredients = child.child("ingredients").children.map { ing ->
                IngredientData(
                    name = ing.child("name").getValue(String::class.java),
                    quantity = ing.child("quantity").getValue(String::class.java),
                    unit = ing.child("unit").getValue(String::class.java)
                )
            }
            RecipeData(id = id, title = title, ingredients = ingredients, isFavourite = true)
        }
    }

    suspend fun loadSelectedRecipeIds(): Set<String> {
        val snapshot = database.getReference(VIDEO_HISTORY_KEY).awaitGet()
        return snapshot.children.mapNotNull { it.key }.toSet()
    }

    suspend fun uploadVideo(videoBytes: ByteArray, monthOf: String): String {
        val bucket = StorageClient.getInstance().bucket()
        val blobName = "$STORAGE_VIDEO_PATH/$monthOf.mp4"
        val blob = bucket.create(blobName, ByteArrayInputStream(videoBytes), "video/mp4")
        blob.createAcl(Acl.of(Acl.User.ofAllUsers(), Acl.Role.READER))
        val downloadUrl = "https://storage.googleapis.com/$storageBucket/$blobName"
        logger.info("Video uploaded to $downloadUrl")
        return downloadUrl
    }

    suspend fun writeRecipeOfTheMonth(record: RecipeOfTheMonthRecord): Unit = suspendCancellableCoroutine { cont ->
        val ref = database.getReference(RECIPE_OF_THE_MONTH_KEY).push()
        val data = mapOf(
            "recipeId" to record.recipeId,
            "recipeTitle" to record.recipeTitle,
            "videoUrl" to record.videoUrl,
            "monthOf" to record.monthOf,
            "createdAt" to record.createdAt
        )
        ref.setValue(data) { error: DatabaseError?, _ ->
            if (error == null) {
                logger.info("RecipeOfTheMonth record written: ${ref.key}")
                cont.resume(Unit)
            } else {
                cont.resumeWithException(RuntimeException("writeRecipeOfTheMonth failed: ${error.message}"))
            }
        }
    }

    suspend fun markRecipeSelected(recipeId: String): Unit = suspendCancellableCoroutine { cont ->
        database.getReference(VIDEO_HISTORY_KEY).child(recipeId).setValue(true) { error: DatabaseError?, _ ->
            if (error == null) {
                cont.resume(Unit)
            } else {
                cont.resumeWithException(RuntimeException("markRecipeSelected failed: ${error.message}"))
            }
        }
    }

    suspend fun updateRecipeVideoUrl(recipeId: String, videoUrl: String): Unit =
        suspendCancellableCoroutine { cont ->
            database.getReference(RECIPES_KEY).child(recipeId).child("videoUrl")
                .setValue(videoUrl) { error: DatabaseError?, _ ->
                    if (error == null) {
                        logger.info("Updated videoUrl on recipe $recipeId")
                        cont.resume(Unit)
                    } else {
                        cont.resumeWithException(RuntimeException("updateRecipeVideoUrl failed: ${error.message}"))
                    }
                }
        }

    private suspend fun DatabaseReference.awaitGet(): DataSnapshot = suspendCancellableCoroutine { cont ->
        addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) = cont.resume(snapshot)
            override fun onCancelled(error: DatabaseError) =
                cont.resumeWithException(RuntimeException("Firebase read cancelled: ${error.message}"))
        })
    }
}
