package com.formulae.chef

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ChefRoutesTest {

    @Test
    fun `collection without arguments is the bare tab route`() {
        assertEquals("collection", ChefRoutes.collection())
    }

    @Test
    fun `collection with tab only`() {
        assertEquals("collection?tab=COMMUNITY", ChefRoutes.collection(tab = "COMMUNITY"))
    }

    @Test
    fun `collection with recipe only`() {
        assertEquals("collection?recipeId=abc", ChefRoutes.collection(recipeId = "abc"))
    }

    @Test
    fun `collection with tab and recipe`() {
        assertEquals(
            "collection?tab=SAVED&recipeId=abc",
            ChefRoutes.collection(tab = "SAVED", recipeId = "abc")
        )
    }

    @Test
    fun `collection pattern declares both optional arguments`() {
        assertEquals("collection?tab={tab}&recipeId={recipeId}", ChefRoutes.COLLECTION_PATTERN)
    }

    @Test
    fun `collection entry with a recipeId is a recipe opened from Home`() {
        assertTrue(ChefRoutes.isRecipeOpenedFromHome(ChefRoutes.COLLECTION_PATTERN, "abc"))
    }

    @Test
    fun `collection entry without a recipeId is the regular Collections tab`() {
        assertFalse(ChefRoutes.isRecipeOpenedFromHome(ChefRoutes.COLLECTION_PATTERN, null))
        assertFalse(ChefRoutes.isRecipeOpenedFromHome(ChefRoutes.COLLECTION_PATTERN, ""))
    }

    @Test
    fun `recipeId on another destination is not a recipe opened from Home`() {
        assertFalse(ChefRoutes.isRecipeOpenedFromHome(CHAT_ROUTE_PATTERN, "abc"))
        assertFalse(ChefRoutes.isRecipeOpenedFromHome(null, "abc"))
    }
}
