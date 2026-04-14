package com.formulae.chef.rotw.service

import com.formulae.chef.rotw.model.RecipeData

/**
 * Builds a Veo 2 prompt from a recipe's data.
 * Pure function — no external dependencies, fully unit-testable.
 */
class GeminiPromptBuilder {

    fun buildPrompt(recipe: RecipeData): String {
        val topIngredients = recipe.ingredients
            .mapNotNull { it.name?.takeIf { name -> name.isNotBlank() } }
            .take(3)
            .joinToString(", ")
            .ifEmpty { "fresh ingredients" }

        return buildString {
            append("Cinematic close-up food video of ${recipe.title}. ")
            append("Key ingredients: $topIngredients. ")
            append("Professional food photography lighting, warm tones. ")
            append("Starts with fresh raw ingredients on a wooden cutting board, ")
            append("shows cooking action on a modern stovetop, ")
            append("ends with a beautifully plated serving of ${recipe.title} on an elegant dish with steam rising. ")
            append("No text overlays, no human hands visible, 16:9 aspect ratio, 4K quality.")
        }
    }
}
