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

package com.formulae.chef.feature.collection

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.formulae.chef.CollectionViewModelFactory
import com.formulae.chef.feature.model.Recipe
import com.formulae.chef.services.persistence.RecipeRepository

@Composable
internal fun CollectionRoute(
    repository: RecipeRepository,
    collectionViewModel: CollectionViewModel = viewModel(factory = CollectionViewModelFactory(repository)),
    navController: NavController
) {
    val collectionUiState by collectionViewModel.uiState.collectAsState()
    val isLoading by collectionViewModel.isLoading.collectAsState()
    val listState = rememberLazyListState()
    val selectedRecipe by collectionViewModel.selectedRecipe.collectAsState()
    var searchQuery by rememberSaveable { mutableStateOf("") }
    val filteredMessages = collectionUiState.recipes.filter { recipe ->
        recipe.title.contains(searchQuery, ignoreCase = true)
    }

    BackHandler {
        navController.navigate("home")
    }

    Scaffold { paddingValues ->
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
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                ) {
                    SearchBar(
                        searchQuery = searchQuery,
                        onSearchQueryChanged = { query -> searchQuery = query },
                    )
                    // Recipe List
                    RecipeList(
                        recipes = filteredMessages,
                        onRecipeClick = { recipe: Recipe ->
                            // Navigate to RecipeDetail
                            collectionViewModel.onRecipeSelected(recipe)
                        },
                        onRecipeRemove = { recipeId: String ->
                            collectionViewModel.onRecipeRemove(recipeId)
                        },
                        listState = listState
                    )
                }
            } else {
                // Recipe Detail
                DetailRoute(recipe = selectedRecipe!!, navController = navController)
            }
        }
    }
}

@Composable
fun SearchBar(
    searchQuery: String,
    onSearchQueryChanged: (String) -> Unit,
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
fun RecipeList(
    recipes: List<Recipe>,
    onRecipeClick: (Recipe) -> Unit,
    onRecipeRemove: (String) -> Unit,
    listState: LazyListState
) {
    if (recipes.isEmpty()) {
        Text(
            text = "No recipes found!",
            modifier = Modifier
                .fillMaxSize()
                .wrapContentSize(Alignment.Center),
            style = MaterialTheme.typography.headlineSmall,
        )
    } else {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
        ) {
            items(recipes) { recipe ->
                RecipeItem(recipe = recipe, onRecipeClick = onRecipeClick, onRecipeRemove = onRecipeRemove)
            }
        }
    }
}


@Composable
fun RecipeItem(
    recipe: Recipe,
    onRecipeClick: (Recipe) -> Unit,
    onRecipeRemove: (String) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .clickable { onRecipeClick(recipe) },
        elevation = CardDefaults.elevatedCardElevation(4.dp),
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically // Ensures both items are aligned properly
        ) {
            Text(
                text = recipe.title,
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier
                    .weight(1f)
                    .align(Alignment.CenterVertically)
            )
            IconButton(
                onClick = {
                    onRecipeRemove(recipe.id!!)
                }, // Callback to handle add to collection
                modifier = Modifier
                    .align(Alignment.CenterVertically)
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Remove recipe from collection",
                    tint = Color.Red
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewSearchBar() {
    SearchBar(
        searchQuery = "Search Query",
        onSearchQueryChanged = { }
    )
}

@Preview(showBackground = true)
@Composable
fun PreviewRecipeList() {
    RecipeList(
        recipes = listOf(Recipe(title = "West African Peanut stew"), Recipe(title = "Pasta Carbonara")),
        onRecipeClick = {},
        onRecipeRemove = {},
        listState = rememberLazyListState()
    )
}
