package com.formulae.chef.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.formulae.chef.R
import com.formulae.chef.ui.theme.GenerativeAISample
import com.formulae.chef.ui.theme.Terracotta600

@Composable
fun ChefFab(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    FloatingActionButton(
        onClick = onClick,
        shape = CircleShape,
        containerColor = Terracotta600,
        elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 3.dp),
        modifier = modifier
            .size(48.dp)
            .border(
                width = 1.dp,
                color = Color.White.copy(alpha = 0.5f),
                shape = CircleShape
            )
    ) {
        Image(
            painter = painterResource(R.drawable.logo),
            contentDescription = "Generate recipe",
            modifier = Modifier.size(28.dp)
        )
    }
}

@Preview
@Composable
private fun ChefFabPreview() {
    GenerativeAISample {
        ChefFab(onClick = {})
    }
}
