package com.formulae.chef.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import com.formulae.chef.R
import com.formulae.chef.ui.theme.Terracotta50
import com.formulae.chef.ui.theme.Terracotta600
import com.formulae.chef.ui.theme.Terracotta700
import com.formulae.chef.ui.theme.TextSecondary

private data class NavItem(
    val route: String,
    val label: String,
    @DrawableRes val iconRes: Int,
    @DrawableRes val selectedIconRes: Int
)

private val navItems = listOf(
    NavItem("home", "Home", R.drawable.ic_home, R.drawable.ic_home_filled),
    NavItem("generate", "Generate", R.drawable.ic_chef_hat, R.drawable.ic_chef_hat_filled),
    NavItem("collections", "Collections", R.drawable.ic_collections, R.drawable.ic_collections_filled)
)

@Composable
fun ChefNavigationBar(
    currentRoute: String,
    onNavigate: (String) -> Unit
) {
    NavigationBar(containerColor = Terracotta50) {
        navItems.forEach { item ->
            val selected = currentRoute == item.route
            NavigationBarItem(
                selected = selected,
                onClick = { onNavigate(item.route) },
                icon = {
                    Icon(
                        painter = painterResource(
                            if (selected) item.selectedIconRes else item.iconRes
                        ),
                        contentDescription = item.label,
                        tint = if (selected) Color.Unspecified else TextSecondary
                    )
                },
                label = {
                    Text(
                        text = item.label,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = Terracotta600,
                    selectedTextColor = Terracotta700,
                    unselectedIconColor = TextSecondary,
                    unselectedTextColor = TextSecondary,
                    indicatorColor = Terracotta50
                )
            )
        }
    }
}
