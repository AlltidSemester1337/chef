package com.formulae.chef.feature.collection.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class ServingsStepperTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun showsServingsAndReportsIncrementAndDecrement() {
        val changes = mutableListOf<Int>()
        composeTestRule.setContent {
            ServingsStepper(servings = 4, maxServings = 30, onServingsChanged = { changes += it })
        }

        composeTestRule.onNodeWithText("4 servings").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Increase servings").performClick()
        composeTestRule.onNodeWithContentDescription("Decrease servings").performClick()

        assertEquals(listOf(5, 3), changes)
    }

    @Test
    fun decreaseIsDisabledAtOneServing() {
        composeTestRule.setContent {
            ServingsStepper(servings = 1, maxServings = 30, onServingsChanged = {})
        }

        composeTestRule.onNodeWithText("1 serving").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Decrease servings").assertIsNotEnabled()
        composeTestRule.onNodeWithContentDescription("Increase servings").assertIsEnabled()
    }

    @Test
    fun increaseIsDisabledAtMaxServings() {
        composeTestRule.setContent {
            ServingsStepper(servings = 30, maxServings = 30, onServingsChanged = {})
        }

        composeTestRule.onNodeWithContentDescription("Increase servings").assertIsNotEnabled()
    }
}
