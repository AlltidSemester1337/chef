package com.formulae.chef.feature.collection.ui

import com.formulae.chef.feature.chat.ui.sanitizeForTts
import com.formulae.chef.feature.model.Recipe

internal fun buildRecipeTtsText(
    recipe: Recipe,
    showIngredients: Boolean,
    checkedSteps: Set<Int>
): String =
    if (showIngredients) {
        recipe.ingredients.joinToString("\n") { ingredient ->
            "${ingredient.quantity.orEmpty()} ${ingredient.unit.orEmpty()} ${ingredient.name.orEmpty()}".trim()
        }.sanitizeForTts()
    } else {
        val firstUncheckedIndex = recipe.instructions.indices.firstOrNull { it !in checkedSteps } ?: 0
        recipe.instructions.getOrElse(firstUncheckedIndex) { "" }.sanitizeForTts()
    }
