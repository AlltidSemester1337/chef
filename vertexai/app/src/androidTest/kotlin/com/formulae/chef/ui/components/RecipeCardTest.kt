package com.formulae.chef.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.height
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class RecipeCardTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun cardsWithShortAndLongTitlesHaveTheSameHeight() {
        composeTestRule.setContent {
            Row {
                RecipeCard(
                    title = "Soup",
                    imageUrl = null,
                    onClick = {},
                    modifier = Modifier
                        .width(160.dp)
                        .testTag("short")
                )
                RecipeCard(
                    title = "Slow-Braised Moroccan Lamb Shoulder with Apricots, Chickpeas, " +
                        "Preserved Lemon and a Fragrant Herb Couscous on the Side",
                    imageUrl = null,
                    onClick = {},
                    modifier = Modifier
                        .width(160.dp)
                        .testTag("long")
                )
            }
        }

        val shortHeight = composeTestRule.onNodeWithTag("short").getBoundsInRoot().height
        val longHeight = composeTestRule.onNodeWithTag("long").getBoundsInRoot().height

        assertEquals(longHeight, shortHeight)
    }
}
