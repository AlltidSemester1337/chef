package com.formulae.chef.rotw

import com.formulae.chef.rotw.model.RecipeOfTheMonthRecord
import com.formulae.chef.rotw.service.FirebaseAdminService
import com.google.cloud.Timestamp
import com.google.cloud.firestore.FirestoreOptions
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest

/**
 * Runs the Firestore half of [FirebaseAdminService] against the Firestore emulator. Skipped (passes
 * without assertions) unless FIRESTORE_EMULATOR_HOST is set. Run it with:
 *
 *   cd firestore-tools && npx firebase emulators:exec --only firestore --project demo-rotw \
 *     "cd .. && ./gradlew :backend:rotw-job:test --rerun"
 *
 * Storage (video upload) is not covered — there is no Storage emulator wiring here.
 */
class FirebaseAdminServiceEmulatorTest {
    private val emulatorHost: String? = System.getenv("FIRESTORE_EMULATOR_HOST")

    @Test
    fun `reads favourites and history, writes the month record, history and videoUrl`() = runTest {
        if (emulatorHost == null) {
            println("FIRESTORE_EMULATOR_HOST not set — skipping emulator test")
            return@runTest
        }
        // Fresh project ID per run isolates this test from leftovers of earlier runs.
        val firestore = FirestoreOptions.getDefaultInstance().toBuilder()
            .setProjectId("demo-rotw-${UUID.randomUUID().toString().take(8)}")
            .setEmulatorHost(emulatorHost)
            .build()
            .service
        val service = FirebaseAdminService(firestore, storageBucket = "unused")

        firestore.collection("recipes").document("fav1").set(
            mapOf("title" to "Moussaka", "isFavourite" to true, "uid" to "u1",
                "ingredients" to listOf(mapOf("name" to "lamm", "quantity" to "500", "unit" to "g")))
        ).get()
        firestore.collection("recipes").document("fav2").set(mapOf("title" to "Soppa", "isFavourite" to true)).get()
        firestore.collection("recipes").document("plain").set(mapOf("title" to "Gryta", "isFavourite" to false)).get()
        firestore.collection("video_generation_history").document("fav2").set(mapOf("selectedAt" to null)).get()

        assertEquals(setOf("fav1", "fav2"), service.loadFavouriteRecipes().map { it.id }.toSet())
        assertEquals("lamm", service.loadFavouriteRecipes().first { it.id == "fav1" }.ingredients.single().name)
        assertEquals(setOf("fav2"), service.loadSelectedRecipeIds())

        service.writeRecipeOfTheMonth(
            RecipeOfTheMonthRecord("fav1", "Moussaka", "https://v", "2026-11", "2026-11-01T22:00:05Z")
        )
        service.markRecipeSelected("fav1")
        service.updateRecipeVideoUrl("fav1", "https://v")

        val months = firestore.collection("recipe_of_the_month").get().get().documents
        assertEquals(1, months.size)
        assertTrue(months.single().get("createdAt") is Timestamp)
        assertEquals("2026-11", months.single().getString("monthOf"))
        assertEquals(setOf("fav1", "fav2"), service.loadSelectedRecipeIds())
        assertTrue(firestore.collection("video_generation_history").document("fav1").get().get().get("selectedAt") is Timestamp)
        assertEquals("https://v", firestore.collection("recipes").document("fav1").get().get().getString("videoUrl"))
        // update() must not clobber the rest of the recipe
        assertEquals("u1", firestore.collection("recipes").document("fav1").get().get().getString("uid"))
    }
}
