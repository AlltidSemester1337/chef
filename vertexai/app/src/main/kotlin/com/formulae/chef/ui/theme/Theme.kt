package com.formulae.chef.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColorScheme = lightColorScheme(
    primary = Terracotta600,
    onPrimary = White,
    primaryContainer = Terracotta100,
    onPrimaryContainer = Terracotta800,
    secondary = TextSecondary,
    background = BackgroundColor,
    surface = White,
    onBackground = TextPrimary,
    onSurface = TextPrimary,
    surfaceVariant = Terracotta50,
    outline = Terracotta200
)

@Composable
fun GenerativeAISample(
    @Suppress("UNUSED_PARAMETER") darkTheme: Boolean = false,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = LightColorScheme,
        typography = AppTypography,
        shapes = ChefShapes,
        content = content
    )
}
