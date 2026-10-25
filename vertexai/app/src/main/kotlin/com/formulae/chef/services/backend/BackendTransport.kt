package com.formulae.chef.services.backend

import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Status code and body of an HTTP response. For error statuses, [body] is the error body. */
class BackendResponse(val status: Int, val body: ByteArray)

/** Sends one HTTP POST. Seam so the client's URLs, bodies and error handling are unit tested. */
fun interface BackendTransport {
    /** @throws java.io.IOException for network failures. */
    suspend fun post(url: String, headers: Map<String, String>, jsonBody: String, readTimeoutMs: Int): BackendResponse
}

class HttpUrlConnectionTransport : BackendTransport {

    override suspend fun post(
        url: String,
        headers: Map<String, String>,
        jsonBody: String,
        readTimeoutMs: Int
    ): BackendResponse = withContext(Dispatchers.IO) {
        val connection = URL(url).openConnection() as HttpURLConnection
        try {
            connection.requestMethod = "POST"
            connection.setRequestProperty("Content-Type", "application/json; charset=utf-8")
            headers.forEach { (name, value) -> connection.setRequestProperty(name, value) }
            connection.doOutput = true
            connection.connectTimeout = CONNECT_TIMEOUT_MS
            connection.readTimeout = readTimeoutMs
            connection.outputStream.use { it.write(jsonBody.toByteArray(Charsets.UTF_8)) }

            val status = connection.responseCode
            val stream = if (status == HttpURLConnection.HTTP_OK) connection.inputStream else connection.errorStream
            BackendResponse(status, stream?.use { it.readBytes() } ?: ByteArray(0))
        } finally {
            connection.disconnect()
        }
    }

    private companion object {
        const val CONNECT_TIMEOUT_MS = 15_000
    }
}
