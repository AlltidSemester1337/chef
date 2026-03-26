package com.formulae.chef.feature.chat

import com.formulae.chef.feature.model.UserPreferences
import com.google.firebase.vertexai.type.Content
import com.google.firebase.vertexai.type.TextPart
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ChatHistoryHelpersTest {

    private fun makeContent(role: String, text: String) = Content(role, listOf(TextPart(text)))

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
        val userText = (result[0].parts.first() as TextPart).text
        assertTrue(userText.contains("no fish, metric units"))
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
}
