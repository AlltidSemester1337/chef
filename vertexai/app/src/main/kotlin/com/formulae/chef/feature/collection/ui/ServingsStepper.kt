package com.formulae.chef.feature.collection.ui

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.formulae.chef.ui.theme.AppTypography
import com.formulae.chef.ui.theme.Terracotta200
import com.formulae.chef.ui.theme.TextPrimary

/**
 * Compact "– 4 servings +" control. Has no leading padding so it lines up with the left edge of
 * the ingredient list below it (#58).
 */
@Composable
internal fun ServingsStepper(
    servings: Int,
    maxServings: Int,
    onServingsChanged: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        StepperButton(
            icon = Icons.Default.Remove,
            contentDescription = "Decrease servings",
            enabled = servings > 1,
            onClick = { onServingsChanged(servings - 1) }
        )
        Text(
            text = if (servings == 1) "1 serving" else "$servings servings",
            style = AppTypography.labelMedium.copy(color = TextPrimary)
        )
        StepperButton(
            icon = Icons.Default.Add,
            contentDescription = "Increase servings",
            enabled = servings < maxServings,
            onClick = { onServingsChanged(servings + 1) }
        )
    }
}

@Composable
private fun StepperButton(
    icon: ImageVector,
    contentDescription: String,
    enabled: Boolean,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(8.dp)
    Box(
        modifier = Modifier
            .size(32.dp)
            .clip(shape)
            .border(1.dp, Terracotta200, shape)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .alpha(if (enabled) 1f else 0.38f),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = TextPrimary,
            modifier = Modifier.size(18.dp)
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun PreviewServingsStepper() {
    ServingsStepper(servings = 4, maxServings = 30, onServingsChanged = {})
}
