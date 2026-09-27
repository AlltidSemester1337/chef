package com.formulae.chef

import org.junit.Assert.assertEquals
import org.junit.Test

class ChatRouteTest {

    @Test
    fun `route pattern keeps generate as base route with optional recipeId`() {
        assertEquals("generate?recipeId={recipeId}", CHAT_ROUTE_PATTERN)
        assertEquals("generate", CHAT_ROUTE_PATTERN.substringBefore("?"))
    }

    @Test
    fun `chatRouteForRecipe passes firebase push id as recipeId argument`() {
        assertEquals("generate?recipeId=-Nabc_123", chatRouteForRecipe("-Nabc_123"))
    }

    @Test
    fun `chatRouteForRecipe url-encodes reserved characters`() {
        assertEquals("generate?recipeId=a%26b%3Dc", chatRouteForRecipe("a&b=c"))
    }
}
