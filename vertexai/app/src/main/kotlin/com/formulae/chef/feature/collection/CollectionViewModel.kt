package com.formulae.chef.feature.collection

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.formulae.chef.feature.model.Recipe
import com.formulae.chef.feature.model.RecipeList
import com.formulae.chef.feature.model.parsedServingsCount
import com.formulae.chef.services.persistence.RecipeListRepository
import com.formulae.chef.services.persistence.RecipeRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class CollectionViewModel(
    private val repository: RecipeRepository,
    private val listRepository: RecipeListRepository
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

    private val _showIngredients = MutableStateFlow(true)
    val showIngredients: StateFlow<Boolean> = _showIngredients.asStateFlow()

    private val _lists = MutableStateFlow<List<RecipeList>>(emptyList())
    val lists: StateFlow<List<RecipeList>> = _lists.asStateFlow()

    private val _expandedListId = MutableStateFlow<String?>(null)
    val expandedListId: StateFlow<String?> = _expandedListId.asStateFlow()

    private var recipes: List<Recipe> = emptyList()
    private var cookingRecipeId: String? = null
    private var currentUid: String? = null

    init {
        fetchRecipes()
    }

    private fun fetchRecipes() {
        viewModelScope.launch {
            recipes = repository.loadAllRecipes()
            _uiState.value = CollectionUiState(recipes = recipes)
        }
    }

    fun setCurrentUser(uid: String?) {
        currentUid = uid
        if (uid != null) {
            fetchLists()
        } else {
            _lists.value = emptyList()
        }
    }

    private fun fetchLists() {
        val uid = currentUid ?: return
        viewModelScope.launch {
            _lists.value = listRepository.loadUserLists(uid)
        }
    }

    fun onCreateList(name: String) {
        val uid = currentUid ?: return
        listRepository.createList(uid, name)
        viewModelScope.launch {
            _lists.value = listRepository.loadUserLists(uid)
        }
    }

    fun onDeleteList(listId: String) {
        val uid = currentUid ?: return
        listRepository.deleteList(uid, listId)
        _lists.value = _lists.value.filter { it.id != listId }
        if (_expandedListId.value == listId) {
            _expandedListId.value = null
        }
    }

    fun onAddRecipeToList(recipeId: String, listId: String) {
        val uid = currentUid ?: return
        listRepository.addRecipeToList(uid, listId, recipeId)
        _lists.value = _lists.value.map { list ->
            if (list.id == listId && !list.recipeIds.contains(recipeId)) {
                list.copy(recipeIds = list.recipeIds + recipeId)
            } else {
                list
            }
        }
    }

    fun onRemoveRecipeFromList(recipeId: String, listId: String) {
        val uid = currentUid ?: return
        listRepository.removeRecipeFromList(uid, listId, recipeId)
        _lists.value = _lists.value.map { list ->
            if (list.id == listId) {
                list.copy(recipeIds = list.recipeIds.filter { it != recipeId })
            } else {
                list
            }
        }
    }

    fun onExpandList(listId: String?) {
        _expandedListId.value = if (_expandedListId.value == listId) null else listId
    }

    fun onRecipeSelected(recipe: Recipe) {
        if (recipe.id != cookingRecipeId) {
            _isCookingMode.value = false
            _checkedSteps.value = emptySet()
            _currentServings.value = null
            _showIngredients.value = true
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

    fun onTabChanged(showIngredients: Boolean) {
        _showIngredients.value = showIngredients
    }

    fun clearSelectedRecipe() {
        _selectedRecipe.value = null
    }

    data class CollectionUiState(
        val recipes: List<Recipe> = emptyList()
    )

    companion object {
        const val MAX_SERVINGS = 30
    }
}
