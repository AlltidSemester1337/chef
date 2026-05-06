package com.formulae.chef

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
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
import com.formulae.chef.ui.theme.Terracotta600
import com.formulae.chef.ui.theme.TextSecondary
import com.formulae.chef.ui.theme.White
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
            val firstName = currentUser?.displayName?.split(" ")?.firstOrNull() ?: "Chef"
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
        // Decorative carrot — behind all content, top-right area
        Image(
            painter = painterResource(id = R.drawable.carrot),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .offset(x = 200.dp, y = 24.dp)
                .width(168.dp)
                .height(225.dp)
                .rotate(10.89f)
                .clip(RoundedCornerShape(16.dp))
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            // Header block (white background)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(White)
                    .padding(horizontal = 16.dp)
                    .padding(top = 48.dp, bottom = 24.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
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

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = "Looking for kitchen inspiration? Chat with Chef to generate your first recipe!",
                    style = AppTypography.bodyLarge,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = onNavigateToChat,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Terracotta600),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "Generate personalized recipes",
                        style = AppTypography.labelLarge
                    )
                }
            }

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
