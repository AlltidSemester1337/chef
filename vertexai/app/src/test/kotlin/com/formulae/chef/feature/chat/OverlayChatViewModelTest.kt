package com.formulae.chef.feature.chat

import com.formulae.chef.feature.model.Ingredient
import com.formulae.chef.feature.model.Recipe
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OverlayChatViewModelTest {

    private val sampleRecipe = Recipe(
        title = "Pasta Carbonara",
        summary = "Classic Italian pasta dish.",
        ingredients = listOf(
            Ingredient(name = "spaghetti", quantity = "400", unit = "g"),
            Ingredient(name = "guanciale", quantity = "150", unit = "g")
        ),
        instructions = listOf(
            "Boil pasta in salted water.",
            "Fry guanciale until crispy.",
            "Mix eggs and cheese, combine with pasta off heat."
        ),
        tipsAndTricks = "Use pasta water to loosen the sauce."
    )

    @Test
    fun buildRecipeContextText_includesTitle() {
        val result = OverlayChatViewModel.buildRecipeContextText(sampleRecipe)
        assertTrue(result.contains("Pasta Carbonara"))
    }

    @Test
    fun buildRecipeContextText_includesSummary() {
        val result = OverlayChatViewModel.buildRecipeContextText(sampleRecipe)
        assertTrue(result.contains("Classic Italian pasta dish."))
    }

    @Test
    fun buildRecipeContextText_includesIngredients() {
        val result = OverlayChatViewModel.buildRecipeContextText(sampleRecipe)
        assertTrue(result.contains("spaghetti"))
        assertTrue(result.contains("guanciale"))
    }

    @Test
    fun buildRecipeContextText_includesInstructions() {
        val result = OverlayChatViewModel.buildRecipeContextText(sampleRecipe)
        assertTrue(result.contains("Boil pasta"))
        assertTrue(result.contains("Fry guanciale"))
    }

    @Test
    fun buildRecipeContextText_includesTips() {
        val result = OverlayChatViewModel.buildRecipeContextText(sampleRecipe)
        assertTrue(result.contains("pasta water"))
    }

    @Test
    fun buildRecipeContextText_withNoIngredients_omitsIngredientSection() {
        val recipe = sampleRecipe.copyOf(ingredients = emptyList())
        val result = OverlayChatViewModel.buildRecipeContextText(recipe)
        assertFalse(result.contains("Ingredients:"))
    }

    @Test
    fun buildRecipeContextText_withNoInstructions_omitsInstructionSection() {
        val recipe = sampleRecipe.copyOf(instructions = emptyList())
        val result = OverlayChatViewModel.buildRecipeContextText(recipe)
        assertFalse(result.contains("Instructions:"))
    }

    @Test
    fun buildRecipeContextText_withBlankTips_omitsTipsSection() {
        val recipe = sampleRecipe.copyOf(tipsAndTricks = "")
        val result = OverlayChatViewModel.buildRecipeContextText(recipe)
        assertFalse(result.contains("Tips:"))
    }

    @Test
    fun buildRecipeContextText_instructionsAreNumbered() {
        val result = OverlayChatViewModel.buildRecipeContextText(sampleRecipe)
        assertTrue(result.contains("1."))
        assertTrue(result.contains("2."))
        assertTrue(result.contains("3."))
    }
}
