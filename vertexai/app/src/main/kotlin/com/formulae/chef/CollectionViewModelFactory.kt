package com.formulae.chef

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.formulae.chef.feature.collection.CollectionViewModel
import com.formulae.chef.services.persistence.RecipeListRepository
import com.formulae.chef.services.persistence.RecipeRepository
import com.formulae.chef.services.persistence.RecipeVariantRepository

class CollectionViewModelFactory(
    private val repository: RecipeRepository,
    private val listRepository: RecipeListRepository,
    private val variantRepository: RecipeVariantRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(CollectionViewModel::class.java)) {
            return CollectionViewModel(repository, listRepository, variantRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
