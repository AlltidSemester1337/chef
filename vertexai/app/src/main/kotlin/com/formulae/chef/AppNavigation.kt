package com.formulae.chef

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

private val bottomBarRoutes = setOf("home", "generate", "collection")

@Composable
fun AppNavigation(
    recipeRepository: RecipeRepository,
    recipeListRepository: RecipeListRepository,
    recipeVariantRepository: RecipeVariantRepository,
    userSessionService: UserSessionService
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentBaseRoute = (navBackStackEntry?.destination?.route ?: "home").substringBefore("?")

    Scaffold(
        bottomBar = {
            if (currentBaseRoute in bottomBarRoutes) {
                ChefNavigationBar(
                    currentRoute = currentBaseRoute,
                    onNavigate = { route ->
                        navController.navigate(route) {
                            popUpTo("home") { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = "home",
            modifier = Modifier.padding(innerPadding)
        ) {
            composable("home") {
                val homeViewModel: HomeScreenViewModel = viewModel(
                    factory = HomeScreenViewModelFactory(recipeRepository)
                )
                HomeScreen(
                    viewModel = homeViewModel,
                    userSessionService = userSessionService,
                    onNavigateToChat = { navController.navigate("generate") },
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
            composable("generate") {
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
}
