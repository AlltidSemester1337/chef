package com.formulae.chef

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
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

@Composable
fun AppNavigation(
    recipeRepository: RecipeRepository,
    recipeListRepository: RecipeListRepository,
    recipeVariantRepository: RecipeVariantRepository,
    userSessionService: UserSessionService
) {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = "home") {
        composable("home") {
            val homeViewModel: HomeScreenViewModel = viewModel(
                factory = HomeScreenViewModelFactory(recipeRepository)
            )
            HomeScreen(
                viewModel = homeViewModel,
                userSessionService = userSessionService,
                onNavigateToChat = { navController.navigate("chat") },
                onNavigateToCollection = { navController.navigate("collection") },
                onNavigateToCommunity = {
                    navController.navigate("collection?tab=${RecipeSource.ALL_RECIPES.name}")
                },
                onSignOut = {
                    userSessionService.signOut()
                    navController.navigate("signIn") {
                        popUpTo("home") { inclusive = true }
                    }
                }
            )
        }
        composable("chat") {
            ChatRoute()
        }
        composable(
            route = "collection?tab={tab}",
            arguments = listOf(
                navArgument("tab") {
                    type = NavType.StringType
                    defaultValue = RecipeSource.USER_FAVOURITES.name
                }
            )
        ) { backStackEntry ->
            val tab = RecipeSource.valueOf(
                backStackEntry.arguments?.getString("tab") ?: RecipeSource.USER_FAVOURITES.name
            )
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
                initialRecipeSource = tab
            )
        }
        composable("signIn") {
            SignInRoute(userSessionService, navController)
        }
    }
}
