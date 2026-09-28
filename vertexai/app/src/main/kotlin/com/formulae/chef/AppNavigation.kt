package com.formulae.chef

import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.formulae.chef.feature.chat.ui.ChatRoute
import com.formulae.chef.feature.collection.ui.CollectionRoute
import com.formulae.chef.feature.collection.ui.RecipeSource
import com.formulae.chef.feature.home.HomeScreenViewModel
import com.formulae.chef.feature.useraccount.ui.SignInRoute
import com.formulae.chef.services.authentication.UserSessionService
import com.formulae.chef.services.persistence.RecipeListRepository
import com.formulae.chef.services.persistence.RecipeRepository
import com.formulae.chef.services.persistence.RecipeVariantRepository
import com.formulae.chef.ui.components.ChefNavigationBar
import java.net.URLEncoder

private val bottomBarRoutes = setOf(ChefRoutes.HOME, ChefRoutes.GENERATE, ChefRoutes.COLLECTION)

internal const val CHAT_RECIPE_ID_ARG = "recipeId"

/** Route pattern for the chat destination; [CHAT_RECIPE_ID_ARG] is optional (issue #60). */
internal const val CHAT_ROUTE_PATTERN = "generate?$CHAT_RECIPE_ID_ARG={$CHAT_RECIPE_ID_ARG}"

/**
 * Chat route that primes the conversation with the given recipe's context, used when opening
 * chat from the Recipe screen's "Chat with Chef anytime!" link (issue #60).
 */
fun chatRouteForRecipe(recipeId: String): String =
    "generate?$CHAT_RECIPE_ID_ARG=${URLEncoder.encode(recipeId, Charsets.UTF_8.name())}"

@Composable
fun AppNavigation(
    recipeRepository: RecipeRepository,
    recipeListRepository: RecipeListRepository,
    recipeVariantRepository: RecipeVariantRepository,
    userSessionService: UserSessionService
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentBaseRoute = (navBackStackEntry?.destination?.route ?: ChefRoutes.HOME).substringBefore("?")

    Scaffold(
        bottomBar = {
            if (currentBaseRoute in bottomBarRoutes) {
                ChefNavigationBar(
                    currentRoute = currentBaseRoute,
                    onNavigate = { route -> navController.navigateToTab(route) }
                )
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = ChefRoutes.HOME,
            // consumeWindowInsets tells descendants that the Scaffold already accounted for these
            // insets (bottom bar + system bars), so a screen-level Modifier.imePadding() only adds
            // the keyboard height *beyond* what is already reserved instead of stacking on top.
            modifier = Modifier
                .padding(innerPadding)
                .consumeWindowInsets(innerPadding)
        ) {
            composable(ChefRoutes.HOME) {
                val homeViewModel: HomeScreenViewModel = viewModel(
                    factory = HomeScreenViewModelFactory(recipeRepository)
                )
                HomeScreen(
                    viewModel = homeViewModel,
                    userSessionService = userSessionService,
                    onNavigateToCollection = {
                        navController.navigateToCollection(tab = RecipeSource.SAVED.name)
                    },
                    onNavigateToCommunity = {
                        navController.navigateToCollection(tab = RecipeSource.COMMUNITY.name)
                    },
                    // Recipes live in the Collections tab: open the detail there instead of on
                    // top of Home, so the bottom bar reflects where the user is (#56).
                    onOpenRecipe = { recipeId -> navController.navigateToCollection(recipeId = recipeId) },
                    onSignOut = {
                        userSessionService.signOut()
                        navController.navigate(ChefRoutes.SIGN_IN) {
                            popUpTo(ChefRoutes.HOME) { inclusive = true }
                        }
                        // Bottom-nav tab switches save ChatViewModel/CollectionViewModel state
                        // (saveState = true below) so it survives tab switches. That saved state
                        // is keyed independently of the back stack, so popping "home" alone does
                        // not drop it — without this, a new sign-in would restore the previous
                        // account's cached chat history and uid-bound Firebase repositories.
                        navController.clearBackStack(CHAT_ROUTE_PATTERN)
                        navController.clearBackStack(ChefRoutes.COLLECTION_PATTERN)
                    }
                )
            }
            composable(
                route = CHAT_ROUTE_PATTERN,
                arguments = listOf(
                    navArgument(CHAT_RECIPE_ID_ARG) {
                        type = NavType.StringType
                        nullable = true
                        defaultValue = null
                    }
                )
            ) { backStackEntry ->
                ChatRoute(
                    userSessionService = userSessionService,
                    recipeContextId = backStackEntry.arguments?.getString(CHAT_RECIPE_ID_ARG)
                )
            }
            composable(
                route = ChefRoutes.COLLECTION_PATTERN,
                arguments = listOf(
                    navArgument(ChefRoutes.ARG_TAB) {
                        type = NavType.StringType
                        defaultValue = RecipeSource.SAVED.name
                    },
                    navArgument(ChefRoutes.ARG_RECIPE_ID) {
                        type = NavType.StringType
                        nullable = true
                        defaultValue = null
                    }
                )
            ) { backStackEntry ->
                val tab = RecipeSource.valueOf(
                    backStackEntry.arguments?.getString(ChefRoutes.ARG_TAB) ?: RecipeSource.SAVED.name
                )
                val initialRecipeId = backStackEntry.arguments?.getString(ChefRoutes.ARG_RECIPE_ID)
                CollectionRoute(
                    repository = recipeRepository,
                    listRepository = recipeListRepository,
                    collectionViewModel = viewModel(
                        factory = CollectionViewModelFactory(
                            recipeRepository,
                            recipeListRepository,
                            recipeVariantRepository
                        )
                    ),
                    navController = navController,
                    userSessionService = userSessionService,
                    initialRecipeSource = tab,
                    initialRecipeId = initialRecipeId
                )
            }
            composable(ChefRoutes.SIGN_IN) {
                SignInRoute(userSessionService, navController)
            }
        }
    }
}
