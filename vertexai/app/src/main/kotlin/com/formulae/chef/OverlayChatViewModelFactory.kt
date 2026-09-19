package com.formulae.chef

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import com.formulae.chef.feature.chat.OverlayChatViewModel
import com.formulae.chef.services.ai.BERGET_MISTRAL_SMALL_3_2
import com.formulae.chef.services.ai.BergetChatCompletionService
import com.formulae.chef.services.ai.BergetModelConfig

private const val OVERLAY_DEFAULT_SYSTEM_INSTRUCTIONS =
    """You are Chef, a friendly AI cooking assistant. Answer questions about cooking, recipes,
and food concisely and helpfully."""

private const val OVERLAY_RECIPE_CONTEXT_SYSTEM_INSTRUCTIONS =
    """You are Chef, a cooking assistant helping the user with a specific recipe they are currently viewing.
Focus on Q&A assistance: answer questions about steps, ingredient substitutions, timing, tips, and serving suggestions.
Do not proactively suggest new recipes unless specifically asked."""

val OverlayChatViewModelFactory = object : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
        val chatCompletionService = BergetChatCompletionService(BuildConfig.bergetApiKey)

        val defaultConfig = BergetModelConfig(
            model = BERGET_MISTRAL_SMALL_3_2,
            systemInstruction = OVERLAY_DEFAULT_SYSTEM_INSTRUCTIONS,
            temperature = 1.0f,
            topP = 0.95f,
            maxTokens = 2048
        )

        val recipeContextConfig = BergetModelConfig(
            model = BERGET_MISTRAL_SMALL_3_2,
            systemInstruction = OVERLAY_RECIPE_CONTEXT_SYSTEM_INSTRUCTIONS,
            temperature = 1.0f,
            topP = 0.95f,
            maxTokens = 2048
        )

        @Suppress("UNCHECKED_CAST")
        return OverlayChatViewModel(chatCompletionService, defaultConfig, recipeContextConfig) as T
    }
}
