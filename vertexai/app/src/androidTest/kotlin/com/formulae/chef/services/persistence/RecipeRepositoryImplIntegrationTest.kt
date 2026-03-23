package com.formulae.chef.services.persistence

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.formulae.chef.feature.model.Difficulty
import com.formulae.chef.feature.model.Ingredient
import com.formulae.chef.feature.model.Nutrient
import com.formulae.chef.feature.model.Recipe
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Integration tests for [RecipeRepositoryImpl] against the Firebase Realtime Database emulator.
 *
 * Prerequisites:
 *   - Firebase Emulator Suite running: `firebase emulators:start --only database`
 *   - An Android emulator or connected device
 *
 * The tests use a dedicated emulator namespace (`chef-integration-test`) and clean up after
 * each test, so they are safe to run against the local emulator without touching production data.
 */
@RunWith(AndroidJUnit4::class)
class RecipeRepositoryImplIntegrationTest {

    private lateinit var testDatabase: FirebaseDatabase
    private lateinit var repository: RecipeRepositoryImpl

    private val testUid = "test-user-1"
    private val otherUid = "test-user-2"

    @Before
    fun setup() {
        testDatabase = FirebaseDatabase.getInstance("http://10.0.2.2:9000?ns=chef-integration-test")
        testDatabase.useEmulator("10.0.2.2", 9000)
        repository = RecipeRepositoryImpl(database = testDatabase)
    }

    @After
    fun tearDown() = runBlocking {
        testDatabase.getReference("/").removeValue().await()
    }

    @Test
    fun saveRecipe_assignsIdAndPersistsRecipe() = runBlocking {
        val recipe = Recipe(
            uid = testUid,
            title = "Pasta Carbonara",
            summary = "Classic Italian pasta",
            difficulty = Difficulty.MEDIUM
        )

        repository.saveRecipe(recipe)

        val loaded = repository.loadAllRecipes()
        assertEquals(1, loaded.size)
        assertNotNull(loaded[0].id)
        assertEquals("Pasta Carbonara", loaded[0].title)
        assertEquals(testUid, loaded[0].uid)
    }

    @Test
    fun loadAllRecipes_returnsAllSavedRecipes() = runBlocking {
        repository.saveRecipe(Recipe(uid = testUid, title = "Recipe A", summary = "A"))
        repository.saveRecipe(Recipe(uid = testUid, title = "Recipe B", summary = "B"))
        repository.saveRecipe(Recipe(uid = otherUid, title = "Recipe C", summary = "C"))

        val loaded = waitForRecipes(count = 3)
        assertEquals(3, loaded.size)
    }

    @Test
    fun loadUserRecipes_returnsOnlyRecipesForThatUser() = runBlocking {
        repository.saveRecipe(Recipe(uid = testUid, title = "My Recipe", summary = "mine"))
        repository.saveRecipe(Recipe(uid = otherUid, title = "Other Recipe", summary = "other"))

        waitForRecipes(count = 2)

        val userRecipes = repository.loadUserRecipes(testUid)
        assertEquals(1, userRecipes.size)
        assertEquals("My Recipe", userRecipes[0].title)
        assertEquals(testUid, userRecipes[0].uid)
    }

    @Test
    fun loadAllRecipes_preservesIsFavouriteField() = runBlocking {
        repository.saveRecipe(Recipe(uid = testUid, title = "Favourite", summary = "fav", isFavourite = true))

        val loaded = waitForRecipes(count = 1)
        assertTrue(loaded[0].isFavourite)
    }

    @Test
    fun loadAllRecipes_preservesIngredientsAndInstructions() = runBlocking {
        val recipe = Recipe(
            uid = testUid,
            title = "Full Recipe",
            summary = "Complete",
            ingredients = listOf(Ingredient(name = "Flour", quantity = "200", unit = "g")),
            instructions = listOf("Mix", "Bake"),
            nutrientsPerServing = listOf(Nutrient(name = "Calories", quantity = "300", unit = "kcal"))
        )
        repository.saveRecipe(recipe)

        val loaded = waitForRecipes(count = 1)
        assertEquals(1, loaded[0].ingredients.size)
        assertEquals("Flour", loaded[0].ingredients[0].name)
        assertEquals(2, loaded[0].instructions.size)
        assertEquals("Mix", loaded[0].instructions[0])
        assertEquals(1, loaded[0].nutrientsPerServing?.size)
    }

    @Test
    fun removeRecipe_deletesRecipeFromDatabase() = runBlocking {
        repository.saveRecipe(Recipe(uid = testUid, title = "To Delete", summary = "delete me"))
        val savedId = waitForRecipes(count = 1)[0].id!!

        repository.removeRecipe(savedId)

        val remaining = waitForRecipes(count = 0)
        assertTrue(remaining.isEmpty())
    }

    @Test
    fun removeRecipeUid_setsUidToNull() = runBlocking {
        repository.saveRecipe(Recipe(uid = testUid, title = "Shared Recipe", summary = "shared"))
        val savedId = waitForRecipes(count = 1)[0].id!!

        repository.removeRecipeUid(savedId)

        // After uid is set to null, it should not appear in user-specific queries
        waitForRecipes(count = 1) // still exists in DB
        val userRecipes = repository.loadUserRecipes(testUid)
        assertTrue(userRecipes.none { it.id == savedId })
    }

    @Test
    fun loadUserRecipes_returnsEmptyListWhenNoRecipesForUser() = runBlocking {
        repository.saveRecipe(Recipe(uid = otherUid, title = "Not Mine", summary = "other"))
        waitForRecipes(count = 1)

        val userRecipes = repository.loadUserRecipes(testUid)
        assertTrue(userRecipes.isEmpty())
    }

    /** Polls until [count] recipes are visible in the DB, to account for async writes. */
    private suspend fun waitForRecipes(count: Int): List<Recipe> {
        repeat(10) {
            val recipes = repository.loadAllRecipes()
            if (recipes.size == count) return recipes
            kotlinx.coroutines.delay(200)
        }
        return repository.loadAllRecipes()
    }
}
