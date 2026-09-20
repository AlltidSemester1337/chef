package com.formulae.chef.services.telemetry

import io.opentelemetry.api.common.AttributeKey
import org.junit.Assert.assertEquals
import org.junit.Test

class LlmSpanAttributesTest {

    @Test
    fun input_setsOpenInferenceSpanKindToLlm() {
        val attrs = LlmSpanAttributes.input(
            "generateChatModelResponse",
            "mistralai/Mistral-Small-3.2-24B-Instruct-2506",
            "hi"
        )
        assertEquals("LLM", attrs.get(AttributeKey.stringKey("openinference.span.kind")))
    }

    @Test
    fun input_setsOperationNameToSpanName() {
        val attrs = LlmSpanAttributes.input(
            "generateJsonModelResponse",
            "mistralai/Mistral-Small-3.2-24B-Instruct-2506",
            "hi"
        )
        assertEquals("generateJsonModelResponse", attrs.get(AttributeKey.stringKey("operation.name")))
    }

    @Test
    fun input_derivesProviderAndSystemFromModelNamespace() {
        val attrs = LlmSpanAttributes.input(
            "generateChatModelResponse",
            "mistralai/Mistral-Small-3.2-24B-Instruct-2506",
            "hi"
        )
        assertEquals("mistralai", attrs.get(AttributeKey.stringKey("llm.provider")))
        assertEquals("mistralai", attrs.get(AttributeKey.stringKey("llm.system")))
    }

    @Test
    fun input_derivesProviderAndSystemForVertexAiImageModel() {
        val attrs = LlmSpanAttributes.input("generateImage", "vertexai/gemini-flash-image", "a photo")
        assertEquals("vertexai", attrs.get(AttributeKey.stringKey("llm.provider")))
        assertEquals("vertexai", attrs.get(AttributeKey.stringKey("llm.system")))
    }

    @Test
    fun input_fallsBackToFullModelNameWhenNoNamespace() {
        val attrs = LlmSpanAttributes.input("generateChatModelResponse", "gpt-4", "hi")
        assertEquals("gpt-4", attrs.get(AttributeKey.stringKey("llm.provider")))
        assertEquals("gpt-4", attrs.get(AttributeKey.stringKey("llm.system")))
    }

    @Test
    fun input_setsModelNameAndInputMessageAttributes() {
        val attrs = LlmSpanAttributes.input(
            "generateChatModelResponse",
            "mistralai/Mistral-Small-3.2-24B-Instruct-2506",
            "What's for dinner?"
        )
        assertEquals(
            "mistralai/Mistral-Small-3.2-24B-Instruct-2506",
            attrs.get(AttributeKey.stringKey("llm.model_name"))
        )
        assertEquals("user", attrs.get(AttributeKey.stringKey("llm.input_messages.0.message.role")))
        assertEquals("What's for dinner?", attrs.get(AttributeKey.stringKey("llm.input_messages.0.message.content")))
    }

    @Test
    fun input_setsTopLevelInputValueAndMimeType() {
        val attrs = LlmSpanAttributes.input(
            "generateChatModelResponse",
            "mistralai/Mistral-Small-3.2-24B-Instruct-2506",
            "What's for dinner?"
        )
        assertEquals("What's for dinner?", attrs.get(AttributeKey.stringKey("input.value")))
        assertEquals("text/plain", attrs.get(AttributeKey.stringKey("input.mime_type")))
    }

    @Test
    fun output_setsOutputMessageAttributes() {
        val attrs = LlmSpanAttributes.output("Try a pasta bake.")
        assertEquals("model", attrs.get(AttributeKey.stringKey("llm.output_messages.0.message.role")))
        assertEquals("Try a pasta bake.", attrs.get(AttributeKey.stringKey("llm.output_messages.0.message.content")))
    }

    @Test
    fun output_setsTopLevelOutputValueAndMimeType() {
        val attrs = LlmSpanAttributes.output("Try a pasta bake.")
        assertEquals("Try a pasta bake.", attrs.get(AttributeKey.stringKey("output.value")))
        assertEquals("text/plain", attrs.get(AttributeKey.stringKey("output.mime_type")))
    }
}
