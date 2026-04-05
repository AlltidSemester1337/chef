package com.formulae.chef.feature.collection.ui

import com.formulae.chef.feature.model.Ingredient
import com.formulae.chef.feature.model.Recipe
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RecipeTtsFormatterTest {

    private val recipe = Recipe(
        id = "test-id",
        ingredients = listOf(
            Ingredient(name = "ground lamb", quantity = "500", unit = "g"),
            Ingredient(name = "large onion", quantity = "1", unit = "each"),
            Ingredient(name = "olive oil", quantity = "1", unit = "tbsp")
        ),
        instructions = listOf(
            "Mix all ingredients together.",
            "Shape into kebabs.",
            "Cook for 5 minutes per side."
        )
    )

    // buildIngredientSentences

    @Test
    fun `buildIngredientSentences returns one sentence per ingredient`() {
        val result = buildIngredientSentences(recipe)
        assertEquals(3, result.size)
    }

    @Test
    fun `buildIngredientSentences formats each ingredient as quantity unit name`() {
        val result = buildIngredientSentences(recipe)
        assertTrue(result.any { it.contains("500 g ground lamb") })
        assertTrue(result.any { it.contains("1 each large onion") })
        assertTrue(result.any { it.contains("1 tbsp olive oil") })
    }

    @Test
    fun `buildIngredientSentences each sentence ends with punctuation`() {
        val result = buildIngredientSentences(recipe)
        result.forEach { sentence ->
            assertTrue("Expected punctuation at end of: $sentence", sentence.endsWith('.') || sentence.endsWith('!') || sentence.endsWith('?'))
        }
    }

    @Test
    fun `buildIngredientSentences empty list returns empty`() {
        val emptyRecipe = recipe.copyOf(ingredients = emptyList())
        val result = buildIngredientSentences(emptyRecipe)
        assertTrue(result.isEmpty())
    }

    @Test
    fun `buildIngredientSentences ingredient with null fields trimmed gracefully`() {
        val recipeWithNulls = recipe.copyOf(
            ingredients = listOf(Ingredient(name = "salt", quantity = null, unit = null))
        )
        val result = buildIngredientSentences(recipeWithNulls)
        assertEquals(1, result.size)
        assertTrue(result.first().contains("salt"))
    }

    // buildInstructionStepText

    @Test
    fun `buildInstructionStepText with no checked steps returns first instruction`() {
        val result = buildInstructionStepText(recipe, checkedSteps = emptySet())
        assertTrue(result.contains("Mix all ingredients together"))
    }

    @Test
    fun `buildInstructionStepText skips checked steps and returns first unchecked`() {
        val result = buildInstructionStepText(recipe, checkedSteps = setOf(0))
        assertTrue(result.contains("Shape into kebabs"))
    }

    @Test
    fun `buildInstructionStepText skips multiple checked steps`() {
        val result = buildInstructionStepText(recipe, checkedSteps = setOf(0, 1))
        assertTrue(result.contains("Cook for 5 minutes per side"))
    }

    @Test
    fun `buildInstructionStepText when all steps checked returns first step`() {
        val result = buildInstructionStepText(recipe, checkedSteps = setOf(0, 1, 2))
        assertTrue(result.contains("Mix all ingredients together"))
    }

    @Test
    fun `buildInstructionStepText empty instructions returns empty string`() {
        val emptyRecipe = recipe.copyOf(instructions = emptyList())
        val result = buildInstructionStepText(emptyRecipe, checkedSteps = emptySet())
        assertEquals("", result)
    }
}
