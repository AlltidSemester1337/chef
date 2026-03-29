package com.formulae.chef

<<<<<<< HEAD
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
=======
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
>>>>>>> 30785eb0 (WIP)
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
<<<<<<< HEAD
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.formulae.chef.feature.chat.OverlayChatViewModel
import com.formulae.chef.feature.chat.ui.ChefOverlay
import com.formulae.chef.services.authentication.UserSessionService
=======
import com.formulae.chef.services.authentication.UserSessionService
import com.formulae.chef.ui.components.ChefNavigationBar
import com.formulae.chef.ui.theme.GenerativeAISample
>>>>>>> 30785eb0 (WIP)
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
<<<<<<< HEAD
private fun HomeScreenContent(
    onNavigateToChat: () -> Unit,
    onNavigateToCollection: () -> Unit,
    onSignOut: () -> Unit,
    signedIn: Boolean
) {
    val overlayViewModel: OverlayChatViewModel = viewModel(factory = OverlayChatViewModelFactory)
    var showChefOverlay by remember { mutableStateOf(false) }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = { showChefOverlay = true }) {
                Icon(Icons.Default.Chat, contentDescription = "Chat with Chef")
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Button(
                onClick = onNavigateToChat,
                enabled = signedIn,
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

        if (showChefOverlay) {
            ChefOverlay(
                viewModel = overlayViewModel,
                recipe = null,
                onDismiss = { showChefOverlay = false }
            )
        }
=======
private fun HomeScreenContent() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(text = "Home", style = MaterialTheme.typography.headlineLarge)
>>>>>>> 30785eb0 (WIP)
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
