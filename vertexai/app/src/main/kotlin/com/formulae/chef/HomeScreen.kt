package com.formulae.chef

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.formulae.chef.feature.chat.OverlayChatViewModel
import com.formulae.chef.feature.chat.ui.ChefOverlay
import com.formulae.chef.feature.home.HomeUiState
import com.formulae.chef.feature.home.HomeViewModel
import com.formulae.chef.feature.model.CookingResource
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
        CircularProgressIndicator() // Show loader while waiting for user state
    } else {
        if (!userSessionService.anonymousSession && currentUser == null) {
            onSignOut()
        }
        val signedIn = !userSessionService.anonymousSession && currentUser != null
        val homeViewModel: HomeViewModel = viewModel(
            factory = HomeViewModelFactory(userSessionService)
        )
        val homeUiState by homeViewModel.uiState.collectAsState()
        HomeScreenContent(
            onNavigateToChat = onNavigateToChat,
            onNavigateToCollection = onNavigateToCollection,
            onSignOut = onSignOut,
            signedIn = signedIn,
            homeUiState = if (signedIn) homeUiState else HomeUiState()
        )
    }
}

@Composable
private fun HomeScreenContent(
    onNavigateToChat: () -> Unit,
    onNavigateToCollection: () -> Unit,
    onSignOut: () -> Unit,
    signedIn: Boolean,
    homeUiState: HomeUiState = HomeUiState()
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
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp),
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
            }

            if (homeUiState.isLoading) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
            } else if (homeUiState.resources.isNotEmpty()) {
                item {
                    Text(
                        text = "Cooking Resources",
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
                items(homeUiState.resources) { resource ->
                    CookingResourceCard(resource = resource)
                }
            }
        }

        if (showChefOverlay) {
            ChefOverlay(
                viewModel = overlayViewModel,
                recipe = null,
                onDismiss = { showChefOverlay = false }
            )
        }
    }
}

@Composable
private fun CookingResourceCard(resource: CookingResource) {
    val uriHandler = LocalUriHandler.current
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .clickable {
                if (resource.url.isNotBlank()) {
                    uriHandler.openUri(resource.url)
                }
            }
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = resource.title,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f)
                )
                if (resource.type.isNotBlank()) {
                    SuggestionChip(
                        onClick = {},
                        label = { Text(resource.type) }
                    )
                }
            }
            if (resource.description.isNotBlank()) {
                Text(
                    text = resource.description,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
            Text(
                text = resource.url,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewHomeNavigationScreen() {
    HomeScreenContent(
        onNavigateToChat = {},
        onNavigateToCollection = {},
        onSignOut = {},
        signedIn = true,
        homeUiState = HomeUiState(
            resources = listOf(
                CookingResource(
                    title = "Köket.se",
                    url = "https://koket.se",
                    type = "website",
                    description = "Sveriges största matsite med massor av recept."
                ),
                CookingResource(
                    title = "Gordon Ramsay",
                    url = "https://youtube.com/@gordonramsay",
                    type = "youtube",
                    description = "World-class cooking techniques from a Michelin-starred chef."
                )
            )
        )
    )
}
