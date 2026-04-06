package com.formulae.chef.feature.chat

import com.formulae.chef.feature.model.Ingredient
import com.formulae.chef.feature.model.Recipe
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AskChefVariantViewModelTest {

    private val sampleRecipe = Recipe(
        title = "Pasta Carbonara",
        summary = "Classic Italian pasta dish.",
        ingredients = listOf(
            Ingredient(name = "spaghetti", quantity = "400", unit = "g"),
            Ingredient(name = "guanciale", quantity = "150", unit = "g"),
            Ingredient(name = "eggs", quantity = "4", unit = "")
        ),
        instructions = listOf(
            "Boil pasta in salted water.",
            "Fry guanciale until crispy.",
            "Mix eggs and cheese, combine with pasta off heat."
        ),
        tipsAndTricks = "Use pasta water to loosen the sauce."
    )

    @Test
    fun buildAdjustPrompt_includesRecipeTitle() {
        val result = AskChefVariantViewModel.buildAdjustPrompt(sampleRecipe, "make it vegetarian")
        assertTrue(result.contains("Pasta Carbonara"))
    }

    @Test
    fun buildAdjustPrompt_includesUserRequest() {
        val result = AskChefVariantViewModel.buildAdjustPrompt(sampleRecipe, "make it vegetarian")
        assertTrue(result.contains("make it vegetarian"))
    }

    @Test
    fun buildAdjustPrompt_includesIngredients() {
        val result = AskChefVariantViewModel.buildAdjustPrompt(sampleRecipe, "reduce spice")
        assertTrue(result.contains("spaghetti"))
        assertTrue(result.contains("guanciale"))
    }

    @Test
    fun buildAdjustPrompt_includesInstructions() {
        val result = AskChefVariantViewModel.buildAdjustPrompt(sampleRecipe, "reduce spice")
        assertTrue(result.contains("Boil pasta"))
    }

    @Test
    fun buildAdjustPrompt_includesModificationInstruction() {
        val result = AskChefVariantViewModel.buildAdjustPrompt(sampleRecipe, "make it vegan")
        assertTrue(result.contains("modify") || result.contains("modification") || result.contains("updated"))
    }

    @Test
    fun buildAdjustPrompt_withEmptyRequest_stillContainsRecipe() {
        val result = AskChefVariantViewModel.buildAdjustPrompt(sampleRecipe, "")
        assertTrue(result.contains("Pasta Carbonara"))
        assertFalse(result.isBlank())
    }

    @Test
    fun buildAdjustPrompt_userRequestLabelPresent() {
        val result = AskChefVariantViewModel.buildAdjustPrompt(sampleRecipe, "reduce salt")
        assertTrue(result.contains("User request:"))
        assertTrue(result.contains("reduce salt"))
    }
}
