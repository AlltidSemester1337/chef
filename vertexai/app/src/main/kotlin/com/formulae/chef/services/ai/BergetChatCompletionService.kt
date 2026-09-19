package com.formulae.chef.services.ai

import android.util.Log
import com.formulae.chef.services.persistence.Content
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

const val BERGET_MISTRAL_SMALL_3_2 = "mistralai/Mistral-Small-3.2-24B-Instruct-2506"

data class BergetModelConfig(
    val model: String,
    val systemInstruction: String? = null,
    val temperature: Float,
    val topP: Float,
    val maxTokens: Int,
    val jsonMode: Boolean = false
)

class BergetChatCompletionService(private val apiKey: String) {

    suspend fun createChatCompletion(config: BergetModelConfig, messages: List<Content>): String =
        withContext(Dispatchers.IO) {
            val url = URL("https://api.berget.ai/v1/chat/completions")
            val connection = url.openConnection() as HttpURLConnection
            try {
                connection.requestMethod = "POST"
                connection.setRequestProperty("Content-Type", "application/json")
                connection.setRequestProperty("Authorization", "Bearer $apiKey")
                connection.doOutput = true
                connection.connectTimeout = 15_000
                connection.readTimeout = 60_000

                val body = buildRequestBody(config, messages)
                OutputStreamWriter(connection.outputStream).use { it.write(body) }

                val responseCode = connection.responseCode
                if (responseCode != HttpURLConnection.HTTP_OK) {
                    val error = connection.errorStream?.bufferedReader()?.readText() ?: "Unknown error"
                    Log.e("BergetChatCompletionService", "HTTP $responseCode: $error")
                    throw BergetChatCompletionException(
                        "Chat completion request failed with HTTP $responseCode: $error"
                    )
                }

                val responseBody = connection.inputStream.bufferedReader().readText()
                parseChatCompletionResponse(responseBody)
            } finally {
                connection.disconnect()
            }
        }

    companion object {
        fun buildRequestBody(config: BergetModelConfig, messages: List<Content>): String {
            val messagesArray = JSONArray()
            config.systemInstruction?.let { systemInstruction ->
                messagesArray.put(
                    JSONObject().apply {
                        put("role", "system")
                        put("content", systemInstruction)
                    }
                )
            }
            messages.forEach { message ->
                messagesArray.put(
                    JSONObject().apply {
                        put("role", toOpenAiRole(message.role))
                        put("content", message.parts.joinToString("") { it.text })
                    }
                )
            }
            return JSONObject().apply {
                put("model", config.model)
                put("messages", messagesArray)
                put("temperature", config.temperature.toDouble())
                put("top_p", config.topP.toDouble())
                put("max_tokens", config.maxTokens)
                if (config.jsonMode) {
                    put("response_format", JSONObject().put("type", "json_object"))
                }
            }.toString()
        }

        fun parseChatCompletionResponse(responseBody: String): String {
            val choices = JSONObject(responseBody).optJSONArray("choices")
            if (choices == null || choices.length() == 0) {
                throw BergetChatCompletionException("No choices in chat completion response")
            }
            return choices.getJSONObject(0).getJSONObject("message").getString("content")
        }

        private fun toOpenAiRole(role: String): String = if (role == "model") "assistant" else role
    }
}

class BergetChatCompletionException(message: String) : Exception(message)
