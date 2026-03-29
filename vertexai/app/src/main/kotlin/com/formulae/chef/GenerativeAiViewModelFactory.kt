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
import com.google.firebase.Firebase
import com.google.firebase.vertexai.type.ResponseModality
import com.google.firebase.vertexai.type.content
import com.google.firebase.vertexai.type.generationConfig
import com.google.firebase.vertexai.vertexAI

private const val EXTRACT_PREFERENCES_SYSTEM_INSTRUCTIONS =
    """You are a preference detector for a cooking assistant. Given a user message, detect if the user
explicitly states a personal food preference (dietary restriction, measurement system preference,
cuisine preference, or similar). Return JSON matching this exact schema:
{"detected": true, "updatedSummary": "complete merged summary of all stated preferences"}
If nothing preference-related is detected, return:
{"detected": false, "updatedSummary": ""}
The updatedSummary should incorporate any previously known preferences provided in context."""

private const val COMPACT_HISTORY_SYSTEM_INSTRUCTIONS =
    """You are a memory compactor for a cooking assistant. Given a chat transcript and an existing
preference summary, produce a single updated prose summary capturing all stated user preferences,
dietary restrictions, and recurring cooking context. Be concise but complete. Return only the
summary text, no JSON wrapping."""

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
      "tipsAndTricks": "string",
      "tags": ["string"]
    }
  ]
}
Use metric units for ingredient quantities. Omit any fields that are not applicable or cannot be determined from the text.
For the tags field, generate a flat list of descriptive tags covering: main ingredient (e.g. 'chicken', 'lamb', 'vegetarian', 'vegan', 'fish'), cuisine (e.g. 'korean', 'italian', 'indonesian', 'mexican', 'indian'), effort level (e.g. 'under 30 minutes', 'under 1 hour', '1-2 hours', 'slow cook'), and season or occasion where applicable (e.g. 'christmas', 'easter', 'summer', 'weeknight'). Include only tags that genuinely apply."""

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

        val textConfig = generationConfig {
            temperature = 0.2f
            maxOutputTokens = 8192
            topP = 0.95f
        }

        val application = extras[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as Application

        val systemPrompt = application.assets
            .open("chat_system_prompt.txt")
            .bufferedReader()
            .use { it.readText() }

        return with(viewModelClass) {
            when {
                isAssignableFrom(ChatViewModel::class.java) -> {
                    // Initialize a GenerativeModel with the `gemini-2.5-flash` AI model for chat
                    val chatGenerativeModel = Firebase.vertexAI.generativeModel(
                        modelName = "gemini-2.5-flash",
                        generationConfig = chatConfig,
                        systemInstruction = content { text(systemPrompt) }
                    )

                    val jsonGenerativeModel = Firebase.vertexAI.generativeModel(
                        modelName = "gemini-2.5-flash-lite",
                        generationConfig = jsonConfig,
                        systemInstruction = content { text(DERIVE_RECIPE_JSON_SYSTEM_INSTRUCTIONS) }
                    )

                    val imageConfig = generationConfig {
                        responseModalities = listOf(ResponseModality.TEXT, ResponseModality.IMAGE)
                    }

                    val imageGenerativeModel = Firebase.vertexAI.generativeModel(
                        modelName = "gemini-2.5-flash-image",
                        generationConfig = imageConfig
                    )

                    val preferencesGenerativeModel = Firebase.vertexAI.generativeModel(
                        modelName = "gemini-2.5-flash-lite",
                        generationConfig = jsonConfig,
                        systemInstruction = content { text(EXTRACT_PREFERENCES_SYSTEM_INSTRUCTIONS) }
                    )

                    val compactionGenerativeModel = Firebase.vertexAI.generativeModel(
                        modelName = "gemini-2.5-flash-lite",
                        generationConfig = textConfig,
                        systemInstruction = content { text(COMPACT_HISTORY_SYSTEM_INSTRUCTIONS) }
                    )

                    val userSessionService = UserSessionServiceFirebaseImpl()
                    val applicationScope = (application as ChefApplication).applicationScope

                    ChatViewModel(
                        chatGenerativeModel = chatGenerativeModel,
                        jsonGenerativeModel = jsonGenerativeModel,
                        preferencesGenerativeModel = preferencesGenerativeModel,
                        compactionGenerativeModel = compactionGenerativeModel,
                        imageGenerativeModel = imageGenerativeModel,
                        application = application,
                        userSessionService = userSessionService,
                        applicationScope = applicationScope
                    )
                }

                else ->
                    throw IllegalArgumentException("Unknown ViewModel class: ${viewModelClass.name}")
            }
        } as T
    }
}
