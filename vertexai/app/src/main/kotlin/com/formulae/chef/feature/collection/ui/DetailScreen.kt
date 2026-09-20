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
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Sell
import androidx.compose.material.icons.outlined.TipsAndUpdates
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import coil.compose.rememberAsyncImagePainter
import com.formulae.chef.BuildConfig
import com.formulae.chef.R
import com.formulae.chef.feature.collection.parseTips
import com.formulae.chef.feature.model.Difficulty
import com.formulae.chef.feature.model.Ingredient
import com.formulae.chef.feature.model.Nutrient
import com.formulae.chef.feature.model.Recipe
import com.formulae.chef.feature.model.RecipeVariant
import com.formulae.chef.services.voice.AudioPlayer
import com.formulae.chef.services.voice.GcpTextToSpeechService
import com.formulae.chef.services.voice.buildTtsFlow
import com.formulae.chef.ui.components.BottomWaveAccent
import com.formulae.chef.ui.components.BottomWaveAccentOffset
import com.formulae.chef.ui.components.SectionHeader
import com.formulae.chef.ui.components.SegmentedTabRow
import com.formulae.chef.ui.components.WaveDivider
import com.formulae.chef.ui.components.WavyBottomShape
import com.formulae.chef.ui.theme.AppTypography
import com.formulae.chef.ui.theme.BackgroundColor
import com.formulae.chef.ui.theme.Terracotta200
import com.formulae.chef.ui.theme.Terracotta50
import com.formulae.chef.ui.theme.Terracotta600
import com.formulae.chef.ui.theme.TextPrimary
import com.formulae.chef.ui.theme.TextSecondary

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
    onStartCreateVariant: () -> Unit = {},
    onNavigateToChat: () -> Unit = {}
) {
    BackHandler { onBack() }
    CreateDetailScreen(
        recipe = recipe,
        onBack = onBack,
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
        onStartCreateVariant = onStartCreateVariant,
        onNavigateToChat = onNavigateToChat
    )
}

@Composable
private fun CreateDetailScreen(
    recipe: Recipe,
    onBack: () -> Unit,
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
    onStartCreateVariant: () -> Unit = {},
    onNavigateToChat: () -> Unit = {}
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
        Box(modifier = Modifier.fillMaxWidth()) {
            if (hasImage) {
                Image(
                    painter = rememberAsyncImagePainter(recipe.imageUrl),
                    contentDescription = "Recipe Image",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(280.dp)
                        .clip(WavyBottomShape())
                )
                BottomWaveAccent(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .offset(y = BottomWaveAccentOffset)
                )
            } else {
                Spacer(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                )
            }
            HeaderIconButton(
                icon = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                onClick = onBack,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(8.dp)
            )
            HeaderIconButton(
                icon = Icons.Filled.Share,
                contentDescription = "Share recipe",
                onClick = {
                    clipboardManager.setText(
                        AnnotatedString("https://humlekotte.nu/chef-web/recipe/?id=${recipe.id}")
                    )
                    Toast.makeText(
                        context,
                        "Recipe URL copied to clipboard",
                        Toast.LENGTH_SHORT
                    ).show()
                },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(20.dp))

            Text(text = recipe.title, style = AppTypography.headlineLarge)

            val prepCookText = formatPrepCookTime(recipe.prepTime, recipe.cookingTime)
            val difficultyText = formatDifficultyLabel(recipe.difficulty)
            if (prepCookText != null || difficultyText != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    prepCookText?.let {
                        InfoIconText(icon = Icons.Outlined.Schedule, text = it, modifier = Modifier.weight(1f))
                    }
                    difficultyText?.let { InfoIconText(icon = Icons.Outlined.BarChart, text = it) }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = recipe.summary.replace("\\n", "\n"),
                style = AppTypography.bodyLarge.copy(color = TextPrimary)
            )

            recipe.videoUrl?.takeIf { it.isNotEmpty() }?.let { videoUrl ->
                Spacer(modifier = Modifier.height(16.dp))
                RecipeVideoSection(videoUrl = videoUrl)
            }

            if (isOwner || variants.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
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

            Spacer(modifier = Modifier.height(16.dp))

            // Cooking mode toggle — always below the description
            if (!isCookingMode) {
                Button(
                    onClick = onToggleCookingMode,
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.small,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("Let's cook!", style = AppTypography.labelLarge)
                }
            } else {
                IconButton(
                    onClick = onToggleCookingMode,
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Exit cooking mode", tint = TextPrimary)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            SegmentedTabRow(
                tabs = listOf("Ingredients", "Instructions"),
                selectedIndex = if (showIngredients) 0 else 1,
                onTabSelected = { index -> onTabChanged(index == 0) }
            )

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
                        tint = if (isSpeaking) Terracotta600 else TextSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

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
            } else if (showIngredients) {
                IngredientsTabContent(recipe = recipe)
            } else {
                InstructionsTabContent(recipe = recipe)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        WaveDivider()
        Spacer(modifier = Modifier.height(16.dp))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            if (listNames.isNotEmpty()) {
                SectionHeader(title = "Featured in lists")
                Spacer(modifier = Modifier.height(12.dp))
                ChipFlowRow(items = listNames)
            }

            if (recipe.tags.isNotEmpty()) {
                Spacer(modifier = Modifier.height(24.dp))
                SectionHeader(title = "Tags")
                Spacer(modifier = Modifier.height(12.dp))
                TagFlowRow(tags = recipe.tags)
            }

            recipe.tipsAndTricks?.takeIf { it.isNotBlank() }?.let { tips ->
                Spacer(modifier = Modifier.height(24.dp))
                TipsSection(tipsAndTricks = tips)
            }

            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = buildAnnotatedString {
                    append("Got a question or want to change something in this recipe? ")
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                        append("Chat with Chef anytime!")
                    }
                },
                style = AppTypography.bodyMedium.copy(fontStyle = FontStyle.Italic, color = TextPrimary),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp)
                    .clickable { onNavigateToChat() }
            )
        }
    }
}

