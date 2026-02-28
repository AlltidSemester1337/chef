package com.formulae.chef.feature.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RecipesTest {

    @Test
    fun `default recipes has empty list`() {
        val recipes = Recipes()

        assertTrue(recipes.recipes.isEmpty())
    }

    @Test
    fun `recipes with multiple items`() {
        val recipeList = listOf(
            Recipe(id = "1", title = "Cake"),
            Recipe(id = "2", title = "Pie"),
            Recipe(id = "3", title = "Bread")
        )

        val recipes = Recipes(recipes = recipeList)

        assertEquals(3, recipes.recipes.size)
        assertEquals("Cake", recipes.recipes[0].title)
        assertEquals("Pie", recipes.recipes[1].title)
        assertEquals("Bread", recipes.recipes[2].title)
    }

    @Test
    fun `recipes data class equality`() {
        val list = listOf(Recipe(id = "1", title = "Cake"))
        val a = Recipes(recipes = list)
        val b = Recipes(recipes = list)

        assertEquals(a, b)
    }
}
