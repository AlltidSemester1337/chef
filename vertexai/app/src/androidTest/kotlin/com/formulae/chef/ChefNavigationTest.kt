package com.formulae.chef

import androidx.compose.material3.Text
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/**
 * Regression tests for #56: exercises [navigateToTab] / [navigateToCollection] against a real
 * NavHostController with the same route shapes as [AppNavigation], asserting the back stack stays
 * `[home]` or `[home, <tab>]` and that tapping Home always lands on Home.
 */
class ChefNavigationTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private lateinit var navController: NavHostController

    @Before
    fun setUp() {
        composeTestRule.setContent {
            navController = rememberNavController()
            NavHost(navController = navController, startDestination = ChefRoutes.HOME) {
                composable(ChefRoutes.HOME) { Text("home") }
                composable(ChefRoutes.GENERATE) { Text("generate") }
                composable(
                    route = ChefRoutes.COLLECTION_PATTERN,
                    arguments = listOf(
                        navArgument(ChefRoutes.ARG_TAB) {
                            type = NavType.StringType
                            defaultValue = "SAVED"
                        },
                        navArgument(ChefRoutes.ARG_RECIPE_ID) {
                            type = NavType.StringType
                            nullable = true
                            defaultValue = null
                        }
                    )
                ) { Text("collection") }
            }
        }
    }

    private fun act(block: NavHostController.() -> Unit) = composeTestRule.runOnIdle { navController.block() }

    private fun currentRoute(): String? = composeTestRule.runOnIdle {
        navController.currentBackStackEntry?.destination?.route
    }

    private fun previousRoute(): String? = composeTestRule.runOnIdle {
        navController.previousBackStackEntry?.destination?.route
    }

    private fun currentArg(name: String): String? = composeTestRule.runOnIdle {
        navController.currentBackStackEntry?.arguments?.getString(name)
    }

    @Test
    fun tappingRecipeOnHomeOpensItInCollectionsTab() {
        act { navigateToCollection(recipeId = "r1") }

        assertEquals(ChefRoutes.COLLECTION_PATTERN, currentRoute())
        assertEquals("r1", currentArg(ChefRoutes.ARG_RECIPE_ID))
        assertEquals(ChefRoutes.HOME, previousRoute())
    }

    @Test
    fun tappingHomeWhileViewingRecipeReturnsHome() {
        act { navigateToCollection(recipeId = "r1") }

        act { navigateToTab(ChefRoutes.HOME) }

        assertEquals(ChefRoutes.HOME, currentRoute())
        assertNull(previousRoute())
    }

    @Test
    fun tappingHomeFromChatOpenedViaRecipeReturnsHome() {
        act { navigateToCollection(recipeId = "r1") }
        // "Chat with Chef" on the recipe screen
        act { navigateToTab(ChefRoutes.GENERATE) }
        assertEquals(ChefRoutes.GENERATE, currentRoute())
        assertEquals(ChefRoutes.HOME, previousRoute())

        act { navigateToTab(ChefRoutes.HOME) }

        assertEquals(ChefRoutes.HOME, currentRoute())
        assertNull(previousRoute())
    }

    @Test
    fun collectionsTabRestoresRecipeNotChatAfterGoingHome() {
        act { navigateToCollection(recipeId = "r1") }
        act { navigateToTab(ChefRoutes.GENERATE) }
        act { navigateToTab(ChefRoutes.HOME) }

        act { navigateToTab(ChefRoutes.COLLECTION) }

        assertEquals(ChefRoutes.COLLECTION_PATTERN, currentRoute())
        assertEquals("r1", currentArg(ChefRoutes.ARG_RECIPE_ID))
        assertEquals(ChefRoutes.HOME, previousRoute())
    }

    @Test
    fun homeLinkToCommunityReplacesSavedCollectionsState() {
        act { navigateToCollection(recipeId = "r1") }
        act { navigateToTab(ChefRoutes.HOME) }

        act { navigateToCollection(tab = "COMMUNITY") }

        assertEquals(ChefRoutes.COLLECTION_PATTERN, currentRoute())
        assertEquals("COMMUNITY", currentArg(ChefRoutes.ARG_TAB))
        assertNull(currentArg(ChefRoutes.ARG_RECIPE_ID))
    }

    @Test
    fun homeTabAlwaysReturnsHomeAcrossRepeatedTabSwitches() {
        repeat(3) {
            act { navigateToTab(ChefRoutes.GENERATE) }
            act { navigateToTab(ChefRoutes.COLLECTION) }
            act { navigateToTab(ChefRoutes.HOME) }

            assertEquals(ChefRoutes.HOME, currentRoute())
            assertNull(previousRoute())
        }
    }
}
