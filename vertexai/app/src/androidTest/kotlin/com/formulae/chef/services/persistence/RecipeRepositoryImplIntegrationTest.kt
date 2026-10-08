package com.formulae.chef.services.persistence

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.formulae.chef.feature.model.Difficulty
import com.formulae.chef.feature.model.Ingredient
import com.formulae.chef.feature.model.Nutrient
import com.formulae.chef.feature.model.Recipe
import com.formulae.chef.services.persistence.FirestoreEmulator.awaitCondition
import com.formulae.chef.services.persistence.FirestoreEmulator.signInAsNewUser
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** [RecipeRepositoryImpl] against the Firestore emulator with production rules. See [FirestoreEmulator]. */
@RunWith(AndroidJUnit4::class)
class RecipeRepositoryImplIntegrationTest {

    private lateinit var repository: RecipeRepositoryImpl

    @Before
    fun setup() {
        repository = RecipeRepositoryImpl(firestore = FirestoreEmulator.firestore)
    }

    private suspend fun awaitRecipe(id: String) = awaitCondition({ repository.getRecipeById(id) }) { it != null }

    @Test
    fun saveRecipe_assignsIdAndPersistsAllFields() = runBlocking {
        val uid = signInAsNewUser()
        val recipe = Recipe(
            id = "recipe-$uid",
            uid = uid,
            title = "Pasta Carbonara",
            summary = "Classic Italian pasta",
            difficulty = Difficulty.MEDIUM,
            isFavourite = true,
            updatedAt = "2025-02-12T13:58:18.650Z",
            ingredients = listOf(Ingredient(name = "Flour", quantity = "1/2", unit = "g")),
            instructions = listOf("Mix", "Bake"),
            nutrientsPerServing = listOf(Nutrient(name = "Calories", quantity = "300", unit = "kcal")),
            tags = listOf("italian", "weeknight")
        )

        repository.saveRecipe(recipe)
        val loaded = awaitRecipe("recipe-$uid")!!

        assertEquals("recipe-$uid", loaded.id)
        assertEquals(uid, loaded.uid)
        assertEquals("Pasta Carbonara", loaded.title)
        assertEquals(Difficulty.MEDIUM, loaded.difficulty)
        assertTrue(loaded.isFavourite)
        assertEquals("2025-02-12T13:58:18.650Z", loaded.updatedAt)
        assertEquals("1/2", loaded.ingredients[0].quantity)
        assertEquals(listOf("Mix", "Bake"), loaded.instructions)
        assertEquals("kcal", loaded.nutrientsPerServing!![0].unit)
        assertEquals(listOf("italian", "weeknight"), loaded.tags)
    }

    @Test
    fun saveRecipe_withoutId_generatesOne() = runBlocking {
        val uid = signInAsNewUser()
        repository.saveRecipe(Recipe(uid = uid, title = "No id"))

        val mine = awaitCondition({ repository.loadUserRecipes(uid) }) { it.size == 1 }
        assertNotNull(mine.single().id)
    }

    @Test
    fun saveRecipe_withExistingId_overwritesWithoutDuplicate_andRemoveDeletes() = runBlocking {
        val uid = signInAsNewUser()
        repository.saveRecipe(Recipe(id = "fixed-$uid", uid = uid, title = "v1"))
        awaitRecipe("fixed-$uid")
        repository.saveRecipe(Recipe(id = "fixed-$uid", uid = uid, title = "v2"))

        val mine = awaitCondition({ repository.loadUserRecipes(uid) }) { it.singleOrNull()?.title == "v2" }
        assertEquals(1, mine.size)

        repository.removeRecipe("fixed-$uid")
        assertNull(awaitCondition({ repository.getRecipeById("fixed-$uid") }) { it == null })
    }

    @Test
    fun loadUserRecipes_returnsOnlyThatUsersRecipes_andAllRecipesIncludesOthers() = runBlocking {
        val alice = signInAsNewUser()
        repository.saveRecipe(Recipe(id = "a1-$alice", uid = alice, title = "Alice 1"))
        repository.saveRecipe(Recipe(id = "a2-$alice", uid = alice, title = "Alice 2"))
        awaitRecipe("a2-$alice")

        val bob = signInAsNewUser()
        repository.saveRecipe(Recipe(id = "b1-$bob", uid = bob, title = "Bob 1"))
        awaitRecipe("b1-$bob")

        assertEquals(listOf("Alice 1", "Alice 2"), repository.loadUserRecipes(alice).map { it.title }.sorted())
        val allIds = repository.loadAllRecipes().map { it.id }
        assertTrue(allIds.containsAll(listOf("a1-$alice", "a2-$alice", "b1-$bob")))
    }

    @Test
    fun removeRecipeUid_orphansRecipe_whichStillLoadsWithoutCrashing() = runBlocking {
        val uid = signInAsNewUser()
        repository.saveRecipe(Recipe(id = "orphan-$uid", uid = uid, title = "Shared"))
        awaitRecipe("orphan-$uid")

        repository.removeRecipeUid("orphan-$uid")

        val orphan = awaitCondition({ repository.getRecipeById("orphan-$uid") }) { it?.uid == "" }!!
        assertEquals("", orphan.uid)
        assertTrue(repository.loadUserRecipes(uid).none { it.id == "orphan-$uid" })
        // Regression: an explicit `uid: null` would make toObject() throw for the whole query.
        assertTrue(repository.loadAllRecipes().any { it.id == "orphan-$uid" })
    }

    @Test
    fun loadRecipes_areSortedNewestFirst() = runBlocking {
        val uid = signInAsNewUser()
        repository.saveRecipe(Recipe(id = "late-$uid", uid = uid, title = "late", updatedAt = "2026-02-01T00:00:00Z"))
        repository.saveRecipe(Recipe(id = "early-$uid", uid = uid, title = "early", updatedAt = "2026-01-01T00:00:00Z"))

        val mine = awaitCondition({ repository.loadUserRecipes(uid) }) { it.size == 2 }
        assertEquals(listOf("late", "early"), mine.map { it.title })
        assertEquals(listOf("late", "early"), repository.loadAllRecipes().filter { it.uid == uid }.map { it.title })
    }
}
