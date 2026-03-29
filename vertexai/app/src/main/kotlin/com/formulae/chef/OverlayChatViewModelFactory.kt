package com.formulae.chef

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import com.formulae.chef.feature.chat.OverlayChatViewModel
import com.google.firebase.Firebase
import com.google.firebase.vertexai.type.content
import com.google.firebase.vertexai.type.generationConfig
import com.google.firebase.vertexai.vertexAI

private const val OVERLAY_DEFAULT_SYSTEM_INSTRUCTIONS =
    """You are Chef, a friendly AI cooking assistant. Answer questions about cooking, recipes,
and food concisely and helpfully."""

private const val OVERLAY_RECIPE_CONTEXT_SYSTEM_INSTRUCTIONS =
    """You are Chef, a cooking assistant helping the user with a specific recipe they are currently viewing.
Focus on Q&A assistance: answer questions about steps, ingredient substitutions, timing, tips, and serving suggestions.
Do not proactively suggest new recipes unless specifically asked."""

val OverlayChatViewModelFactory = object : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
        val chatConfig = generationConfig {
            temperature = 1.0f
            maxOutputTokens = 2048
            topP = 0.95f
        }

        val defaultModel = Firebase.vertexAI.generativeModel(
            modelName = "gemini-2.5-flash",
            generationConfig = chatConfig,
            systemInstruction = content { text(OVERLAY_DEFAULT_SYSTEM_INSTRUCTIONS) }
        )

        val recipeContextModel = Firebase.vertexAI.generativeModel(
            modelName = "gemini-2.5-flash",
            generationConfig = chatConfig,
            systemInstruction = content { text(OVERLAY_RECIPE_CONTEXT_SYSTEM_INSTRUCTIONS) }
        )

        @Suppress("UNCHECKED_CAST")
        return OverlayChatViewModel(defaultModel, recipeContextModel) as T
    }
}
