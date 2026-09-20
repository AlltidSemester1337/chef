package com.formulae.chef

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import com.formulae.chef.feature.home.HomeViewModel
import com.formulae.chef.services.authentication.UserSessionService
import com.google.firebase.Firebase
import com.google.firebase.vertexai.type.content
import com.google.firebase.vertexai.type.generationConfig
import com.google.firebase.vertexai.vertexAI

private const val COOKING_RESOURCES_SYSTEM_INSTRUCTIONS =
    """You are a cooking resource recommender. Given a user locale and their dietary/cooking preference
summary, return a JSON array of 5-8 recommended cooking websites, YouTube channels, or blogs.
Each item must have: title (string), url (valid full URL starting with https://), type (one of
"website", "youtube", "blog", "instagram"), description (one sentence about the resource, in the
user's locale language if non-English).
Prioritize resources in the user's locale language and relevant to their stated preferences.
Return only the JSON array with no wrapper object, markdown, or explanation."""

class HomeViewModelFactory(
    private val userSessionService: UserSessionService
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
        val jsonConfig = generationConfig {
            temperature = 0.4f
            maxOutputTokens = 2048
            topP = 0.95f
            responseMimeType = "application/json"
        }
        val model = Firebase.vertexAI.generativeModel(
            modelName = "gemini-2.5-flash-lite",
            generationConfig = jsonConfig,
            systemInstruction = content { text(COOKING_RESOURCES_SYSTEM_INSTRUCTIONS) }
        )
        return HomeViewModel(
            userSessionService = userSessionService,
            resourcesGenerativeModel = model
        ) as T
    }
}
