package com.formulae.chef

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import com.formulae.chef.feature.chat.AskChefVariantViewModel
import com.formulae.chef.services.backend.ChefBackendClient

class AskChefVariantViewModelFactory : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
        @Suppress("UNCHECKED_CAST")
        return AskChefVariantViewModel(ChefBackendClient(BuildConfig.chefBackendUrl)) as T
    }
}
