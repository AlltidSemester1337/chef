package com.formulae.chef.feature.collection.ui

import android.app.Activity
import android.view.WindowManager
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import kotlin.math.floor
import kotlinx.coroutines.launch

@Composable
internal fun CookingModeContent(
    recipe: Recipe,
    showIngredients: Boolean,
    checkedSteps: Set<Int>,
    currentServings: Int?,
    scrollState: ScrollState,
    onStepChecked: (Int) -> Unit,
    onServingsChanged: (Int) -> Unit
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

    val originalServings = parseServingsCount(recipe.servings)
    val displayServings = currentServings ?: originalServings ?: 1
    val multiplier = displayServings.toDouble() / (originalServings?.toDouble() ?: 1.0)

    // Servings stepper
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        IconButton(onClick = { onServingsChanged((displayServings - 1).coerceAtLeast(1)) }) {
            Icon(Icons.Default.Remove, contentDescription = "Decrease servings")
        }
        Text(
            text = "$displayServings servings",
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(horizontal = 8.dp)
        )
        IconButton(onClick = { onServingsChanged(displayServings + 1) }) {
            Icon(Icons.Default.Add, contentDescription = "Increase servings")
        }
    }

    Spacer(modifier = Modifier.height(16.dp))

    if (showIngredients) {
        // Ingredients with scaled quantities
        Text(text = "Ingredients", style = MaterialTheme.typography.headlineSmall)
        Spacer(modifier = Modifier.height(8.dp))
        recipe.ingredients.forEach { ingredient ->
            val scaledQty = scaleQuantity(ingredient.quantity, multiplier)
            val unit = ingredient.unit?.takeIf { it.isNotBlank() }?.let { "$it " } ?: ""
            Text(
                text = "• $scaledQty $unit${ingredient.name}",
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(vertical = 2.dp)
            )
        }
    } else {
        // Instructions with checkboxes and auto-scroll
        Text(text = "Instructions", style = MaterialTheme.typography.headlineSmall)
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
                        style = MaterialTheme.typography.bodyLarge,
                        textDecoration = if (isChecked) TextDecoration.LineThrough else TextDecoration.None
                    )
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
        }
    }
}

private fun scaleQuantity(quantity: String?, multiplier: Double): String {
    if (quantity.isNullOrBlank()) return quantity ?: ""
    quantity.toDoubleOrNull()?.let { return formatScaled(it * multiplier) }
    Regex("""^(\d+)/(\d+)$""").matchEntire(quantity.trim())?.let { m ->
        val num = m.groupValues[1].toDouble()
        val den = m.groupValues[2].toDouble()
        return formatScaled(num / den * multiplier)
    }
    return quantity
}

private fun formatScaled(value: Double): String =
    if (value == floor(value)) {
        value.toLong().toString()
    } else {
        "%.2f".format(value).trimEnd('0').trimEnd('.')
    }

private fun parseServingsCount(servings: String?): Int? =
    servings?.let { Regex("""\d+""").find(it)?.value?.toIntOrNull() }

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
            currentServings = 4,
            scrollState = androidx.compose.foundation.rememberScrollState(),
            onStepChecked = {},
            onServingsChanged = {}
        )
    }
}
