package com.formulae.chef.feature.collection.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.formulae.chef.ui.theme.AppTypography
import com.formulae.chef.ui.theme.TextPrimary

internal const val CLOSE_COOKING_MODE_LABEL = "Close cooking mode"

/**
 * Exit control for Cooking mode: a visible "Close cooking mode" label to the left of the
 * close (X) icon. The label and icon form a single tap target.
 */
@Composable
internal fun CookingModeCloseButton(
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    TextButton(onClick = onClose, modifier = modifier) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = CLOSE_COOKING_MODE_LABEL,
                style = AppTypography.labelLarge.copy(color = TextPrimary)
            )
            // The label already describes the action; the icon is decorative for accessibility.
            Icon(Icons.Default.Close, contentDescription = null, tint = TextPrimary)
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun PreviewCookingModeCloseButton() {
    CookingModeCloseButton(onClose = {}, modifier = Modifier.padding(8.dp))
}
