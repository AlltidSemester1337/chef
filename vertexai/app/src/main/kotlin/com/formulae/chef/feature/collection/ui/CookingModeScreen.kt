package com.formulae.chef.feature.collection.ui

import android.app.Activity
import android.view.WindowManager
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.formulae.chef.feature.model.Difficulty
import com.formulae.chef.feature.model.Ingredient
import com.formulae.chef.feature.model.Nutrient
import com.formulae.chef.feature.model.Recipe
import com.formulae.chef.ui.theme.AppTypography
import com.formulae.chef.ui.theme.TextPrimary
import kotlinx.coroutines.launch

@Composable
internal fun CookingModeContent(
    recipe: Recipe,
    showIngredients: Boolean,
    checkedSteps: Set<Int>,
    scrollState: ScrollState,
    onStepChecked: (Int) -> Unit,
    onStepUnchecked: (Int) -> Unit
) {
    val activity = LocalContext.current as? Activity
    DisposableEffect(Unit) {
        activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onDispose {
            activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    val coroutineScope = rememberCoroutineScope()
    val stepHeights = remember { mutableStateMapOf<Int, Int>() }

    if (showIngredients) {
        // Ingredient quantities arrive already scaled to the selected servings (see DetailScreen)
        Text(text = "Ingredients", style = AppTypography.labelLarge.copy(color = TextPrimary))
        Spacer(modifier = Modifier.height(8.dp))
        recipe.ingredients.forEach { ingredient ->
            val unit = ingredient.unit?.takeIf { it.isNotBlank() }?.let { "$it " } ?: ""
            Text(
                text = "• ${ingredient.quantity.orEmpty()} $unit${ingredient.name}",
                style = AppTypography.bodyLarge.copy(color = TextPrimary),
                modifier = Modifier.padding(vertical = 2.dp)
            )
        }
    } else {
        // Instructions with checkboxes and auto-scroll
        Text(text = "Instructions", style = AppTypography.labelLarge.copy(color = TextPrimary))
        Spacer(modifier = Modifier.height(8.dp))
        recipe.instructions.forEachIndexed { index, step ->
            val isChecked = index in checkedSteps
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .onSizeChanged { size -> stepHeights[index] = size.height },
                verticalAlignment = Alignment.Top
            ) {
                Checkbox(
                    checked = isChecked,
                    onCheckedChange = { checked ->
                        if (checked) {
                            onStepChecked(index)
                            coroutineScope.launch {
                                scrollState.animateScrollTo(
                                    scrollState.value + (stepHeights[index] ?: 0)
                                )
                            }
                        } else {
                            onStepUnchecked(index)
                        }
                    }
                )
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(top = 12.dp)
                ) {
                    Text(
                        text = "${index + 1}. $step",
                        style = AppTypography.bodyLarge.copy(color = TextPrimary),
                        textDecoration = if (isChecked) TextDecoration.LineThrough else TextDecoration.None
                    )
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewCookingModeContent() {
    androidx.compose.foundation.layout.Box {
        CookingModeContent(
            recipe = Recipe(
                title = "Lebanese Kafta Kebabs",
                servings = "4 servings",
                ingredients = listOf(
                    Ingredient(name = "ground lamb", quantity = "500", unit = "g"),
                    Ingredient(name = "large onion", quantity = "1", unit = "each"),
                    Ingredient(name = "fresh parsley", quantity = "1/2", unit = "cup"),
                    Ingredient(name = "ground cumin", quantity = "1", unit = "tbsp")
                ),
                instructions = listOf(
                    "Combine ground lamb, onion, garlic, parsley, and spices. Mix until combined.",
                    "Divide mixture into 4 portions and shape into kebabs.",
                    "Cook over medium-high heat for 4–5 minutes per side."
                ),
                difficulty = Difficulty.EASY,
                nutrientsPerServing = listOf(Nutrient(name = "Calories", quantity = "550", unit = "kcal"))
            ),
            showIngredients = false,
            checkedSteps = setOf(0),
            scrollState = androidx.compose.foundation.rememberScrollState(),
            onStepChecked = {},
            onStepUnchecked = {}
        )
    }
}
