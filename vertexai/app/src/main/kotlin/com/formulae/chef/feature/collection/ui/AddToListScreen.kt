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
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
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
 * Full-screen replacement for the previous AlertDialog-based "add to list" flow: lets the user
 * toggle a recipe's membership across their lists, or create a new list on the spot.
 */
@Composable
internal fun AddToListScreen(
    recipeId: String?,
    lists: List<RecipeList>,
    onToggleList: (RecipeList) -> Unit,
    onCreateNewList: (String) -> Unit,
    onClose: () -> Unit,
    allRecipes: List<Recipe> = emptyList()
) {
    var showCreateDialog by remember { mutableStateOf(false) }

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
            Text(
                text = "Add recipe to list",
                style = AppTypography.headlineLarge.copy(color = TextPrimary),
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = onClose) {
                Icon(Icons.Default.Close, contentDescription = "Close", tint = TextPrimary)
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        Text(text = "Select a list below to add the recipe.", style = AppTypography.bodyLarge)
        Spacer(modifier = Modifier.height(16.dp))

        if (lists.isEmpty()) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .wrapContentSize(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "No lists yet",
                    style = AppTypography.headlineLarge.copy(color = TextPrimary),
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Create a list to add the recipe.",
                    style = AppTypography.bodyLarge,
                    textAlign = TextAlign.Center
                )
            }
        } else {
            LazyColumn(modifier = Modifier.weight(1f)) {
                items(lists, key = { it.id ?: it.name }) { list ->
                    val inList = recipeId != null && list.recipeIds.contains(recipeId)
                    ListRow(
                        list = list,
                        onClick = { onToggleList(list) },
                        thumbnailUrl = firstRecipeThumbnail(list, allRecipes),
                        trailingContent = {
                            IconButton(onClick = { onToggleList(list) }) {
                                Icon(
                                    imageVector = if (inList) Icons.Default.CheckCircle else Icons.Default.AddCircle,
                                    contentDescription = if (inList) "Remove from list" else "Add to list",
                                    tint = if (inList) Terracotta600 else TextSecondary
                                )
                            }
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = { showCreateDialog = true },
            colors = ButtonDefaults.buttonColors(containerColor = Terracotta600),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Create new list", style = AppTypography.labelLarge.copy(color = White))
        }

        Spacer(modifier = Modifier.height(16.dp))
    }

    if (showCreateDialog) {
        CreateListDialog(
            onConfirm = { name ->
                onCreateNewList(name)
                showCreateDialog = false
            },
            onDismiss = { showCreateDialog = false }
        )
    }
}
