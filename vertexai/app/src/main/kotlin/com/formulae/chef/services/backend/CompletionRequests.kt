package com.formulae.chef.services.backend

import com.formulae.chef.services.persistence.Content
import org.json.JSONArray
import org.json.JSONObject

/** Pure request/response shaping for `POST /v1/ai/complete`, kept free of I/O so it is unit tested. */
object CompletionRequests {

    /**
     * Builds `{"purpose": ..., "messages": [{"role": ..., "content": ...}]}`.
     *
     * - The app's `"model"` role is sent as `"assistant"`; `"user"` stays `"user"`. Any other role
     *   is a bug and throws [IllegalArgumentException] (the backend chooses the system prompt).
     * - A message's parts are concatenated into one `content` string.
     * - Messages whose content is blank are left out: the backend rejects empty content with 422.
     * - No other fields: the backend rejects unknown fields (model, temperature, ...) with 422.
     */
    fun buildRequestBody(purpose: AiPurpose, messages: List<Content>): String {
        val jsonMessages = messages.mapNotNull { (role, parts) ->
            val wireRole = toWireRole(role)
            val content = parts.joinToString("") { it.text }
            if (content.isBlank()) null else JSONObject().put("role", wireRole).put("content", content)
        }
        return JSONObject()
            .put("purpose", purpose.wireName)
            .put("messages", JSONArray(jsonMessages))
            .toString()
    }

    /** Extracts `content` from a 200 response body `{"content": "..."}`. */
    fun parseResponse(responseBody: String): String = JSONObject(responseBody).getString("content")

    private fun toWireRole(role: String): String = when (role) {
        "user" -> "user"
        "model" -> "assistant"
        else -> throw IllegalArgumentException("Unsupported chat role: $role")
    }
}
