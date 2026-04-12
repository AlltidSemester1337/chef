package com.formulae.chef.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.formulae.chef.feature.model.Recipe
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

    private var currentUid: String? = null

    fun setCurrentUser(uid: String?) {
        currentUid = uid
        viewModelScope.launch { fetchRecipes(uid) }
    }

    fun onRecipeSelected(recipe: Recipe) {
        _selectedRecipe.value = recipe
    }

    fun clearSelectedRecipe() {
        _selectedRecipe.value = null
    }

    private suspend fun fetchRecipes(uid: String?) {
        _isLoading.value = true
        val all = repository.loadAllRecipes()
        _userRecipes.value = all.filter { it.uid == uid }.shuffled().take(USER_RECIPE_COUNT)
        _communityRecipes.value = all
            .filter { it.isFavourite && it.copyId == null && it.uid != uid }
            .shuffled()
            .take(COMMUNITY_RECIPE_COUNT)
        _isLoading.value = false
    }

    companion object {
        const val USER_RECIPE_COUNT = 2
        const val COMMUNITY_RECIPE_COUNT = 6
    }
}
