package com.formulae.chef.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ExitToApp
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.formulae.chef.ui.theme.Terracotta600
import com.formulae.chef.ui.theme.TextPrimary

/**
 * Overflow ("more") menu for account actions on the Home screen header.
 *
 * Keeps destructive/rare account actions (sign out, delete account) out of the main
 * screen content. "Delete account" is only offered when [onDeleteAccount] is non-null
 * (i.e. for a signed-in, non-anonymous user).
 */
@Composable
fun AccountMenu(
    onSignOut: () -> Unit,
    onDeleteAccount: (() -> Unit)?
) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        IconButton(onClick = { expanded = true }) {
            Icon(
                imageVector = Icons.Default.MoreVert,
                contentDescription = "Account options",
                tint = Terracotta600
            )
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            DropdownMenuItem(
                text = { Text("Sign out", color = TextPrimary) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.ExitToApp,
                        contentDescription = null,
                        tint = Terracotta600
                    )
                },
                onClick = {
                    expanded = false
                    onSignOut()
                }
            )
            if (onDeleteAccount != null) {
                DropdownMenuItem(
                    text = { Text("Delete account", color = MaterialTheme.colorScheme.error) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Outlined.Delete,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error
                        )
                    },
                    onClick = {
                        expanded = false
                        onDeleteAccount()
                    }
                )
            }
        }
    }
}
