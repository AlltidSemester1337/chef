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
import androidx.compose.foundation.layout.Arrangement
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
import com.formulae.chef.feature.collection.CollectionViewModel
import com.formulae.chef.feature.model.Difficulty
import com.formulae.chef.feature.model.Ingredient
import com.formulae.chef.feature.model.Nutrient
import com.formulae.chef.feature.model.Recipe

@Composable
internal fun DetailRoute(
    collectionViewModel: CollectionViewModel,
    recipe: Recipe,
    navController: NavController
) {

    val onToggleCookingModeClick = collectionViewModel::onToggleCookingMode

    BackHandler {
        navController.navigate("collection")
    }

    CreateDetailScreen(
        recipe,
        onToggleCookingModeClick,
    )
}

@Composable
private fun CreateDetailScreen(
    recipe: Recipe,
    // TODO: DEV-47
    onToggleCookingModeClick: () -> Unit,
) {
    var showIngredients by remember { mutableStateOf(true) }
    val scrollState = rememberScrollState()
    val hasImage = recipe.imageUrl?.isNotEmpty() ?: false
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current

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
            //Button(
            //    onClick = onToggleCookingModeClick,
            //    modifier = Modifier
            //        .fillMaxWidth()
            //) {
            //    Text("Start cooking!")
            //}
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
            Text(text = recipe.ingredients.joinToString("\n"), style = MaterialTheme.typography.bodyMedium)
            Spacer(modifier = Modifier.height(8.dp))
        } else {
            Text(text = "Instructions", style = MaterialTheme.typography.headlineSmall)
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = recipe.instructions.joinToString("\n"), style = MaterialTheme.typography.bodyMedium)
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
fun PreviewCreateDetailScreen() {
    CreateDetailScreen(
        Recipe(
            title = "West African Peanut Stew (Peanut Butter Stew)",
            summary = "This recipe features flavorful Lebanese-style kafta kebabs, cooked to juicy perfection, served with a vibrant harissa yogurt sauce and a medley of roasted vegetables.\\n\\n**Yields:** 4 servings\\n\\n**Nutritional Information per serving (approximate):**\\n\\n* Calories: 550 kcal\\n* Protein: 30g\\n* Carbohydrates: 40g\\n* Fat: 25g\\n\\n\\n", // Shortened for readability
            servings = "4 servings",
            prepTime = "30 minutes",
            cookingTime = "2 hours",
            nutrientsPerServing = listOf(
                Nutrient(name = "Calories", quantity = "550", unit = "kcal"),
                Nutrient(name = "Protein", quantity = "30", unit = "g"),
                Nutrient(name = "Carbohydrates", quantity = "40", unit = "g"),
                Nutrient(name = "Fat", quantity = "25", unit = "g")
            ),
            ingredients = listOf(
                Ingredient(name = "ground lamb or a mix of ground lamb and beef", quantity = "500", unit = "g"),
                Ingredient(name = "large onion", quantity = "1", unit = "each"),
                Ingredient(name = "garlic", quantity = "2", unit = "cloves"),
                Ingredient(name = "fresh parsley", quantity = "1/2 cup", unit = "(20g)"),
                Ingredient(name = "fresh mint", quantity = "1/4 cup", unit = "(10g)"),
                Ingredient(name = "ground cumin", quantity = "1 tbsp", unit = "(15g)"),
                Ingredient(name = "ground coriander", quantity = "1 tsp", unit = "(5g)"),
                Ingredient(name = "allspice", quantity = "1/2 tsp", unit = "(2.5g)"),
                Ingredient(name = "cayenne pepper", quantity = "1/4 tsp", unit = "(optional)"),
                Ingredient(name = "Greek yogurt", quantity = "250g", unit = "plain"),
                Ingredient(name = "harissa paste", quantity = "1 tbsp", unit = "(15g)"),
                Ingredient(name = "lemon juice", quantity = "1 tbsp", unit = "(15ml)"),
                Ingredient(name = "salt", quantity = "1/4 tsp", unit = null),
                Ingredient(name = "sweet potato", quantity = "1 large", unit = null),
                Ingredient(name = "red bell pepper", quantity = "1", unit = "each"),
                Ingredient(name = "broccoli florets", quantity = "1/2 cup", unit = null),
                Ingredient(name = "olive oil", quantity = "1 tbsp", unit = "(15ml)"),
                Ingredient(name = "black pepper", quantity = "1/4 tsp", unit = null)
            ),
            difficulty = Difficulty.EASY,
            instructions = listOf(
                "Combine ground lamb (or mix), onion, garlic, parsley, mint, cumin, coriander, allspice, cayenne pepper (if using), salt, and pepper. Gently mix until combined.",
                "Divide mixture into 4 portions. Shape into kebabs (~10-15cm) or make meatballs.",
                "Mix yogurt, harissa paste, lemon juice, and salt to make sauce.",
                "Toss sweet potato, bell pepper, and broccoli with olive oil, salt, and pepper. Spread on baking sheet.",
                "Roast vegetables at 200°C (400°F) for 20-25 minutes until tender.",
                "Cook kebabs in a pan/skillet over medium-high heat for 4-5 minutes per side, or use grill/oven for meatballs.",
                "Serve kebabs with sauce and roasted vegetables"
            ),
            tipsAndTricks = "For milder flavor: reduce/omit cayenne pepper.\n" +
                    "Use zucchini, carrots, or Brussels sprouts instead of listed vegetables.\n" +
                    "Serve with pita, hummus, or tabbouleh for a complete meal.",
            imageUrl = "https://storage.googleapis.com/idyllic-bloom-425307-r6.firebasestorage.app/recipes/71204b99-36e5-419d-8fed-8fba949bd3d4"
        ),
        onToggleCookingModeClick = {}
    )

}


