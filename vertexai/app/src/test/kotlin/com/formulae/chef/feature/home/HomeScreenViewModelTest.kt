package com.formulae.chef.feature.home

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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HomeScreenViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    // user-1 owns 3 recipes; user-2 owns 2 favourites (community candidates)
    private val userUid = "user-1"
    private val otherUid = "user-2"

    private val userRecipes = (1..3).map { i ->
        Recipe(id = "u$i", uid = userUid, title = "User Recipe $i")
    }
    private val communityRecipes = (1..8).map { i ->
        Recipe(id = "c$i", uid = otherUid, title = "Community Recipe $i", isFavourite = true)
    }
    private val nonFavouriteOtherRecipe =
        Recipe(id = "nf1", uid = otherUid, title = "Not Favourite", isFavourite = false)
    private val copyRecipe =
        Recipe(id = "cp1", uid = otherUid, title = "Copy Recipe", isFavourite = true, copyId = "c1")

    private val allRecipes = userRecipes + communityRecipes + nonFavouriteOtherRecipe + copyRecipe

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun makeViewModel(recipes: List<Recipe> = allRecipes): HomeScreenViewModel =
        HomeScreenViewModel(FakeRecipeRepository(recipes))

    @Test
    fun `setCurrentUser filters user recipes and caps at USER_RECIPE_COUNT`() = runTest(testDispatcher) {
        val viewModel = makeViewModel()

        viewModel.setCurrentUser(userUid)
        advanceUntilIdle()

        val result = viewModel.userRecipes.value
        assertTrue(result.all { it.uid == userUid })
        assertTrue(result.size <= HomeScreenViewModel.USER_RECIPE_COUNT)
    }

    @Test
    fun `setCurrentUser filters community recipes — isFavourite, no copyId, not owned`() = runTest(testDispatcher) {
        val viewModel = makeViewModel()

        viewModel.setCurrentUser(userUid)
        advanceUntilIdle()

        val result = viewModel.communityRecipes.value
        assertTrue(result.all { it.isFavourite })
        assertTrue(result.all { it.copyId == null })
        assertTrue(result.all { it.uid != userUid })
        assertTrue(result.size <= HomeScreenViewModel.COMMUNITY_RECIPE_COUNT)
    }

    @Test
    fun `community recipes caps at COMMUNITY_RECIPE_COUNT when more are available`() = runTest(testDispatcher) {
        val viewModel = makeViewModel()

        viewModel.setCurrentUser(userUid)
        advanceUntilIdle()

        assertEquals(HomeScreenViewModel.COMMUNITY_RECIPE_COUNT, viewModel.communityRecipes.value.size)
    }

    @Test
    fun `null uid returns empty user recipes but still returns community recipes`() = runTest(testDispatcher) {
        val viewModel = makeViewModel()

        viewModel.setCurrentUser(null)
        advanceUntilIdle()

        assertTrue(viewModel.userRecipes.value.isEmpty())
        assertTrue(viewModel.communityRecipes.value.isNotEmpty())
    }

    @Test
    fun `isLoading is false after fetch completes`() = runTest(testDispatcher) {
        val viewModel = makeViewModel()

        viewModel.setCurrentUser(userUid)
        advanceUntilIdle()

        assertFalse(viewModel.isLoading.value)
    }

    @Test
    fun `onRecipeSelected sets selectedRecipe`() = runTest(testDispatcher) {
        val viewModel = makeViewModel()
        val recipe = userRecipes[0]

        viewModel.onRecipeSelected(recipe)

        assertEquals(recipe, viewModel.selectedRecipe.value)
    }

    @Test
    fun `clearSelectedRecipe clears selectedRecipe`() = runTest(testDispatcher) {
        val viewModel = makeViewModel()
        viewModel.onRecipeSelected(userRecipes[0])

        viewModel.clearSelectedRecipe()

        assertNull(viewModel.selectedRecipe.value)
    }

    @Test
    fun `non-favourite and copy recipes are excluded from community`() = runTest(testDispatcher) {
        val viewModel = makeViewModel()

        viewModel.setCurrentUser(userUid)
        advanceUntilIdle()

        val ids = viewModel.communityRecipes.value.map { it.id }
        assertFalse(ids.contains("nf1"))
        assertFalse(ids.contains("cp1"))
    }
}

private class FakeRecipeRepository(
    private val recipes: List<Recipe>
) : RecipeRepository {
    override fun saveRecipe(recipe: Recipe) = Unit
    override suspend fun loadUserRecipes(uid: String): List<Recipe> = recipes.filter { it.uid == uid }
    override suspend fun loadAllRecipes(): List<Recipe> = recipes
    override fun removeRecipe(recipeId: String) = Unit
    override fun removeRecipeUid(recipeId: String) = Unit
}
