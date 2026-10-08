package com.formulae.chef.rotw

import com.formulae.chef.rotw.model.RecipeOfTheMonthRecord
import com.formulae.chef.rotw.service.recipeDataFrom
import com.formulae.chef.rotw.service.recipeOfTheMonthData
import com.google.cloud.Timestamp
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class FirestoreMappingTest {

    @Test
    fun `recipeDataFrom maps title, favourite flag and ingredients`() {
        val data = mapOf(
            "title" to "Moussaka",
            "isFavourite" to true,
            "ingredients" to listOf(
                mapOf("name" to "lamm", "quantity" to "1/2", "unit" to "kg"),
                mapOf("name" to "salt", "quantity" to "", "unit" to "")
            )
        )

        val recipe = recipeDataFrom("r1", data)!!

        assertEquals("r1", recipe.id)
        assertEquals("Moussaka", recipe.title)
        assertTrue(recipe.isFavourite)
        assertEquals(listOf("lamm", "salt"), recipe.ingredients.map { it.name })
        assertEquals("1/2", recipe.ingredients[0].quantity)
    }

    @Test
    fun `recipeDataFrom tolerates missing ingredients and orphaned recipes`() {
        val recipe = recipeDataFrom("r2", mapOf("title" to "Soppa", "isFavourite" to true))!!
        assertTrue(recipe.ingredients.isEmpty())
    }

    @Test
    fun `recipeDataFrom skips documents without a title`() {
        assertNull(recipeDataFrom("r3", mapOf("isFavourite" to true)))
    }

    @Test
    fun `recipeOfTheMonthData stores createdAt as a Timestamp`() {
        val data = recipeOfTheMonthData(
            RecipeOfTheMonthRecord(
                recipeId = "r1",
                recipeTitle = "Moussaka",
                videoUrl = "https://v",
                monthOf = "2026-11",
                createdAt = "2026-11-01T22:00:05.123Z"
            )
        )

        val createdAt = data["createdAt"] as Timestamp
        assertEquals(1_793_570_405L, createdAt.seconds)
        assertEquals(123_000_000, createdAt.nanos)
        assertEquals("2026-11", data["monthOf"])
        assertEquals("r1", data["recipeId"])
    }
}
