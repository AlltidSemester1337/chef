package com.formulae.chef

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import com.formulae.chef.feature.chat.AskChefVariantViewModel
import com.formulae.chef.services.BetaQuotaService
import com.formulae.chef.services.ai.BERGET_MISTRAL_SMALL_3_2
import com.formulae.chef.services.ai.BergetChatCompletionService
import com.formulae.chef.services.ai.BergetModelConfig
import com.formulae.chef.services.authentication.UserSessionService

private const val RECIPE_ADJUST_SYSTEM_INSTRUCTIONS =
    """You are Chef, a cooking assistant. The user will provide an existing recipe and a modification request.
Apply the requested changes and return the complete modified recipe as detailed text, including:
- Title
- A brief summary of the dish
- All ingredients with precise quantities and units
- Numbered step-by-step instructions
- Difficulty level (Easy, Medium, or Hard)
- Prep time and cooking time
- Any relevant tips or tricks
Be precise about quantities and keep the style consistent with the original."""

class AskChefVariantViewModelFactory(
    private val userSessionService: UserSessionService
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
        val chatCompletionService = BergetChatCompletionService(BuildConfig.bergetApiKey)

        val recipeAdjustConfig = BergetModelConfig(
            model = BERGET_MISTRAL_SMALL_3_2,
            systemInstruction = RECIPE_ADJUST_SYSTEM_INSTRUCTIONS,
            temperature = 0.7f,
            topP = 0.95f,
            maxTokens = 4096
        )

        val jsonConfig = BergetModelConfig(
            model = BERGET_MISTRAL_SMALL_3_2,
            systemInstruction = DERIVE_RECIPE_JSON_SYSTEM_INSTRUCTIONS,
            temperature = 0.2f,
            topP = 0.95f,
            maxTokens = 8192,
            jsonMode = true
        )

        @Suppress("UNCHECKED_CAST")
        return AskChefVariantViewModel(
            chatCompletionService,
            recipeAdjustConfig,
            jsonConfig,
            BetaQuotaService(userSessionService)
        ) as T
    }
}
