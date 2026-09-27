package com.formulae.chef

import org.junit.Assert.assertEquals
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
}
