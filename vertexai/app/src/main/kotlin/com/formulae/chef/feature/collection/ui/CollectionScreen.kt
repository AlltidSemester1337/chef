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
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.PlaylistAdd
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SuggestionChip
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import com.formulae.chef.AskChefVariantViewModelFactory
import com.formulae.chef.OverlayChatViewModelFactory
import com.formulae.chef.R
import com.formulae.chef.feature.chat.AskChefVariantViewModel
import com.formulae.chef.feature.chat.OverlayChatViewModel
import com.formulae.chef.feature.chat.ui.ChefOverlay
import com.formulae.chef.feature.collection.CollectionViewModel
import com.formulae.chef.feature.model.Recipe
import com.formulae.chef.feature.model.RecipeList
import com.formulae.chef.services.authentication.UserSessionService
import com.formulae.chef.services.persistence.RecipeListRepository
import com.formulae.chef.services.persistence.RecipeRepository
import com.google.firebase.auth.UserInfo

enum class RecipeSource {
    MY_LISTS,
    USER_FAVOURITES,
    ALL_RECIPES
}

@Composable
internal fun CollectionRoute(
    repository: RecipeRepository,
    listRepository: RecipeListRepository,
    collectionViewModel: CollectionViewModel,
    navController: NavController,
    userSessionService: UserSessionService,
    initialRecipeSource: RecipeSource = RecipeSource.USER_FAVOURITES
) {
    val collectionUiState by collectionViewModel.uiState.collectAsState()
    val isLoading by collectionViewModel.isLoading.collectAsState()
    val listState = rememberLazyListState()
    val selectedRecipe by collectionViewModel.selectedRecipe.collectAsState()
    val displayedRecipe by collectionViewModel.displayedRecipe.collectAsState()
    val isCookingMode by collectionViewModel.isCookingMode.collectAsState()
    val showIngredients by collectionViewModel.showIngredients.collectAsState()
    val checkedSteps by collectionViewModel.checkedSteps.collectAsState()
    val currentServings by collectionViewModel.currentServings.collectAsState()
    val lists by collectionViewModel.lists.collectAsState()
    val expandedListId by collectionViewModel.expandedListId.collectAsState()
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
        if (initialRecipeSource == RecipeSource.USER_FAVOURITES) {
            recipesSource = if (signedIn) RecipeSource.USER_FAVOURITES else RecipeSource.ALL_RECIPES
        }
    }

    LaunchedEffect(currentUser) {
        collectionViewModel.setCurrentUser(currentUser?.uid)
    }

    val recipesSourceList = if (recipesSource == RecipeSource.USER_FAVOURITES && currentUser != null) {
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

    val overlayViewModel: OverlayChatViewModel = viewModel(factory = OverlayChatViewModelFactory)
    var showChefOverlay by remember { mutableStateOf(false) }

    val askChefVariantViewModel: AskChefVariantViewModel = viewModel(factory = AskChefVariantViewModelFactory)
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
            else -> {}
        }
    }

    BackHandler {
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
                .padding(vertical = 30.dp)
                .imePadding()
                .fillMaxSize()
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
            } else if (selectedRecipe == null) {
                RecipeListRoute(
                    signedIn = signedIn,
                    recipesSource = recipesSource,
                    searchQuery = searchQuery,
                    filteredRecipes = filteredRecipes,
                    listState = listState,
                    lists = lists,
                    expandedListId = expandedListId,
                    allRecipes = collectionUiState.recipes,
                    onSearchQueryChanged = { query -> searchQuery = query },
                    onClickMyLists = { recipesSource = RecipeSource.MY_LISTS },
                    onClickUserFavourites = { recipesSource = RecipeSource.USER_FAVOURITES },
                    onClickAllRecipes = { recipesSource = RecipeSource.ALL_RECIPES },
                    onRecipeClick = { recipe: Recipe ->
                        collectionViewModel.onRecipeSelected(recipe)
                    },
                    onRecipeRemoveClick = { recipe: Recipe ->
                        collectionViewModel.onRecipeRemove(recipe)
                    },
                    onCreateList = collectionViewModel::onCreateList,
                    onDeleteList = collectionViewModel::onDeleteList,
                    onExpandList = collectionViewModel::onExpandList,
                    onAddRecipeToList = collectionViewModel::onAddRecipeToList,
                    onRemoveRecipeFromList = collectionViewModel::onRemoveRecipeFromList,
                    listNamesForRecipe = ::listNamesForRecipe
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
                    onStartCreateVariant = collectionViewModel::onStartCreateVariant
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

@Composable
private fun RecipeListRoute(
    signedIn: Boolean,
    recipesSource: RecipeSource,
    searchQuery: String,
    filteredRecipes: List<Recipe>,
    listState: LazyListState,
    lists: List<RecipeList>,
    expandedListId: String?,
    allRecipes: List<Recipe>,
    onSearchQueryChanged: (String) -> Unit,
    onClickMyLists: () -> Unit,
    onClickUserFavourites: () -> Unit,
    onClickAllRecipes: () -> Unit,
    onRecipeClick: (Recipe) -> Unit,
    onRecipeRemoveClick: (Recipe) -> Unit,
    onCreateList: (String) -> Unit,
    onDeleteList: (String) -> Unit,
    onExpandList: (String?) -> Unit,
    onAddRecipeToList: (String, String) -> Unit,
    onRemoveRecipeFromList: (String, String) -> Unit,
    listNamesForRecipe: (Recipe) -> List<String>
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Spacer(modifier = Modifier.height(16.dp))

        ToggleButtonRow(
            signedIn = signedIn,
            recipesSource = recipesSource,
            onClickMyLists = onClickMyLists,
            onClickUserFavourites = onClickUserFavourites,
            onClickAllRecipes = onClickAllRecipes
        )

        SearchBar(
            searchQuery = searchQuery,
            onSearchQueryChanged = onSearchQueryChanged
        )

        if (recipesSource == RecipeSource.MY_LISTS) {
            MyListsView(
                lists = lists,
                expandedListId = expandedListId,
                allRecipes = allRecipes,
                searchQuery = searchQuery,
                onRecipeClick = onRecipeClick,
                onCreateList = onCreateList,
                onDeleteList = onDeleteList,
                onExpandList = onExpandList,
                onRemoveRecipeFromList = onRemoveRecipeFromList
            )
        } else {
            val showRemove = recipesSource == RecipeSource.USER_FAVOURITES
            var recipeForListDialog by remember { mutableStateOf<Recipe?>(null) }

            RecipeList(
                recipes = filteredRecipes,
                onRecipeClick = onRecipeClick,
                onRecipeRemove = onRecipeRemoveClick,
                listState = listState,
                recipeRemoveEnabled = showRemove,
                listNamesForRecipe = if (showRemove) listNamesForRecipe else { _ -> emptyList() },
                onAddToListClick = if (showRemove) {
                    { recipe -> recipeForListDialog = recipe }
                } else {
                    null
                }
            )

            recipeForListDialog?.let { recipe ->
                AddToListDialog(
                    recipe = recipe,
                    lists = lists,
                    onAddToList = { listId ->
                        recipe.id?.let { onAddRecipeToList(it, listId) }
                    },
                    onRemoveFromList = { listId ->
                        recipe.id?.let { onRemoveRecipeFromList(it, listId) }
                    },
                    onDismiss = { recipeForListDialog = null }
                )
            }
        }
    }
}

@Composable
private fun ToggleButtonRow(
    signedIn: Boolean,
    recipesSource: RecipeSource,
    onClickMyLists: () -> Unit,
    onClickUserFavourites: () -> Unit,
    onClickAllRecipes: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        Button(
            onClick = onClickMyLists,
            enabled = signedIn,
            colors = ButtonDefaults.buttonColors(
                containerColor = if (recipesSource == RecipeSource.MY_LISTS) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.secondary
                }
            )
        ) {
            Text("My lists")
        }

        Button(
            onClick = onClickUserFavourites,
            enabled = signedIn,
            colors = ButtonDefaults.buttonColors(
                containerColor = if (recipesSource == RecipeSource.USER_FAVOURITES) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.secondary
                }
            )
        ) {
            Text("My favourites")
        }

        Button(
            onClick = onClickAllRecipes,
            colors = ButtonDefaults.buttonColors(
                containerColor = if (recipesSource == RecipeSource.ALL_RECIPES) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.secondary
                }
            )
        ) {
            Text("Browse all")
        }
    }
}

