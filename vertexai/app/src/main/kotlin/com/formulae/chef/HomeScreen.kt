package com.formulae.chef

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.formulae.chef.services.authentication.UserSessionService
import com.google.firebase.auth.UserInfo


@Composable
fun HomeScreen(
    userSessionService: UserSessionService,
    onNavigateToChat: () -> Unit = {},
    onNavigateToCollection: () -> Unit = {},
    onSignOut: () -> Unit = {}
) {

    var isLoading by remember { mutableStateOf(true) }
    val currentUser by produceState<UserInfo?>(initialValue = null) {
        if (!userSessionService.anonymousSession) {
            userSessionService.currentUser.collect { user ->
                if (user == null) {
                    //onSignOut()
                } else {
                    value = user
                }
                isLoading = false
            }
        } else {
            isLoading = false
        }
    }

    if (isLoading) {
        CircularProgressIndicator() // Show loader while waiting for user state
    } else {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Button(
                onClick = onNavigateToChat,
                enabled = !userSessionService.anonymousSession && currentUser != null,
                modifier = Modifier.padding(16.dp)
            ) {
                Text(text = "Go to Chat")
            }
            Button(
                onClick = onNavigateToCollection,
                modifier = Modifier.padding(16.dp)
            ) {
                Text(text = "View Collection")
            }

            Button(
                onClick = onSignOut,
                modifier = Modifier.padding(16.dp)
            ) {
                Text(text = "Sign out")
            }
        }
    }
}
