/*
 * Copyright 2023 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.formulae.chef

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import com.formulae.chef.feature.chat.ChatViewModel
import com.google.auth.oauth2.GoogleCredentials
import com.google.cloud.aiplatform.v1.PredictionServiceClient
import com.google.cloud.aiplatform.v1.PredictionServiceSettings
import com.google.firebase.Firebase
import com.google.firebase.vertexai.type.content
import com.google.firebase.vertexai.type.generationConfig
import com.google.firebase.vertexai.vertexAI
import org.json.JSONObject
import java.io.InputStream
import java.lang.String
import java.nio.charset.Charset


val GenerativeViewModelFactory = object : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(
        viewModelClass: Class<T>,
        extras: CreationExtras
    ): T {
        val config = generationConfig {
            temperature = 2f
            maxOutputTokens = 8192
            topP = 0.95f
        }

        val application = extras[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as Application

        return with(viewModelClass) {
            when {
                isAssignableFrom(ChatViewModel::class.java) -> {
                    // Initialize a GenerativeModel with the `gemini-flash` AI model for chat
                    val generativeModel = Firebase.vertexAI.generativeModel(
                        modelName = "gemini-1.5-flash-002",
                        generationConfig = config,
                        systemInstruction = content { text("You are a personal chef / cooking assistant to help with coming up for new ideas on recipes. Use https://www.honestgreens.com/en/menu as inspiration for the whole foods, healthy, simple and savory cooking / recipe style. Please use metric units and centiliters / decilitres for liquid measurements and state the nutritional values for each recipe.") }
                    )


                    val settingsStream: InputStream =
                        application.applicationContext.assets.open("gcp.json")
                    val configString = settingsStream.bufferedReader(Charset.defaultCharset()).use { it.readText() }
                    val location = JSONObject(configString).getString("location")

                    val credentialsStream: InputStream =
                        application.applicationContext.assets.open("imagen-google-services.json")
                    val credentials = GoogleCredentials.fromStream(credentialsStream)
                        .createScoped(listOf("https://www.googleapis.com/auth/cloud-platform"))

                    val endpoint = String.format("$location-aiplatform.googleapis.com:443")

                    val predictionServiceSettings: PredictionServiceSettings =
                        PredictionServiceSettings.newBuilder()
                            .setCredentialsProvider { credentials }
                            .setEndpoint(endpoint)
                            .build()

                    val predictionServiceClient = PredictionServiceClient.create(predictionServiceSettings)

                    ChatViewModel(
                        generativeModel = generativeModel,
                        predictionServiceClient = predictionServiceClient,
                        location = location,
                        application = application
                    )
                }

                else ->
                    throw IllegalArgumentException("Unknown ViewModel class: ${viewModelClass.name}")
            }
        } as T
    }
}
