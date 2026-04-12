package com.formulae.chef

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.formulae.chef.feature.home.HomeScreenViewModel
import com.formulae.chef.services.persistence.RecipeRepository

class HomeScreenViewModelFactory(
    private val repository: RecipeRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(HomeScreenViewModel::class.java)) {
            return HomeScreenViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
