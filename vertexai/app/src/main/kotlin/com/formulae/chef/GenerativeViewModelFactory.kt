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
import com.formulae.chef.services.authentication.UserSessionService
import com.formulae.chef.services.backend.ChefBackendClient
import com.google.firebase.Firebase
import com.google.firebase.vertexai.type.ResponseModality
import com.google.firebase.vertexai.type.generationConfig
import com.google.firebase.vertexai.vertexAI

class GenerativeViewModelFactory(
    private val userSessionService: UserSessionService
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(
        viewModelClass: Class<T>,
        extras: CreationExtras
    ): T {
        val application = extras[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as Application

        return with(viewModelClass) {
            when {
                isAssignableFrom(ChatViewModel::class.java) -> {
                    // Text generation goes through chef-backend, which picks prompts and models per purpose.
                    // Image generation stays on Gemini via Firebase Vertex AI (protected by App Check).
                    val imageConfig = generationConfig {
                        responseModalities = listOf(ResponseModality.TEXT, ResponseModality.IMAGE)
                    }

                    val imageGenerativeModel = Firebase.vertexAI.generativeModel(
                        modelName = "gemini-2.5-flash-image",
                        generationConfig = imageConfig
                    )

                    val applicationScope = (application as ChefApplication).applicationScope

                    ChatViewModel(
                        aiService = ChefBackendClient(BuildConfig.chefBackendUrl),
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
