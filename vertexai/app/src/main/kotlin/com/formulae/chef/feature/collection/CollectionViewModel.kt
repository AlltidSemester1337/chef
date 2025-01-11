package com.formulae.chef.feature.collection

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.formulae.chef.services.persistence.DummyRecipeRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch


class CollectionViewModel(
    private val repository: DummyRecipeRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(CollectionUiState())
    val uiState: StateFlow<CollectionUiState> = _uiState.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        fetchRecipes()
    }

    fun fetchRecipes() {
        viewModelScope.launch {
            _isLoading.value = true
            val recipes = repository.getDummyRecipes()
            _uiState.value = CollectionUiState(recipes = recipes)
            _isLoading.value = false
        }
    }

    fun onRecipeSelected(recipe: Recipe) {
        // Trigger navigation or handle selection
    }
}

data class CollectionUiState(
    val recipes: List<Recipe> = emptyList()
)