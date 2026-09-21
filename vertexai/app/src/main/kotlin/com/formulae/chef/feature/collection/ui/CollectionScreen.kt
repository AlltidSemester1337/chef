/*
 * Copyright 2023 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.formulae.chef.feature.collection.ui

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import com.formulae.chef.AskChefVariantViewModelFactory
import com.formulae.chef.OverlayChatViewModelFactory
import com.formulae.chef.feature.chat.AskChefVariantViewModel
import com.formulae.chef.feature.chat.OverlayChatViewModel
import com.formulae.chef.feature.chat.ui.ChefOverlay
import com.formulae.chef.feature.collection.CollectionViewModel
import com.formulae.chef.feature.model.Recipe
import com.formulae.chef.feature.model.RecipeList
import com.formulae.chef.services.authentication.UserSessionService
import com.formulae.chef.services.persistence.RecipeListRepository
import com.formulae.chef.services.persistence.RecipeRepository
import com.formulae.chef.ui.components.BetaQuotaExceededDialog
import com.formulae.chef.ui.components.RecipeCard
import com.formulae.chef.ui.components.SectionHeader
import com.formulae.chef.ui.components.SegmentedTabRow
import com.formulae.chef.ui.theme.AppTypography
import com.formulae.chef.ui.theme.Terracotta200
import com.formulae.chef.ui.theme.Terracotta600
import com.formulae.chef.ui.theme.Terracotta800
import com.formulae.chef.ui.theme.TextPrimary
import com.formulae.chef.ui.theme.TextSecondary
import com.formulae.chef.ui.theme.White
import com.google.firebase.auth.UserInfo

enum class RecipeSource {
    SAVED,
    COMMUNITY
}

@Composable
internal fun CollectionRoute(
    repository: RecipeRepository,
    listRepository: RecipeListRepository,
    collectionViewModel: CollectionViewModel,
    navController: NavController,
    userSessionService: UserSessionService,
    initialRecipeSource: RecipeSource = RecipeSource.SAVED
) {
    val collectionUiState by collectionViewModel.uiState.collectAsState()
    val isLoading by collectionViewModel.isLoading.collectAsState()
    val selectedRecipe by collectionViewModel.selectedRecipe.collectAsState()
    val displayedRecipe by collectionViewModel.displayedRecipe.collectAsState()
    val isCookingMode by collectionViewModel.isCookingMode.collectAsState()
    val showIngredients by collectionViewModel.showIngredients.collectAsState()
    val checkedSteps by collectionViewModel.checkedSteps.collectAsState()
    val currentServings by collectionViewModel.currentServings.collectAsState()
    val lists by collectionViewModel.lists.collectAsState()
    val viewingListId by collectionViewModel.viewingListId.collectAsState()
    val variants by collectionViewModel.variants.collectAsState()
    val selectedVariantId by collectionViewModel.selectedVariantId.collectAsState()
    val isEditingVariant by collectionViewModel.isEditingVariant.collectAsState()
    var searchQuery by rememberSaveable { mutableStateOf("") }
    val currentUser by produceState<UserInfo?>(initialValue = null) {
        if (!userSessionService.anonymousSession) {
            userSessionService.currentUser.collect { user ->
                if (user != null) {
                    value = user
                }
            }
        }
    }

    val signedIn = !userSessionService.anonymousSession && currentUser != null

    var recipesSource by rememberSaveable { mutableStateOf(initialRecipeSource) }

    LaunchedEffect(signedIn) {
        if (initialRecipeSource == RecipeSource.SAVED) {
            recipesSource = if (signedIn) RecipeSource.SAVED else RecipeSource.COMMUNITY
        }
    }

    LaunchedEffect(currentUser) {
        collectionViewModel.setCurrentUser(currentUser?.uid)
    }

    val recipesSourceList = if (recipesSource == RecipeSource.SAVED && currentUser != null) {
        getUserFavouritesRecipeSourceList(collectionUiState, currentUser)
    } else {
        getBrowseRecipeSourceList(collectionUiState, currentUser)
    }

    val filteredRecipes = recipesSourceList.filter { recipe ->
        searchQuery.isEmpty() ||
            recipe.title.contains(searchQuery, ignoreCase = true) ||
            recipe.tags.any { tag -> tag.contains(searchQuery, ignoreCase = true) }
    }

    fun listNamesForRecipe(recipe: Recipe): List<String> {
        val id = recipe.id ?: return emptyList()
        return lists.filter { it.recipeIds.contains(id) }.map { it.name }
    }

    val overlayViewModel: OverlayChatViewModel = viewModel(
        factory = remember { OverlayChatViewModelFactory(userSessionService) }
    )
    val overlayQuotaExceeded by overlayViewModel.quotaExceeded.collectAsState()
    var showChefOverlay by remember { mutableStateOf(false) }

    val askChefVariantViewModel: AskChefVariantViewModel = viewModel(
        factory = remember { AskChefVariantViewModelFactory(userSessionService) }
    )
    val askChefState by askChefVariantViewModel.state.collectAsState()
    var editBaseRecipe by remember { mutableStateOf<Recipe?>(null) }
    val context = LocalContext.current

    LaunchedEffect(selectedRecipe) {
        showChefOverlay = false
        overlayViewModel.reset()
    }

    LaunchedEffect(isEditingVariant) {
        if (isEditingVariant) editBaseRecipe = null
    }

    var showAskChefQuotaExceeded by remember { mutableStateOf(false) }

    LaunchedEffect(askChefState) {
        when (val s = askChefState) {
            is AskChefVariantViewModel.State.Success -> {
                editBaseRecipe = s.recipe
                askChefVariantViewModel.reset()
            }
            is AskChefVariantViewModel.State.Error -> {
                Toast.makeText(
                    context,
                    "Sorry, Chef couldn't adjust that. Try editing manually.",
                    Toast.LENGTH_LONG
                ).show()
                askChefVariantViewModel.reset()
            }
            is AskChefVariantViewModel.State.QuotaExceeded -> {
                showAskChefQuotaExceeded = true
                askChefVariantViewModel.reset()
            }
            else -> {}
        }
    }

    BackHandler(enabled = selectedRecipe == null) {
        navController.navigate("home")
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = { showChefOverlay = true }) {
                Icon(Icons.Default.Chat, contentDescription = "Chat with Chef")
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .imePadding()
                .fillMaxSize()
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
            } else if (selectedRecipe == null) {
                CollectionHomeRoute(
                    signedIn = signedIn,
                    recipesSource = recipesSource,
                    onRecipesSourceChanged = { recipesSource = it },
                    searchQuery = searchQuery,
                    onSearchQueryChanged = { query -> searchQuery = query },
                    filteredRecipes = filteredRecipes,
                    lists = lists,
                    viewingListId = viewingListId,
                    allRecipes = collectionUiState.recipes,
                    onRecipeClick = { recipe: Recipe ->
                        collectionViewModel.onRecipeSelected(recipe)
                    },
                    onRecipeRemoveClick = { recipe: Recipe ->
                        collectionViewModel.onRecipeRemove(recipe)
                    },
                    onCreateList = collectionViewModel::onCreateList,
                    onDeleteList = collectionViewModel::onDeleteList,
                    onViewList = collectionViewModel::onViewList,
                    onCloseListView = collectionViewModel::onCloseListView,
                    onAddRecipeToList = collectionViewModel::onAddRecipeToList,
                    onRemoveRecipeFromList = collectionViewModel::onRemoveRecipeFromList
                )
            } else if (isEditingVariant && displayedRecipe != null) {
                val baseForEdit = editBaseRecipe ?: displayedRecipe!!
                key(editBaseRecipe) {
                    EditVariantScreen(
                        baseRecipe = baseForEdit,
                        isAiLoading = askChefState is AskChefVariantViewModel.State.Loading,
                        onAskChef = { prompt ->
                            askChefVariantViewModel.adjustRecipe(baseForEdit, prompt)
                        },
                        onSave = collectionViewModel::onSaveVariant,
                        onCancel = {
                            collectionViewModel.onCancelEditVariant()
                            askChefVariantViewModel.reset()
                        }
                    )
                }
            } else {
                DetailRoute(
                    recipe = displayedRecipe ?: selectedRecipe!!,
                    onBack = { collectionViewModel.clearSelectedRecipe() },
                    isCookingMode = isCookingMode,
                    showIngredients = showIngredients,
                    checkedSteps = checkedSteps,
                    currentServings = currentServings,
                    listNames = listNamesForRecipe(selectedRecipe!!),
                    variants = variants,
                    selectedVariantId = selectedVariantId,
                    isOwner = collectionViewModel.isRecipeOwner,
                    onToggleCookingMode = collectionViewModel::onToggleCookingMode,
                    onTabChanged = collectionViewModel::onTabChanged,
                    onStepChecked = collectionViewModel::onStepChecked,
                    onStepUnchecked = collectionViewModel::onStepUnchecked,
                    onServingsChanged = collectionViewModel::onServingsChanged,
                    onVariantSelected = collectionViewModel::onVariantSelected,
                    onPinVariant = collectionViewModel::onPinVariant,
                    onDeleteVariant = collectionViewModel::onDeleteVariant,
                    onStartCreateVariant = collectionViewModel::onStartCreateVariant,
                    onNavigateToChat = { navController.navigate("generate") }
                )
            }
        }

        if (showChefOverlay) {
            ChefOverlay(
                viewModel = overlayViewModel,
                recipe = selectedRecipe,
                onDismiss = { showChefOverlay = false }
            )
        }

        if (overlayQuotaExceeded) {
            BetaQuotaExceededDialog(onDismiss = overlayViewModel::onQuotaExceededDialogDismissed)
        }

        if (showAskChefQuotaExceeded) {
            BetaQuotaExceededDialog(onDismiss = { showAskChefQuotaExceeded = false })
        }
    }
}

private fun getUserFavouritesRecipeSourceList(
    collectionUiState: CollectionViewModel.CollectionUiState,
    currentUser: UserInfo?
) = collectionUiState.recipes.filter { recipe ->
    recipe.uid.contains(currentUser!!.uid)
}.filter { recipe ->
    recipe.isFavourite
}

private fun getBrowseRecipeSourceList(
    collectionUiState: CollectionViewModel.CollectionUiState,
    currentUser: UserInfo?
) = collectionUiState.recipes
    .filter { recipe ->
        recipe.copyId == null
    }
    .filter { recipe ->
        recipe.isFavourite
    }.filter { recipe ->
        recipe.uid != currentUser?.uid
    }

/**
 * Orchestrates the internal navigation between the Saved/Community browsing screen, a single
 * list's dedicated "List view" screen, and the full-screen "Add recipe to list" screen -
 * extending the same state-machine navigation pattern [CollectionRoute] uses for detail/edit.
 */
