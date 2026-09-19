package com.formulae.chef.services.ai

import com.formulae.chef.services.persistence.Content
import com.formulae.chef.services.persistence.Part
import org.json.JSONException
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class BergetChatCompletionServiceTest {

    private fun defaultConfig(jsonMode: Boolean = false) = BergetModelConfig(
        model = BERGET_MISTRAL_SMALL_3_2,
        temperature = 0.5f,
        topP = 0.9f,
        maxTokens = 1024,
        jsonMode = jsonMode
    )

    // --- buildRequestBody ---

    @Test
    fun buildRequestBody_noSystemInstruction_omitsSystemMessage() {
        val body = JSONObject(
            BergetChatCompletionService.buildRequestBody(
                defaultConfig(),
                listOf(Content(role = "user", parts = listOf(Part("hello"))))
            )
        )
        val messages = body.getJSONArray("messages")
        assertEquals(1, messages.length())
        assertEquals("user", messages.getJSONObject(0).getString("role"))
    }

    @Test
    fun buildRequestBody_withSystemInstruction_prependsSystemMessage() {
        val config = defaultConfig().copy(systemInstruction = "You are Chef.")
        val body = JSONObject(
            BergetChatCompletionService.buildRequestBody(
                config,
                listOf(Content(role = "user", parts = listOf(Part("hello"))))
            )
        )
        val messages = body.getJSONArray("messages")
        assertEquals(2, messages.length())
        assertEquals("system", messages.getJSONObject(0).getString("role"))
        assertEquals("You are Chef.", messages.getJSONObject(0).getString("content"))
        assertEquals("user", messages.getJSONObject(1).getString("role"))
    }

    @Test
    fun buildRequestBody_modelRole_translatedToAssistant() {
        val body = JSONObject(
            BergetChatCompletionService.buildRequestBody(
                defaultConfig(),
                listOf(
                    Content(role = "user", parts = listOf(Part("hi"))),
                    Content(role = "model", parts = listOf(Part("hello there")))
                )
            )
        )
        val messages = body.getJSONArray("messages")
        assertEquals("assistant", messages.getJSONObject(1).getString("role"))
        assertEquals("hello there", messages.getJSONObject(1).getString("content"))
    }

    @Test
    fun buildRequestBody_jsonModeTrue_includesResponseFormat() {
        val body = JSONObject(
            BergetChatCompletionService.buildRequestBody(
                defaultConfig(jsonMode = true),
                listOf(Content(role = "user", parts = listOf(Part("hi"))))
            )
        )
        assertEquals("json_object", body.getJSONObject("response_format").getString("type"))
    }

    @Test
    fun buildRequestBody_jsonModeFalse_omitsResponseFormat() {
        val body = JSONObject(
            BergetChatCompletionService.buildRequestBody(
                defaultConfig(jsonMode = false),
                listOf(Content(role = "user", parts = listOf(Part("hi"))))
            )
        )
        assertFalse(body.has("response_format"))
    }

    @Test
    fun buildRequestBody_numericFieldsSerializedCorrectly() {
        val config = BergetModelConfig(
            model = BERGET_MISTRAL_SMALL_3_2,
            temperature = 1.0f,
            topP = 0.95f,
            maxTokens = 8192
        )
        val body = JSONObject(
            BergetChatCompletionService.buildRequestBody(
                config,
                listOf(Content(role = "user", parts = listOf(Part("hi"))))
            )
        )
        assertEquals(BERGET_MISTRAL_SMALL_3_2, body.getString("model"))
        assertEquals(1.0, body.getDouble("temperature"), 0.0001)
        assertEquals(0.95, body.getDouble("top_p"), 0.0001)
        assertEquals(8192, body.getInt("max_tokens"))
    }

    // --- parseChatCompletionResponse ---

    @Test
    fun parseChatCompletionResponse_happyPath_extractsContent() {
        val json = """{"choices":[{"message":{"role":"assistant","content":"Hello, chef!"}}]}"""
        assertEquals("Hello, chef!", BergetChatCompletionService.parseChatCompletionResponse(json))
    }

    @Test
    fun parseChatCompletionResponse_emptyChoices_throwsBergetChatCompletionException() {
        val json = """{"choices":[]}"""
        try {
            BergetChatCompletionService.parseChatCompletionResponse(json)
            fail("Expected BergetChatCompletionException")
        } catch (e: BergetChatCompletionException) {
            assertTrue(e.message!!.contains("No choices"))
        }
    }

    @Test
    fun parseChatCompletionResponse_missingChoices_throwsBergetChatCompletionException() {
        val json = """{"id":"abc"}"""
        try {
            BergetChatCompletionService.parseChatCompletionResponse(json)
            fail("Expected BergetChatCompletionException")
        } catch (e: BergetChatCompletionException) {
            assertTrue(e.message!!.contains("No choices"))
        }
    }

    @Test(expected = JSONException::class)
    fun parseChatCompletionResponse_malformedJson_throwsJSONException() {
        BergetChatCompletionService.parseChatCompletionResponse("not json")
    }
}
