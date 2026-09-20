package com.formulae.chef.feature.home

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.formulae.chef.feature.model.Recipe
import com.formulae.chef.feature.model.parsedServingsCount
import com.formulae.chef.services.persistence.RecipeRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class HomeScreenViewModel(
    private val repository: RecipeRepository
) : ViewModel() {

    private val _userRecipes = MutableStateFlow<List<Recipe>>(emptyList())
    val userRecipes: StateFlow<List<Recipe>> = _userRecipes.asStateFlow()

    private val _communityRecipes = MutableStateFlow<List<Recipe>>(emptyList())
    val communityRecipes: StateFlow<List<Recipe>> = _communityRecipes.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _selectedRecipe = MutableStateFlow<Recipe?>(null)
    val selectedRecipe: StateFlow<Recipe?> = _selectedRecipe.asStateFlow()

    private var cookingRecipeId: String? = null

    private val _isCookingMode = MutableStateFlow(false)
    val isCookingMode: StateFlow<Boolean> = _isCookingMode.asStateFlow()

    private val _checkedSteps = MutableStateFlow<Set<Int>>(emptySet())
    val checkedSteps: StateFlow<Set<Int>> = _checkedSteps.asStateFlow()

    private val _currentServings = MutableStateFlow<Int?>(null)
    val currentServings: StateFlow<Int?> = _currentServings.asStateFlow()

    fun setCurrentUser(uid: String?) {
        viewModelScope.launch { fetchRecipes(uid) }
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

    fun clearSelectedRecipe() {
        _selectedRecipe.value = null
    }

    fun onToggleCookingMode() {
        val entering = !_isCookingMode.value
        _isCookingMode.value = entering
        if (entering) {
            cookingRecipeId = _selectedRecipe.value?.id
            _currentServings.value = _selectedRecipe.value?.parsedServingsCount()
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

    fun onStepUnchecked(stepIndex: Int) {
        _checkedSteps.value = _checkedSteps.value - stepIndex
    }

    fun onServingsChanged(newServings: Int) {
        _currentServings.value = newServings.coerceIn(1, MAX_SERVINGS)
    }

    private suspend fun fetchRecipes(uid: String?) {
        _isLoading.value = true
        try {
            val all = repository.loadAllRecipes()
            _userRecipes.value = all
                .filter { it.uid == uid && it.isFavourite }
                .shuffled()
                .take(USER_RECIPE_COUNT)
            _communityRecipes.value = all
                .filter { it.isFavourite && it.copyId == null && it.uid != uid }
                .shuffled()
                .take(COMMUNITY_RECIPE_COUNT)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load recipes for dashboard", e)
        } finally {
            _isLoading.value = false
        }
    }

    companion object {
        const val USER_RECIPE_COUNT = 2
        const val COMMUNITY_RECIPE_COUNT = 6
        const val MAX_SERVINGS = 30
        private const val TAG = "HomeScreenViewModel"
    }
}
