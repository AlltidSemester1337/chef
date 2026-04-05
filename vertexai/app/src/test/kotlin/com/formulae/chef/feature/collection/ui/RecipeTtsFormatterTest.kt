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

    @Test
    fun `ingredients tab formats each ingredient as quantity unit name`() {
        val result = buildRecipeTtsText(recipe, showIngredients = true, checkedSteps = emptySet())
        assertTrue(result.contains("500 g ground lamb"))
        assertTrue(result.contains("1 each large onion"))
        assertTrue(result.contains("1 tbsp olive oil"))
    }

    @Test
    fun `instructions tab with no checked steps returns first instruction`() {
        val result = buildRecipeTtsText(recipe, showIngredients = false, checkedSteps = emptySet())
        assertTrue(result.contains("Mix all ingredients together"))
    }

    @Test
    fun `instructions tab skips checked steps and returns first unchecked`() {
        val result = buildRecipeTtsText(recipe, showIngredients = false, checkedSteps = setOf(0))
        assertTrue(result.contains("Shape into kebabs"))
    }

    @Test
    fun `instructions tab skips multiple checked steps`() {
        val result = buildRecipeTtsText(recipe, showIngredients = false, checkedSteps = setOf(0, 1))
        assertTrue(result.contains("Cook for 5 minutes per side"))
    }

    @Test
    fun `instructions tab when all steps checked returns first step`() {
        val result = buildRecipeTtsText(recipe, showIngredients = false, checkedSteps = setOf(0, 1, 2))
        assertTrue(result.contains("Mix all ingredients together"))
    }

    @Test
    fun `empty ingredients list returns empty-ish string`() {
        val emptyRecipe = recipe.copyOf(ingredients = emptyList())
        val result = buildRecipeTtsText(emptyRecipe, showIngredients = true, checkedSteps = emptySet())
        assertTrue(result.isBlank())
    }

    @Test
    fun `empty instructions list returns empty string`() {
        val emptyRecipe = recipe.copyOf(instructions = emptyList())
        val result = buildRecipeTtsText(emptyRecipe, showIngredients = false, checkedSteps = emptySet())
        assertEquals("", result)
    }

    @Test
    fun `ingredient with null fields is trimmed gracefully`() {
        val recipeWithNullFields = recipe.copyOf(
            ingredients = listOf(Ingredient(name = "salt", quantity = null, unit = null))
        )
        val result = buildRecipeTtsText(recipeWithNullFields, showIngredients = true, checkedSteps = emptySet())
        assertTrue(result.contains("salt"))
    }
}