@Composable
private fun CollectionHomeRoute(
    signedIn: Boolean,
    recipesSource: RecipeSource,
    onRecipesSourceChanged: (RecipeSource) -> Unit,
    searchQuery: String,
    onSearchQueryChanged: (String) -> Unit,
    filteredRecipes: List<Recipe>,
    lists: List<RecipeList>,
    viewingListId: String?,
    allRecipes: List<Recipe>,
    onRecipeClick: (Recipe) -> Unit,
    onRecipeRemoveClick: (Recipe) -> Unit,
    onCreateList: (String) -> Unit,
    onDeleteList: (String) -> Unit,
    onViewList: (String) -> Unit,
    onCloseListView: () -> Unit,
    onAddRecipeToList: (String, String) -> Unit,
    onRemoveRecipeFromList: (String, String) -> Unit
) {
    var addingToListRecipe by remember { mutableStateOf<Recipe?>(null) }
    val viewingList = lists.firstOrNull { it.id == viewingListId }

    BackHandler(enabled = addingToListRecipe != null) {
        addingToListRecipe = null
    }
    BackHandler(enabled = addingToListRecipe == null && viewingList != null) {
        onCloseListView()
    }

    when {
        viewingList != null -> {
            val recipesInList = allRecipes.filter { recipe ->
                recipe.id != null && viewingList.recipeIds.contains(recipe.id)
            }
            ListDetailScreen(
                list = viewingList,
                recipes = recipesInList,
                searchQuery = searchQuery,
                onSearchQueryChanged = onSearchQueryChanged,
                onBack = onCloseListView,
                onDeleteList = { viewingList.id?.let(onDeleteList) },
                onRecipeClick = onRecipeClick,
                onRemoveFromList = { recipe ->
                    val recipeId = recipe.id ?: return@ListDetailScreen
                    val listId = viewingList.id ?: return@ListDetailScreen
                    onRemoveRecipeFromList(recipeId, listId)
                },
                onDiscoverRecipes = {
                    onCloseListView()
                    onRecipesSourceChanged(RecipeSource.SAVED)
                }
            )
        }
        addingToListRecipe != null -> {
            val recipe = addingToListRecipe!!
            AddToListScreen(
                recipeId = recipe.id,
                lists = lists,
                onToggleList = { list ->
                    val recipeId = recipe.id ?: return@AddToListScreen
                    val listId = list.id ?: return@AddToListScreen
                    if (list.recipeIds.contains(recipeId)) {
                        onRemoveRecipeFromList(recipeId, listId)
                    } else {
                        onAddRecipeToList(recipeId, listId)
                    }
                },
                onCreateNewList = onCreateList,
                onClose = { addingToListRecipe = null },
                allRecipes = allRecipes
            )
        }
        else -> {
            RecipeListRoute(
                signedIn = signedIn,
                recipesSource = recipesSource,
                onRecipesSourceChanged = onRecipesSourceChanged,
                searchQuery = searchQuery,
                onSearchQueryChanged = onSearchQueryChanged,
                filteredRecipes = filteredRecipes,
                lists = lists,
                allRecipes = allRecipes,
                onRecipeClick = onRecipeClick,
                onRecipeRemoveClick = onRecipeRemoveClick,
                onCreateList = onCreateList,
                onDeleteList = onDeleteList,
                onViewList = onViewList,
                onOpenAddToList = { recipe -> addingToListRecipe = recipe }
            )
        }
    }
}