@Composable
fun SearchBar(
    searchQuery: String,
    onSearchQueryChanged: (String) -> Unit
) {
    OutlinedTextField(
        value = searchQuery,
        onValueChange = onSearchQueryChanged,
        label = { Text("Filter Recipes") },
        keyboardOptions = KeyboardOptions.Default.copy(
            capitalization = KeyboardCapitalization.Sentences
        ),
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
    )
}

@Composable
private fun MyListsView(
    lists: List<RecipeList>,
    expandedListId: String?,
    allRecipes: List<Recipe>,
    searchQuery: String,
    onRecipeClick: (Recipe) -> Unit,
    onCreateList: (String) -> Unit,
    onDeleteList: (String) -> Unit,
    onExpandList: (String?) -> Unit,
    onRemoveRecipeFromList: (String, String) -> Unit
) {
    var showCreateDialog by remember { mutableStateOf(false) }
    var listToDelete by remember { mutableStateOf<RecipeList?>(null) }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.End
        ) {
            Button(onClick = { showCreateDialog = true }) {
                Text("+ New List")
            }
        }

        if (lists.isEmpty()) {
            Text(
                text = "No lists yet. Create your first list!",
                modifier = Modifier
                    .fillMaxSize()
                    .wrapContentSize(Alignment.Center),
                style = MaterialTheme.typography.bodyLarge
            )
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(lists, key = { it.id ?: it.name }) { list ->
                    val isExpanded = list.id == expandedListId
                    val recipesInList = allRecipes.filter { recipe ->
                        list.recipeIds.contains(recipe.id)
                    }.filter { recipe ->
                        searchQuery.isEmpty() ||
                            recipe.title.contains(searchQuery, ignoreCase = true) ||
                            recipe.tags.any { tag -> tag.contains(searchQuery, ignoreCase = true) }
                    }

                    ListHeader(
                        list = list,
                        isExpanded = isExpanded,
                        recipeCount = recipesInList.size,
                        onExpand = { list.id?.let { onExpandList(it) } },
                        onDelete = { listToDelete = list }
                    )

                    if (isExpanded) {
                        if (searchQuery.isNotEmpty()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = list.name,
                                    style = MaterialTheme.typography.labelSmall,
                                    modifier = Modifier
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                        if (recipesInList.isEmpty()) {
                            val emptyText = if (searchQuery.isEmpty()) {
                                "No recipes in this list yet."
                            } else {
                                "No matching recipes."
                            }
                            Text(
                                text = emptyText,
                                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            recipesInList.forEach { recipe ->
                                RecipeItem(
                                    recipe = recipe,
                                    onRecipeClick = onRecipeClick,
                                    onRecipeRemove = {
                                        val listId = list.id ?: return@RecipeItem
                                        val recipeId = recipe.id ?: return@RecipeItem
                                        onRemoveRecipeFromList(recipeId, listId)
                                    },
                                    recipeRemoveEnabled = true,
                                    painter = rememberAsyncImagePainter(recipe.imageUrl),
                                    recipeLists = emptyList(),
                                    onAddToListClick = null
                                )
                            }
                        }
                    }
                }
            }
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
                list.id?.let { onDeleteList(it) }
                listToDelete = null
            },
            onDismiss = { listToDelete = null }
        )
    }
}

