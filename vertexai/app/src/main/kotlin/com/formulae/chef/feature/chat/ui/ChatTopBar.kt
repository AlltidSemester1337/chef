package com.formulae.chef.feature.chat.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.formulae.chef.R
import com.formulae.chef.ui.components.ChefTopBar
import com.formulae.chef.ui.theme.BackgroundColor
import com.formulae.chef.ui.theme.GenerativeAISample
import com.formulae.chef.ui.theme.TextPrimary

/**
 * Sticky header of the chat (Generate) screen.
 *
 * Actions are laid out right-to-left with the close (X) always right-most. Further actions
 * (e.g. a search icon, #66) go to the left of the close button.
 */
@Composable
fun ChatTopBar(
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Shadow at the bottom edge separates the sticky header from the scrolling messages (#62).
    // zIndex keeps the shadow drawn above the message list that follows in the Column.
    Surface(
        color = BackgroundColor,
        shadowElevation = 4.dp,
        modifier = modifier.zIndex(1f)
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
                IconButton(onClick = onClose) {
                    Icon(Icons.Default.Close, contentDescription = "Close chat", tint = TextPrimary)
                }
            }
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