@Composable
private fun RecipeListRoute(
    signedIn: Boolean,
    recipesSource: RecipeSource,
    onRecipesSourceChanged: (RecipeSource) -> Unit,
    searchQuery: String,
    onSearchQueryChanged: (String) -> Unit,
    filteredRecipes: List<Recipe>,
    lists: List<RecipeList>,
    allRecipes: List<Recipe>,
    onRecipeClick: (Recipe) -> Unit,
    onRecipeRemoveClick: (Recipe) -> Unit,
    onCreateList: (String) -> Unit,
    onDeleteList: (String) -> Unit,
    onViewList: (String) -> Unit,
    onOpenAddToList: (Recipe) -> Unit
) {
    var showCreateDialog by remember { mutableStateOf(false) }
    var listToDelete by remember { mutableStateOf<RecipeList?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(24.dp))
        Text(text = "Collections", style = AppTypography.headlineLarge)
        Spacer(modifier = Modifier.height(16.dp))

        SegmentedTabRow(
            tabs = listOf("Saved recipes", "Community recipes"),
            selectedIndex = if (recipesSource == RecipeSource.SAVED) 0 else 1,
            onTabSelected = { index ->
                onRecipesSourceChanged(if (index == 0) RecipeSource.SAVED else RecipeSource.COMMUNITY)
            }
        )

        Spacer(modifier = Modifier.height(16.dp))

        CollectionSearchBar(
            searchQuery = searchQuery,
            onSearchQueryChanged = onSearchQueryChanged,
            placeholder = if (recipesSource == RecipeSource.SAVED) {
                "Search recipes...."
            } else {
                "Search ingredients, dishes, recipes...."
            }
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (recipesSource == RecipeSource.SAVED) {
            if (!signedIn) {
                Text(
                    text = "Sign in to view your saved recipes and lists.",
                    style = AppTypography.bodyLarge,
                    modifier = Modifier
                        .fillMaxSize()
                        .wrapContentSize(Alignment.Center)
                )
            } else {
                SavedTabContent(
                    lists = lists,
                    recipes = filteredRecipes,
                    allRecipes = allRecipes,
                    onNewListClick = { showCreateDialog = true },
                    onListClick = { list -> list.id?.let(onViewList) },
                    onDeleteListClick = { list -> listToDelete = list },
                    onRecipeClick = onRecipeClick,
                    onRecipeRemoveClick = onRecipeRemoveClick,
                    onAddToListClick = onOpenAddToList,
                    modifier = Modifier.weight(1f)
                )
            }
        } else {
            CommunityTabContent(
                recipes = filteredRecipes,
                onRecipeClick = onRecipeClick,
                modifier = Modifier.weight(1f)
            )
        }
    }

    if (showCreateDialog) {
        CreateListDialog(
            onConfirm = { name ->
                onCreateList(name)
                showCreateDialog = false
            },
            onDismiss = { showCreateDialog = false }
        )
    }

    listToDelete?.let { list ->
        ConfirmDeleteListDialog(
            listName = list.name,
            onConfirm = {
                list.id?.let(onDeleteList)
                listToDelete = null
            },
            onDismiss = { listToDelete = null }
        )
    }
}

