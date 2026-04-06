package com.formulae.chef.feature.collection.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.formulae.chef.feature.model.Difficulty
import com.formulae.chef.feature.model.Ingredient
import com.formulae.chef.feature.model.Nutrient
import com.formulae.chef.feature.model.Recipe

@Composable
internal fun EditVariantScreen(
    baseRecipe: Recipe,
    onSave: (label: String, recipe: Recipe) -> Unit,
    onCancel: () -> Unit
) {
    BackHandler { onCancel() }

    var label by rememberSaveable { mutableStateOf("") }
    var title by rememberSaveable { mutableStateOf(baseRecipe.title) }
    var summary by rememberSaveable { mutableStateOf(baseRecipe.summary) }
    var servings by rememberSaveable { mutableStateOf(baseRecipe.servings ?: "") }
    var prepTime by rememberSaveable { mutableStateOf(baseRecipe.prepTime ?: "") }
    var cookingTime by rememberSaveable { mutableStateOf(baseRecipe.cookingTime ?: "") }
    var tipsAndTricks by rememberSaveable { mutableStateOf(baseRecipe.tipsAndTricks ?: "") }
    var difficulty by rememberSaveable { mutableStateOf(baseRecipe.difficulty ?: Difficulty.EASY) }

    var ingredients by rememberSaveable {
        mutableStateOf(
            baseRecipe.ingredients.map { Triple(it.name ?: "", it.quantity ?: "", it.unit ?: "") }
        )
    }
    var instructions by rememberSaveable {
        mutableStateOf(baseRecipe.instructions.toList())
    }
    var nutrients by rememberSaveable {
        mutableStateOf(
            (baseRecipe.nutrientsPerServing ?: emptyList()).map {
                Triple(it.name ?: "", it.quantity ?: "", it.unit ?: "")
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text("Create Variant", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = label,
            onValueChange = { label = it },
            label = { Text("Variant name *") },
            singleLine = true,
            keyboardOptions = KeyboardOptions.Default.copy(capitalization = KeyboardCapitalization.Sentences),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))
        Text("Recipe Details", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = title,
            onValueChange = { title = it },
            label = { Text("Title") },
            singleLine = true,
            keyboardOptions = KeyboardOptions.Default.copy(capitalization = KeyboardCapitalization.Sentences),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = summary,
            onValueChange = { summary = it },
            label = { Text("Summary") },
            minLines = 3,
            keyboardOptions = KeyboardOptions.Default.copy(capitalization = KeyboardCapitalization.Sentences),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = servings,
                onValueChange = { servings = it },
                label = { Text("Servings") },
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
            OutlinedTextField(
                value = prepTime,
                onValueChange = { prepTime = it },
                label = { Text("Prep time") },
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
        }
        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = cookingTime,
            onValueChange = { cookingTime = it },
            label = { Text("Cooking time") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))

        DifficultyPicker(
            selected = difficulty,
            onSelected = { difficulty = it }
        )
        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = tipsAndTricks,
            onValueChange = { tipsAndTricks = it },
            label = { Text("Tips & Tricks") },
            minLines = 2,
            keyboardOptions = KeyboardOptions.Default.copy(capitalization = KeyboardCapitalization.Sentences),
            modifier = Modifier.fillMaxWidth()
        )

        // Ingredients
        Spacer(modifier = Modifier.height(16.dp))
        Text("Ingredients", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(8.dp))
        ingredients.forEachIndexed { index, (name, qty, unit) ->
            IngredientEditRow(
                name = name,
                quantity = qty,
                unit = unit,
                onNameChange = {
                    ingredients = ingredients.toMutableList().also { list ->
                        list[index] = Triple(
                            it,
                            qty,
                            unit
                        )
                    }
                },
                onQuantityChange = {
                    ingredients = ingredients.toMutableList().also { list ->
                        list[index] = Triple(
                            name,
                            it,
                            unit
                        )
                    }
                },
                onUnitChange = {
                    ingredients = ingredients.toMutableList().also { list ->
                        list[index] = Triple(
                            name,
                            qty,
                            it
                        )
                    }
                },
                onRemove = { ingredients = ingredients.toMutableList().also { list -> list.removeAt(index) } }
            )
            Spacer(modifier = Modifier.height(4.dp))
        }
        TextButton(onClick = { ingredients = ingredients + Triple("", "", "") }) {
            Text("+ Add ingredient")
        }

        // Instructions
        Spacer(modifier = Modifier.height(16.dp))
        Text("Instructions", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(8.dp))
        instructions.forEachIndexed { index, step ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                Text(
                    text = "${index + 1}.",
                    modifier = Modifier.padding(top = 16.dp, end = 8.dp),
                    style = MaterialTheme.typography.bodyMedium
                )
                OutlinedTextField(
                    value = step,
                    onValueChange = { instructions = instructions.toMutableList().also { list -> list[index] = it } },
                    label = { Text("Step ${index + 1}") },
                    minLines = 2,
                    keyboardOptions = KeyboardOptions.Default.copy(capitalization = KeyboardCapitalization.Sentences),
                    modifier = Modifier.weight(1f)
                )
                IconButton(
                    onClick = { instructions = instructions.toMutableList().also { list -> list.removeAt(index) } }
                ) {
                    Icon(Icons.Default.Delete, contentDescription = "Remove step", tint = Color.Red)
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
        }
        TextButton(onClick = { instructions = instructions + "" }) {
            Text("+ Add step")
        }

        // Nutrients
        Spacer(modifier = Modifier.height(16.dp))
        Text("Nutrients per Serving", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(8.dp))
        nutrients.forEachIndexed { index, (name, qty, unit) ->
            IngredientEditRow(
                name = name,
                quantity = qty,
                unit = unit,
                namePlaceholder = "Nutrient",
                onNameChange = {
                    nutrients = nutrients.toMutableList().also { list ->
                        list[index] = Triple(
                            it,
                            qty,
                            unit
                        )
                    }
                },
                onQuantityChange = {
                    nutrients = nutrients.toMutableList().also { list ->
                        list[index] = Triple(
                            name,
                            it,
                            unit
                        )
                    }
                },
                onUnitChange = {
                    nutrients = nutrients.toMutableList().also { list ->
                        list[index] = Triple(
                            name,
                            qty,
                            it
                        )
                    }
                },
                onRemove = { nutrients = nutrients.toMutableList().also { list -> list.removeAt(index) } }
            )
            Spacer(modifier = Modifier.height(4.dp))
        }
        TextButton(onClick = { nutrients = nutrients + Triple("", "", "") }) {
            Text("+ Add nutrient")
        }

        Spacer(modifier = Modifier.height(24.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = onCancel,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                modifier = Modifier.weight(1f)
            ) {
                Text("Cancel")
            }
            Button(
                onClick = {
                    val editedRecipe = Recipe(
                        id = baseRecipe.id,
                        uid = baseRecipe.uid,
                        imageUrl = baseRecipe.imageUrl,
                        isFavourite = baseRecipe.isFavourite,
                        copyId = baseRecipe.copyId,
                        tags = baseRecipe.tags,
                        title = title,
                        summary = summary,
                        servings = servings.ifBlank { null },
                        prepTime = prepTime.ifBlank { null },
                        cookingTime = cookingTime.ifBlank { null },
                        tipsAndTricks = tipsAndTricks.ifBlank { null },
                        difficulty = difficulty,
                        ingredients = ingredients.map { (n, q, u) -> Ingredient(name = n, quantity = q, unit = u) },
                        instructions = instructions.filter { it.isNotBlank() },
                        nutrientsPerServing = nutrients.map { (n, q, u) -> Nutrient(name = n, quantity = q, unit = u) },
                        updatedAt = baseRecipe.updatedAt
                    )
                    onSave(label.trim(), editedRecipe)
                },
                enabled = label.isNotBlank(),
                modifier = Modifier.weight(1f)
            ) {
                Text("Save variant")
            }
        }

        Spacer(modifier = Modifier.height(30.dp))
    }
}

@Composable
private fun IngredientEditRow(
    name: String,
    quantity: String,
    unit: String,
    namePlaceholder: String = "Ingredient",
    onNameChange: (String) -> Unit,
    onQuantityChange: (String) -> Unit,
    onUnitChange: (String) -> Unit,
    onRemove: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        OutlinedTextField(
            value = name,
            onValueChange = onNameChange,
            label = { Text(namePlaceholder) },
            singleLine = true,
            keyboardOptions = KeyboardOptions.Default.copy(capitalization = KeyboardCapitalization.Sentences),
            modifier = Modifier.weight(3f)
        )
        OutlinedTextField(
            value = quantity,
            onValueChange = onQuantityChange,
            label = { Text("Qty") },
            singleLine = true,
            modifier = Modifier.weight(1.5f)
        )
        OutlinedTextField(
            value = unit,
            onValueChange = onUnitChange,
            label = { Text("Unit") },
            singleLine = true,
            modifier = Modifier.weight(1.5f)
        )
        IconButton(onClick = onRemove) {
            Icon(Icons.Default.Delete, contentDescription = "Remove", tint = Color.Red)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DifficultyPicker(
    selected: Difficulty,
    onSelected: (Difficulty) -> Unit
) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it }
    ) {
        OutlinedTextField(
            value = selected.name,
            onValueChange = {},
            readOnly = true,
            label = { Text("Difficulty") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                .fillMaxWidth()
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            Difficulty.entries.forEach { diff ->
                DropdownMenuItem(
                    text = { Text(diff.name) },
                    onClick = {
                        onSelected(diff)
                        expanded = false
                    }
                )
            }
        }
    }
}
