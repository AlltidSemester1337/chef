package com.formulae.chef.feature.collection.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.formulae.chef.feature.model.Recipe
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class CookingModeTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val recipe = Recipe(
        title = "Test recipe",
        servings = "2 servings",
        instructions = listOf("Chop the onion.", "Fry the onion.")
    )

    @Test
    fun tappingStepTextTogglesStepCompletion() {
        val checkedEvents = mutableListOf<Int>()
        val uncheckedEvents = mutableListOf<Int>()
        composeTestRule.setContent {
            var checkedSteps by remember { mutableStateOf(emptySet<Int>()) }
            val scrollState = rememberScrollState()
            Column(modifier = Modifier.verticalScroll(scrollState)) {
                CookingModeContent(
                    recipe = recipe,
                    showIngredients = false,
                    checkedSteps = checkedSteps,
                    scrollState = scrollState,
                    onStepChecked = {
                        checkedEvents += it
                        checkedSteps = checkedSteps + it
                    },
                    onStepUnchecked = {
                        uncheckedEvents += it
                        checkedSteps = checkedSteps - it
                    }
                )
            }
        }

        val step = composeTestRule.onNodeWithText("2. Fry the onion.")
        step.assertIsOff()

        step.performClick()
        composeTestRule.waitForIdle()
        step.assertIsOn()
        assertEquals(listOf(1), checkedEvents)

        step.performClick()
        composeTestRule.waitForIdle()
        step.assertIsOff()
        assertEquals(listOf(1), uncheckedEvents)
    }

    @Test
    fun closeButtonShowsLabelAndInvokesOnClose() {
        var closeCount = 0
        composeTestRule.setContent {
            CookingModeCloseButton(onClose = { closeCount++ })
        }

        composeTestRule.onNodeWithText("Close cooking mode").assertIsDisplayed().performClick()

        assertEquals(1, closeCount)
    }
}
