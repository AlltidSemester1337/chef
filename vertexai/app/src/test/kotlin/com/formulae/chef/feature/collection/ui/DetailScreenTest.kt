package com.formulae.chef.feature.collection.ui

import com.formulae.chef.feature.model.Difficulty
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DetailScreenTest {

    @Test
    fun `formatPrepCookTime combines both when present`() {
        assertEquals(
            "Prep 20 mins, cook 2 hours",
            formatPrepCookTime(prepTime = "20 mins", cookingTime = "2 hours")
        )
    }

    @Test
    fun `formatPrepCookTime uses prep only when cook time missing`() {
        assertEquals("Prep 20 mins", formatPrepCookTime(prepTime = "20 mins", cookingTime = null))
    }

    @Test
    fun `formatPrepCookTime uses cook only when prep time missing`() {
        assertEquals("Cook 2 hours", formatPrepCookTime(prepTime = null, cookingTime = "2 hours"))
    }

    @Test
    fun `formatPrepCookTime returns null when both blank or null`() {
        assertNull(formatPrepCookTime(prepTime = null, cookingTime = null))
        assertNull(formatPrepCookTime(prepTime = "  ", cookingTime = ""))
    }

    @Test
    fun `formatDifficultyLabel capitalizes each enum value`() {
        assertEquals("Easy", formatDifficultyLabel(Difficulty.EASY))
        assertEquals("Medium", formatDifficultyLabel(Difficulty.MEDIUM))
        assertEquals("Hard", formatDifficultyLabel(Difficulty.HARD))
    }

    @Test
    fun `formatDifficultyLabel returns null for null difficulty`() {
        assertNull(formatDifficultyLabel(null))
    }
}
