package com.formulae.chef.services.telemetry

import io.opentelemetry.api.common.AttributeKey
import io.opentelemetry.api.common.Attributes

/**
 * Builds OpenInference-compliant span attributes for LLM call spans. Pure/no OTel context or
 * span state so it's unit-testable with hardcoded fixtures.
 *
 * See https://github.com/Arize-ai/openinference/blob/main/spec/semantic_conventions.md
 */
object LlmSpanAttributes {

    fun input(spanName: String, modelName: String, prompt: String): Attributes {
        val vendor = modelName.substringBefore("/", missingDelimiterValue = modelName)
        return Attributes.builder()
            .put(AttributeKey.stringKey("openinference.span.kind"), "LLM")
            .put(AttributeKey.stringKey("operation.name"), spanName)
            .put(AttributeKey.stringKey("llm.model_name"), modelName)
            .put(AttributeKey.stringKey("llm.provider"), vendor)
            .put(AttributeKey.stringKey("llm.system"), vendor)
            .put(AttributeKey.stringKey("llm.input_messages.0.message.role"), "user")
            .put(AttributeKey.stringKey("llm.input_messages.0.message.content"), prompt)
            .put(AttributeKey.stringKey("input.value"), prompt)
            .put(AttributeKey.stringKey("input.mime_type"), "text/plain")
            .build()
    }

    fun output(response: String): Attributes =
        Attributes.builder()
            .put(AttributeKey.stringKey("llm.output_messages.0.message.role"), "model")
            .put(AttributeKey.stringKey("llm.output_messages.0.message.content"), response)
            .put(AttributeKey.stringKey("output.value"), response)
            .put(AttributeKey.stringKey("output.mime_type"), "text/plain")
            .build()
}
