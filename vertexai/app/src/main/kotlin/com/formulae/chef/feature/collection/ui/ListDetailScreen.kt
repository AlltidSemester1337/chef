package com.formulae.chef.feature.collection.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.formulae.chef.feature.model.Recipe
import com.formulae.chef.feature.model.RecipeList
import com.formulae.chef.ui.theme.AppTypography
import com.formulae.chef.ui.theme.Terracotta600
import com.formulae.chef.ui.theme.TextPrimary
import com.formulae.chef.ui.theme.TextSecondary
import com.formulae.chef.ui.theme.White

/**
 * Dedicated full-screen view for a single list, replacing the previous in-place expansion of a
 * list row within the Saved tab.
 */
@Composable
internal fun ListDetailScreen(
    list: RecipeList,
    recipes: List<Recipe>,
    searchQuery: String,
    onSearchQueryChanged: (String) -> Unit,
    onBack: () -> Unit,
    onDeleteList: () -> Unit,
    onRecipeClick: (Recipe) -> Unit,
    onRemoveFromList: (Recipe) -> Unit,
    onDiscoverRecipes: () -> Unit
) {
    var showDeleteConfirm by remember { mutableStateOf(false) }

    val filteredRecipes = recipes.filter { recipe ->
        searchQuery.isEmpty() ||
            recipe.title.contains(searchQuery, ignoreCase = true) ||
            recipe.tags.any { tag -> tag.contains(searchQuery, ignoreCase = true) }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(24.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextPrimary)
            }
            Text(
                text = list.name,
                style = AppTypography.headlineLarge.copy(color = TextPrimary),
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 4.dp)
            )
            IconButton(onClick = { showDeleteConfirm = true }) {
                Icon(Icons.Default.MoreVert, contentDescription = "List options", tint = TextSecondary)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (recipes.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .wrapContentSize(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "No recipes in this list yet",
                    style = AppTypography.headlineLarge.copy(color = TextPrimary),
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Add recipes to this list to see them here.",
                    style = AppTypography.bodyLarge,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = onDiscoverRecipes,
                    colors = ButtonDefaults.buttonColors(containerColor = Terracotta600)
                ) {
                    Text("Discover recipes", style = AppTypography.labelLarge.copy(color = White))
                }
            }
        } else {
            CollectionSearchBar(
                searchQuery = searchQuery,
                onSearchQueryChanged = onSearchQueryChanged
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "${filteredRecipes.size} recipe${if (filteredRecipes.size != 1) "s" else ""} in this list",
                style = AppTypography.bodyMedium
            )

            Spacer(modifier = Modifier.height(8.dp))

            LazyColumn(modifier = Modifier.fillMaxSize()) {
                recipeCardRows(
                    recipes = filteredRecipes,
                    onRecipeClick = onRecipeClick,
                    onBookmarkClick = onRemoveFromList,
                    onAddToListClick = null
                )
            }
        }
    }

    if (showDeleteConfirm) {
        ConfirmDeleteListDialog(
            listName = list.name,
            onConfirm = {
                showDeleteConfirm = false
                onDeleteList()
            },
            onDismiss = { showDeleteConfirm = false }
        )
    }
}
