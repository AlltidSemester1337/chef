package com.formulae.chef

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import com.formulae.chef.feature.chat.AskChefVariantViewModel
import com.google.firebase.Firebase
import com.google.firebase.vertexai.type.content
import com.google.firebase.vertexai.type.generationConfig
import com.google.firebase.vertexai.vertexAI

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

val AskChefVariantViewModelFactory = object : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
        val recipeAdjustModel = Firebase.vertexAI.generativeModel(
            modelName = ModelConfig.CHAT_MODEL,
            generationConfig = generationConfig {
                temperature = 0.7f
                maxOutputTokens = 4096
                topP = 0.95f
            },
            systemInstruction = content { text(RECIPE_ADJUST_SYSTEM_INSTRUCTIONS) }
        )

        val jsonGenerativeModel = Firebase.vertexAI.generativeModel(
            modelName = ModelConfig.LITE_MODEL,
            generationConfig = generationConfig {
                temperature = 0.2f
                maxOutputTokens = 8192
                topP = 0.95f
                responseMimeType = "application/json"
            },
            systemInstruction = content { text(DERIVE_RECIPE_JSON_SYSTEM_INSTRUCTIONS) }
        )

        @Suppress("UNCHECKED_CAST")
        return AskChefVariantViewModel(recipeAdjustModel, jsonGenerativeModel) as T
    }
}
