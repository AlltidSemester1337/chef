package com.formulae.chef.feature.chat

import com.formulae.chef.feature.chat.ui.ChatMessage
import com.formulae.chef.feature.chat.ui.Participant
import com.formulae.chef.feature.model.Recipe
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ChatUiStateTest {

    @Test
    fun `initial state has no messages`() {
        val state = ChatUiState()

        assertTrue(state.messages.isEmpty())
    }

    @Test
    fun `addMessage appends message to list`() {
        val state = ChatUiState()

        state.addMessage(ChatMessage(text = "Hello"))

        assertEquals(1, state.messages.size)
        assertEquals("Hello", state.messages[0].text)
    }

    @Test
    fun `addMessage appends multiple messages in order`() {
        val state = ChatUiState()

        state.addMessage(ChatMessage(text = "First"))
        state.addMessage(ChatMessage(text = "Second"))

        assertEquals(2, state.messages.size)
        assertEquals("First", state.messages[0].text)
        assertEquals("Second", state.messages[1].text)
    }

    @Test
    fun `replaceLastPendingMessage sets isPending false on last message`() {
        val state = ChatUiState()
        state.addMessage(ChatMessage(text = "Done", isPending = false))
        state.addMessage(ChatMessage(text = "Pending", isPending = true))

        state.replaceLastPendingMessage()

        assertFalse(state.messages.last().isPending)
        assertEquals(2, state.messages.size)
    }

    @Test
    fun `replaceLastPendingMessage on empty state does not crash`() {
        val state = ChatUiState()

        state.replaceLastPendingMessage()

        assertTrue(state.messages.isEmpty())
    }

    @Test
    fun `updateStarredMessage marks matching message as starred`() {
        val msg = ChatMessage(text = "Here is a recipe", participant = Participant.MODEL, isStarred = false)
        val state = ChatUiState(listOf(msg))

        state.updateStarredMessage(msg, isStarred = true)

        assertTrue(state.messages[0].isStarred)
    }

    @Test
    fun `updateStarredMessage does not affect other messages`() {
        val msg1 = ChatMessage(id = "m1", text = "First", isStarred = false)
        val msg2 = ChatMessage(id = "m2", text = "Second", isStarred = false)
        val state = ChatUiState(listOf(msg1, msg2))

        state.updateStarredMessage(msg1, isStarred = true)

        assertTrue(state.messages[0].isStarred)
        assertFalse(state.messages[1].isStarred)
    }

    @Test
    fun `updateMessageRecipes replaces recipe list on matching message`() {
        val messageId = "msg-1"
        val initial = ChatMessage(id = messageId, participant = Participant.MODEL, recipes = emptyList())
        val state = ChatUiState(listOf(initial))
        val updatedRecipes = listOf(
            Recipe(id = "r1", title = "Pasta", imageUrl = "https://example.com/pasta.jpg")
        )

        state.updateMessageRecipes(messageId, updatedRecipes)

        assertEquals(1, state.messages[0].recipes.size)
        assertEquals("Pasta", state.messages[0].recipes[0].title)
        assertEquals("https://example.com/pasta.jpg", state.messages[0].recipes[0].imageUrl)
    }

    @Test
    fun `updateMessageRecipes preserves other message fields`() {
        val messageId = "msg-1"
        val initial = ChatMessage(
            id = messageId,
            participant = Participant.MODEL,
            starredRecipeIds = setOf("r1"),
            recipes = emptyList()
        )
        val state = ChatUiState(listOf(initial))

        state.updateMessageRecipes(messageId, listOf(Recipe(id = "r1", title = "Pasta")))

        assertEquals(setOf("r1"), state.messages[0].starredRecipeIds)
    }

    @Test
    fun `updateMessageRecipes does nothing for unknown message id`() {
        val state = ChatUiState(listOf(ChatMessage(id = "msg-1", participant = Participant.MODEL)))

        state.updateMessageRecipes("nonexistent", listOf(Recipe(title = "Pasta")))

        assertTrue(state.messages[0].recipes.isEmpty())
    }

    @Test
    fun `updateRecipeStarred adds recipe id to starred set`() {
        val messageId = "msg-1"
        val state = ChatUiState(
            listOf(ChatMessage(id = messageId, participant = Participant.MODEL))
        )

        state.updateRecipeStarred(messageId, "r1", isStarred = true)

        assertTrue(state.messages[0].starredRecipeIds.contains("r1"))
    }

    @Test
    fun `updateRecipeStarred can star multiple recipes independently`() {
        val messageId = "msg-1"
        val state = ChatUiState(
            listOf(ChatMessage(id = messageId, participant = Participant.MODEL))
        )

        state.updateRecipeStarred(messageId, "r1", isStarred = true)
        state.updateRecipeStarred(messageId, "r2", isStarred = true)

        assertTrue(state.messages[0].starredRecipeIds.contains("r1"))
        assertTrue(state.messages[0].starredRecipeIds.contains("r2"))
    }

    @Test
    fun `updateRecipeStarred removes recipe id from starred set`() {
        val messageId = "msg-1"
        val state = ChatUiState(
            listOf(ChatMessage(id = messageId, participant = Participant.MODEL, starredRecipeIds = setOf("r1", "r2")))
        )

        state.updateRecipeStarred(messageId, "r1", isStarred = false)

        assertFalse(state.messages[0].starredRecipeIds.contains("r1"))
        assertTrue(state.messages[0].starredRecipeIds.contains("r2"))
    }

    @Test
    fun `updateRecipeStarred does nothing for unknown message id`() {
        val messageId = "msg-1"
        val state = ChatUiState(listOf(ChatMessage(id = messageId, participant = Participant.MODEL)))

        state.updateRecipeStarred("nonexistent", "r1", isStarred = true)

        assertTrue(state.messages[0].starredRecipeIds.isEmpty())
    }
}