@Composable
private fun SavedTabContent(
    lists: List<RecipeList>,
    recipes: List<Recipe>,
    allRecipes: List<Recipe>,
    onNewListClick: () -> Unit,
    onListClick: (RecipeList) -> Unit,
    onDeleteListClick: (RecipeList) -> Unit,
    onRecipeClick: (Recipe) -> Unit,
    onRecipeRemoveClick: (Recipe) -> Unit,
    onAddToListClick: (Recipe) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(modifier = modifier.fillMaxSize()) {
        item {
            SectionHeader(
                title = "Your lists (${lists.size})",
                linkText = "New list",
                onLinkClick = onNewListClick
            )
        }
        items(lists, key = { it.id ?: it.name }) { list ->
            ListRow(
                list = list,
                onClick = { onListClick(list) },
                onOverflowClick = { onDeleteListClick(list) },
                thumbnailUrl = firstRecipeThumbnail(list, allRecipes)
            )
        }

        item {
            Spacer(modifier = Modifier.height(16.dp))
            SectionHeader(title = "Your saved recipes (${recipes.size})")
            Spacer(modifier = Modifier.height(8.dp))
        }

        recipeCardRows(
            recipes = recipes,
            onRecipeClick = onRecipeClick,
            onBookmarkClick = onRecipeRemoveClick,
            onAddToListClick = onAddToListClick
        )
    }
}

