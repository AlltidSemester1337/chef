package com.formulae.chef.services.backend

import android.util.Log
import com.formulae.chef.feature.model.CookingResource
import com.formulae.chef.services.persistence.Content
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

/** Generates and stores a picture of a recipe. Seam for ViewModel tests. */
fun interface RecipeImageService {
    /**
     * @param recipeText plain-text recipe (title, summary, ingredients, steps).
     * @return public HTTPS download URL of the stored image.
     * @throws BackendException for error responses, `IOException` for network failures.
     */
    suspend fun generateRecipeImage(recipeText: String): String
}

/** Recommends cooking websites, channels and blogs for the signed-in user. Seam for ViewModel tests. */
fun interface CookingResourcesService {
    /**
     * @param languageTag BCP 47 tag, see [CookingResourcesRequests.languageTag].
     * @return validated resources, possibly empty.
     * @throws BackendException for error responses, `IOException` for network failures.
     */
    suspend fun fetchCookingResources(languageTag: String): List<CookingResource>
}

/** HTTP client for chef-backend, which proxies the AI, image and TTS calls. */
class ChefBackendClient(
    private val baseUrl: String,
    private val tokenProvider: BackendTokenProvider = FirebaseBackendTokenProvider(),
    private val transport: BackendTransport = HttpUrlConnectionTransport()
) : AiCompletionService, TextToSpeechService, RecipeImageService, CookingResourcesService {

    override suspend fun complete(purpose: AiPurpose, messages: List<Content>): String {
        val body = CompletionRequests.buildRequestBody(purpose, messages)
        val response = post("/v1/ai/complete", body, AI_READ_TIMEOUT_MS)
        return CompletionRequests.parseResponse(response.decodeToString())
    }

    override suspend fun synthesize(text: String): ByteArray {
        val body = JSONObject().put("text", text).toString()
        return post("/v1/tts/synthesize", body, TTS_READ_TIMEOUT_MS)
    }

    override suspend fun generateRecipeImage(recipeText: String): String {
        val body = RecipeImageRequests.buildRequestBody(recipeText)
        val response = post("/v1/images/recipe", body, IMAGE_READ_TIMEOUT_MS)
        return RecipeImageRequests.parseResponse(response.decodeToString())
    }

    override suspend fun fetchCookingResources(languageTag: String): List<CookingResource> {
        val body = CookingResourcesRequests.buildRequestBody(languageTag)
        val response = post("/v1/cooking-resources", body, AI_READ_TIMEOUT_MS)
        return CookingResourcesRequests.parseResponse(response.decodeToString())
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
        val response = transport.post(baseUrl.trimEnd('/') + path, headers, jsonBody, readTimeoutMs)
        if (response.status != HTTP_OK) {
            val exception = BackendException.fromResponse(response.status, response.body.decodeToString())
            // Not the raw body: a 422 body echoes the request, i.e. user content.
            Log.e(TAG, "POST $path failed: ${exception.message}")
            throw exception
        }
        return response.body
    }

    private companion object {
        const val TAG = "ChefBackendClient"
        const val HTTP_OK = 200

        // Cloud Run may cold-start, and long chat answers take a while to generate.
        const val AI_READ_TIMEOUT_MS = 90_000
        const val TTS_READ_TIMEOUT_MS = 30_000

        // Image generation, JPEG conversion and the upload all happen before the server answers.
        const val IMAGE_READ_TIMEOUT_MS = 120_000
    }
}
