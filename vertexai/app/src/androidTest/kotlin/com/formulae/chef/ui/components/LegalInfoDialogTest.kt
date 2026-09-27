package com.formulae.chef.ui.components

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class LegalInfoDialogTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun closeButtonIsShownAndDismissesDialog() {
        var dismissCount = 0
        composeTestRule.setContent {
            LegalInfoDialog(onDismiss = { dismissCount++ })
        }

        composeTestRule.onNodeWithText("Privacy & AI content").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Close").assertIsDisplayed().performClick()

        assertEquals(1, dismissCount)
    }

    @Test
    fun noBottomConfirmButtonIsShown() {
        composeTestRule.setContent {
            LegalInfoDialog(onDismiss = {})
        }

        composeTestRule.onNodeWithText("OK").assertDoesNotExist()
    }
}