@Composable
private fun CommunityTabContent(
    recipes: List<Recipe>,
    onRecipeClick: (Recipe) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(modifier = modifier.fillMaxSize()) {
        item {
            SectionHeader(title = "All recipes (${recipes.size})")
            Spacer(modifier = Modifier.height(8.dp))
        }

        recipeCardRows(
            recipes = recipes,
            onRecipeClick = onRecipeClick,
            onBookmarkClick = null,
            onAddToListClick = null
        )
    }
}

internal fun LazyListScope.recipeCardRows(
    recipes: List<Recipe>,
    onRecipeClick: (Recipe) -> Unit,
    onBookmarkClick: ((Recipe) -> Unit)?,
    onAddToListClick: ((Recipe) -> Unit)?
) {
    if (recipes.isEmpty()) {
        item {
            Text(
                text = "No recipes found!",
                style = AppTypography.bodyLarge,
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentSize(Alignment.Center)
                    .padding(vertical = 24.dp)
            )
        }
        return
    }

    items(recipes.chunked(2), key = { row -> row.joinToString { it.id ?: it.title } }) { row ->
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            row.forEach { recipe ->
                RecipeCard(
                    title = recipe.title,
                    imageUrl = recipe.imageUrl,
                    showBookmark = onBookmarkClick != null,
                    onBookmarkClick = onBookmarkClick?.let { callback -> { callback(recipe) } },
                    onAddToListClick = onAddToListClick?.let { callback -> { callback(recipe) } },
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

@Composable
internal fun CollectionSearchBar(
    searchQuery: String,
    onSearchQueryChanged: (String) -> Unit,
    placeholder: String = "Search recipes...."
) {
    OutlinedTextField(
        value = searchQuery,
        onValueChange = onSearchQueryChanged,
        placeholder = { Text(placeholder, style = AppTypography.bodyMedium) },
        leadingIcon = {
            Icon(Icons.Default.Search, contentDescription = null, tint = TextSecondary)
        },
        singleLine = true,
        shape = RoundedCornerShape(8.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = White,
            unfocusedContainerColor = White,
            focusedBorderColor = Terracotta200,
            unfocusedBorderColor = Terracotta200
        ),
        keyboardOptions = KeyboardOptions.Default.copy(
            capitalization = KeyboardCapitalization.Sentences
        ),
        modifier = Modifier.fillMaxWidth()
    )
}

/**
 * Resolves a list's thumbnail as its first recipe's image, if any.
 */
internal fun firstRecipeThumbnail(list: RecipeList, allRecipes: List<Recipe>): String? {
    val firstRecipeId = list.recipeIds.firstOrNull() ?: return null
    return allRecipes.firstOrNull { it.id == firstRecipeId }?.imageUrl
}

/**
 * A single "list row": thumbnail + name + recipe count + overflow menu, used both in the
 * Saved tab's lists section and the "Add recipe to list" screen.
 */
@Composable
internal fun ListRow(
    list: RecipeList,
    onClick: () -> Unit,
    onOverflowClick: (() -> Unit)? = null,
    thumbnailUrl: String? = null,
    trailingContent: (@Composable () -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(width = 65.dp, height = 48.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Terracotta200.copy(alpha = 0.4f)),
            contentAlignment = Alignment.Center
        ) {
            if (!thumbnailUrl.isNullOrEmpty()) {
                Image(
                    painter = rememberAsyncImagePainter(thumbnailUrl),
                    contentDescription = list.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.matchParentSize()
                )
            } else {
                Icon(
                    imageVector = Icons.Outlined.MenuBook,
                    contentDescription = null,
                    tint = Terracotta600
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(text = list.name, style = AppTypography.labelMedium.copy(color = TextPrimary))
            Text(
                text = "${list.recipeIds.size} recipe${if (list.recipeIds.size != 1) "s" else ""}",
                style = AppTypography.bodyMedium
            )
        }

        if (trailingContent != null) {
            trailingContent()
        } else if (onOverflowClick != null) {
            IconButton(onClick = onOverflowClick) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = "List options",
                    tint = TextSecondary
                )
            }
        }
    }
}

@Composable
internal fun CreateListDialog(
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = White,
        shape = RoundedCornerShape(8.dp),
        title = {
            Text(
                text = "New list",
                style = AppTypography.headlineLarge.copy(color = TextPrimary)
            )
        },
        text = {
            Column {
                Text(
                    text = "Enter a name for your new list.",
                    style = AppTypography.bodyLarge
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    placeholder = { Text("List name") },
                    singleLine = true,
                    shape = RoundedCornerShape(8.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Terracotta200,
                        unfocusedBorderColor = Terracotta200
                    ),
                    keyboardOptions = KeyboardOptions.Default.copy(
                        capitalization = KeyboardCapitalization.Sentences
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { if (name.isNotBlank()) onConfirm(name.trim()) },
                enabled = name.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = Terracotta600)
            ) {
                Text("Create list", style = AppTypography.labelLarge)
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                border = BorderStroke(1.dp, Terracotta200)
            ) {
                Text("Cancel", style = AppTypography.labelMedium.copy(color = Terracotta600))
            }
        }
    )
}

@Composable
internal fun ConfirmDeleteListDialog(
    listName: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Delete list?") },
        text = { Text("Delete \"$listName\"? This will not remove the recipes themselves.") },
        confirmButton = {
            TextButton(onClick = onConfirm) { Text("Delete", color = Terracotta800) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Preview(showBackground = true)
@Composable
fun PreviewRecipeListRoute() {
    RecipeListRoute(
        signedIn = true,
        recipesSource = RecipeSource.SAVED,
        onRecipesSourceChanged = {},
        searchQuery = "Search Query",
        onSearchQueryChanged = {},
        filteredRecipes = listOf(Recipe(title = "West African Peanut stew"), Recipe(title = "Pasta Carbonara")),
        lists = listOf(RecipeList(id = "1", name = "Work week", recipeIds = listOf("a", "b"))),
        allRecipes = emptyList(),
        onRecipeClick = {},
        onRecipeRemoveClick = {},
        onCreateList = {},
        onDeleteList = {},
        onViewList = {},
        onOpenAddToList = {}
    )
}
