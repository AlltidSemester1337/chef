package com.formulae.chef.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.formulae.chef.ui.theme.GenerativeAISample
import com.formulae.chef.ui.theme.SafeFigtreeFamily
import com.formulae.chef.ui.theme.Terracotta100
import com.formulae.chef.ui.theme.Terracotta800
import com.formulae.chef.ui.theme.TextPrimary
import com.formulae.chef.ui.theme.White

@Composable
fun SegmentedTabRow(
    tabs: List<String>,
    selectedIndex: Int,
    onTabSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(Terracotta100)
            .padding(4.dp)
    ) {
        tabs.forEachIndexed { index, tab ->
            val isSelected = index == selectedIndex
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(5.dp))
                    .background(if (isSelected) White else Color.Transparent)
                    .clickable { onTabSelected(index) }
                    .padding(vertical = 8.dp)
            ) {
                Text(
                    text = tab,
                    fontFamily = SafeFigtreeFamily,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    fontStyle = FontStyle.Italic,
                    fontSize = 16.sp,
                    color = if (isSelected) Terracotta800 else TextPrimary,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFFFFDFC)
@Composable
private fun SegmentedTabRowPreview() {
    GenerativeAISample {
        SegmentedTabRow(
            tabs = listOf("Saved", "Community"),
            selectedIndex = 0,
            onTabSelected = {},
            modifier = Modifier.padding(16.dp)
        )
    }
}
