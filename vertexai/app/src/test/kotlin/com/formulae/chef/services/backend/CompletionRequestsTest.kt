package com.formulae.chef.services.backend

import com.formulae.chef.services.persistence.Content
import com.formulae.chef.services.persistence.Part
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class CompletionRequestsTest {

    private fun content(role: String, vararg texts: String) =
        Content(role = role, parts = texts.map { Part(it) })

    private fun build(purpose: AiPurpose, vararg messages: Content): JSONObject =
        JSONObject(CompletionRequests.buildRequestBody(purpose, messages.toList()))

    private fun JSONObject.messages(): JSONArray = getJSONArray("messages")

    // --- buildRequestBody: top level ---

    @Test
    fun buildRequestBody_sendsPurposeWireName() {
        val body = build(AiPurpose.DERIVE_RECIPE_JSON, content("user", "hi"))
        assertEquals("derive_recipe_json", body.getString("purpose"))
    }

    @Test
    fun buildRequestBody_hasOnlyPurposeAndMessages() {
        val body = build(AiPurpose.CHAT, content("user", "hi"))
        assertEquals(setOf("purpose", "messages"), body.keys().asSequence().toSet())
    }

    // --- buildRequestBody: messages ---

    @Test
    fun buildRequestBody_messageHasOnlyRoleAndContent() {
        val message = build(AiPurpose.CHAT, content("user", "hi")).messages().getJSONObject(0)
        assertEquals(setOf("role", "content"), message.keys().asSequence().toSet())
    }

    @Test
    fun buildRequestBody_userRoleStaysUser() {
        val message = build(AiPurpose.CHAT, content("user", "hi")).messages().getJSONObject(0)
        assertEquals("user", message.getString("role"))
    }

    @Test
    fun buildRequestBody_modelRoleIsSentAsAssistant() {
        val message = build(AiPurpose.CHAT, content("model", "hello")).messages().getJSONObject(0)
        assertEquals("assistant", message.getString("role"))
    }

    @Test
    fun buildRequestBody_unknownRole_throws() {
        assertThrows(IllegalArgumentException::class.java) {
            CompletionRequests.buildRequestBody(AiPurpose.CHAT, listOf(content("system", "be nice")))
        }
    }

    @Test
    fun buildRequestBody_unknownRoleOnBlankMessage_stillThrows() {
        assertThrows(IllegalArgumentException::class.java) {
            CompletionRequests.buildRequestBody(AiPurpose.CHAT, listOf(content("system", "")))
        }
    }

    @Test
    fun buildRequestBody_keepsWhitespaceOnlyPartsInsideContent() {
        val message = build(AiPurpose.CHAT, content("user", "Hello,", " ", "world")).messages().getJSONObject(0)
        assertEquals("Hello, world", message.getString("content"))
    }

    @Test
    fun buildRequestBody_joinsPartsIntoOneContentString() {
        val message = build(AiPurpose.CHAT, content("user", "Hello, ", "world")).messages().getJSONObject(0)
        assertEquals("Hello, world", message.getString("content"))
    }

    @Test
    fun buildRequestBody_preservesMessageOrder() {
        val messages = build(
            AiPurpose.CHAT,
            content("user", "first"),
            content("model", "second"),
            content("user", "third")
        ).messages()
        assertEquals(3, messages.length())
        assertEquals("first", messages.getJSONObject(0).getString("content"))
        assertEquals("second", messages.getJSONObject(1).getString("content"))
        assertEquals("third", messages.getJSONObject(2).getString("content"))
    }

    @Test
    fun buildRequestBody_dropsBlankMessages() {
        val messages = build(
            AiPurpose.CHAT,
            content("user", "question"),
            content("model", ""),
            content("model", "  ", "\n"),
            content("model"),
            content("user", "follow-up")
        ).messages()
        assertEquals(2, messages.length())
        assertEquals("question", messages.getJSONObject(0).getString("content"))
        assertEquals("follow-up", messages.getJSONObject(1).getString("content"))
    }

    @Test
    fun buildRequestBody_keepsContentVerbatimIncludingQuotesAndNewlines() {
        val text = "Line 1\nSay \"hej\" — 200 g smör"
        val message = build(AiPurpose.CHAT, content("user", text)).messages().getJSONObject(0)
        assertEquals(text, message.getString("content"))
    }

    // --- parseResponse ---

    @Test
    fun parseResponse_returnsContent() {
        assertEquals("Hello!", CompletionRequests.parseResponse("""{"content":"Hello!"}"""))
    }
}
