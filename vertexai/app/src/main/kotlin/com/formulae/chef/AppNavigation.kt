package com.formulae.chef

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.formulae.chef.feature.chat.ChatRoute
import com.formulae.chef.feature.collection.CollectionRoute
import com.formulae.chef.services.persistence.RecipeRepository


@Composable
fun AppNavigation(recipeRepository: RecipeRepository) {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = "home") {
        composable("home") {
            HomeScreen(
                onNavigateToChat = { navController.navigate("chat") },
                onNavigateToCollection = { navController.navigate("collection") }
            )
        }
        composable("chat") {
            ChatRoute() // Your existing ChatRoute
        }
        composable("collection") {
            CollectionRoute(repository=recipeRepository) // Your new CollectionRoute
        }
    }
}
