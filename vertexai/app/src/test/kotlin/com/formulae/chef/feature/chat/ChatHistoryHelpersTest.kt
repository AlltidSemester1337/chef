package com.formulae.chef.feature.chat

import com.formulae.chef.feature.model.Ingredient
import com.formulae.chef.feature.model.Recipe
import com.formulae.chef.feature.model.UserPreferences
import com.formulae.chef.services.persistence.Content
import com.formulae.chef.services.persistence.Part
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ChatHistoryHelpersTest {

    private fun makeContent(role: String, text: String) = Content(role, listOf(Part(text)))

    // --- buildChatHistoryWithPreferences ---

    @Test
    fun buildChatHistory_nullPrefs_returnsUnchanged() {
        val history = listOf(makeContent("user", "hello"), makeContent("model", "hi"))
        val result = ChatViewModel.buildChatHistoryWithPreferences(history, null)
        assertEquals(history, result)
    }

    @Test
    fun buildChatHistory_blankSummary_returnsUnchanged() {
        val history = listOf(makeContent("user", "hello"))
        val result = ChatViewModel.buildChatHistoryWithPreferences(history, UserPreferences(summary = ""))
        assertEquals(history, result)
    }

    @Test
    fun buildChatHistory_withPrefs_prependsTwoEntries() {
        val history = listOf(makeContent("user", "hello"))
        val prefs = UserPreferences(summary = "prefers metric")
        val result = ChatViewModel.buildChatHistoryWithPreferences(history, prefs)
        assertEquals(3, result.size)
        assertEquals("user", result[0].role)
        assertEquals("model", result[1].role)
        assertEquals("user", result[2].role)
    }

    @Test
    fun buildChatHistory_prependedUserEntry_containsPrefSummary() {
        val prefs = UserPreferences(summary = "no fish, metric units")
        val result = ChatViewModel.buildChatHistoryWithPreferences(emptyList(), prefs)
        val userText = result[0].parts.first().text
        assertTrue(userText.contains("no fish, metric units"))
    }

    @Test
    fun buildChatHistory_withCollectionTitles_includesTitlesInContextMessage() {
        val prefs = UserPreferences(summary = "no fish")
        val titles = listOf("Pasta Carbonara", "Chicken Tikka")
        val result = ChatViewModel.buildChatHistoryWithPreferences(emptyList(), prefs, titles)
        val userText = result[0].parts.first().text
        assertTrue(userText.contains("Pasta Carbonara"))
        assertTrue(userText.contains("Chicken Tikka"))
    }

    @Test
    fun buildChatHistory_collectionTitlesOnly_prependsContextWithoutPrefs() {
        val titles = listOf("Beef Rendang")
        val result = ChatViewModel.buildChatHistoryWithPreferences(emptyList(), null, titles)
        assertEquals(2, result.size)
        val userText = result[0].parts.first().text
        assertTrue(userText.contains("Beef Rendang"))
    }

    @Test
    fun buildChatHistory_emptyCollectionTitles_behavesLikeNoTitles() {
        val history = listOf(makeContent("user", "hello"))
        val result = ChatViewModel.buildChatHistoryWithPreferences(history, null, emptyList())
        assertEquals(history, result)
    }

    @Test
    fun buildChatHistory_withPrefsAndTitles_containsBoth() {
        val prefs = UserPreferences(summary = "vegan")
        val titles = listOf("Lentil Soup", "Falafel")
        val result = ChatViewModel.buildChatHistoryWithPreferences(emptyList(), prefs, titles)
        val userText = result[0].parts.first().text
        assertTrue(userText.contains("vegan"))
        assertTrue(userText.contains("Lentil Soup"))
        assertTrue(userText.contains("Falafel"))
    }

    // --- selectEntriesToCompact ---

    @Test
    fun selectToCompact_emptyList_returnsEmpty() {
        val result = ChatViewModel.selectEntriesToCompact(emptyList())
        assertTrue(result.isEmpty())
    }

    @Test
    fun selectToCompact_exactlyKeepLast_returnsEmpty() {
        val entries = (1..20).map { Pair("id$it", makeContent("user", "msg$it")) }
        val result = ChatViewModel.selectEntriesToCompact(entries)
        assertTrue(result.isEmpty())
    }

    @Test
    fun selectToCompact_belowThreshold_returnsEmpty() {
        val entries = (1..10).map { Pair("id$it", makeContent("user", "msg$it")) }
        val result = ChatViewModel.selectEntriesToCompact(entries)
        assertTrue(result.isEmpty())
    }

    @Test
    fun selectToCompact_aboveThreshold_returnsOldEntries() {
        val entries = (1..25).map { Pair("id$it", makeContent("user", "msg$it")) }
        val result = ChatViewModel.selectEntriesToCompact(entries)
        assertEquals(5, result.size)
        assertEquals("id1", result.first().first)
        assertEquals("id5", result.last().first)
    }

    @Test
    fun selectToCompact_40entries_returns20() {
        val entries = (1..40).map { Pair("id$it", makeContent("user", "msg$it")) }
        val result = ChatViewModel.selectEntriesToCompact(entries)
        assertEquals(20, result.size)
    }

    // --- recipe context priming (issue #60) ---

    private val recipe = Recipe(
        id = "-Nabc123",
        title = "Healthier Moussaka",
        summary = "A lighter take on the Greek classic.",
        ingredients = listOf(Ingredient(name = "ground lamb", quantity = "500", unit = "g")),
        instructions = listOf("Brown the lamb.", "Layer and bake.")
    )

    @Test
    fun recipeContextGreeting_includesRecipeTitle() {
        val greeting = ChatViewModel.recipeContextGreeting("  Healthier Moussaka ")
        assertEquals("Ask me about or adjust something in the recipe \"Healthier Moussaka\".", greeting)
    }

    @Test
    fun buildRecipeContextEntries_returnsUserThenModelPair() {
        val entries = ChatViewModel.buildRecipeContextEntries(recipe)
        assertEquals(2, entries.size)
        assertEquals("user", entries[0].role)
        assertEquals("model", entries[1].role)
    }

    @Test
    fun buildRecipeContextEntries_userTurnContainsRecipeDetails() {
        val contextText = ChatViewModel.buildRecipeContextEntries(recipe)[0].parts.first().text
        assertTrue(contextText.contains("Recipe: Healthier Moussaka"))
        assertTrue(contextText.contains("500 g ground lamb"))
        assertTrue(contextText.contains("1. Brown the lamb."))
        assertTrue(contextText.contains("2. Layer and bake."))
    }

    @Test
    fun buildRecipeContextEntries_modelTurnMatchesShownGreeting() {
        val modelText = ChatViewModel.buildRecipeContextEntries(recipe)[1].parts.first().text
        assertEquals(ChatViewModel.recipeContextGreeting("Healthier Moussaka"), modelText)
    }
}
