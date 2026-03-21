package com.formulae.chef

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.formulae.chef.feature.chat.ui.ChatRoute
import com.formulae.chef.feature.collection.ui.CollectionRoute
import com.formulae.chef.feature.useraccount.ui.SignInRoute
import com.formulae.chef.services.authentication.UserSessionService
import com.formulae.chef.services.persistence.RecipeRepository
import com.formulae.chef.ui.components.ChefNavigationBar

private val bottomBarRoutes = setOf("home", "generate", "collections")

@Composable
fun AppNavigation(recipeRepository: RecipeRepository, userSessionService: UserSessionService) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: "home"

    Scaffold(
        bottomBar = {
            if (currentRoute in bottomBarRoutes) {
                ChefNavigationBar(
                    currentRoute = currentRoute,
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
                HomeScreen(
                    userSessionService = userSessionService,
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
            composable("collections") {
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
}
