package com.formulae.chef

import com.formulae.chef.feature.model.Recipe

const val DERIVE_RECIPE_JSON_SYSTEM_INSTRUCTIONS =
    """Extract recipes from the provided text and return them as JSON matching this schema:
{
  "recipes": [
    {
      "title": "string",
      "summary": "string, brief description of the dish",
      "servings": "string, e.g. '4 servings'",
      "prepTime": "string, e.g. '25 minutes'",
      "cookingTime": "string, e.g. '35 minutes'",
      "nutrientsPerServing": [{"name": "string", "quantity": "string", "unit": "string"}],
      "ingredients": [{"name": "string", "quantity": "numeric string only, e.g. '500', '2', '0.5', '1/2' — never include the unit here", "unit": "string"}],
      "difficulty": "EASY | MEDIUM | HARD",
      "instructions": ["string, one step per element"],
      "tipsAndTricks": "string, formatted as '- ' prefixed bullet lines, one tip per line starting with '- '",
      "tags": ["string"]
    }
  ]
}
Use metric units for ingredient quantities. Omit any fields that are not applicable or cannot be determined from the text.
For the tags field, generate a flat list of descriptive tags covering: main ingredient (e.g. 'chicken', 'lamb', 'vegetarian', 'vegan', 'fish'), cuisine (e.g. 'korean', 'italian', 'indonesian', 'mexican', 'indian'), effort level (e.g. 'under 30 minutes', 'under 1 hour', '1-2 hours', 'slow cook'), and season or occasion where applicable (e.g. 'christmas', 'easter', 'summer', 'weeknight'). Include only tags that genuinely apply. All tags must be lowercase."""

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
