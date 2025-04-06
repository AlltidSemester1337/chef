package com.formulae.chef.feature.collection

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.formulae.chef.feature.model.Recipe
import com.formulae.chef.services.persistence.RecipeRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class CollectionViewModel(
    private val repository: RecipeRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(CollectionUiState())
    val uiState: StateFlow<CollectionUiState> = _uiState.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _selectedRecipe = MutableStateFlow<Recipe?>(null)  // Holds selected recipe
    val selectedRecipe: StateFlow<Recipe?> = _selectedRecipe.asStateFlow()

    var recipes: List<Recipe> = emptyList()


    init {
        fetchRecipes()
    }

    fun fetchRecipes() {
        viewModelScope.launch {
            recipes = repository.loadAllRecipes()
            _uiState.value = CollectionUiState(recipes = recipes)
        }
    }

    fun onRecipeSelected(recipe: Recipe) {
        _selectedRecipe.value = recipe
    }

    fun onRecipeRemove(recipe: Recipe) {
        val recipeId = recipe.id!!
        if (recipe.copyId != null) {
            repository.removeRecipe(recipeId)
        } else {
            repository.removeRecipeUid(recipeId)
        }
        _selectedRecipe.value = null
        recipes = recipes.filter { it.id != recipeId }
        _uiState.value = CollectionUiState(recipes = recipes)
    }

    fun onToggleCookingMode() {
        TODO("stop screen from locking, enlarge text, combined instructions and ingredients (replaces separate sections)")
    }

    data class CollectionUiState(
        val recipes: List<Recipe> = emptyList()
    )
}