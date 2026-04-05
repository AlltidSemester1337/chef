package com.formulae.chef.feature.collection

import com.formulae.chef.feature.model.Recipe
import com.formulae.chef.feature.model.RecipeList
import com.formulae.chef.services.persistence.RecipeListRepository
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
        val viewModel = CollectionViewModel(repository, FakeRecipeListRepository())

        advanceUntilIdle()

        assertEquals(3, viewModel.uiState.value.recipes.size)
        assertEquals("Pasta Carbonara", viewModel.uiState.value.recipes[0].title)
    }

    @Test
    fun `init with empty repository produces empty state`() = runTest(testDispatcher) {
        val repository = FakeRecipeRepository(emptyList())
        val viewModel = CollectionViewModel(repository, FakeRecipeListRepository())

        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.recipes.isEmpty())
    }

    @Test
    fun `onRecipeSelected updates selectedRecipe`() = runTest(testDispatcher) {
        val repository = FakeRecipeRepository(sampleRecipes)
        val viewModel = CollectionViewModel(repository, FakeRecipeListRepository())
        advanceUntilIdle()

        val recipe = sampleRecipes[1]
        viewModel.onRecipeSelected(recipe)

        assertEquals(recipe, viewModel.selectedRecipe.value)
    }

    @Test
    fun `selectedRecipe is initially null`() = runTest(testDispatcher) {
        val repository = FakeRecipeRepository(sampleRecipes)
        val viewModel = CollectionViewModel(repository, FakeRecipeListRepository())
        advanceUntilIdle()

        assertNull(viewModel.selectedRecipe.value)
    }

    @Test
    fun `onRecipeRemove with copyId calls removeRecipe`() = runTest(testDispatcher) {
        val repository = FakeRecipeRepository(sampleRecipes)
        val viewModel = CollectionViewModel(repository, FakeRecipeListRepository())
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
        val viewModel = CollectionViewModel(repository, FakeRecipeListRepository())
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
        val viewModel = CollectionViewModel(repository, FakeRecipeListRepository())
        advanceUntilIdle()

        viewModel.onRecipeSelected(sampleRecipes[0])
        assertEquals(sampleRecipes[0], viewModel.selectedRecipe.value)

        viewModel.onRecipeRemove(sampleRecipes[0])
        assertNull(viewModel.selectedRecipe.value)
    }

    @Test
    fun `isCookingMode is initially false`() = runTest(testDispatcher) {
        val viewModel = CollectionViewModel(FakeRecipeRepository(sampleRecipes), FakeRecipeListRepository())
        assertFalse(viewModel.isCookingMode.value)
    }

    @Test
    fun `onToggleCookingMode enables cooking mode`() = runTest(testDispatcher) {
        val viewModel = CollectionViewModel(FakeRecipeRepository(sampleRecipes), FakeRecipeListRepository())
        advanceUntilIdle()
        viewModel.onRecipeSelected(sampleRecipes[0])

        viewModel.onToggleCookingMode()

        assertTrue(viewModel.isCookingMode.value)
    }

    @Test
    fun `onToggleCookingMode disables cooking mode and clears state`() = runTest(testDispatcher) {
        val viewModel = CollectionViewModel(FakeRecipeRepository(sampleRecipes), FakeRecipeListRepository())
        advanceUntilIdle()
        viewModel.onRecipeSelected(sampleRecipes[0])
        viewModel.onToggleCookingMode() // enable
        viewModel.onStepChecked(0)
        viewModel.onServingsChanged(8)

        viewModel.onToggleCookingMode() // disable

        assertFalse(viewModel.isCookingMode.value)
        assertTrue(viewModel.checkedSteps.value.isEmpty())
        assertNull(viewModel.currentServings.value)
    }

    @Test
    fun `onToggleCookingMode initializes currentServings from recipe servings string`() = runTest(testDispatcher) {
        val recipeWithServings = Recipe(id = "4", uid = "user-1", title = "Soup", servings = "4 servings")
        val viewModel =
            CollectionViewModel(FakeRecipeRepository(listOf(recipeWithServings)), FakeRecipeListRepository())
        advanceUntilIdle()
        viewModel.onRecipeSelected(recipeWithServings)

        viewModel.onToggleCookingMode()

        assertEquals(4, viewModel.currentServings.value)
    }

    @Test
    fun `onToggleCookingMode clears checkedSteps on activation`() = runTest(testDispatcher) {
        val viewModel = CollectionViewModel(FakeRecipeRepository(sampleRecipes), FakeRecipeListRepository())
        advanceUntilIdle()
        viewModel.onRecipeSelected(sampleRecipes[0])
        viewModel.onToggleCookingMode() // enable
        viewModel.onStepChecked(1)
        viewModel.onToggleCookingMode() // disable
        viewModel.onToggleCookingMode() // re-enable

        assertTrue(viewModel.checkedSteps.value.isEmpty())
    }

    @Test
    fun `onStepChecked adds step index to checkedSteps`() = runTest(testDispatcher) {
        val viewModel = CollectionViewModel(FakeRecipeRepository(sampleRecipes), FakeRecipeListRepository())
        advanceUntilIdle()

        viewModel.onStepChecked(2)
        viewModel.onStepChecked(4)

        assertTrue(viewModel.checkedSteps.value.containsAll(setOf(2, 4)))
    }

    @Test
    fun `onServingsChanged updates currentServings`() = runTest(testDispatcher) {
        val viewModel = CollectionViewModel(FakeRecipeRepository(sampleRecipes), FakeRecipeListRepository())
        advanceUntilIdle()

        viewModel.onServingsChanged(6)

        assertEquals(6, viewModel.currentServings.value)
    }

    @Test
    fun `clearSelectedRecipe clears selectedRecipe without resetting cooking mode`() = runTest(testDispatcher) {
        val viewModel = CollectionViewModel(FakeRecipeRepository(sampleRecipes), FakeRecipeListRepository())
        advanceUntilIdle()
        viewModel.onRecipeSelected(sampleRecipes[0])
        viewModel.onToggleCookingMode()
        viewModel.onStepChecked(1)

        viewModel.clearSelectedRecipe()

        assertNull(viewModel.selectedRecipe.value)
        assertTrue(viewModel.isCookingMode.value)
        assertTrue(viewModel.checkedSteps.value.contains(1))
    }

    @Test
    fun `onRecipeSelected resets cooking mode state when selecting a different recipe`() = runTest(testDispatcher) {
        val viewModel = CollectionViewModel(FakeRecipeRepository(sampleRecipes), FakeRecipeListRepository())
        advanceUntilIdle()
        viewModel.onRecipeSelected(sampleRecipes[0])
        viewModel.onToggleCookingMode()
        viewModel.onStepChecked(0)
        viewModel.onServingsChanged(8)

        viewModel.onRecipeSelected(sampleRecipes[1])

        assertFalse(viewModel.isCookingMode.value)
        assertTrue(viewModel.checkedSteps.value.isEmpty())
        assertNull(viewModel.currentServings.value)
    }

    @Test
    fun `onStepUnchecked removes step index from checkedSteps`() = runTest(testDispatcher) {
        val viewModel = CollectionViewModel(FakeRecipeRepository(sampleRecipes), FakeRecipeListRepository())
        advanceUntilIdle()

        viewModel.onStepChecked(2)
        viewModel.onStepChecked(3)
        viewModel.onStepUnchecked(2)

        assertFalse(viewModel.checkedSteps.value.contains(2))
        assertTrue(viewModel.checkedSteps.value.contains(3))
    }

    @Test
    fun `onServingsChanged caps at MAX_SERVINGS`() = runTest(testDispatcher) {
        val viewModel = CollectionViewModel(FakeRecipeRepository(sampleRecipes), FakeRecipeListRepository())

        viewModel.onServingsChanged(CollectionViewModel.MAX_SERVINGS + 10)

        assertEquals(CollectionViewModel.MAX_SERVINGS, viewModel.currentServings.value)
    }

    @Test
    fun `onServingsChanged floors at 1`() = runTest(testDispatcher) {
        val viewModel = CollectionViewModel(FakeRecipeRepository(sampleRecipes), FakeRecipeListRepository())

        viewModel.onServingsChanged(0)

        assertEquals(1, viewModel.currentServings.value)
    }

    @Test
    fun `onTabChanged updates showIngredients`() = runTest(testDispatcher) {
        val viewModel = CollectionViewModel(FakeRecipeRepository(sampleRecipes), FakeRecipeListRepository())

        viewModel.onTabChanged(false)
        assertFalse(viewModel.showIngredients.value)

        viewModel.onTabChanged(true)
        assertTrue(viewModel.showIngredients.value)
    }

    @Test
    fun `showIngredients resets to true when a different recipe is selected`() = runTest(testDispatcher) {
        val viewModel = CollectionViewModel(FakeRecipeRepository(sampleRecipes), FakeRecipeListRepository())
        advanceUntilIdle()
        viewModel.onRecipeSelected(sampleRecipes[0])
        viewModel.onTabChanged(false)

        viewModel.onRecipeSelected(sampleRecipes[1])

        assertTrue(viewModel.showIngredients.value)
    }

    @Test
    fun `showIngredients persists when re-selecting the same recipe`() = runTest(testDispatcher) {
        val viewModel = CollectionViewModel(FakeRecipeRepository(sampleRecipes), FakeRecipeListRepository())
        advanceUntilIdle()
        viewModel.onRecipeSelected(sampleRecipes[0])
        viewModel.onToggleCookingMode()
        viewModel.onTabChanged(false)
        viewModel.clearSelectedRecipe()

        viewModel.onRecipeSelected(sampleRecipes[0])

        assertFalse(viewModel.showIngredients.value)
    }

    @Test
    fun `onRecipeSelected preserves cooking mode state when re-selecting the same recipe`() = runTest(testDispatcher) {
        val viewModel = CollectionViewModel(FakeRecipeRepository(sampleRecipes), FakeRecipeListRepository())
        advanceUntilIdle()
        viewModel.onRecipeSelected(sampleRecipes[0])
        viewModel.onToggleCookingMode()
        viewModel.onStepChecked(0)
        viewModel.onServingsChanged(8)

        viewModel.onRecipeSelected(sampleRecipes[0])

        assertTrue(viewModel.isCookingMode.value)
        assertTrue(viewModel.checkedSteps.value.contains(0))
        assertEquals(8, viewModel.currentServings.value)
    }

    // --- List management tests ---

    @Test
    fun `setCurrentUser loads lists for uid`() = runTest(testDispatcher) {
        val sampleLists = listOf(RecipeList(id = "list-1", name = "Work week", recipeIds = listOf("1")))
        val listRepo = FakeRecipeListRepository(sampleLists)
        val viewModel = CollectionViewModel(FakeRecipeRepository(sampleRecipes), listRepo)

        viewModel.setCurrentUser("user-1")
        advanceUntilIdle()

        assertEquals(1, viewModel.lists.value.size)
        assertEquals("Work week", viewModel.lists.value[0].name)
    }

    @Test
    fun `setCurrentUser with null uid clears lists`() = runTest(testDispatcher) {
        val sampleLists = listOf(RecipeList(id = "list-1", name = "Work week"))
        val listRepo = FakeRecipeListRepository(sampleLists)
        val viewModel = CollectionViewModel(FakeRecipeRepository(sampleRecipes), listRepo)
        viewModel.setCurrentUser("user-1")
        advanceUntilIdle()

        viewModel.setCurrentUser(null)

        assertTrue(viewModel.lists.value.isEmpty())
    }

    @Test
    fun `onCreateList adds list to state immediately (optimistic)`() = runTest(testDispatcher) {
        val listRepo = FakeRecipeListRepository()
        val viewModel = CollectionViewModel(FakeRecipeRepository(sampleRecipes), listRepo)
        viewModel.setCurrentUser("user-1")
        advanceUntilIdle()

        viewModel.onCreateList("Festive occasions")

        assertTrue(listRepo.createdLists.any { it.second == "Festive occasions" })
        assertTrue(viewModel.lists.value.any { it.name == "Festive occasions" })
    }

    @Test
    fun `onDeleteList removes list from state immediately`() = runTest(testDispatcher) {
        val sampleLists = listOf(
            RecipeList(id = "list-1", name = "Work week"),
            RecipeList(id = "list-2", name = "Festive")
        )
        val listRepo = FakeRecipeListRepository(sampleLists)
        val viewModel = CollectionViewModel(FakeRecipeRepository(sampleRecipes), listRepo)
        viewModel.setCurrentUser("user-1")
        advanceUntilIdle()

        viewModel.onDeleteList("list-1")

        assertEquals(1, viewModel.lists.value.size)
        assertEquals("Festive", viewModel.lists.value[0].name)
        assertTrue(listRepo.deletedListIds.contains("list-1"))
    }

    @Test
    fun `onDeleteList clears expandedListId if it matches`() = runTest(testDispatcher) {
        val sampleLists = listOf(RecipeList(id = "list-1", name = "Work week"))
        val listRepo = FakeRecipeListRepository(sampleLists)
        val viewModel = CollectionViewModel(FakeRecipeRepository(sampleRecipes), listRepo)
        viewModel.setCurrentUser("user-1")
        advanceUntilIdle()
        viewModel.onExpandList("list-1")
        assertEquals("list-1", viewModel.expandedListId.value)

        viewModel.onDeleteList("list-1")

        assertNull(viewModel.expandedListId.value)
    }

    @Test
    fun `onAddRecipeToList updates local state`() = runTest(testDispatcher) {
        val sampleLists = listOf(RecipeList(id = "list-1", name = "Work week", recipeIds = emptyList()))
        val listRepo = FakeRecipeListRepository(sampleLists)
        val viewModel = CollectionViewModel(FakeRecipeRepository(sampleRecipes), listRepo)
        viewModel.setCurrentUser("user-1")
        advanceUntilIdle()

        viewModel.onAddRecipeToList("recipe-1", "list-1")

        assertTrue(viewModel.lists.value[0].recipeIds.contains("recipe-1"))
        assertTrue(listRepo.addedRecipes.contains(Triple("user-1", "list-1", "recipe-1")))
    }

    @Test
    fun `onAddRecipeToList does not duplicate recipe`() = runTest(testDispatcher) {
        val sampleLists = listOf(RecipeList(id = "list-1", name = "Work week", recipeIds = listOf("recipe-1")))
        val listRepo = FakeRecipeListRepository(sampleLists)
        val viewModel = CollectionViewModel(FakeRecipeRepository(sampleRecipes), listRepo)
        viewModel.setCurrentUser("user-1")
        advanceUntilIdle()

        viewModel.onAddRecipeToList("recipe-1", "list-1")

        assertEquals(1, viewModel.lists.value[0].recipeIds.size)
    }

    @Test
    fun `onRemoveRecipeFromList updates local state`() = runTest(testDispatcher) {
        val sampleLists =
            listOf(RecipeList(id = "list-1", name = "Work week", recipeIds = listOf("recipe-1", "recipe-2")))
        val listRepo = FakeRecipeListRepository(sampleLists)
        val viewModel = CollectionViewModel(FakeRecipeRepository(sampleRecipes), listRepo)
        viewModel.setCurrentUser("user-1")
        advanceUntilIdle()

        viewModel.onRemoveRecipeFromList("recipe-1", "list-1")

        assertFalse(viewModel.lists.value[0].recipeIds.contains("recipe-1"))
        assertTrue(viewModel.lists.value[0].recipeIds.contains("recipe-2"))
        assertTrue(listRepo.removedRecipes.contains(Triple("user-1", "list-1", "recipe-1")))
    }

    @Test
    fun `onExpandList toggles expandedListId`() = runTest(testDispatcher) {
        val viewModel = CollectionViewModel(FakeRecipeRepository(sampleRecipes), FakeRecipeListRepository())

        viewModel.onExpandList("list-1")
        assertEquals("list-1", viewModel.expandedListId.value)

        viewModel.onExpandList("list-1")
        assertNull(viewModel.expandedListId.value)
    }

    @Test
    fun `onExpandList switches to a different list`() = runTest(testDispatcher) {
        val viewModel = CollectionViewModel(FakeRecipeRepository(sampleRecipes), FakeRecipeListRepository())

        viewModel.onExpandList("list-1")
        viewModel.onExpandList("list-2")

        assertEquals("list-2", viewModel.expandedListId.value)
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

private class FakeRecipeListRepository(
    private val lists: List<RecipeList> = emptyList()
) : RecipeListRepository {
    val createdLists = mutableListOf<Pair<String, String>>() // uid to name
    val deletedListIds = mutableListOf<String>()
    val addedRecipes = mutableListOf<Triple<String, String, String>>() // uid, listId, recipeId
    val removedRecipes = mutableListOf<Triple<String, String, String>>() // uid, listId, recipeId

    override suspend fun loadUserLists(uid: String): List<RecipeList> = lists

    override fun createList(uid: String, name: String): RecipeList {
        createdLists.add(Pair(uid, name))
        return RecipeList(id = "fake-${name.hashCode()}", name = name)
    }

    override fun deleteList(uid: String, listId: String) {
        deletedListIds.add(listId)
    }

    override fun addRecipeToList(uid: String, listId: String, recipeId: String) {
        addedRecipes.add(Triple(uid, listId, recipeId))
    }

    override fun removeRecipeFromList(uid: String, listId: String, recipeId: String) {
        removedRecipes.add(Triple(uid, listId, recipeId))
    }
}
