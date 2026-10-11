package com.formulae.chef.services.backend

import android.util.Log
import com.formulae.chef.services.persistence.Content
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

/** Generates text for a [purpose]. Seam for ViewModel tests. */
fun interface AiCompletionService {
    /** @throws BackendException for error responses, `IOException` for network failures. */
    suspend fun complete(purpose: AiPurpose, messages: List<Content>): String
}

/** Turns a sentence into MP3 audio. Seam for UI-layer code and tests. */
fun interface TextToSpeechService {
    /** @throws BackendException for error responses, `IOException` for network failures. */
    suspend fun synthesize(text: String): ByteArray
}

/** HTTP client for chef-backend, which proxies the AI and TTS calls. */
class ChefBackendClient(
    private val baseUrl: String,
    private val tokenProvider: BackendTokenProvider = FirebaseBackendTokenProvider()
) : AiCompletionService, TextToSpeechService {

    override suspend fun complete(purpose: AiPurpose, messages: List<Content>): String {
        val body = CompletionRequests.buildRequestBody(purpose, messages)
        val response = post("/v1/ai/complete", body, AI_READ_TIMEOUT_MS)
        return CompletionRequests.parseResponse(response.decodeToString())
    }

    override suspend fun synthesize(text: String): ByteArray {
        val body = JSONObject().put("text", text).toString()
        return post("/v1/tts/synthesize", body, TTS_READ_TIMEOUT_MS)
    }

    // Tokens are cached by Firebase. If the backend still rejects them (e.g. revoked or a fresh
    // custom claim), retry once with newly minted tokens before giving up.
    private suspend fun post(path: String, jsonBody: String, readTimeoutMs: Int): ByteArray =
        try {
            postOnce(path, jsonBody, readTimeoutMs, forceRefresh = false)
        } catch (e: BackendException.Unauthorized) {
            postOnce(path, jsonBody, readTimeoutMs, forceRefresh = true)
        }

    private suspend fun postOnce(
        path: String,
        jsonBody: String,
        readTimeoutMs: Int,
        forceRefresh: Boolean
    ): ByteArray {
        val headers = tokenProvider.authHeaders(forceRefresh)
        return withContext(Dispatchers.IO) {
            val connection = URL(baseUrl.trimEnd('/') + path).openConnection() as HttpURLConnection
            try {
                connection.requestMethod = "POST"
                connection.setRequestProperty("Content-Type", "application/json; charset=utf-8")
                headers.forEach { (name, value) -> connection.setRequestProperty(name, value) }
                connection.doOutput = true
                connection.connectTimeout = CONNECT_TIMEOUT_MS
                connection.readTimeout = readTimeoutMs
                connection.outputStream.use { it.write(jsonBody.toByteArray(Charsets.UTF_8)) }

                val status = connection.responseCode
                if (status != HttpURLConnection.HTTP_OK) {
                    val error = connection.errorStream?.use { it.readBytes().decodeToString() }
                    val exception = BackendException.fromResponse(status, error)
                    // Not the raw body: a 422 body echoes the request, i.e. user content.
                    Log.e(TAG, "POST $path failed: ${exception.message}")
                    throw exception
                }
                connection.inputStream.use { it.readBytes() }
            } finally {
                connection.disconnect()
            }
        }
    }

    private companion object {
        const val TAG = "ChefBackendClient"
        const val CONNECT_TIMEOUT_MS = 15_000

        // Cloud Run may cold-start, and long chat answers take a while to generate.
        const val AI_READ_TIMEOUT_MS = 90_000
        const val TTS_READ_TIMEOUT_MS = 30_000
    }
}
