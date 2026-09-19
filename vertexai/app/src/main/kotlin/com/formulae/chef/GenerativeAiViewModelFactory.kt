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
import com.formulae.chef.services.ai.BERGET_MISTRAL_SMALL_3_2
import com.formulae.chef.services.ai.BergetChatCompletionService
import com.formulae.chef.services.ai.BergetModelConfig
import com.formulae.chef.services.authentication.UserSessionServiceFirebaseImpl
import com.google.firebase.Firebase
import com.google.firebase.vertexai.type.ResponseModality
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

val GenerativeViewModelFactory = object : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(
        viewModelClass: Class<T>,
        extras: CreationExtras
    ): T {
        val application = extras[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as Application

        val systemPrompt = application.assets
            .open("chat_system_prompt.txt")
            .bufferedReader()
            .use { it.readText() }

        return with(viewModelClass) {
            when {
                isAssignableFrom(ChatViewModel::class.java) -> {
                    val chatCompletionService = BergetChatCompletionService(BuildConfig.bergetApiKey)

                    val chatConfig = BergetModelConfig(
                        model = BERGET_MISTRAL_SMALL_3_2,
                        systemInstruction = systemPrompt,
                        temperature = 1.0f,
                        topP = 0.95f,
                        maxTokens = 8192
                    )

                    val jsonConfig = BergetModelConfig(
                        model = BERGET_MISTRAL_SMALL_3_2,
                        systemInstruction = DERIVE_RECIPE_JSON_SYSTEM_INSTRUCTIONS,
                        temperature = 0.2f,
                        topP = 0.95f,
                        maxTokens = 8192,
                        jsonMode = true
                    )

                    val preferencesConfig = BergetModelConfig(
                        model = BERGET_MISTRAL_SMALL_3_2,
                        systemInstruction = EXTRACT_PREFERENCES_SYSTEM_INSTRUCTIONS,
                        temperature = 0.2f,
                        topP = 0.95f,
                        maxTokens = 8192,
                        jsonMode = true
                    )

                    val compactionConfig = BergetModelConfig(
                        model = BERGET_MISTRAL_SMALL_3_2,
                        systemInstruction = COMPACT_HISTORY_SYSTEM_INSTRUCTIONS,
                        temperature = 0.2f,
                        topP = 0.95f,
                        maxTokens = 8192
                    )

                    // Image generation stays on Gemini via Firebase Vertex AI (out of scope for CHE-35).
                    val imageConfig = generationConfig {
                        responseModalities = listOf(ResponseModality.TEXT, ResponseModality.IMAGE)
                    }

                    val imageGenerativeModel = Firebase.vertexAI.generativeModel(
                        modelName = "gemini-2.5-flash-image",
                        generationConfig = imageConfig
                    )

                    val userSessionService = UserSessionServiceFirebaseImpl()
                    val applicationScope = (application as ChefApplication).applicationScope

                    ChatViewModel(
                        chatCompletionService = chatCompletionService,
                        chatConfig = chatConfig,
                        jsonConfig = jsonConfig,
                        preferencesConfig = preferencesConfig,
                        compactionConfig = compactionConfig,
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
