package com.formulae.chef.feature.chat.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class ChatTopBarTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun closeButtonIsShownAndInvokesOnClose() {
        var closeCount = 0
        composeTestRule.setContent {
            ChatTopBar(onClose = { closeCount++ })
        }

        composeTestRule.onNodeWithContentDescription("Close chat").assertIsDisplayed().performClick()

        assertEquals(1, closeCount)
    }

    @Test
    fun infoIconOpensAndClosesDisclaimerDialog() {
        composeTestRule.setContent {
            ChatTopBar(onClose = {})
        }

        composeTestRule.onNodeWithText(CHAT_AI_DISCLAIMER).assertDoesNotExist()

        composeTestRule.onNodeWithContentDescription("About AI-generated recipes")
            .assertIsDisplayed()
            .performClick()
        composeTestRule.onNodeWithText(CHAT_AI_DISCLAIMER).assertIsDisplayed()

        composeTestRule.onNodeWithContentDescription("Close").performClick()
        composeTestRule.onNodeWithText(CHAT_AI_DISCLAIMER).assertDoesNotExist()
    }
}
