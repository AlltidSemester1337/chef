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

import android.graphics.Color
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import com.formulae.chef.feature.model.Recipe

@Composable
internal fun DetailRoute(
    recipe: Recipe,
    navController: NavController
) {

    var showIngredients by remember { mutableStateOf(true) }
    val scrollState = rememberScrollState()
    var hasImage = recipe.imageUrl?.isNotEmpty() ?: false
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current

    BackHandler {
        navController.navigate("collection")
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState),
    ) {

        Text(text = recipe.title, style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(8.dp))

        // Conditional Display
        if (hasImage) {
            Image(
                painter = rememberAsyncImagePainter(recipe.imageUrl),
                contentDescription = "Recipe Image",
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .clip(RoundedCornerShape(8.dp))
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        Text(text = recipe.summary.replace("\\n", "\n"), style = MaterialTheme.typography.bodyMedium)
        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Button(
                onClick = { showIngredients = true },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (showIngredients) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface
                )
            ) {
                Text("Ingredients")
            }

            Button(
                onClick = { showIngredients = false },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (!showIngredients) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface
                )
            ) {
                Text("Instructions")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Conditional Display
        if (showIngredients) {
            Text(text = "Ingredients", style = MaterialTheme.typography.headlineSmall)
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = recipe.ingredients.replace("\\n", "\n"), style = MaterialTheme.typography.bodyMedium)
            Spacer(modifier = Modifier.height(8.dp))
        } else {
            Text(text = "Instructions", style = MaterialTheme.typography.headlineSmall)
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = recipe.instructions.replace("\\n", "\n"), style = MaterialTheme.typography.bodyMedium)
        }

        // Centered Share Button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 30.dp),
            horizontalArrangement = Arrangement.Center
        ) {
            Button(
                onClick = {
                    clipboardManager.setText(AnnotatedString("https://humlekotte.nu/chef-web/recipe/?id=${recipe.id}"))
                    Toast.makeText(
                        context,
                        "Recipe URL copied to clipboard",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            ) {
                Text("Share Recipe")
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewDetailRoute() {
    DetailRoute(
        Recipe(
            title = "West African Peanut Stew (Peanut Butter Stew)",
            summary = "...", // Shortened for readability
            ingredients = "...",
            instructions = "...",
            imageUrl = "https://storage.googleapis.com/idyllic-bloom-425307-r6.firebasestorage.app/recipes/71204b99-36e5-419d-8fed-8fba949bd3d4"
        ),
        navController = NavController(LocalContext.current)
    )

}


