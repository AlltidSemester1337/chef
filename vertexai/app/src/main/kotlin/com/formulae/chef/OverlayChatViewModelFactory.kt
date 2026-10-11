package com.formulae.chef

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import com.formulae.chef.feature.chat.OverlayChatViewModel
import com.formulae.chef.services.backend.ChefBackendClient

class OverlayChatViewModelFactory : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
        @Suppress("UNCHECKED_CAST")
        return OverlayChatViewModel(ChefBackendClient(BuildConfig.chefBackendUrl)) as T
    }
}
