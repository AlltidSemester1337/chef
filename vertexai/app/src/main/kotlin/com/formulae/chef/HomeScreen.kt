package com.formulae.chef

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.formulae.chef.services.authentication.UserSessionService
import com.formulae.chef.ui.components.ChefNavigationBar
import com.formulae.chef.ui.theme.GenerativeAISample
import com.google.firebase.auth.UserInfo

@Composable
fun HomeScreen(
    userSessionService: UserSessionService,
    onSignOut: () -> Unit = {}
) {
    var isLoading by remember { mutableStateOf(true) }
    val currentUser by produceState<UserInfo?>(initialValue = null) {
        if (!userSessionService.anonymousSession) {
            userSessionService.currentUser.collect { user ->
                if (user != null) {
                    value = user
                }
                isLoading = false
            }
        } else {
            isLoading = false
        }
    }

    if (isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
    } else {
        if (!userSessionService.anonymousSession && currentUser == null) {
            onSignOut()
        }
        HomeScreenContent()
    }
}

@Composable
private fun HomeScreenContent() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(text = "Home", style = MaterialTheme.typography.headlineLarge)
    }
}

@Preview(showBackground = true)
@Composable
private fun PreviewHomeScreen() {
    GenerativeAISample {
        Scaffold(
            bottomBar = {
                ChefNavigationBar(currentRoute = "home", onNavigate = {})
            }
        ) { innerPadding ->
            Box(modifier = Modifier.padding(innerPadding)) {
                HomeScreenContent()
            }
        }
    }
}
