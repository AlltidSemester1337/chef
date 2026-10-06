package com.formulae.chef.feature.chat.ui

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.formulae.chef.feature.chat.RecipeTextPreview
import com.formulae.chef.feature.chat.parseRecipeText
import com.formulae.chef.services.voice.sanitizeMarkdown
import com.formulae.chef.ui.theme.AppTypography
import com.formulae.chef.ui.theme.Terracotta100
import com.formulae.chef.ui.theme.Terracotta600
import com.formulae.chef.ui.theme.TextPrimary
import com.formulae.chef.ui.theme.TextSecondary

/**
 * Renders a recipe that Chef sent as plain text (e.g. a message restored from chat history, or one
 * where structured recipe extraction failed) as a compact card instead of a long text block.
 * The full recipe text is available by expanding the card.
 */
@Composable
fun ChatRecipeTextCard(
    preview: RecipeTextPreview,
    messageId: String,
    modifier: Modifier = Modifier
) {
    var expanded by rememberSaveable(messageId) { mutableStateOf(false) }

    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        if (preview.intro.isNotBlank()) {
            Text(
                text = preview.intro.sanitizeMarkdown(),
                style = AppTypography.bodyLarge.copy(color = TextPrimary),
                modifier = Modifier.fillMaxWidth()
            )
        }

        Column(
            verticalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(Terracotta100)
                .clickable { expanded = !expanded }
                .animateContentSize()
                .padding(12.dp)
        ) {
            Text(
                text = preview.title ?: "Recipe",
                style = AppTypography.labelMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontStyle = FontStyle.Italic,
                    color = TextPrimary
                ),
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
            if (preview.description.isNotBlank() && !expanded) {
                Text(
                    text = preview.description,
                    style = AppTypography.bodyMedium.copy(color = TextSecondary),
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (preview.details.isNotEmpty()) {
                Text(
                    text = preview.details.joinToString(" · "),
                    style = AppTypography.bodyMedium.copy(color = TextSecondary)
                )
            }
            Text(
                text = "${preview.ingredientCount} ingredients · ${preview.stepCount} steps",
                style = AppTypography.bodyMedium.copy(color = TextSecondary)
            )

            if (expanded) {
                Text(
                    text = preview.body.sanitizeMarkdown(),
                    style = AppTypography.bodyLarge.copy(color = TextPrimary),
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(top = 4.dp)
            ) {
                Text(
                    text = if (expanded) "Hide full recipe" else "Show full recipe",
                    style = AppTypography.labelLarge.copy(color = Terracotta600)
                )
                Icon(
                    imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null,
                    tint = Terracotta600,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ChatRecipeTextCardPreview() {
    val preview = parseRecipeText(
        """
        Here's a warming curry for tonight!

        ## Beef Rendang
        A rich, slow-cooked Indonesian curry.

        **Prep time:** 20 minutes
        **Servings:** 4

        **Ingredients:**
        - 800 g beef chuck
        - 400 ml coconut milk

        **Instructions:**
        1. Brown the beef.
        2. Simmer in coconut milk for 2 hours.
        """.trimIndent()
    )!!
    ChatRecipeTextCard(preview = preview, messageId = "preview")
}
