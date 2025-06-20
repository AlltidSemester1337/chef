package com.formulae.chef.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.formulae.chef.R

val FigtreeFamily = FontFamily(
    Font(R.font.figtree_regular, FontWeight.Normal),
    Font(R.font.figtree_semibold, FontWeight.SemiBold),
    Font(R.font.figtree_bold, FontWeight.Bold),
    Font(R.font.figtree_extrabold, FontWeight.ExtraBold)
)
// TODO: Delete if not used, also ttf font file
val RobotoFamily = FontFamily(
    Font(R.font.roboto_regular, FontWeight.Normal)
)

// Fallback to default system fonts if custom fonts are not available
val SafeFigtreeFamily = try {
    FigtreeFamily
} catch (e: Exception) {
    FontFamily.Default
}

val SafeRobotoFamily = try {
    RobotoFamily
} catch (e: Exception) {
    FontFamily.Default
}

// TODO: Delete if not used
val AppTypography = Typography(
    headlineLarge = TextStyle(
        fontFamily = SafeFigtreeFamily,
        fontWeight = FontWeight.ExtraBold,
        fontStyle = FontStyle.Italic,
        fontSize = 24.sp,
        lineHeight = 29.sp,
        color = TextPrimary
    ),
    labelMedium = TextStyle(
        fontFamily = SafeFigtreeFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        color = TextPrimary
    ),
    bodyLarge = TextStyle(
        fontFamily = SafeFigtreeFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        color = TextSecondary
    ),
    bodyMedium = TextStyle(
        fontFamily = SafeFigtreeFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        color = TextSupporting
    ),
    labelLarge = TextStyle(
        fontFamily = SafeFigtreeFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 20.sp,
        color = White
    ),
    bodySmall = TextStyle(
        fontFamily = SafeRobotoFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.25.sp,
        color = TextPrimary
    )
)