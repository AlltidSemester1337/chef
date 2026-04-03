package com.formulae.chef.services.voice

import android.util.Base64
import android.util.Log
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

class GcpTextToSpeechService(private val apiKey: String) {

    suspend fun synthesize(text: String): ByteArray = withContext(Dispatchers.IO) {
        val url = URL("https://texttospeech.googleapis.com/v1/text:synthesize?key=$apiKey")
        val connection = url.openConnection() as HttpURLConnection
        try {
            connection.requestMethod = "POST"
            connection.setRequestProperty("Content-Type", "application/json")
            connection.doOutput = true
            connection.connectTimeout = 15_000
            connection.readTimeout = 30_000

            val body = JSONObject().apply {
                put("input", JSONObject().put("text", text))
                put(
                    "voice",
                    JSONObject().apply {
                        put("languageCode", "en-US")
                        put("name", "en-US-Chirp3-HD-Aoede")
                    }
                )
                put("audioConfig", JSONObject().put("audioEncoding", "MP3"))
            }.toString()

            OutputStreamWriter(connection.outputStream).use { it.write(body) }

            val responseCode = connection.responseCode
            if (responseCode != HttpURLConnection.HTTP_OK) {
                val error = connection.errorStream?.bufferedReader()?.readText() ?: "Unknown error"
                Log.e("GcpTextToSpeechService", "HTTP $responseCode: $error")
                throw GcpTtsException("TTS request failed with HTTP $responseCode: $error")
            }

            val responseBody = connection.inputStream.bufferedReader().readText()
            val audioContent = JSONObject(responseBody).getString("audioContent")
            Base64.decode(audioContent, Base64.DEFAULT)
        } finally {
            connection.disconnect()
        }
    }
}

class GcpTtsException(message: String) : Exception(message)
