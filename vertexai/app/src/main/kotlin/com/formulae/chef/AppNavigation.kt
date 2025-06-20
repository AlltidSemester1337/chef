package com.formulae.chef

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.formulae.chef.feature.chat.ui.ChatRoute
import com.formulae.chef.feature.collection.ui.CollectionRoute
import com.formulae.chef.feature.useraccount.ui.SignInRoute
import com.formulae.chef.services.authentication.UserSessionService
import com.formulae.chef.services.persistence.RecipeRepository


@Composable
fun AppNavigation(recipeRepository: RecipeRepository, userSessionService: UserSessionService) {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = "home") {
        composable("home") {
            HomeScreen(
                userSessionService = userSessionService,
                onNavigateToChat = { navController.navigate("chat") },
                onNavigateToCollection = { navController.navigate("collection") },
                onSignOut = {
                    userSessionService.signOut(); navController.navigate("signIn") {
                    popUpTo("home") { inclusive = true }
                }
                }
            )
        }
        composable("chat") {
            ChatRoute() // Your existing ChatRoute
        }
        composable("collection") {
            CollectionRoute(
                repository = recipeRepository,
                navController = navController,
                userSessionService = userSessionService
            )
        }
        composable("signIn") {
            SignInRoute(userSessionService, navController)
        }
    }
}