@Composable
private fun HeaderIconButton(
    icon: ImageVector,
    contentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(36.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(BackgroundColor.copy(alpha = 0.9f)),
        contentAlignment = Alignment.Center
    ) {
        IconButton(onClick = onClick) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = TextPrimary,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun InfoIconText(icon: ImageVector, text: String, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = TextPrimary,
            modifier = Modifier
                .padding(top = 2.dp)
                .size(16.dp)
        )
        Text(text = text, style = AppTypography.bodyMedium.copy(color = TextPrimary))
    }
}

@Composable
private fun IngredientsTabContent(recipe: Recipe) {
    Box(modifier = Modifier.fillMaxWidth()) {
        Image(
            painter = painterResource(R.drawable.ic_recipe_carrot_decoration),
            contentDescription = null,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(end = 20.dp, top = 8.dp)
                .fillMaxWidth(0.64f)
        )
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "${recipe.ingredients.size} ingredients",
                style = AppTypography.bodyMedium.copy(fontStyle = FontStyle.Italic, color = TextSecondary)
            )
            recipe.ingredients.forEach { ingredient ->
                val quantity = listOfNotNull(
                    ingredient.quantity?.takeIf { it.isNotBlank() },
                    ingredient.unit?.takeIf { it.isNotBlank() }
                ).joinToString(" ")
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (quantity.isNotBlank()) {
                        Text(
                            text = quantity,
                            style = AppTypography.bodyLarge.copy(fontWeight = FontWeight.SemiBold, color = TextPrimary)
                        )
                    }
                    Text(
                        text = ingredient.name ?: "",
                        style = AppTypography.bodyLarge.copy(color = TextPrimary)
                    )
                }
            }
        }
    }
}

@Composable
private fun InstructionsTabContent(recipe: Recipe) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        recipe.instructions.forEachIndexed { index, step ->
            Text(
                text = "${index + 1}. $step",
                style = AppTypography.bodyLarge.copy(color = TextPrimary)
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ChipFlowRow(items: List<String>) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items.forEach { item ->
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(Terracotta50)
                    .border(1.dp, Terracotta200, RoundedCornerShape(8.dp))
                    .padding(horizontal = 12.dp, vertical = 4.dp)
            ) {
                Text(
                    text = item,
                    style = AppTypography.labelMedium.copy(color = Terracotta600, fontWeight = FontWeight.SemiBold)
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TagFlowRow(tags: List<String>) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        tags.forEach { tag ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Outlined.Sell,
                    contentDescription = null,
                    tint = Terracotta600,
                    modifier = Modifier.size(16.dp)
                )
                Text(text = tag, style = AppTypography.bodyMedium.copy(color = TextPrimary))
            }
        }
    }
}

internal fun formatPrepCookTime(prepTime: String?, cookingTime: String?): String? {
    val prep = prepTime?.trim()?.takeIf { it.isNotBlank() }
    val cook = cookingTime?.trim()?.takeIf { it.isNotBlank() }
    return when {
        prep != null && cook != null -> "Prep $prep, cook $cook"
        prep != null -> "Prep $prep"
        cook != null -> "Cook $cook"
        else -> null
    }
}

internal fun formatDifficultyLabel(difficulty: Difficulty?): String? =
    difficulty?.name?.lowercase()?.replaceFirstChar { it.uppercase() }

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
            tags = listOf("Stew", "Comfort food", "West African"),
            imageUrl = "https://storage.googleapis.com/idyllic-bloom-425307-r6.firebasestorage.app/" +
                "recipes/71204b99-36e5-419d-8fed-8fba949bd3d4"
        ),
        onBack = {},
        isCookingMode = false,
        showIngredients = true,
        checkedSteps = emptySet(),
        currentServings = null,
        listNames = listOf("Dinner", "Family favorites"),
        onToggleCookingMode = {},
        onTabChanged = {},
        onStepChecked = {},
        onStepUnchecked = {},
        onServingsChanged = {}
    )
}

@Composable
private fun TipsSection(tipsAndTricks: String) {
    val tips = parseTips(tipsAndTricks)
    if (tips.isEmpty()) return
    SectionHeader(title = "Tips & tricks")
    Spacer(modifier = Modifier.height(12.dp))
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        tips.forEach { tip ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(
                    imageVector = Icons.Outlined.TipsAndUpdates,
                    contentDescription = null,
                    tint = Terracotta600,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = tip,
                    style = AppTypography.bodyLarge.copy(color = TextPrimary),
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
internal fun RecipeVideoSection(videoUrl: String, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var isPlaying by remember { mutableStateOf(false) }
    val player = remember {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(videoUrl))
            prepare()
        }
    }
    DisposableEffect(player) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_ENDED) {
                    isPlaying = false
                }
            }
        }
        player.addListener(listener)
        onDispose {
            player.removeListener(listener)
            player.release()
        }
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = "🎬 Recipe of the Month",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 4.dp)
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .clip(RoundedCornerShape(8.dp))
        ) {
            AndroidView(
                factory = { ctx ->
                    PlayerView(ctx).apply {
                        this.player = player
                        useController = false
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
            if (!isPlaying) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
                    modifier = Modifier
                        .size(56.dp)
                        .align(Alignment.Center)
                ) {
                    IconButton(onClick = {
                        player.seekTo(0)
                        player.play()
                        isPlaying = true
                    }) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Play recipe video",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }
            }
        }
    }
}
