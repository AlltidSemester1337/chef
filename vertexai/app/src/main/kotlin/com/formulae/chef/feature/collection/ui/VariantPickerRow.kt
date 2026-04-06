package com.formulae.chef.feature.collection.ui

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarOutline
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.formulae.chef.feature.model.RecipeVariant

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun VariantPickerRow(
    variants: List<RecipeVariant>,
    selectedVariantId: String?,
    isOwner: Boolean,
    onVariantSelected: (String?) -> Unit,
    onPinVariant: (String?) -> Unit,
    onDeleteVariant: (String) -> Unit,
    onCreateVariant: () -> Unit
) {
    val sortedVariants = variants.sortedBy { it.label }
    val selectedLabel = if (selectedVariantId == null) {
        "Default"
    } else {
        sortedVariants.find { it.id == selectedVariantId }?.label ?: "Default"
    }

    var expanded by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = it },
            modifier = Modifier.weight(1f)
        ) {
            OutlinedTextField(
                value = selectedLabel,
                onValueChange = {},
                readOnly = true,
                label = { Text("Variants") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                modifier = Modifier
                    .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                    .fillMaxWidth()
            )

            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                // Default (canonical) item
                val defaultPinned = sortedVariants.none { it.isPinned }
                DropdownMenuItem(
                    text = { Text("Default") },
                    onClick = {
                        onVariantSelected(null)
                        expanded = false
                    },
                    trailingIcon = if (isOwner) {
                        {
                            IconButton(onClick = {
                                if (!defaultPinned) onPinVariant(null)
                                expanded = false
                            }) {
                                Icon(
                                    imageVector = if (defaultPinned) Icons.Filled.Star else Icons.Outlined.StarOutline,
                                    contentDescription = if (defaultPinned) "Default is pinned" else "Pin default",
                                    tint = if (defaultPinned) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    }
                                )
                            }
                        }
                    } else {
                        null
                    }
                )

                sortedVariants.forEach { variant ->
                    DropdownMenuItem(
                        text = { Text(variant.label) },
                        onClick = {
                            onVariantSelected(variant.id)
                            expanded = false
                        },
                        trailingIcon = if (isOwner) {
                            {
                                Row {
                                    IconButton(onClick = {
                                        if (variant.isPinned) {
                                            onPinVariant(null)
                                        } else {
                                            variant.id?.let { onPinVariant(it) }
                                        }
                                        expanded = false
                                    }) {
                                        Icon(
                                            imageVector = if (variant.isPinned) {
                                                Icons.Filled.Star
                                            } else {
                                                Icons.Outlined.StarOutline
                                            },
                                            contentDescription = if (variant.isPinned) "Pinned" else "Pin this variant",
                                            tint = if (variant.isPinned) {
                                                MaterialTheme.colorScheme.primary
                                            } else {
                                                MaterialTheme.colorScheme.onSurfaceVariant
                                            }
                                        )
                                    }
                                    IconButton(onClick = {
                                        variant.id?.let { onDeleteVariant(it) }
                                        expanded = false
                                    }) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Delete variant",
                                            tint = MaterialTheme.colorScheme.error
                                        )
                                    }
                                }
                            }
                        } else {
                            null
                        }
                    )
                }
            }
        }

        if (isOwner) {
            IconButton(
                onClick = onCreateVariant,
                modifier = Modifier.padding(start = 4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Create variant"
                )
            }
        }
    }
}
