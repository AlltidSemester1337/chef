package com.formulae.chef.feature.useraccount.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.formulae.chef.ui.theme.AppTypography
import com.formulae.chef.ui.theme.Terracotta200
import com.formulae.chef.ui.theme.TextPrimary
import com.formulae.chef.ui.theme.TextSecondary
import com.formulae.chef.ui.theme.TextSupporting
import com.formulae.chef.ui.theme.White

@Composable
fun InputField(
    label: String,
    placeholder: String,
    value: String? = null,
    onValueChange: (String) -> Unit,
    supportingText: String? = null,
    visualTransformation: VisualTransformation? = null
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = label,
            style = AppTypography.labelMedium,
            modifier = Modifier.fillMaxWidth()
        )

        Column(
            verticalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedTextField(
                value = value ?: "",
                onValueChange = onValueChange,
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = White,
                    unfocusedContainerColor = White,
                    focusedBorderColor = Terracotta200,
                    unfocusedBorderColor = Terracotta200,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                    focusedPlaceholderColor = TextSecondary,
                    unfocusedPlaceholderColor = TextSecondary
                ),
                placeholder = { Text(placeholder, style = AppTypography.bodyLarge) },
                visualTransformation = visualTransformation ?: VisualTransformation.None
            )

            supportingText?.let { text ->
                Text(
                    text = text,
                    style = AppTypography.bodyMedium.copy(color = TextSupporting),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewInputField() {
    InputField(
        label = "Email Address",
        placeholder = "Enter your email address",
        value = null,
        onValueChange = {},
        supportingText = "We'll never share your email with anyone else."
    )
}
