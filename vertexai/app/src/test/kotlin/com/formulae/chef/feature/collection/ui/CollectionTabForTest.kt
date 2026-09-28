package com.formulae.chef.feature.collection.ui

import com.formulae.chef.feature.model.Recipe
import org.junit.Assert.assertEquals
import org.junit.Test

class CollectionTabForTest {

    @Test
    fun `own favourite recipe opens under Saved`() {
        val recipe = Recipe(id = "r1", uid = "me", isFavourite = true)
        assertEquals(RecipeSource.SAVED, collectionTabFor(recipe, currentUid = "me"))
    }

    @Test
    fun `someone else's recipe opens under Community`() {
        val recipe = Recipe(id = "r1", uid = "other", isFavourite = true)
        assertEquals(RecipeSource.COMMUNITY, collectionTabFor(recipe, currentUid = "me"))
    }

    @Test
    fun `own non-favourite recipe is not in Saved so opens under Community`() {
        val recipe = Recipe(id = "r1", uid = "me", isFavourite = false)
        assertEquals(RecipeSource.COMMUNITY, collectionTabFor(recipe, currentUid = "me"))
    }

    @Test
    fun `anonymous user always opens under Community`() {
        val recipe = Recipe(id = "r1", uid = "", isFavourite = true)
        assertEquals(RecipeSource.COMMUNITY, collectionTabFor(recipe, currentUid = null))
    }
}
