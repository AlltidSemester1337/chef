package com.formulae.chef.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.formulae.chef.R
import com.formulae.chef.ui.theme.GenerativeAISample
import com.formulae.chef.ui.theme.SafeFigtreeFamily
import com.formulae.chef.ui.theme.Terracotta600
import com.formulae.chef.ui.theme.TextPrimary

@Composable
fun SectionHeader(
    title: String,
    linkText: String? = null,
    onLinkClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            fontFamily = SafeFigtreeFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 16.sp,
            color = TextPrimary
        )

        if (linkText != null && onLinkClick != null) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.clickable(onClick = onLinkClick)
            ) {
                Text(
                    text = linkText,
                    fontFamily = SafeFigtreeFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp,
                    color = Terracotta600,
                    textDecoration = TextDecoration.Underline
                )
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    painter = painterResource(R.drawable.ic_arrow_right),
                    contentDescription = null,
                    tint = Terracotta600,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFFFFDFC)
@Composable
private fun SectionHeaderPreview() {
    GenerativeAISample {
        SectionHeader(
            title = "Your saved recipes",
            linkText = "See all",
            onLinkClick = {}
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFFFFDFC)
@Composable
private fun SectionHeaderNoLinkPreview() {
    GenerativeAISample {
        SectionHeader(title = "What's cooking?")
    }
}
