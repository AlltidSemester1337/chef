package com.formulae.chef.feature.chat

import com.formulae.chef.feature.chat.ui.ChatMessage
import com.formulae.chef.feature.chat.ui.Participant
import com.formulae.chef.feature.model.Recipe
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ChatMessageTest {

    @Test
    fun `default chat message has expected defaults`() {
        val message = ChatMessage()

        assertTrue(message.id.isNotEmpty())
        assertEquals("", message.text)
        assertEquals(Participant.USER, message.participant)
        assertFalse(message.isPending)
        assertFalse(message.isStarred)
        assertTrue(message.recipes.isEmpty())
        assertTrue(message.starredRecipeIds.isEmpty())
    }

    @Test
    fun `chat message with custom values`() {
        val message = ChatMessage(
            id = "msg-1",
            text = "Hello, can you suggest a recipe?",
            participant = Participant.USER,
            isPending = true,
            isStarred = false
        )

        assertEquals("msg-1", message.id)
        assertEquals("Hello, can you suggest a recipe?", message.text)
        assertEquals(Participant.USER, message.participant)
        assertTrue(message.isPending)
        assertFalse(message.isStarred)
    }

    @Test
    fun `model participant message`() {
        val message = ChatMessage(
            text = "Here is a recipe for pasta",
            participant = Participant.MODEL
        )

        assertEquals(Participant.MODEL, message.participant)
    }

    @Test
    fun `error participant message`() {
        val message = ChatMessage(
            text = "Something went wrong",
            participant = Participant.ERROR
        )

        assertEquals(Participant.ERROR, message.participant)
    }

    @Test
    fun `each message gets a unique id`() {
        val msg1 = ChatMessage()
        val msg2 = ChatMessage()

        assertNotEquals(msg1.id, msg2.id)
    }

    @Test
    fun `copy preserves id and updates fields`() {
        val original = ChatMessage(
            text = "Original",
            participant = Participant.USER,
            isStarred = false
        )

        val copy = original.copy(isStarred = true)

        assertEquals(original.id, copy.id)
        assertEquals(original.text, copy.text)
        assertTrue(copy.isStarred)
    }

    @Test
    fun `recipe grid message carries recipes and no text`() {
        val recipes = listOf(
            Recipe(id = "r1", title = "Pasta Carbonara"),
            Recipe(id = "r2", title = "Chicken Curry")
        )
        val message = ChatMessage(participant = Participant.MODEL, recipes = recipes)

        assertEquals(2, message.recipes.size)
        assertEquals("Pasta Carbonara", message.recipes[0].title)
        assertEquals("Chicken Curry", message.recipes[1].title)
        assertEquals("", message.text)
    }

    @Test
    fun `copy preserves recipes and updates starredRecipeIds`() {
        val recipes = listOf(Recipe(id = "r1", title = "Pasta"))
        val original = ChatMessage(participant = Participant.MODEL, recipes = recipes)

        val starred = original.copy(starredRecipeIds = setOf("r1"))

        assertEquals(original.id, starred.id)
        assertEquals(1, starred.recipes.size)
        assertTrue(starred.starredRecipeIds.contains("r1"))
    }

    @Test
    fun `starred recipe ids are independent per message`() {
        val msg1 = ChatMessage(participant = Participant.MODEL, starredRecipeIds = setOf("r1"))
        val msg2 = ChatMessage(participant = Participant.MODEL, starredRecipeIds = emptySet())

        assertTrue(msg1.starredRecipeIds.contains("r1"))
        assertFalse(msg2.starredRecipeIds.contains("r1"))
    }
}

class ParticipantTest {

    @Test
    fun `participant enum has three values`() {
        val values = Participant.entries

        assertEquals(3, values.size)
        assertTrue(values.contains(Participant.USER))
        assertTrue(values.contains(Participant.MODEL))
        assertTrue(values.contains(Participant.ERROR))
    }
}
