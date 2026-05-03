package com.formulae.chef.ui.components

import androidx.compose.foundation.layout.RowScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.formulae.chef.ui.theme.BackgroundColor
import com.formulae.chef.ui.theme.GenerativeAISample

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChefTopBar(
    title: String,
    navigationIcon: @Composable (() -> Unit)? = null,
    actions: @Composable (RowScope.() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    TopAppBar(
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineLarge
            )
        },
        navigationIcon = { navigationIcon?.invoke() },
        actions = { actions?.invoke(this) },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = BackgroundColor
        ),
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
private fun ChefTopBarPreview() {
    GenerativeAISample {
        ChefTopBar(title = "Chef")
    }
}

@Preview(showBackground = true)
@Composable
private fun ChefTopBarWithNavPreview() {
    GenerativeAISample {
        ChefTopBar(
            title = "Recipe Detail",
            navigationIcon = {
                IconButton(onClick = {}) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back"
                    )
                }
            }
        )
    }
}
