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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import com.formulae.chef.BuildConfig
import com.formulae.chef.feature.model.Difficulty
import com.formulae.chef.feature.model.Ingredient
import com.formulae.chef.feature.model.Nutrient
import com.formulae.chef.feature.model.Recipe
import com.formulae.chef.feature.model.RecipeVariant
import com.formulae.chef.services.voice.AudioPlayer
import com.formulae.chef.services.voice.GcpTextToSpeechService
import com.formulae.chef.services.voice.buildTtsFlow

@Composable
internal fun DetailRoute(
    recipe: Recipe,
    onBack: () -> Unit,
    isCookingMode: Boolean = false,
    showIngredients: Boolean = true,
    checkedSteps: Set<Int> = emptySet(),
    currentServings: Int? = null,
    listNames: List<String> = emptyList(),
    variants: List<RecipeVariant> = emptyList(),
    selectedVariantId: String? = null,
    isOwner: Boolean = false,
    onToggleCookingMode: () -> Unit = {},
    onTabChanged: (Boolean) -> Unit = {},
    onStepChecked: (Int) -> Unit = {},
    onStepUnchecked: (Int) -> Unit = {},
    onServingsChanged: (Int) -> Unit = {},
    onVariantSelected: (String?) -> Unit = {},
    onPinVariant: (String?) -> Unit = {},
    onDeleteVariant: (String) -> Unit = {},
    onStartCreateVariant: () -> Unit = {}
) {
    BackHandler { onBack() }
    CreateDetailScreen(
        recipe = recipe,
        isCookingMode = isCookingMode,
        showIngredients = showIngredients,
        checkedSteps = checkedSteps,
        currentServings = currentServings,
        listNames = listNames,
        variants = variants,
        selectedVariantId = selectedVariantId,
        isOwner = isOwner,
        onToggleCookingMode = onToggleCookingMode,
        onTabChanged = onTabChanged,
        onStepChecked = onStepChecked,
        onStepUnchecked = onStepUnchecked,
        onServingsChanged = onServingsChanged,
        onVariantSelected = onVariantSelected,
        onPinVariant = onPinVariant,
        onDeleteVariant = onDeleteVariant,
        onStartCreateVariant = onStartCreateVariant
    )
}

