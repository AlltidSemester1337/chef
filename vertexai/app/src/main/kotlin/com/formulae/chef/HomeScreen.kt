package com.formulae.chef

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.rememberAsyncImagePainter
import com.formulae.chef.feature.chat.OverlayChatViewModel
import com.formulae.chef.feature.chat.ui.ChefOverlay
import com.formulae.chef.feature.collection.ui.DetailRoute
import com.formulae.chef.feature.collection.ui.RecipeItem
import com.formulae.chef.feature.home.HomeScreenViewModel
import com.formulae.chef.services.authentication.UserSessionService
import com.google.firebase.auth.UserInfo

@Composable
fun HomeScreen(
    viewModel: HomeScreenViewModel,
    userSessionService: UserSessionService,
    onNavigateToChat: () -> Unit = {},
    onNavigateToCollection: () -> Unit = {},
    onNavigateToCommunity: () -> Unit = {},
    onSignOut: () -> Unit = {}
) {
    var isLoading by remember { mutableStateOf(true) }
    val currentUser by produceState<UserInfo?>(initialValue = null) {
        if (!userSessionService.anonymousSession) {
            userSessionService.currentUser.collect { user ->
                if (user != null) {
                    value = user
                }
                isLoading = false
            }
        } else {
            isLoading = false
        }
    }

    LaunchedEffect(isLoading, currentUser?.uid) {
        if (!isLoading) {
            viewModel.setCurrentUser(currentUser?.uid)
        }
    }

    val selectedRecipe by viewModel.selectedRecipe.collectAsState()
    var showIngredients by rememberSaveable(selectedRecipe?.id) { mutableStateOf(true) }

    if (isLoading) {
        CircularProgressIndicator()
    } else {
        if (!userSessionService.anonymousSession && currentUser == null) {
            onSignOut()
        }
        if (selectedRecipe != null) {
            DetailRoute(
                recipe = selectedRecipe!!,
                onBack = { viewModel.clearSelectedRecipe() },
                showIngredients = showIngredients,
                onTabChanged = { showIngredients = it },
                isOwner = currentUser?.uid == selectedRecipe?.uid
            )
        } else {
            HomeScreenContent(
                viewModel = viewModel,
                onNavigateToChat = onNavigateToChat,
                onNavigateToCollection = onNavigateToCollection,
                onNavigateToCommunity = onNavigateToCommunity,
                onSignOut = onSignOut,
                signedIn = !userSessionService.anonymousSession && currentUser != null
            )
        }
    }
}

@Composable
private fun HomeScreenContent(
    viewModel: HomeScreenViewModel,
    onNavigateToChat: () -> Unit,
    onNavigateToCollection: () -> Unit,
    onNavigateToCommunity: () -> Unit,
    onSignOut: () -> Unit,
    signedIn: Boolean
) {
    val overlayViewModel: OverlayChatViewModel = viewModel(factory = OverlayChatViewModelFactory)
    var showChefOverlay by remember { mutableStateOf(false) }

    val userRecipes by viewModel.userRecipes.collectAsState()
    val communityRecipes by viewModel.communityRecipes.collectAsState()
    val isLoadingRecipes by viewModel.isLoading.collectAsState()

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = { showChefOverlay = true }) {
                Icon(Icons.Default.Chat, contentDescription = "Chat with Chef")
            }
        }
    ) { paddingValues ->
        if (isLoadingRecipes) {
            CircularProgressIndicator(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .wrapContentSize()
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 16.dp)
            ) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "My Recipes",
                            style = MaterialTheme.typography.titleMedium
                        )
                        TextButton(onClick = onNavigateToCollection) {
                            Text("View all")
                        }
                    }
                }

                if (userRecipes.isEmpty()) {
                    item {
                        Text(
                            text = "No recipes yet. Start chatting to create some!",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }
                } else {
                    items(userRecipes) { recipe ->
                        RecipeItem(
                            recipe = recipe,
                            onRecipeClick = viewModel::onRecipeSelected,
                            onRecipeRemove = {},
                            recipeRemoveEnabled = false,
                            painter = rememberAsyncImagePainter(recipe.imageUrl)
                        )
                    }
                }

                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Community",
                            style = MaterialTheme.typography.titleMedium
                        )
                        TextButton(onClick = onNavigateToCommunity) {
                            Text("Browse all")
                        }
                    }
                }

                if (communityRecipes.isEmpty()) {
                    item {
                        Text(
                            text = "No community recipes available yet.",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }
                } else {
                    items(communityRecipes) { recipe ->
                        RecipeItem(
                            recipe = recipe,
                            onRecipeClick = viewModel::onRecipeSelected,
                            onRecipeRemove = {},
                            recipeRemoveEnabled = false,
                            painter = rememberAsyncImagePainter(recipe.imageUrl)
                        )
                    }
                }

                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Button(
                            onClick = onNavigateToChat,
                            enabled = signedIn,
                            modifier = Modifier.padding(4.dp)
                        ) {
                            Text(text = "Go to Chat")
                        }
                        Button(
                            onClick = onSignOut,
                            modifier = Modifier.padding(4.dp)
                        ) {
                            Text(text = "Sign out")
                        }
                    }
                }
            }
        }

        if (showChefOverlay) {
            ChefOverlay(
                viewModel = overlayViewModel,
                recipe = null,
                onDismiss = { showChefOverlay = false }
            )
        }
    }
}
