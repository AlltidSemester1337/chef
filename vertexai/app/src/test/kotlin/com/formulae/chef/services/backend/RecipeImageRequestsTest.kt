package com.formulae.chef.services.backend

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Test

class RecipeImageRequestsTest {

    @Test
    fun buildRequestBody_hasOnlyTheTrimmedRecipe() {
        val json = JSONObject(RecipeImageRequests.buildRequestBody("  Recipe: Pancakes\n"))

        assertEquals(setOf("recipe"), json.keys().asSequence().toSet())
        assertEquals("Recipe: Pancakes", json.getString("recipe"))
    }

    @Test
    fun buildRequestBody_cutsLongRecipesToTheBackendLimit() {
        val json = JSONObject(RecipeImageRequests.buildRequestBody("a".repeat(10_000)))

        assertEquals(RecipeImageRequests.MAX_RECIPE_LENGTH, json.getString("recipe").length)
    }

    @Test(expected = IllegalArgumentException::class)
    fun buildRequestBody_blankRecipe_throws() {
        RecipeImageRequests.buildRequestBody(" \n ")
    }

    @Test
    fun parseResponse_returnsImageUrl() {
        val url = "https://firebasestorage.googleapis.com/v0/b/bucket/o/recipes%2Fabc.jpg?alt=media"

        assertEquals(url, RecipeImageRequests.parseResponse("""{"imageUrl":"$url"}"""))
    }
}
