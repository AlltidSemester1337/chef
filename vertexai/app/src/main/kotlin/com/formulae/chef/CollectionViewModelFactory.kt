package com.formulae.chef

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.formulae.chef.feature.collection.CollectionViewModel
import com.formulae.chef.services.authentication.UserSessionServiceFirebaseImpl
import com.formulae.chef.services.persistence.RecipeRepository

class CollectionViewModelFactory(
    private val repository: RecipeRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        val userSessionService = UserSessionServiceFirebaseImpl()

        if (modelClass.isAssignableFrom(CollectionViewModel::class.java)) {
            return CollectionViewModel(repository, userSessionService) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
