package com.formulae.chef.rotw.service

import com.google.auth.oauth2.GoogleCredentials
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.headers
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import java.util.Base64
import kotlinx.coroutines.delay
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.slf4j.LoggerFactory

private val logger = LoggerFactory.getLogger(VeoClient::class.java)

private const val POLL_INTERVAL_MS = 30_000L
private const val MAX_POLL_ATTEMPTS = 20

/**
 * Calls the Vertex AI Veo 2 API to generate a short food video.
 *
 * Endpoint: predictLongRunning → returns an LRO name → poll until done.
 * Video is returned as base64-encoded MP4 bytes.
 *
 * Required env vars: GCP_PROJECT_ID, GCP_LOCATION (default: us-central1)
 */
class VeoClient(
    private val projectId: String = System.getenv("GCP_PROJECT_ID")
        ?: error("GCP_PROJECT_ID env var not set"),
    private val location: String = System.getenv("GCP_LOCATION") ?: "us-central1",
    private val modelId: String = "veo-002"
) {
    private val httpClient = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true })
        }
    }

    private fun accessToken(): String {
        val credentials = GoogleCredentials.getApplicationDefault()
            .createScoped(listOf("https://www.googleapis.com/auth/cloud-platform"))
        credentials.refreshIfExpired()
        return credentials.accessToken.tokenValue
    }

    suspend fun generateVideo(prompt: String, durationSeconds: Int = 15): ByteArray {
        val token = accessToken()
        val baseUrl = "https://$location-aiplatform.googleapis.com/v1"
        val predictUrl =
            "$baseUrl/projects/$projectId/locations/$location/publishers/google/models/$modelId:predictLongRunning"

        // JsonPrimitive.toString() produces a properly JSON-escaped quoted string
        val escapedPrompt = kotlinx.serialization.json.JsonPrimitive(prompt).toString()
        val requestBody = """
            {
              "instances": [{ "prompt": $escapedPrompt }],
              "parameters": {
                "durationSeconds": $durationSeconds,
                "aspectRatio": "16:9",
                "sampleCount": 1,
                "enhancePrompt": true
              }
            }
        """.trimIndent()

        logger.info("Submitting Veo 2 generation request for prompt: ${prompt.take(80)}...")

        val lroResponse: JsonObject = httpClient.post(predictUrl) {
            contentType(ContentType.Application.Json)
            headers { append("Authorization", "Bearer $token") }
            setBody(requestBody)
        }.body()

        val operationName = lroResponse["name"]?.jsonPrimitive?.content
            ?: error("No operation name returned from Veo 2 API")

        logger.info("Veo 2 LRO started: $operationName")

        return pollForCompletion(operationName, "$baseUrl/$operationName", token)
    }

    private suspend fun pollForCompletion(
        operationName: String,
        operationUrl: String,
        initialToken: String
    ): ByteArray {
        repeat(MAX_POLL_ATTEMPTS) { attempt ->
            delay(POLL_INTERVAL_MS)
            val token = accessToken()
            val status: JsonObject = httpClient.get(operationUrl) {
                headers { append("Authorization", "Bearer $token") }
            }.body()

            val done = status["done"]?.jsonPrimitive?.boolean ?: false
            if (done) {
                logger.info("Veo 2 generation complete after ${attempt + 1} polls")
                val videoBase64 = status["response"]
                    ?.jsonObject?.get("videos")
                    ?.jsonArray?.firstOrNull()
                    ?.jsonObject?.get("bytesBase64Encoded")
                    ?.jsonPrimitive?.content
                    ?: error("No video bytes in Veo 2 response")
                return Base64.getDecoder().decode(videoBase64)
            }

            val error = status["error"]
            if (error != null) {
                error("Veo 2 operation failed: $error")
            }

            logger.info("Veo 2 still processing (attempt ${attempt + 1}/$MAX_POLL_ATTEMPTS)...")
        }
        error("Veo 2 generation timed out after $MAX_POLL_ATTEMPTS poll attempts")
    }
}
