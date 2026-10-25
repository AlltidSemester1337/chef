package com.formulae.chef

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import com.formulae.chef.feature.home.HomeViewModel
import com.formulae.chef.services.authentication.UserSessionService
import com.formulae.chef.services.backend.ChefBackendClient

class HomeViewModelFactory(
    private val userSessionService: UserSessionService
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T =
        HomeViewModel(
            userSessionService = userSessionService,
            cookingResourcesService = ChefBackendClient(BuildConfig.chefBackendUrl)
        ) as T
}
