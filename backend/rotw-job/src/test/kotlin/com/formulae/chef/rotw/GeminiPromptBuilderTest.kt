package com.formulae.chef.rotw

import com.formulae.chef.rotw.model.IngredientData
import com.formulae.chef.rotw.model.RecipeData
import com.formulae.chef.rotw.service.GeminiPromptBuilder
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class GeminiPromptBuilderTest {

    private val builder = GeminiPromptBuilder()

    @Test
    fun `prompt contains recipe title`() {
        val recipe = RecipeData(id = "r1", title = "West African Peanut Stew")

        val prompt = builder.buildPrompt(recipe)

        assertContains(prompt, "West African Peanut Stew")
    }

    @Test
    fun `prompt contains top 3 ingredients`() {
        val recipe = RecipeData(
            id = "r1",
            title = "Pasta Carbonara",
            ingredients = listOf(
                IngredientData(name = "spaghetti"),
                IngredientData(name = "pancetta"),
                IngredientData(name = "eggs"),
                IngredientData(name = "Parmesan cheese"),
                IngredientData(name = "black pepper")
            )
        )

        val prompt = builder.buildPrompt(recipe)

        assertContains(prompt, "spaghetti")
        assertContains(prompt, "pancetta")
        assertContains(prompt, "eggs")
        assertFalse(prompt.contains("Parmesan cheese"), "Should only include top 3 ingredients")
    }

    @Test
    fun `prompt handles recipe with no ingredients`() {
        val recipe = RecipeData(id = "r1", title = "Mystery Dish", ingredients = emptyList())

        val prompt = builder.buildPrompt(recipe)

        assertContains(prompt, "Mystery Dish")
        assertContains(prompt, "fresh ingredients")
    }

    @Test
    fun `prompt handles ingredients with null names`() {
        val recipe = RecipeData(
            id = "r1",
            title = "Simple Salad",
            ingredients = listOf(
                IngredientData(name = null),
                IngredientData(name = "lettuce"),
                IngredientData(name = "tomatoes")
            )
        )

        val prompt = builder.buildPrompt(recipe)

        assertContains(prompt, "lettuce")
        assertContains(prompt, "tomatoes")
    }

    @Test
    fun `prompt includes cinematic and food photography cues`() {
        val recipe = RecipeData(id = "r1", title = "Any Dish")

        val prompt = builder.buildPrompt(recipe)

        assertTrue(prompt.contains("cinematic", ignoreCase = true) || prompt.contains("Cinematic"))
        assertTrue(prompt.contains("16:9"))
    }

    @Test
    fun `prompt specifies no text overlays and no hands`() {
        val recipe = RecipeData(id = "r1", title = "Any Dish")

        val prompt = builder.buildPrompt(recipe)

        assertContains(prompt, "No text")
        assertContains(prompt, "no human hands")
    }
}
