package com.formulae.chef.ui.components

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class AccountMenuTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun menuItemsAreHiddenUntilOverflowIsOpened() {
        composeTestRule.setContent {
            AccountMenu(onSignOut = {}, onDeleteAccount = {})
        }

        composeTestRule.onNodeWithText("Sign out").assertDoesNotExist()
        composeTestRule.onNodeWithText("Delete account").assertDoesNotExist()

        composeTestRule.onNodeWithContentDescription("Account options").performClick()

        composeTestRule.onNodeWithText("Sign out").assertIsDisplayed()
        composeTestRule.onNodeWithText("Delete account").assertIsDisplayed()
    }

    @Test
    fun deleteAccountItemInvokesCallback() {
        var deleteCount = 0
        var signOutCount = 0
        composeTestRule.setContent {
            AccountMenu(onSignOut = { signOutCount++ }, onDeleteAccount = { deleteCount++ })
        }

        composeTestRule.onNodeWithContentDescription("Account options").performClick()
        composeTestRule.onNodeWithText("Delete account").performClick()

        assertEquals(1, deleteCount)
        assertEquals(0, signOutCount)
        composeTestRule.onNodeWithText("Delete account").assertDoesNotExist()
    }

    @Test
    fun signOutItemInvokesCallback() {
        var signOutCount = 0
        composeTestRule.setContent {
            AccountMenu(onSignOut = { signOutCount++ }, onDeleteAccount = {})
        }

        composeTestRule.onNodeWithContentDescription("Account options").performClick()
        composeTestRule.onNodeWithText("Sign out").performClick()

        assertEquals(1, signOutCount)
    }

    @Test
    fun deleteAccountIsNotOfferedWithoutCallback() {
        composeTestRule.setContent {
            AccountMenu(onSignOut = {}, onDeleteAccount = null)
        }

        composeTestRule.onNodeWithContentDescription("Account options").performClick()

        composeTestRule.onNodeWithText("Sign out").assertIsDisplayed()
        composeTestRule.onNodeWithText("Delete account").assertDoesNotExist()
    }
}
