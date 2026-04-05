package com.formulae.chef.feature.collection.ui

import com.formulae.chef.feature.model.Recipe
import com.formulae.chef.services.voice.sanitizeForTts
import com.formulae.chef.services.voice.splitIntoSentences

/**
 * Returns one TTS-ready string per ingredient for sentence-chunked streaming playback.
 * Each element can be passed directly to [GcpTextToSpeechService.synthesize].
 */
internal fun buildIngredientSentences(recipe: Recipe): List<String> =
    recipe.ingredients
        .joinToString("\n") { ingredient ->
            "${ingredient.quantity.orEmpty()} ${ingredient.unit.orEmpty()} ${ingredient.name.orEmpty()}".trim()
        }
        .splitIntoSentences()

/** Returns a single TTS-ready string for the first unchecked instruction step. */
internal fun buildInstructionStepText(recipe: Recipe, checkedSteps: Set<Int>): String {
    val firstUncheckedIndex = recipe.instructions.indices.firstOrNull { it !in checkedSteps } ?: 0
    return recipe.instructions.getOrElse(firstUncheckedIndex) { "" }.sanitizeForTts()
}
