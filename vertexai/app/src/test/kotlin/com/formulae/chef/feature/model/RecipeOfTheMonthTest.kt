package com.formulae.chef.feature.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RecipeOfTheMonthTest {

    @Test
    fun `default RecipeOfTheMonth has expected default values`() {
        val rotw = RecipeOfTheMonth()

        assertNull(rotw.id)
        assertEquals("", rotw.recipeId)
        assertEquals("", rotw.recipeTitle)
        assertEquals("", rotw.videoUrl)
        assertEquals("", rotw.monthOf)
        assertEquals("", rotw.createdAt)
    }

    @Test
    fun `RecipeOfTheMonth with all fields populated`() {
        val rotw = RecipeOfTheMonth(
            id = "push-id-1",
            recipeId = "recipe-abc",
            recipeTitle = "West African Peanut Stew",
            videoUrl = "https://storage.googleapis.com/bucket/videos/rotw/2026-04.mp4",
            monthOf = "2026-04",
            createdAt = "2026-04-06T22:00:00Z"
        )

        assertEquals("push-id-1", rotw.id)
        assertEquals("recipe-abc", rotw.recipeId)
        assertEquals("West African Peanut Stew", rotw.recipeTitle)
        assertEquals("https://storage.googleapis.com/bucket/videos/rotw/2026-04.mp4", rotw.videoUrl)
        assertEquals("2026-04", rotw.monthOf)
        assertEquals("2026-04-06T22:00:00Z", rotw.createdAt)
    }

    @Test
    fun `data class equality works correctly`() {
        val a = RecipeOfTheMonth(recipeId = "abc", monthOf = "2026-04")
        val b = RecipeOfTheMonth(recipeId = "abc", monthOf = "2026-04")

        assertEquals(a, b)
    }

    @Test
    fun `videoUrl empty string indicates no video available`() {
        val rotw = RecipeOfTheMonth(recipeId = "abc", videoUrl = "")

        assertTrue(rotw.videoUrl.isEmpty())
    }
}
