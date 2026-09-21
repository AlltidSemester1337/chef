package com.formulae.chef.ui.components

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import com.formulae.chef.ui.theme.AppTypography
import com.formulae.chef.ui.theme.Terracotta600
import com.formulae.chef.ui.theme.TextPrimary
import com.formulae.chef.ui.theme.White

@Composable
fun BetaQuotaExceededDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = White,
        shape = RoundedCornerShape(8.dp),
        title = {
            Text(
                text = "Beta limit reached",
                style = AppTypography.headlineLarge.copy(color = TextPrimary)
            )
        },
        text = {
            Text(
                text = "You have reached the end of the beta. Thank you for participating, " +
                    "stay tuned for updates!",
                style = AppTypography.bodyLarge.copy(color = TextPrimary)
            )
        },
        confirmButton = {
            TextButton(
                onClick = onDismiss,
                colors = ButtonDefaults.textButtonColors(contentColor = Terracotta600)
            ) {
                Text("OK", style = AppTypography.labelLarge)
            }
        }
    )
}
