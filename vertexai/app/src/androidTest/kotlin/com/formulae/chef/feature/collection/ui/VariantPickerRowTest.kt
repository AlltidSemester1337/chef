package com.formulae.chef.feature.collection.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.formulae.chef.feature.model.RecipeVariant
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class VariantPickerRowTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val variants = listOf(RecipeVariant(id = "v1", label = "Vegan"))

    @Test
    fun nonOwnerCanBrowseVariantsButHasNoCreatePinOrDeleteActions() {
        val selected = mutableListOf<String?>()
        composeTestRule.setContent {
            VariantPickerRow(
                variants = variants,
                selectedVariantId = null,
                isOwner = false,
                onVariantSelected = { selected.add(it) },
                onPinVariant = {},
                onDeleteVariant = {},
                onCreateVariant = {}
            )
        }

        composeTestRule.onNodeWithContentDescription("Create variant").assertDoesNotExist()

        composeTestRule.onNodeWithText("Default").performClick()
        composeTestRule.onNodeWithText("Vegan").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Pin default").assertDoesNotExist()
        composeTestRule.onNodeWithContentDescription("Pin this variant").assertDoesNotExist()
        composeTestRule.onNodeWithContentDescription("Delete variant").assertDoesNotExist()

        composeTestRule.onNodeWithText("Vegan").performClick()
        assertEquals(listOf<String?>("v1"), selected)
    }

    @Test
    fun ownerWithHandlerSeesCreateButtonAndItInvokesHandler() {
        var createCount = 0
        composeTestRule.setContent {
            VariantPickerRow(
                variants = variants,
                selectedVariantId = null,
                isOwner = true,
                onVariantSelected = {},
                onPinVariant = {},
                onDeleteVariant = {},
                onCreateVariant = { createCount++ }
            )
        }

        composeTestRule.onNodeWithContentDescription("Create variant").assertIsDisplayed().performClick()
        assertEquals(1, createCount)

        composeTestRule.onNodeWithText("Default").performClick()
        composeTestRule.onNodeWithContentDescription("Pin this variant").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Delete variant").assertIsDisplayed()
    }

    @Test
    fun ownerWithoutHandlerSeesNoCreateButton() {
        composeTestRule.setContent {
            VariantPickerRow(
                variants = variants,
                selectedVariantId = null,
                isOwner = true,
                onVariantSelected = {},
                onPinVariant = {},
                onDeleteVariant = {},
                onCreateVariant = null
            )
        }

        composeTestRule.onNodeWithContentDescription("Create variant").assertDoesNotExist()
    }
}
