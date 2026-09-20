package com.formulae.chef.rotw.job

import com.formulae.chef.rotw.model.RecipeData
import com.formulae.chef.rotw.model.RecipeOfTheMonthRecord
import com.formulae.chef.rotw.service.FirebaseAdminService
import com.formulae.chef.rotw.service.GeminiPromptBuilder
import com.formulae.chef.rotw.service.VeoClient
import java.nio.file.Files
import java.nio.file.Path
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
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
        val today = LocalDate.now(ZoneOffset.UTC)
        if (!isFirstSundayOfMonth(today)) {
            logger.info("Not the first Sunday of the month ($today) — skipping this run.")
            return
        }

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
        logger.info("Generated Veo 3.1 prompt: ${prompt.take(100)}...")

        val monthOf = YearMonth.now(ZoneOffset.UTC).toString()
        val videoBytes = veoClient.generateVideo(prompt, durationSeconds = 8)
        logger.info("Video generated: ${videoBytes.size} bytes")

        try {
            val videoUrl = firebaseAdminService.uploadVideo(videoBytes, monthOf, selected.id)

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
        } catch (e: Exception) {
            // Veo 3.1 generation already completed (cost incurred). Attempt to preserve
            // the video bytes to /tmp so they survive for the duration of this Cloud Run Job
            // execution and can be manually re-uploaded without re-running Veo 3.1.
            try {
                val tempFile: Path = Files.createTempFile("rotw-$monthOf-", ".mp4")
                Files.write(tempFile, videoBytes)
                logger.error(
                    "Upload or RTDB write failed. Video preserved at $tempFile — " +
                        "re-upload manually to Firebase Storage at videos/rotw/$monthOf-${selected.id}.mp4 " +
                        "and write the recipe_of_the_month record by hand. Recipe: ${selected.id}",
                    e
                )
            } catch (ioException: Exception) {
                logger.error("Failed to preserve video bytes to /tmp after upload failure", ioException)
            }
            throw e
        }
    }

    /**
     * Completes a run whose Veo operation already succeeded but whose upload/RTDB
     * write step crashed (or whose client-side polling itself was broken). Video
     * generation is billed at submission time, so this avoids paying twice for the
     * same clip — see VeoClient.fetchExistingOperation.
     */
    suspend fun recoverOperation(operationName: String, recipeId: String, recipeTitle: String, monthOf: String) {
        logger.info("Recovering existing Veo operation: $operationName")
        val videoBytes = veoClient.fetchExistingOperation(operationName)
        logger.info("Recovered video: ${videoBytes.size} bytes")

        val videoUrl = firebaseAdminService.uploadVideo(videoBytes, monthOf, recipeId)
        val record = RecipeOfTheMonthRecord(
            recipeId = recipeId,
            recipeTitle = recipeTitle,
            videoUrl = videoUrl,
            monthOf = monthOf,
            createdAt = Instant.now().toString()
        )

        firebaseAdminService.writeRecipeOfTheMonth(record)
        firebaseAdminService.markRecipeSelected(recipeId)
        firebaseAdminService.updateRecipeVideoUrl(recipeId, videoUrl)

        logger.info("Recovery complete. Recipe: $recipeTitle, Month: $monthOf")
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

/**
 * Cloud Scheduler cron can't express "Nth weekday of month" directly — a day-of-month
 * AND day-of-week restriction is OR'd together by standard cron, not AND'd, so this
 * check runs against a daily trigger instead. Pure function — fully unit-testable.
 */
fun isFirstSundayOfMonth(date: LocalDate): Boolean =
    date.dayOfWeek == DayOfWeek.SUNDAY && date.dayOfMonth <= 7