@Composable
private fun ListHeader(
    list: RecipeList,
    isExpanded: Boolean,
    recipeCount: Int,
    onExpand: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp)
            .clickable { onExpand() },
        elevation = CardDefaults.elevatedCardElevation(2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = list.name, style = MaterialTheme.typography.titleMedium)
                Text(
                    text = "$recipeCount recipe${if (recipeCount != 1) "s" else ""}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete list",
                    tint = Color.Red
                )
            }
            Icon(
                imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                contentDescription = if (isExpanded) "Collapse" else "Expand"
            )
        }
    }
}

@Composable
private fun CreateListDialog(
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New List") },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("List name") },
                singleLine = true,
                keyboardOptions = KeyboardOptions.Default.copy(
                    capitalization = KeyboardCapitalization.Sentences
                )
            )
        },
        confirmButton = {
            TextButton(
                onClick = { if (name.isNotBlank()) onConfirm(name.trim()) },
                enabled = name.isNotBlank()
            ) {
                Text("Create")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
private fun ConfirmDeleteListDialog(
    listName: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Delete list?") },
        text = { Text("Delete \"$listName\"? This will not remove the recipes themselves.") },
        confirmButton = {
            TextButton(onClick = onConfirm) { Text("Delete", color = Color.Red) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
private fun AddToListDialog(
    recipe: Recipe,
    lists: List<RecipeList>,
    onAddToList: (String) -> Unit,
    onRemoveFromList: (String) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add to list") },
        text = {
            if (lists.isEmpty()) {
                Text("No lists yet — create one in the My lists tab.")
            } else {
                Column {
                    lists.forEach { list ->
                        val inList = list.recipeIds.contains(recipe.id)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    val listId = list.id ?: return@clickable
                                    if (inList) onRemoveFromList(listId) else onAddToList(listId)
                                }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = inList,
                                onCheckedChange = { checked ->
                                    val listId = list.id ?: return@Checkbox
                                    if (checked) onAddToList(listId) else onRemoveFromList(listId)
                                }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(list.name, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Done") }
        }
    )
}

@Composable
fun RecipeList(
    recipes: List<Recipe>,
    onRecipeClick: (Recipe) -> Unit,
    onRecipeRemove: (Recipe) -> Unit,
    listState: LazyListState,
    recipeRemoveEnabled: Boolean,
    listNamesForRecipe: (Recipe) -> List<String> = { emptyList() },
    onAddToListClick: ((Recipe) -> Unit)? = null
) {
    if (recipes.isEmpty()) {
        Text(
            text = "No recipes found!",
            modifier = Modifier
                .fillMaxSize()
                .wrapContentSize(Alignment.Center),
            style = MaterialTheme.typography.headlineSmall
        )
    } else {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize()
        ) {
            items(recipes) { recipe ->
                RecipeItem(
                    recipe = recipe,
                    onRecipeClick = onRecipeClick,
                    onRecipeRemove = onRecipeRemove,
                    recipeRemoveEnabled = recipeRemoveEnabled,
                    painter = rememberAsyncImagePainter(recipe.imageUrl),
                    recipeLists = listNamesForRecipe(recipe),
                    onAddToListClick = onAddToListClick?.let { callback -> { callback(recipe) } }
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RecipeItem(
    recipe: Recipe,
    onRecipeClick: (Recipe) -> Unit,
    onRecipeRemove: (Recipe) -> Unit,
    recipeRemoveEnabled: Boolean,
    painter: Painter,
    recipeLists: List<String> = emptyList(),
    onAddToListClick: (() -> Unit)? = null
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .clickable { onRecipeClick(recipe) },
        elevation = CardDefaults.elevatedCardElevation(4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (recipe.imageUrl?.isNotEmpty() == true) {
                    Image(
                        painter = painter,
                        contentDescription = "Recipe Image",
                        modifier = Modifier
                            .width(150.dp)
                            .height(100.dp)
                            .padding(end = 10.dp)
                    )
                }
                Text(
                    text = recipe.title,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier
                        .weight(1f)
                        .align(Alignment.CenterVertically)
                )
                if (onAddToListClick != null) {
                    IconButton(
                        onClick = onAddToListClick,
                        modifier = Modifier.align(Alignment.CenterVertically)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlaylistAdd,
                            contentDescription = "Add to list"
                        )
                    }
                }
                if (recipeRemoveEnabled) {
                    IconButton(
                        onClick = { onRecipeRemove(recipe) },
                        modifier = Modifier.align(Alignment.CenterVertically)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Remove recipe from collection",
                            tint = Color.Red
                        )
                    }
                }
            }
            if (recipe.tags.isNotEmpty() || recipeLists.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    recipe.tags.forEach { tag ->
                        SuggestionChip(
                            onClick = {},
                            label = { Text(text = tag, style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                    recipeLists.forEach { listName ->
                        SuggestionChip(
                            onClick = {},
                            label = {
                                Text(
                                    text = listName,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        )
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewRecipeListRoute() {
    RecipeListRoute(
        signedIn = true,
        recipesSource = RecipeSource.USER_FAVOURITES,
        onClickMyLists = {},
        onClickUserFavourites = { },
        onClickAllRecipes = {},
        searchQuery = "Search Query",
        onSearchQueryChanged = { },
        listState = rememberLazyListState(),
        filteredRecipes = listOf(Recipe(title = "West African Peanut stew"), Recipe(title = "Pasta Carbonara")),
        onRecipeClick = {},
        onRecipeRemoveClick = {},
        lists = emptyList(),
        expandedListId = null,
        allRecipes = emptyList(),
        onCreateList = {},
        onDeleteList = {},
        onExpandList = {},
        onAddRecipeToList = { _, _ -> },
        onRemoveRecipeFromList = { _, _ -> },
        listNamesForRecipe = { emptyList() }
    )
}

@Preview(showBackground = true)
@Composable
fun PreviewRecipeItemWithImage() {
    RecipeItem(
        onRecipeClick = {},
        onRecipeRemove = {},
        recipe = Recipe(title = "Pasta Carbonara", imageUrl = "whatever"),
        recipeRemoveEnabled = true,
        painter = painterResource(id = R.drawable.test),
        recipeLists = listOf("Work week"),
        onAddToListClick = {}
    )
}
