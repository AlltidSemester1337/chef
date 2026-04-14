package com.formulae.chef.rotw.job

import com.formulae.chef.rotw.model.RecipeData
import com.formulae.chef.rotw.model.RecipeOfTheMonthRecord
import com.formulae.chef.rotw.service.FirebaseAdminService
import com.formulae.chef.rotw.service.GeminiPromptBuilder
import com.formulae.chef.rotw.service.VeoClient
import java.time.Instant
import java.time.YearMonth
import java.time.ZoneOffset
import org.slf4j.LoggerFactory

private val logger = LoggerFactory.getLogger(RecipeOfTheMonthJob::class.java)

class RecipeOfTheMonthJob(
    private val firebaseAdminService: FirebaseAdminService = FirebaseAdminService(),
    private val veoClient: VeoClient = VeoClient(),
    private val geminiPromptBuilder: GeminiPromptBuilder = GeminiPromptBuilder()
) {
    suspend fun execute() {
        logger.info("Starting Recipe of the Month job")

        val favourites = firebaseAdminService.loadFavouriteRecipes()
        logger.info("Loaded ${favourites.size} favourite recipes")

        val alreadySelected = firebaseAdminService.loadSelectedRecipeIds()
        logger.info("Already selected recipe IDs: ${alreadySelected.size}")

        val selected = selectRecipe(favourites, alreadySelected)
        if (selected == null) {
            logger.warn(
                "No eligible recipes available — all favourites have already been featured. Skipping this month."
            )
            return
        }

        logger.info("Selected recipe: ${selected.id} — ${selected.title}")

        val prompt = geminiPromptBuilder.buildPrompt(selected)
        logger.info("Generated Veo 2 prompt: ${prompt.take(100)}...")

        val videoBytes = veoClient.generateVideo(prompt, durationSeconds = 15)
        logger.info("Video generated: ${videoBytes.size} bytes")

        val monthOf = YearMonth.now(ZoneOffset.UTC).toString()
        val videoUrl = firebaseAdminService.uploadVideo(videoBytes, monthOf)

        val record = RecipeOfTheMonthRecord(
            recipeId = selected.id,
            recipeTitle = selected.title,
            videoUrl = videoUrl,
            monthOf = monthOf,
            createdAt = Instant.now().toString()
        )

        firebaseAdminService.writeRecipeOfTheMonth(record)
        firebaseAdminService.markRecipeSelected(selected.id)
        firebaseAdminService.updateRecipeVideoUrl(selected.id, videoUrl)

        logger.info("Recipe of the Month job complete. Recipe: ${selected.title}, Month: $monthOf")
    }
}

/**
 * Selects a random favourite recipe that has not previously been featured.
 * Pure function — no side effects, fully unit-testable.
 */
fun selectRecipe(
    favourites: List<RecipeData>,
    alreadySelected: Set<String>
): RecipeData? = favourites
    .filter { it.id !in alreadySelected }
    .randomOrNull()
