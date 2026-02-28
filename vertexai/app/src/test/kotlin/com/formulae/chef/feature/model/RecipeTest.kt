package com.formulae.chef.feature.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RecipeTest {

    @Test
    fun `default recipe has expected default values`() {
        val recipe = Recipe()

        assertNull(recipe.id)
        assertEquals("", recipe.uid)
        assertEquals("", recipe.title)
        assertEquals("", recipe.summary)
        assertEquals("", recipe.servings)
        assertNull(recipe.prepTime)
        assertNull(recipe.cookingTime)
        assertNotNull(recipe.nutrientsPerServing)
        assertTrue(recipe.nutrientsPerServing!!.isEmpty())
        assertTrue(recipe.ingredients.isEmpty())
        assertEquals(Difficulty.EASY, recipe.difficulty)
        assertTrue(recipe.instructions.isEmpty())
        assertNull(recipe.tipsAndTricks)
        assertNull(recipe.imageUrl)
        assertEquals("", recipe.updatedAt)
        assertFalse(recipe.isFavourite)
        assertNull(recipe.copyId)
    }

    @Test
    fun `recipe with all fields populated`() {
        val ingredients = listOf(
            Ingredient("Flour", "200", "g"),
            Ingredient("Sugar", "100", "g")
        )
        val nutrients = listOf(
            Nutrient("Calories", "350", "kcal"),
            Nutrient("Protein", "8", "g")
        )
        val instructions = listOf("Mix ingredients", "Bake at 180C")

        val recipe = Recipe(
            id = "recipe-1",
            uid = "user-1",
            title = "Test Cake",
            summary = "A delicious test cake",
            servings = "4",
            prepTime = "15 min",
            cookingTime = "30 min",
            nutrientsPerServing = nutrients,
            ingredients = ingredients,
            difficulty = Difficulty.MEDIUM,
            instructions = instructions,
            tipsAndTricks = "Use room temperature butter",
            imageUrl = "https://example.com/cake.jpg",
            updatedAt = "2026-01-01T00:00:00Z",
            isFavourite = true,
            copyId = "copy-1"
        )

        assertEquals("recipe-1", recipe.id)
        assertEquals("user-1", recipe.uid)
        assertEquals("Test Cake", recipe.title)
        assertEquals("A delicious test cake", recipe.summary)
        assertEquals("4", recipe.servings)
        assertEquals("15 min", recipe.prepTime)
        assertEquals("30 min", recipe.cookingTime)
        assertEquals(2, recipe.nutrientsPerServing!!.size)
        assertEquals(2, recipe.ingredients.size)
        assertEquals(Difficulty.MEDIUM, recipe.difficulty)
        assertEquals(2, recipe.instructions.size)
        assertEquals("Use room temperature butter", recipe.tipsAndTricks)
        assertEquals("https://example.com/cake.jpg", recipe.imageUrl)
        assertEquals("2026-01-01T00:00:00Z", recipe.updatedAt)
        assertTrue(recipe.isFavourite)
        assertEquals("copy-1", recipe.copyId)
    }

    @Test
    fun `copyOf returns recipe with overridden fields`() {
        val original = Recipe(
            id = "1",
            uid = "user-1",
            title = "Original",
            summary = "Original summary",
            isFavourite = false
        )

        val copy = original.copyOf(
            title = "Modified Title",
            isFavourite = true,
            imageUrl = "https://example.com/new.jpg"
        )

        assertEquals("1", copy.id)
        assertEquals("user-1", copy.uid)
        assertEquals("Modified Title", copy.title)
        assertEquals("Original summary", copy.summary)
        assertTrue(copy.isFavourite)
        assertEquals("https://example.com/new.jpg", copy.imageUrl)
    }

    @Test
    fun `copyOf preserves all original fields when no overrides`() {
        val original = Recipe(
            id = "1",
            uid = "user-1",
            title = "Title",
            summary = "Summary",
            servings = "2",
            prepTime = "10 min",
            cookingTime = "20 min",
            difficulty = Difficulty.HARD,
            isFavourite = true,
            updatedAt = "2026-01-01"
        )

        val copy = original.copyOf()

        assertEquals(original.id, copy.id)
        assertEquals(original.uid, copy.uid)
        assertEquals(original.title, copy.title)
        assertEquals(original.summary, copy.summary)
        assertEquals(original.servings, copy.servings)
        assertEquals(original.prepTime, copy.prepTime)
        assertEquals(original.cookingTime, copy.cookingTime)
        assertEquals(original.difficulty, copy.difficulty)
        assertEquals(original.isFavourite, copy.isFavourite)
        assertEquals(original.updatedAt, copy.updatedAt)
    }

    @Test
    fun `data class equality works correctly`() {
        val recipe1 = Recipe(id = "1", title = "Cake")
        val recipe2 = Recipe(id = "1", title = "Cake")

        assertEquals(recipe1, recipe2)
    }

    @Test
    fun `data class inequality for different values`() {
        val recipe1 = Recipe(id = "1", title = "Cake")
        val recipe2 = Recipe(id = "2", title = "Pie")

        assertFalse(recipe1 == recipe2)
    }
}

class IngredientTest {

    @Test
    fun `default ingredient has empty strings`() {
        val ingredient = Ingredient()

        assertEquals("", ingredient.name)
        assertEquals("", ingredient.quantity)
        assertEquals("", ingredient.unit)
    }

    @Test
    fun `ingredient with values`() {
        val ingredient = Ingredient("Flour", "200", "g")

        assertEquals("Flour", ingredient.name)
        assertEquals("200", ingredient.quantity)
        assertEquals("g", ingredient.unit)
    }

    @Test
    fun `ingredient equality`() {
        val a = Ingredient("Salt", "1", "tsp")
        val b = Ingredient("Salt", "1", "tsp")

        assertEquals(a, b)
    }
}

class NutrientTest {

    @Test
    fun `default nutrient has empty strings`() {
        val nutrient = Nutrient()

        assertEquals("", nutrient.name)
        assertEquals("", nutrient.quantity)
        assertEquals("", nutrient.unit)
    }

    @Test
    fun `nutrient with values`() {
        val nutrient = Nutrient("Protein", "25", "g")

        assertEquals("Protein", nutrient.name)
        assertEquals("25", nutrient.quantity)
        assertEquals("g", nutrient.unit)
    }
}

class DifficultyTest {

    @Test
    fun `difficulty enum has three values`() {
        val values = Difficulty.entries

        assertEquals(3, values.size)
        assertTrue(values.contains(Difficulty.EASY))
        assertTrue(values.contains(Difficulty.MEDIUM))
        assertTrue(values.contains(Difficulty.HARD))
    }

    @Test
    fun `difficulty valueOf works`() {
        assertEquals(Difficulty.EASY, Difficulty.valueOf("EASY"))
        assertEquals(Difficulty.MEDIUM, Difficulty.valueOf("MEDIUM"))
        assertEquals(Difficulty.HARD, Difficulty.valueOf("HARD"))
    }
}
