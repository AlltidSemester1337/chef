package com.formulae.chef

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.formulae.chef.feature.chat.OverlayChatViewModel
import com.formulae.chef.feature.chat.ui.ChefOverlay
import com.formulae.chef.feature.collection.ui.DetailRoute
import com.formulae.chef.feature.home.HomeScreenViewModel
import com.formulae.chef.feature.model.Recipe
import com.formulae.chef.services.authentication.UserSessionService
import com.formulae.chef.ui.components.ChefFab
import com.formulae.chef.ui.components.RecipeCard
import com.formulae.chef.ui.components.SectionHeader
import com.formulae.chef.ui.theme.AppTypography
import com.formulae.chef.ui.theme.BackgroundColor
import com.formulae.chef.ui.theme.Terracotta200
import com.formulae.chef.ui.theme.Terracotta600
import com.formulae.chef.ui.theme.TextSecondary
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
            val firstName = currentUser?.displayName
                ?.takeIf { it.isNotBlank() }
                ?.split(" ")?.firstOrNull()
                ?: currentUser?.email
                    ?.substringBefore("@")
                    ?.replaceFirstChar(Char::uppercaseChar)
                ?: "there"
            HomeScreenContent(
                viewModel = viewModel,
                displayName = firstName,
                onNavigateToChat = onNavigateToChat,
                onNavigateToCollection = onNavigateToCollection,
                onNavigateToCommunity = onNavigateToCommunity,
                onSignOut = onSignOut
            )
        }
    }
}

@Composable
private fun HomeScreenContent(
    viewModel: HomeScreenViewModel,
    displayName: String,
    onNavigateToChat: () -> Unit,
    onNavigateToCollection: () -> Unit,
    onNavigateToCommunity: () -> Unit,
    onSignOut: () -> Unit
) {
    val overlayViewModel: OverlayChatViewModel = viewModel(factory = OverlayChatViewModelFactory)
    var showChefOverlay by remember { mutableStateOf(false) }

    val userRecipes by viewModel.userRecipes.collectAsState()
    val communityRecipes by viewModel.communityRecipes.collectAsState()
    val isLoadingRecipes by viewModel.isLoading.collectAsState()

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            // Greeting header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(BackgroundColor)
                    .padding(horizontal = 16.dp)
                    .padding(top = 48.dp, bottom = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Hi, $displayName!",
                    style = AppTypography.headlineLarge
                )
                IconButton(onClick = onSignOut) {
                    Icon(
                        imageVector = Icons.Outlined.Settings,
                        contentDescription = "Sign out",
                        tint = Terracotta600
                    )
                }
            }
            WaveDivider()

            // Content area
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(BackgroundColor)
                    .padding(horizontal = 16.dp)
            ) {
                if (isLoadingRecipes) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 48.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                } else {
                    Spacer(modifier = Modifier.height(24.dp))

                    SectionHeader(
                        title = "Your saved recipes",
                        linkText = "All saved recipes",
                        onLinkClick = onNavigateToCollection
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    if (userRecipes.isEmpty()) {
                        Text(
                            text = "Start chatting to get personalized recipes!",
                            style = AppTypography.bodyMedium.copy(color = TextSecondary),
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    } else {
                        RecipeCardGrid(
                            recipes = userRecipes,
                            onRecipeClick = viewModel::onRecipeSelected
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    SectionHeader(
                        title = "What's cooking?",
                        linkText = "Community collection",
                        onLinkClick = onNavigateToCommunity
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    if (communityRecipes.isEmpty()) {
                        Text(
                            text = "No community recipes yet.",
                            style = AppTypography.bodyMedium.copy(color = TextSecondary),
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    } else {
                        RecipeCardGrid(
                            recipes = communityRecipes,
                            onRecipeClick = viewModel::onRecipeSelected
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Text(
                        text = "Chat with Chef, your personal cooking assistant, to generate recipes, " +
                            "bounce off ideas, or get tips & tricks for your cooking!",
                        style = AppTypography.bodyMedium.copy(
                            fontStyle = FontStyle.Italic,
                            color = TextSecondary
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(80.dp))
                }
            }
        }

        ChefFab(
            onClick = { showChefOverlay = true },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 16.dp, bottom = 16.dp)
        )

        if (showChefOverlay) {
            ChefOverlay(
                viewModel = overlayViewModel,
                recipe = null,
                onDismiss = { showChefOverlay = false }
            )
        }
    }
}

@Composable
private fun WaveDivider(modifier: Modifier = Modifier) {
    val color = Terracotta200
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(14.dp)
    ) {
        val waveHeight = 3.dp.toPx()
        val waveLength = 18.dp.toPx()
        val centerY = size.height / 2f
        val path = Path()
        path.moveTo(0f, centerY)
        var x = 0f
        while (x < size.width + waveLength) {
            path.quadraticBezierTo(
                x + waveLength / 4f,
                centerY - waveHeight,
                x + waveLength / 2f,
                centerY
            )
            path.quadraticBezierTo(
                x + waveLength * 3f / 4f,
                centerY + waveHeight,
                x + waveLength,
                centerY
            )
            x += waveLength
        }
        drawPath(path, color = color, style = Stroke(width = 1.5.dp.toPx()))
    }
}

@Composable
private fun RecipeCardGrid(
    recipes: List<Recipe>,
    onRecipeClick: (Recipe) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        recipes.chunked(2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                row.forEach { recipe ->
                    RecipeCard(
                        title = recipe.title ?: "",
                        imageUrl = recipe.imageUrl,
                        onClick = { onRecipeClick(recipe) },
                        modifier = Modifier.weight(1f)
                    )
                }
                if (row.size == 1) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}
