package com.formulae.chef.feature.model

import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Test

class IngredientScalerTest {

    private fun scale(quantity: String?, multiplier: Double) = IngredientScaler.scaleQuantity(quantity, multiplier)

    // --- multiplier ---

    @Test
    fun `multiplier is ratio of target to original servings`() {
        assertEquals(2.0, IngredientScaler.multiplier(4, 8), 0.0)
        assertEquals(0.5, IngredientScaler.multiplier(4, 2), 0.0)
        assertEquals(1.5, IngredientScaler.multiplier(2, 3), 0.0)
    }

    @Test
    fun `multiplier is 1 when servings are unknown or invalid`() {
        assertEquals(1.0, IngredientScaler.multiplier(null, 8), 0.0)
        assertEquals(1.0, IngredientScaler.multiplier(4, null), 0.0)
        assertEquals(1.0, IngredientScaler.multiplier(0, 8), 0.0)
        assertEquals(1.0, IngredientScaler.multiplier(4, 0), 0.0)
    }

    // --- integers and decimals ---

    @Test
    fun `scales whole numbers`() {
        assertEquals("1000", scale("500", 2.0))
        assertEquals("250", scale("500", 0.5))
        assertEquals("3", scale("2", 1.5))
    }

    @Test
    fun `scales decimals and formats without trailing zeros`() {
        assertEquals("1", scale("0.5", 2.0))
        assertEquals("0.75", scale("0.5", 1.5))
        assertEquals("0.13", scale("0.25", 0.5))
    }

    @Test
    fun `accepts comma as decimal separator`() {
        assertEquals("3", scale("1,5", 2.0))
    }

    @Test
    fun `rounds large amounts to sensible precision`() {
        assertEquals("333", scale("500", 2.0 / 3.0))
        assertEquals("12.5", scale("25", 0.5))
        assertEquals("1.33", scale("2", 2.0 / 3.0))
    }

    @Test
    fun `never rounds a positive amount down to zero`() {
        assertEquals("0.01", scale("0.01", 0.25))
    }

    @Test
    fun `formatting is locale independent`() {
        val original = Locale.getDefault()
        try {
            Locale.setDefault(Locale("sv", "SE"))
            assertEquals("0.75", scale("0.5", 1.5))
        } finally {
            Locale.setDefault(original)
        }
    }

    // --- fractions ---

    @Test
    fun `scales simple fractions and keeps fraction notation`() {
        assertEquals("1", scale("1/2", 2.0))
        assertEquals("1/4", scale("1/2", 0.5))
        assertEquals("3/4", scale("1/2", 1.5))
        assertEquals("1 1/2", scale("3/4", 2.0))
    }

    @Test
    fun `scales mixed numbers`() {
        assertEquals("3", scale("1 1/2", 2.0))
        assertEquals("3/4", scale("1 1/2", 0.5))
    }

    @Test
    fun `scales unicode fractions`() {
        assertEquals("1", scale("½", 2.0))
        assertEquals("3", scale("1½", 2.0))
        assertEquals("2/3", scale("⅓", 2.0))
    }

    @Test
    fun `falls back to decimals when fraction is not kitchen friendly`() {
        assertEquals("0.2", scale("1/2", 0.4))
    }

    // --- ranges and suffixes ---

    @Test
    fun `scales both ends of a range and keeps separator`() {
        assertEquals("4-6", scale("2-3", 2.0))
        assertEquals("4 – 6", scale("2 – 3", 2.0))
        assertEquals("4 to 6", scale("2 to 3", 2.0))
    }

    @Test
    fun `scales leading amount and preserves trailing text`() {
        assertEquals("2 (400g)", scale("1 (400g)", 2.0))
        assertEquals("2 large", scale("1 large", 2.0))
    }

    // --- non-scalable ---

    @Test
    fun `leaves non numeric quantities unchanged`() {
        assertEquals("to taste", scale("to taste", 2.0))
        assertEquals("a pinch", scale("a pinch", 2.0))
        assertEquals("", scale("", 2.0))
        assertNull(scale(null, 2.0))
    }

    @Test
    fun `leaves malformed numbers unchanged`() {
        assertEquals("1.2.3", scale("1.2.3", 2.0))
        assertEquals("1/0", scale("1/0", 2.0))
    }

    @Test
    fun `multiplier of 1 returns quantity verbatim`() {
        assertEquals("0.50", scale("0.50", 1.0))
        assertEquals("1 1/2", scale("1 1/2", 1.0))
    }

    // --- ingredient lists and recipes ---

    @Test
    fun `scaleIngredients scales quantities only`() {
        val ingredients = listOf(
            Ingredient(name = "ground lamb", quantity = "500", unit = "g"),
            Ingredient(name = "salt", quantity = "to taste", unit = "")
        )

        val scaled = IngredientScaler.scaleIngredients(ingredients, 2.0)

        assertEquals(Ingredient(name = "ground lamb", quantity = "1000", unit = "g"), scaled[0])
        assertEquals(Ingredient(name = "salt", quantity = "to taste", unit = ""), scaled[1])
        // Source list is not mutated.
        assertEquals("500", ingredients[0].quantity)
    }

    @Test
    fun `scaledToServings scales from recipe servings to target`() {
        val recipe = Recipe(
            servings = "4 servings",
            ingredients = listOf(Ingredient(name = "parsley", quantity = "1/2", unit = "cup"))
        )

        val scaled = recipe.scaledToServings(6)

        assertEquals("3/4", scaled.ingredients[0].quantity)
        assertEquals("4 servings", scaled.servings)
        assertEquals("1/2", recipe.ingredients[0].quantity)
    }

    @Test
    fun `scaledToServings returns same recipe when nothing to scale`() {
        val recipe = Recipe(servings = "4 servings", ingredients = listOf(Ingredient(quantity = "1")))
        assertSame(recipe, recipe.scaledToServings(null))
        assertSame(recipe, recipe.scaledToServings(4))

        val unknownServings = Recipe(servings = "", ingredients = listOf(Ingredient(quantity = "1")))
        assertSame(unknownServings, unknownServings.scaledToServings(8))
    }
}
