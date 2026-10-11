package com.formulae.chef

import com.formulae.chef.feature.model.Recipe

fun buildRecipeContextText(recipe: Recipe): String = buildString {
    append("Recipe: ${recipe.title}\n\n")
    if (recipe.summary.isNotBlank()) append("${recipe.summary.replace("\\n", "\n")}\n\n")
    if (recipe.ingredients.isNotEmpty()) {
        append("Ingredients:\n")
        recipe.ingredients.forEach { ing ->
            append("- ${ing.quantity.orEmpty()} ${ing.unit.orEmpty()} ${ing.name.orEmpty()}".trim())
            append("\n")
        }
        append("\n")
    }
    if (recipe.instructions.isNotEmpty()) {
        append("Instructions:\n")
        recipe.instructions.forEachIndexed { i, step -> append("${i + 1}. $step\n") }
    }
    recipe.tipsAndTricks?.takeIf { it.isNotBlank() }?.let { append("\nTips: $it") }
}
