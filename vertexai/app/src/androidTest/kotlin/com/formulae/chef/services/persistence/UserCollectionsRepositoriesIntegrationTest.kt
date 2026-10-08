package com.formulae.chef.services.persistence

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.formulae.chef.feature.model.Recipe
import com.formulae.chef.feature.model.RecipeVariant
import com.formulae.chef.services.persistence.FirestoreEmulator.awaitCondition
import com.formulae.chef.services.persistence.FirestoreEmulator.signInAsNewUser
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Lists, liked messages and recipe variants against the Firestore emulator with production rules.
 * See [FirestoreEmulator].
 */
@RunWith(AndroidJUnit4::class)
class UserCollectionsRepositoriesIntegrationTest {
    private val firestore get() = FirestoreEmulator.firestore

    @Test
    fun recipeLists_createAddRemoveDelete() = runBlocking {
        val uid = signInAsNewUser()
        val repository = RecipeListRepositoryImpl(firestore = firestore)

        val first = repository.createList(uid, "Vardag")
        awaitCondition({ repository.loadUserLists(uid) }) { it.size == 1 } // distinct createdAt
        val second = repository.createList(uid, "Fest")
        val lists = awaitCondition({ repository.loadUserLists(uid) }) { it.size == 2 }
        assertEquals(listOf("Vardag", "Fest"), lists.map { it.name })
        assertEquals(first.id, lists[0].id)

        repository.addRecipeToList(uid, first.id!!, "r1")
        repository.addRecipeToList(uid, first.id!!, "r2")
        repository.addRecipeToList(uid, first.id!!, "r1") // duplicate is ignored
        val withRecipes = awaitCondition({ repository.loadUserLists(uid) }) { it[0].recipeIds.size == 2 }
        assertEquals(listOf("r1", "r2"), withRecipes[0].recipeIds)

        repository.removeRecipeFromList(uid, first.id!!, "r1")
        assertEquals(listOf("r2"), awaitCondition({ repository.loadUserLists(uid) }) { it[0].recipeIds.size == 1 }[0].recipeIds)

        repository.deleteList(uid, second.id!!)
        assertEquals(listOf("Vardag"), awaitCondition({ repository.loadUserLists(uid) }) { it.size == 1 }.map { it.name })
    }

    @Test
    fun likedMessages_saveLoadDelete() = runBlocking {
        val repository = LikedMessagesRepositoryImpl(uid = signInAsNewUser(), firestore = firestore)

        val firstId = repository.saveLikedMessage("Första")!!
        awaitCondition({ repository.loadLikedMessages() }) { it.size == 1 }
        repository.saveLikedMessage("Andra")

        val liked = awaitCondition({ repository.loadLikedMessages() }) { it.size == 2 }
        assertEquals(listOf("Första", "Andra"), liked.map { it.second.text })
        assertEquals(firstId, liked[0].first)
        assertTrue(liked[0].second.likedAt.isNotBlank())

        repository.deleteMessages(listOf(firstId))
        assertEquals(listOf("Andra"), awaitCondition({ repository.loadLikedMessages() }) { it.size == 1 }.map { it.second.text })
    }

    @Test
    fun variants_ownerCanSavePinAndDelete() = runBlocking {
        val uid = signInAsNewUser()
        val recipeId = "variant-parent-$uid"
        RecipeRepositoryImpl(firestore = firestore).saveRecipe(Recipe(id = recipeId, uid = uid, title = "Bas"))
        val repository = RecipeVariantRepositoryImpl(firestore = firestore)
        awaitCondition({ RecipeRepositoryImpl(firestore = firestore).getRecipeById(recipeId) }) { it != null }

        val variantId = repository.saveVariant(
            recipeId,
            RecipeVariant(label = "Vegansk", title = "Vegansk bas", createdAt = "2026-10-08T07:24:00Z")
        )
        val loaded = repository.loadVariantsForRecipe(recipeId).single()
        assertEquals(variantId, loaded.id)
        assertEquals("2026-10-08T07:24:00Z", loaded.createdAt)
        assertFalse(loaded.isPinned)

        repository.updateVariantIsPinned(recipeId, variantId, true)
        assertTrue(awaitCondition({ repository.loadVariantsForRecipe(recipeId) }) { it.single().isPinned }.single().isPinned)

        repository.deleteVariant(recipeId, variantId)
        assertTrue(awaitCondition({ repository.loadVariantsForRecipe(recipeId) }) { it.isEmpty() }.isEmpty())
    }

    @Test
    fun variants_nonOwnerCannotSave() = runBlocking {
        val owner = signInAsNewUser()
        val recipeId = "foreign-parent-$owner"
        RecipeRepositoryImpl(firestore = firestore).saveRecipe(Recipe(id = recipeId, uid = owner, title = "Bas"))
        awaitCondition({ RecipeRepositoryImpl(firestore = firestore).getRecipeById(recipeId) }) { it != null }

        signInAsNewUser()
        try {
            RecipeVariantRepositoryImpl(firestore = firestore).saveVariant(recipeId, RecipeVariant(label = "Spam"))
            fail("Expected PERMISSION_DENIED")
        } catch (expected: Exception) {
            // rules: only the parent recipe's owner may write variants
        }
    }
}
