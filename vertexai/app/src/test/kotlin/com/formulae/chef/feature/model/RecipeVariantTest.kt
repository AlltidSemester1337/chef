package com.formulae.chef.feature.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test

class RecipeVariantTest {

    @Test
    fun `default construction has expected defaults`() {
        val variant = RecipeVariant()
        assertNull(variant.id)
        assertEquals("", variant.label)
        assertEquals("", variant.createdAt)
        assertFalse(variant.isPinned)
        assertEquals("", variant.title)
        assertEquals("", variant.summary)
        assertEquals("", variant.servings)
        assertNull(variant.prepTime)
        assertNull(variant.cookingTime)
        assertEquals(emptyList<Nutrient>(), variant.nutrientsPerServing)
        assertEquals(emptyList<Ingredient>(), variant.ingredients)
        assertEquals(Difficulty.EASY, variant.difficulty)
        assertEquals(emptyList<String>(), variant.instructions)
        assertNull(variant.tipsAndTricks)
    }

    @Test
    fun `isPinned can be set to true`() {
        val variant = RecipeVariant(isPinned = true)
        assert(variant.isPinned)
    }

    @Test
    fun `copy preserves all fields`() {
        val original = RecipeVariant(
            id = "v1",
            label = "Vegetarian",
            createdAt = "2026-01-01T00:00:00Z",
            isPinned = true,
            title = "Veggie Stew",
            summary = "A hearty stew",
            servings = "4 servings",
            prepTime = "10 minutes",
            cookingTime = "30 minutes",
            ingredients = listOf(Ingredient(name = "Carrot", quantity = "2", unit = "pcs")),
            instructions = listOf("Chop veggies", "Simmer"),
            difficulty = Difficulty.MEDIUM,
            tipsAndTricks = "Use fresh herbs"
        )
        val copied = original.copy(label = "Updated")
        assertEquals("v1", copied.id)
        assertEquals("Updated", copied.label)
        assertEquals(true, copied.isPinned)
        assertEquals(Difficulty.MEDIUM, copied.difficulty)
        assertEquals("Veggie Stew", copied.title)
    }

    @Test
    fun `difficulty defaults to EASY`() {
        val variant = RecipeVariant()
        assertEquals(Difficulty.EASY, variant.difficulty)
    }
}
