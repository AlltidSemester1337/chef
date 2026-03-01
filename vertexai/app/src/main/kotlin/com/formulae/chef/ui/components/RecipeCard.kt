package com.formulae.chef.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter
import com.formulae.chef.ui.theme.GenerativeAISample
import com.formulae.chef.ui.theme.SafeFigtreeFamily
import com.formulae.chef.ui.theme.Terracotta100
import com.formulae.chef.ui.theme.TextPrimary
import com.formulae.chef.ui.theme.White

@Composable
fun RecipeCard(
    title: String,
    imageUrl: String?,
    showBookmark: Boolean = false,
    onBookmarkClick: (() -> Unit)? = null,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(123.dp)
                .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                .background(Terracotta100)
        ) {
            if (!imageUrl.isNullOrEmpty()) {
                Image(
                    painter = rememberAsyncImagePainter(imageUrl),
                    contentDescription = title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.matchParentSize()
                )
            }

            if (showBookmark) {
                IconButton(
                    onClick = { onBookmarkClick?.invoke() },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .size(32.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(White)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.BookmarkBorder,
                        contentDescription = "Bookmark",
                        modifier = Modifier.size(20.dp),
                        tint = TextPrimary
                    )
                }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(bottomStart = 8.dp, bottomEnd = 8.dp))
                .background(Terracotta100)
                .padding(8.dp)
        ) {
            Text(
                text = title,
                fontFamily = SafeFigtreeFamily,
                fontWeight = FontWeight.Bold,
                fontStyle = FontStyle.Italic,
                fontSize = 14.sp,
                color = TextPrimary,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFFFFDFC)
@Composable
private fun RecipeCardPreview() {
    GenerativeAISample {
        RecipeCard(
            title = "Creamy Tuscan Pasta with Sun-Dried Tomatoes",
            imageUrl = null,
            showBookmark = true,
            onBookmarkClick = {},
            onClick = {},
            modifier = Modifier.padding(16.dp)
        )
    }
}
