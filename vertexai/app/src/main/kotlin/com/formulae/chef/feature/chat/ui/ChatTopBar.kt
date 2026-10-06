package com.formulae.chef.feature.chat.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.formulae.chef.R
import com.formulae.chef.ui.components.ChefTopBar
import com.formulae.chef.ui.components.InfoDialog
import com.formulae.chef.ui.theme.BackgroundColor
import com.formulae.chef.ui.theme.GenerativeAISample
import com.formulae.chef.ui.theme.Terracotta600
import com.formulae.chef.ui.theme.TextPrimary

private val HeaderShadowHeight = 4.dp

internal const val CHAT_AI_DISCLAIMER =
    "Chef's recipes are AI-generated and may contain mistakes — " +
        "always use your own judgment on cooking times and food safety."

/**
 * Sticky header of the chat (Generate) screen.
 *
 * Actions: an info (?) icon that opens the AI-content disclaimer, then the close (X), which is
 * always right-most. Further actions (e.g. a search icon, #66) go between the two.
 */
@Composable
fun ChatTopBar(
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showDisclaimer by rememberSaveable { mutableStateOf(false) }

    // Shadow below the bottom edge only separates the sticky header from the scrolling messages
    // (#62). Surface's shadowElevation would also draw a shadow along the top edge, so the
    // gradient is drawn outside the header's bounds instead. zIndex keeps it above the message
    // list that follows in the Column.
    Surface(
        color = BackgroundColor,
        modifier = modifier
            .zIndex(1f)
            .drawWithContent {
                drawContent()
                val shadowHeight = HeaderShadowHeight.toPx()
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(TextPrimary.copy(alpha = 0.12f), Color.Transparent),
                        startY = size.height,
                        endY = size.height + shadowHeight
                    ),
                    topLeft = Offset(0f, size.height),
                    size = Size(size.width, shadowHeight)
                )
            }
    ) {
        ChefTopBar(
            title = "Chat with Chef",
            navigationIcon = {
                Image(
                    painter = painterResource(R.drawable.logo),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                )
            },
            actions = {
                IconButton(onClick = { showDisclaimer = true }) {
                    Icon(
                        Icons.AutoMirrored.Outlined.HelpOutline,
                        contentDescription = "About AI-generated recipes",
                        tint = Terracotta600
                    )
                }
                // Future header actions (e.g. search, #66) go here, left of the close button.
                IconButton(onClick = onClose) {
                    Icon(Icons.Default.Close, contentDescription = "Close chat", tint = TextPrimary)
                }
            }
        )
    }

    if (showDisclaimer) {
        InfoDialog(
            title = "About Chef's recipes",
            body = CHAT_AI_DISCLAIMER,
            onDismiss = { showDisclaimer = false }
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ChatTopBarPreview() {
    GenerativeAISample {
        ChatTopBar(onClose = {})
    }
}
