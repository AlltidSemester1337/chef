package com.formulae.chef.feature.collection

import com.formulae.chef.feature.model.Recipe
import com.formulae.chef.services.persistence.RecipeRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CollectionViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private val sampleRecipes = listOf(
        Recipe(id = "1", uid = "user-1", title = "Pasta Carbonara", summary = "Classic Italian"),
        Recipe(id = "2", uid = "user-1", title = "Chicken Curry", summary = "Spicy and flavorful"),
        Recipe(id = "3", uid = "user-2", title = "Caesar Salad", summary = "Fresh and crispy", copyId = "copy-3")
    )

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `init loads recipes from repository`() = runTest(testDispatcher) {
        val repository = FakeRecipeRepository(sampleRecipes)
        val viewModel = CollectionViewModel(repository)

        advanceUntilIdle()

        assertEquals(3, viewModel.uiState.value.recipes.size)
        assertEquals("Pasta Carbonara", viewModel.uiState.value.recipes[0].title)
    }

    @Test
    fun `init with empty repository produces empty state`() = runTest(testDispatcher) {
        val repository = FakeRecipeRepository(emptyList())
        val viewModel = CollectionViewModel(repository)

        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.recipes.isEmpty())
    }

    @Test
    fun `onRecipeSelected updates selectedRecipe`() = runTest(testDispatcher) {
        val repository = FakeRecipeRepository(sampleRecipes)
        val viewModel = CollectionViewModel(repository)
        advanceUntilIdle()

        val recipe = sampleRecipes[1]
        viewModel.onRecipeSelected(recipe)

        assertEquals(recipe, viewModel.selectedRecipe.value)
    }

    @Test
    fun `selectedRecipe is initially null`() = runTest(testDispatcher) {
        val repository = FakeRecipeRepository(sampleRecipes)
        val viewModel = CollectionViewModel(repository)
        advanceUntilIdle()

        assertNull(viewModel.selectedRecipe.value)
    }

    @Test
    fun `onRecipeRemove with copyId calls removeRecipe`() = runTest(testDispatcher) {
        val repository = FakeRecipeRepository(sampleRecipes)
        val viewModel = CollectionViewModel(repository)
        advanceUntilIdle()

        val recipeWithCopyId = sampleRecipes[2] // has copyId
        viewModel.onRecipeSelected(recipeWithCopyId)
        viewModel.onRecipeRemove(recipeWithCopyId)

        assertNull(viewModel.selectedRecipe.value)
        assertEquals(2, viewModel.uiState.value.recipes.size)
        assertTrue(viewModel.uiState.value.recipes.none { it.id == "3" })
        assertTrue(repository.removedRecipeIds.contains("3"))
    }

    @Test
    fun `onRecipeRemove without copyId calls removeRecipeUid`() = runTest(testDispatcher) {
        val repository = FakeRecipeRepository(sampleRecipes)
        val viewModel = CollectionViewModel(repository)
        advanceUntilIdle()

        val recipeWithoutCopyId = sampleRecipes[0] // no copyId
        viewModel.onRecipeSelected(recipeWithoutCopyId)
        viewModel.onRecipeRemove(recipeWithoutCopyId)

        assertNull(viewModel.selectedRecipe.value)
        assertEquals(2, viewModel.uiState.value.recipes.size)
        assertTrue(repository.removedUidRecipeIds.contains("1"))
    }

    @Test
    fun `onRecipeRemove clears selectedRecipe`() = runTest(testDispatcher) {
        val repository = FakeRecipeRepository(sampleRecipes)
        val viewModel = CollectionViewModel(repository)
        advanceUntilIdle()

        viewModel.onRecipeSelected(sampleRecipes[0])
        assertEquals(sampleRecipes[0], viewModel.selectedRecipe.value)

        viewModel.onRecipeRemove(sampleRecipes[0])
        assertNull(viewModel.selectedRecipe.value)
    }
}

private class FakeRecipeRepository(
    private val recipes: List<Recipe>
) : RecipeRepository {
    val savedRecipes = mutableListOf<Recipe>()
    val removedRecipeIds = mutableListOf<String>()
    val removedUidRecipeIds = mutableListOf<String>()

    override fun saveRecipe(recipe: Recipe) {
        savedRecipes.add(recipe)
    }

    override suspend fun loadUserRecipes(uid: String): List<Recipe> {
        return recipes.filter { it.uid == uid }
    }

    override suspend fun loadAllRecipes(): List<Recipe> {
        return recipes
    }

    override fun removeRecipe(recipeId: String) {
        removedRecipeIds.add(recipeId)
    }

    override fun removeRecipeUid(recipeId: String) {
        removedUidRecipeIds.add(recipeId)
    }
}
