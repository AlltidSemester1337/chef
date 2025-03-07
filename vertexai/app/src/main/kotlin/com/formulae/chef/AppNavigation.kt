package com.formulae.chef

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.formulae.chef.feature.chat.ChatRoute
import com.formulae.chef.feature.collection.CollectionRoute
import com.formulae.chef.feature.useraccount.SignInRoute
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
                onSignOut = { userSessionService.signOut(); navController.navigate("signIn") {
                    popUpTo("home") { inclusive = true }
                } }
            )
        }
        composable("chat") {
            ChatRoute() // Your existing ChatRoute
        }
        composable("collection") {
            CollectionRoute(repository = recipeRepository, navController = navController) // Your new CollectionRoute
        }
        composable("signIn") {
            SignInRoute(userSessionService, navController)
        }
    }
}
