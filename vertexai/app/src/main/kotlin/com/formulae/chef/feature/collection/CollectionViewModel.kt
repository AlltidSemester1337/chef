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
    private val repository: RecipeRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(CollectionUiState())
    val uiState: StateFlow<CollectionUiState> = _uiState.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _selectedRecipe = MutableStateFlow<Recipe?>(null)
    val selectedRecipe: StateFlow<Recipe?> = _selectedRecipe.asStateFlow()

    private val _isCookingMode = MutableStateFlow(false)
    val isCookingMode: StateFlow<Boolean> = _isCookingMode.asStateFlow()

    private val _checkedSteps = MutableStateFlow<Set<Int>>(emptySet())
    val checkedSteps: StateFlow<Set<Int>> = _checkedSteps.asStateFlow()

    private val _currentServings = MutableStateFlow<Int?>(null)
    val currentServings: StateFlow<Int?> = _currentServings.asStateFlow()

    private var recipes: List<Recipe> = emptyList()
    private var cookingRecipeId: String? = null

    init {
        fetchRecipes()
    }

    private fun fetchRecipes() {
        viewModelScope.launch {
            recipes = repository.loadAllRecipes()
            _uiState.value = CollectionUiState(recipes = recipes)
        }
    }

    fun onRecipeSelected(recipe: Recipe) {
        if (recipe.id != cookingRecipeId) {
            _isCookingMode.value = false
            _checkedSteps.value = emptySet()
            _currentServings.value = null
            cookingRecipeId = null
        }
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
        val entering = !_isCookingMode.value
        _isCookingMode.value = entering
        if (entering) {
            cookingRecipeId = _selectedRecipe.value?.id
            _currentServings.value = parseServingsCount(_selectedRecipe.value?.servings)
            _checkedSteps.value = emptySet()
        } else {
            cookingRecipeId = null
            _checkedSteps.value = emptySet()
            _currentServings.value = null
        }
    }

    fun onStepChecked(stepIndex: Int) {
        _checkedSteps.value = _checkedSteps.value + stepIndex
    }

    fun onServingsChanged(newServings: Int) {
        _currentServings.value = newServings
    }

    fun clearSelectedRecipe() {
        _selectedRecipe.value = null
    }

    data class CollectionUiState(
        val recipes: List<Recipe> = emptyList()
    )
}

private fun parseServingsCount(servings: String?): Int? =
    servings?.let { Regex("""\d+""").find(it)?.value?.toIntOrNull() }
