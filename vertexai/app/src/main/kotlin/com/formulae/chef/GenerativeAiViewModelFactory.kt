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
import com.formulae.chef.services.authentication.UserSessionServiceFirebaseImpl
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


private const val DERIVE_RECIPE_JSON_SYSTEM_INSTRUCTIONS =
    """Extract recipes from the provided text and return them as JSON matching this schema:
{
  "recipes": [
    {
      "title": "string",
      "summary": "string, brief description of the dish",
      "servings": "string, e.g. '4 servings'",
      "prepTime": "string, e.g. '25 minutes'",
      "cookingTime": "string, e.g. '35 minutes'",
      "nutrientsPerServing": [{"name": "string", "quantity": "string", "unit": "string"}],
      "ingredients": [{"name": "string", "quantity": "string", "unit": "string"}],
      "difficulty": "EASY | MEDIUM | HARD",
      "instructions": ["string, one step per element"],
      "tipsAndTricks": "string"
    }
  ]
}
Use metric units for ingredient quantities. Omit any fields that are not applicable or cannot be determined from the text."""

val GenerativeViewModelFactory = object : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(
        viewModelClass: Class<T>,
        extras: CreationExtras
    ): T {
        val chatConfig = generationConfig {
            temperature = 1.0f
            maxOutputTokens = 8192
            topP = 0.95f
        }

        val jsonConfig = generationConfig {
            temperature = 0.2f
            maxOutputTokens = 8192
            topP = 0.95f
            responseMimeType = "application/json"
        }

        val application = extras[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as Application

        return with(viewModelClass) {
            when {
                isAssignableFrom(ChatViewModel::class.java) -> {
                    // Initialize a GenerativeModel with the `gemini-2.5-flash` AI model for chat
                    val chatGenerativeModel = Firebase.vertexAI.generativeModel(
                        modelName = "gemini-2.5-flash",
                        generationConfig = chatConfig,
                        systemInstruction = content { text(BuildConfig.chefMainChatPromptTemplate) }
                    )

                    val jsonGenerativeModel = Firebase.vertexAI.generativeModel(
                        modelName = "gemini-2.5-flash-lite",
                        generationConfig = jsonConfig,
                        systemInstruction = content { text(DERIVE_RECIPE_JSON_SYSTEM_INSTRUCTIONS) }
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

                    val userSessionService = UserSessionServiceFirebaseImpl()

                    ChatViewModel(
                        chatGenerativeModel = chatGenerativeModel,
                        jsonGenerativeModel = jsonGenerativeModel,
                        predictionServiceClient = predictionServiceClient,
                        location = location,
                        application = application,
                        userSessionService = userSessionService
                    )
                }

                else ->
                    throw IllegalArgumentException("Unknown ViewModel class: ${viewModelClass.name}")
            }
        } as T
    }
}
