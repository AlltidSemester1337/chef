package com.formulae.chef

import androidx.compose.foundation.layout.Column
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.formulae.chef.feature.model.RecipeOfTheMonth
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class RecipeOfTheMonthSectionTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val rotw = RecipeOfTheMonth(
        id = "rotm-1",
        recipeId = "recipe-1",
        recipeTitle = "Swedish Meatballs",
        videoUrl = "https://example.invalid/video.mp4"
    )

    @Test
    fun sectionTitleIsShownOnlyOnce() {
        composeTestRule.setContent {
            Column { RecipeOfTheMonthSection(rotw = rotw, onViewRecipe = {}) }
        }

        composeTestRule.onAllNodesWithText("Recipe of the Month", substring = true).assertCountEquals(1)
        composeTestRule.onNodeWithText("🎬 Recipe of the Month").assertDoesNotExist()
    }

    @Test
    fun clickingRecipeTitleOpensRecipe() {
        var clicks = 0
        composeTestRule.setContent {
            Column { RecipeOfTheMonthSection(rotw = rotw, onViewRecipe = { clicks++ }) }
        }

        composeTestRule.onNodeWithText("Swedish Meatballs").assertIsDisplayed().performClick()

        assertEquals(1, clicks)
    }

    @Test
    fun clickingViewRecipeLinkStillOpensRecipe() {
        var clicks = 0
        composeTestRule.setContent {
            Column { RecipeOfTheMonthSection(rotw = rotw, onViewRecipe = { clicks++ }) }
        }

        composeTestRule.onNodeWithText("View recipe").performClick()

        assertEquals(1, clicks)
    }
}
