package com.formulae.chef.feature.collection

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.formulae.chef.feature.model.Recipe
import com.formulae.chef.feature.model.RecipeList
import com.formulae.chef.feature.model.RecipeVariant
import com.formulae.chef.feature.model.parsedServingsCount
import com.formulae.chef.services.persistence.RecipeListRepository
import com.formulae.chef.services.persistence.RecipeRepository
import com.formulae.chef.services.persistence.RecipeVariantRepository
import java.time.Instant
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class CollectionViewModel(
    private val repository: RecipeRepository,
    private val listRepository: RecipeListRepository,
    private val variantRepository: RecipeVariantRepository
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

    private val _variants = MutableStateFlow<List<RecipeVariant>>(emptyList())
    val variants: StateFlow<List<RecipeVariant>> = _variants.asStateFlow()

    private val _selectedVariantId = MutableStateFlow<String?>(null)
    val selectedVariantId: StateFlow<String?> = _selectedVariantId.asStateFlow()

    private val _isEditingVariant = MutableStateFlow(false)
    val isEditingVariant: StateFlow<Boolean> = _isEditingVariant.asStateFlow()

    val displayedRecipe: StateFlow<Recipe?> = combine(
        _selectedRecipe,
        _variants,
        _selectedVariantId
    ) { recipe, variants, variantId ->
        if (recipe == null || variantId == null) return@combine recipe
        val variant = variants.find { it.id == variantId } ?: return@combine recipe
        Recipe(
            id = recipe.id,
            uid = recipe.uid,
            imageUrl = recipe.imageUrl,
            isFavourite = recipe.isFavourite,
            copyId = recipe.copyId,
            tags = recipe.tags,
            title = variant.title,
            summary = variant.summary,
            servings = variant.servings,
            prepTime = variant.prepTime,
            cookingTime = variant.cookingTime,
            nutrientsPerServing = variant.nutrientsPerServing,
            ingredients = variant.ingredients,
            difficulty = variant.difficulty,
            instructions = variant.instructions,
            tipsAndTricks = variant.tipsAndTricks,
            updatedAt = variant.createdAt
        )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val isRecipeOwner: Boolean get() = _selectedRecipe.value?.uid == currentUid

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
        val newList = listRepository.createList(uid, name)
        _lists.value = _lists.value + newList
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
        _variants.value = emptyList()
        _selectedVariantId.value = null

        recipe.id?.let { recipeId ->
            viewModelScope.launch {
                val loaded = variantRepository.loadVariantsForRecipe(recipeId)
                _variants.value = loaded
                if (isRecipeOwner) {
                    _selectedVariantId.value = loaded.firstOrNull { it.isPinned }?.id
                }
            }
        }
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
        resetVariantState()
    }

    fun onToggleCookingMode() {
        val entering = !_isCookingMode.value
        _isCookingMode.value = entering
        if (entering) {
            cookingRecipeId = _selectedRecipe.value?.id
            _currentServings.value = displayedRecipe.value?.parsedServingsCount()
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
        resetVariantState()
    }

    fun onVariantSelected(variantId: String?) {
        if (variantId != _selectedVariantId.value) {
            _isCookingMode.value = false
            _checkedSteps.value = emptySet()
            _currentServings.value = null
        }
        _selectedVariantId.value = variantId
    }

    fun onPinVariant(variantId: String?) {
        val recipeId = _selectedRecipe.value?.id ?: return
        val previouslyPinned = _variants.value.firstOrNull { it.isPinned }

        if (previouslyPinned != null && previouslyPinned.id != variantId) {
            previouslyPinned.id?.let { prevId ->
                variantRepository.updateVariantIsPinned(recipeId, prevId, false)
            }
        }

        if (variantId != null) {
            variantRepository.updateVariantIsPinned(recipeId, variantId, true)
        }

        _variants.value = _variants.value.map { variant ->
            when (variant.id) {
                variantId -> variant.copy(isPinned = true)
                else -> if (variant.isPinned) variant.copy(isPinned = false) else variant
            }
        }
    }

    fun onDeleteVariant(variantId: String) {
        val recipeId = _selectedRecipe.value?.id ?: return
        variantRepository.deleteVariant(recipeId, variantId)
        _variants.value = _variants.value.filter { it.id != variantId }
        if (_selectedVariantId.value == variantId) {
            _selectedVariantId.value = null
        }
    }

    fun onStartCreateVariant() {
        _isEditingVariant.value = true
    }

    fun onSaveVariant(label: String, recipe: Recipe) {
        val recipeId = _selectedRecipe.value?.id ?: return
        val variant = RecipeVariant(
            label = label,
            createdAt = Instant.now().toString(),
            isPinned = false,
            title = recipe.title,
            summary = recipe.summary,
            servings = recipe.servings,
            prepTime = recipe.prepTime,
            cookingTime = recipe.cookingTime,
            nutrientsPerServing = recipe.nutrientsPerServing,
            ingredients = recipe.ingredients,
            difficulty = recipe.difficulty,
            instructions = recipe.instructions,
            tipsAndTricks = recipe.tipsAndTricks
        )
        variantRepository.saveVariant(recipeId, variant)
        _isEditingVariant.value = false

        // Reload to get Firebase-assigned id
        viewModelScope.launch {
            val reloaded = variantRepository.loadVariantsForRecipe(recipeId)
            _variants.value = reloaded
            val newVariant = reloaded
                .filter { it.label == label }
                .maxByOrNull { it.createdAt }
            _selectedVariantId.value = newVariant?.id
        }
    }

    fun onCancelEditVariant() {
        _isEditingVariant.value = false
    }

    private fun resetVariantState() {
        _variants.value = emptyList()
        _selectedVariantId.value = null
        _isEditingVariant.value = false
    }

    data class CollectionUiState(
        val recipes: List<Recipe> = emptyList()
    )

    companion object {
        const val MAX_SERVINGS = 30
    }
}
