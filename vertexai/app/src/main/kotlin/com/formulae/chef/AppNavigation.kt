package com.formulae.chef

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.formulae.chef.feature.chat.ChatRoute
import com.formulae.chef.feature.collection.CollectionRoute


@Composable
fun AppNavigation() {
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
            CollectionRoute() // Your new CollectionRoute
        }
    }
}
