package com.formulae.chef

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ExitToApp
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
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.formulae.chef.feature.chat.OverlayChatViewModel
import com.formulae.chef.feature.chat.ui.ChefOverlay
import com.formulae.chef.feature.collection.ui.DetailRoute
import com.formulae.chef.feature.collection.ui.RecipeVideoSection
import com.formulae.chef.feature.home.HomeScreenViewModel
import com.formulae.chef.feature.home.HomeUiState
import com.formulae.chef.feature.home.HomeViewModel
import com.formulae.chef.feature.model.CookingResource
import com.formulae.chef.feature.model.Recipe
import com.formulae.chef.feature.model.RecipeOfTheMonth
import com.formulae.chef.services.authentication.UserSessionService
import com.formulae.chef.ui.components.BetaQuotaExceededDialog
import com.formulae.chef.ui.components.ChefFab
import com.formulae.chef.ui.components.RecipeCard
import com.formulae.chef.ui.components.SectionHeader
import com.formulae.chef.ui.components.WaveDivider
import com.formulae.chef.ui.theme.AppTypography
import com.formulae.chef.ui.theme.BackgroundColor
import com.formulae.chef.ui.theme.Terracotta100
import com.formulae.chef.ui.theme.Terracotta600
import com.formulae.chef.ui.theme.TextPrimary
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
    val isCookingMode by viewModel.isCookingMode.collectAsState()
    val checkedSteps by viewModel.checkedSteps.collectAsState()
    val currentServings by viewModel.currentServings.collectAsState()

    val cookingViewModel: HomeViewModel = viewModel(
        factory = remember { HomeViewModelFactory(userSessionService) }
    )
    val cookingUiState by cookingViewModel.uiState.collectAsState()

    if (isLoading) {
        CircularProgressIndicator()
    } else {
        if (!userSessionService.anonymousSession && currentUser == null) {
            onSignOut()
        }
        val signedIn = !userSessionService.anonymousSession && currentUser != null
        if (selectedRecipe != null) {
            DetailRoute(
                recipe = selectedRecipe!!,
                onBack = { viewModel.clearSelectedRecipe() },
                isCookingMode = isCookingMode,
                showIngredients = showIngredients,
                checkedSteps = checkedSteps,
                currentServings = currentServings,
                onToggleCookingMode = viewModel::onToggleCookingMode,
                onTabChanged = { showIngredients = it },
                onStepChecked = viewModel::onStepChecked,
                onStepUnchecked = viewModel::onStepUnchecked,
                onServingsChanged = viewModel::onServingsChanged,
                isOwner = currentUser?.uid == selectedRecipe?.uid,
                onNavigateToChat = onNavigateToChat
            )
        } else {
            val firstName = remember(currentUser) {
                resolveDisplayName(currentUser?.displayName, currentUser?.email)
            }
            HomeScreenContent(
                viewModel = viewModel,
                displayName = firstName,
                userSessionService = userSessionService,
                onNavigateToCollection = onNavigateToCollection,
                onNavigateToCommunity = onNavigateToCommunity,
                onSignOut = onSignOut,
                homeUiState = if (signedIn) cookingUiState else HomeUiState()
            )
        }
    }
}

@Composable
private fun HomeScreenContent(
    viewModel: HomeScreenViewModel,
    displayName: String,
    userSessionService: UserSessionService,
    onNavigateToCollection: () -> Unit,
    onNavigateToCommunity: () -> Unit,
    onSignOut: () -> Unit,
    homeUiState: HomeUiState = HomeUiState()
) {
    val overlayViewModel: OverlayChatViewModel = viewModel(
        factory = remember { OverlayChatViewModelFactory(userSessionService) }
    )
    val overlayQuotaExceeded by overlayViewModel.quotaExceeded.collectAsState()
    var showChefOverlay by remember { mutableStateOf(false) }

    val userRecipes by viewModel.userRecipes.collectAsState()
    val communityRecipes by viewModel.communityRecipes.collectAsState()
    val isLoadingRecipes by viewModel.isLoading.collectAsState()
    val recipeOfTheMonth by viewModel.recipeOfTheMonth.collectAsState()

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
                        imageVector = Icons.AutoMirrored.Outlined.ExitToApp,
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

                    recipeOfTheMonth?.takeIf { it.videoUrl.isNotEmpty() }?.let { rotw ->
                        Spacer(modifier = Modifier.height(24.dp))

                        RecipeOfTheMonthSection(
                            rotw = rotw,
                            onViewRecipe = { viewModel.onRecipeOfTheMonthClicked(rotw) }
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

                    if (homeUiState.isLoading) {
                        Spacer(modifier = Modifier.height(24.dp))

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    } else if (homeUiState.resources.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(24.dp))

                        SectionHeader(title = "Cooking resources")

                        Spacer(modifier = Modifier.height(16.dp))

                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            homeUiState.resources.forEach { resource ->
                                CookingResourceCard(resource = resource)
                            }
                        }
                    }

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

        if (overlayQuotaExceeded) {
            BetaQuotaExceededDialog(onDismiss = overlayViewModel::onQuotaExceededDialogDismissed)
        }
    }
}

@Composable
private fun RecipeOfTheMonthSection(rotw: RecipeOfTheMonth, onViewRecipe: () -> Unit) {
    SectionHeader(
        title = "Recipe of the Month",
        linkText = "View recipe",
        onLinkClick = onViewRecipe
    )

    Spacer(modifier = Modifier.height(16.dp))

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(Terracotta100)
            .padding(12.dp)
    ) {
        Text(
            text = rotw.recipeTitle,
            style = AppTypography.labelMedium.copy(fontWeight = FontWeight.Bold, color = TextPrimary)
        )
        Spacer(modifier = Modifier.height(8.dp))
        RecipeVideoSection(videoUrl = rotw.videoUrl)
    }
}

@Composable
private fun CookingResourceCard(resource: CookingResource) {
    val uriHandler = LocalUriHandler.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(Terracotta100)
            .clickable(enabled = resource.url.isNotBlank()) {
                uriHandler.openUri(resource.url)
            }
            .padding(12.dp)
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = resource.title,
                style = AppTypography.labelMedium.copy(fontWeight = FontWeight.Bold, color = TextPrimary),
                modifier = Modifier.weight(1f)
            )
            if (resource.type.isNotBlank()) {
                Text(
                    text = resource.type,
                    style = AppTypography.bodySmall.copy(color = Terracotta600)
                )
            }
        }
        if (resource.description.isNotBlank()) {
            Text(
                text = resource.description,
                style = AppTypography.bodyMedium.copy(color = TextSecondary),
                modifier = Modifier.padding(top = 4.dp)
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

internal fun resolveDisplayName(displayName: String?, email: String?): String =
    displayName?.takeIf { it.isNotBlank() }?.split(" ")?.firstOrNull()
        ?: email?.substringBefore("@")?.replaceFirstChar(Char::uppercaseChar)
        ?: "there"