@Composable
private fun CreateDetailScreen(
    recipe: Recipe,
    isCookingMode: Boolean,
    showIngredients: Boolean,
    checkedSteps: Set<Int>,
    currentServings: Int?,
    listNames: List<String> = emptyList(),
    variants: List<RecipeVariant> = emptyList(),
    selectedVariantId: String? = null,
    isOwner: Boolean = false,
    onToggleCookingMode: () -> Unit,
    onTabChanged: (Boolean) -> Unit,
    onStepChecked: (Int) -> Unit,
    onStepUnchecked: (Int) -> Unit,
    onServingsChanged: (Int) -> Unit,
    onVariantSelected: (String?) -> Unit = {},
    onPinVariant: (String?) -> Unit = {},
    onDeleteVariant: (String) -> Unit = {},
    onStartCreateVariant: () -> Unit = {}
) {
    val scrollState = rememberScrollState()
    val hasImage = recipe.imageUrl?.isNotEmpty() ?: false
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current

    val ttsService = remember { GcpTextToSpeechService(BuildConfig.gcpTtsApiKey) }
    val audioPlayer = remember { AudioPlayer(context) }
    DisposableEffect(Unit) {
        onDispose { audioPlayer.release() }
    }
    val isSpeaking by audioPlayer.isSpeaking.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
    ) {
        Text(text = recipe.title, style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(8.dp))

        if (hasImage) {
            Image(
                painter = rememberAsyncImagePainter(recipe.imageUrl),
                contentDescription = "Recipe Image",
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .clip(RoundedCornerShape(8.dp))
            )
        }

        if (isOwner || variants.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))

            VariantPickerRow(
                variants = variants,
                selectedVariantId = selectedVariantId,
                isOwner = isOwner,
                onVariantSelected = onVariantSelected,
                onPinVariant = onPinVariant,
                onDeleteVariant = onDeleteVariant,
                onCreateVariant = onStartCreateVariant
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Cooking mode toggle — always below image
        if (!isCookingMode) {
            Button(
                onClick = onToggleCookingMode,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Let's cook!")
            }
        } else {
            IconButton(
                onClick = onToggleCookingMode,
                modifier = Modifier.align(Alignment.End)
            ) {
                Icon(Icons.Default.Close, contentDescription = "Exit cooking mode")
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(text = recipe.summary.replace("\\n", "\n"), style = MaterialTheme.typography.bodyMedium)
        Spacer(modifier = Modifier.height(8.dp))

        // Tab toggle — shown in both normal and cooking mode
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Button(
                onClick = { onTabChanged(true) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (showIngredients) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.surface
                    }
                )
            ) {
                Text("Ingredients")
            }

            Button(
                onClick = { onTabChanged(false) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (!showIngredients) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.surface
                    }
                )
            ) {
                Text("Instructions")
            }
        }

        // Voice playback button — just below tab toggle, aligned right
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            IconButton(
                onClick = {
                    if (isSpeaking) {
                        audioPlayer.stop()
                    } else {
                        val sentences = if (showIngredients) {
                            buildIngredientSentences(recipe)
                        } else {
                            val stepText = buildInstructionStepText(recipe, checkedSteps)
                            if (stepText.isNotBlank()) listOf(stepText) else emptyList()
                        }
                        audioPlayer.playChunked(
                            buildTtsFlow(sentences, ttsService, context, "DetailScreen"),
                            "recipe-${recipe.id}"
                        )
                    }
                }
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                    contentDescription = if (isSpeaking) "Stop reading" else "Read aloud",
                    tint = if (isSpeaking) MaterialTheme.colorScheme.primary else Color.Gray
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (isCookingMode) {
            CookingModeContent(
                recipe = recipe,
                showIngredients = showIngredients,
                checkedSteps = checkedSteps,
                currentServings = currentServings,
                scrollState = scrollState,
                onStepChecked = onStepChecked,
                onStepUnchecked = onStepUnchecked,
                onServingsChanged = onServingsChanged
            )
        } else {
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
        }

        if (listNames.isNotEmpty()) {
            Text(
                text = "Lists: ${listNames.joinToString(", ")}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }

        recipe.tipsAndTricks?.takeIf { it.isNotBlank() }?.let { tips ->
            TipsSection(tipsAndTricks = tips)
        }

        // Share Button
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
        recipe = Recipe(
            title = "West African Peanut Stew (Peanut Butter Stew)",
            summary = "This recipe features flavorful Lebanese-style kafta kebabs, cooked to juicy perfection, " +
                "served with a vibrant harissa yogurt sauce and a medley of roasted vegetables.\\n\\n" +
                "**Yields:** 4 servings\\n\\n**Nutritional Information per serving (approximate):**\\n\\n" +
                "* Calories: 550 kcal\\n* Protein: 30g\\n* Carbohydrates: 40g\\n* Fat: 25g\\n\\n\\n",
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
                Ingredient(name = "fresh parsley", quantity = "0.5", unit = "cup"),
                Ingredient(name = "fresh mint", quantity = "0.25", unit = "cup"),
                Ingredient(name = "ground cumin", quantity = "1", unit = "tbsp"),
                Ingredient(name = "ground coriander", quantity = "1", unit = "tsp"),
                Ingredient(name = "allspice", quantity = "0.5", unit = "tsp"),
                Ingredient(name = "cayenne pepper", quantity = "0.25", unit = "tsp"),
                Ingredient(name = "Greek yogurt", quantity = "250", unit = "g"),
                Ingredient(name = "harissa paste", quantity = "1", unit = "tbsp"),
                Ingredient(name = "lemon juice", quantity = "1", unit = "tbsp"),
                Ingredient(name = "salt", quantity = "0.25", unit = "tsp"),
                Ingredient(name = "sweet potato", quantity = "1", unit = "large"),
                Ingredient(name = "red bell pepper", quantity = "1", unit = "each"),
                Ingredient(name = "broccoli florets", quantity = "0.5", unit = "cup"),
                Ingredient(name = "olive oil", quantity = "1", unit = "tbsp"),
                Ingredient(name = "black pepper", quantity = "0.25", unit = "tsp")
            ),
            difficulty = Difficulty.EASY,
            instructions = listOf(
                "Combine ground lamb (or mix), onion, garlic, parsley, mint, cumin, coriander, allspice, " +
                    "cayenne pepper (if using), salt, and pepper. Gently mix until combined.",
                "Divide mixture into 4 portions. Shape into kebabs (~10-15cm) or make meatballs.",
                "Mix yogurt, harissa paste, lemon juice, and salt to make sauce.",
                "Toss sweet potato, bell pepper, and broccoli with olive oil, salt, and pepper. " +
                    "Spread on baking sheet.",
                "Roast vegetables at 200°C (400°F) for 20-25 minutes until tender.",
                "Cook kebabs in a pan/skillet over medium-high heat for 4-5 minutes per side, " +
                    "or use grill/oven for meatballs.",
                "Serve kebabs with sauce and roasted vegetables"
            ),
            tipsAndTricks = "For milder flavor: reduce/omit cayenne pepper.\n" +
                "Use zucchini, carrots, or Brussels sprouts instead of listed vegetables.\n" +
                "Serve with pita, hummus, or tabbouleh for a complete meal.",
            imageUrl = "https://storage.googleapis.com/idyllic-bloom-425307-r6.firebasestorage.app/" +
                "recipes/71204b99-36e5-419d-8fed-8fba949bd3d4"
        ),
        isCookingMode = false,
        showIngredients = true,
        checkedSteps = emptySet(),
        currentServings = null,
        onToggleCookingMode = {},
        onTabChanged = {},
        onStepChecked = {},
        onStepUnchecked = {},
        onServingsChanged = {}
    )
}

internal fun parseTips(tipsAndTricks: String): List<String> {
    val lines = tipsAndTricks.lines()
    val tips = mutableListOf<String>()
    val current = StringBuilder()
    for (line in lines) {
        when {
            line.startsWith("- ") -> {
                if (current.isNotEmpty()) tips.add(current.toString().trim())
                current.clear()
                current.append(line.removePrefix("- ").trim())
            }
            line.isNotBlank() -> {
                if (current.isNotEmpty()) current.append(" ")
                current.append(line.trim())
            }
        }
    }
    if (current.isNotEmpty()) tips.add(current.toString().trim())
    if (tips.isEmpty() && tipsAndTricks.isNotBlank()) tips.add(tipsAndTricks.trim())
    return tips
}

@Composable
private fun TipsSection(tipsAndTricks: String) {
    val tips = parseTips(tipsAndTricks)
    Text(text = "Tips", style = MaterialTheme.typography.headlineSmall)
    Spacer(modifier = Modifier.height(8.dp))
    tips.forEach { tip ->
        Text(
            text = "• $tip",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(bottom = 4.dp)
        )
    }
    Spacer(modifier = Modifier.height(8.dp))
}
