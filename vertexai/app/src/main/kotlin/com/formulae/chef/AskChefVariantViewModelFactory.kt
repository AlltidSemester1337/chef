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
      "ingredients": [{"name": "string", "quantity": "numeric string only, e.g. '500', '2', '0.5', '1/2' — never include the unit here", "unit": "string"}],
      "difficulty": "EASY | MEDIUM | HARD",
      "instructions": ["string, one step per element"],
      "tipsAndTricks": "string",
      "tags": ["string"]
    }
  ]
}
Use metric units for ingredient quantities. Omit any fields that are not applicable or cannot be determined from the text.
For the tags field, generate a flat list of descriptive tags covering: main ingredient (e.g. 'chicken', 'lamb', 'vegetarian', 'vegan', 'fish'), cuisine (e.g. 'korean', 'italian', 'indonesian', 'mexican', 'indian'), effort level (e.g. 'under 30 minutes', 'under 1 hour', '1-2 hours', 'slow cook'), and season or occasion where applicable (e.g. 'christmas', 'easter', 'summer', 'weeknight'). Include only tags that genuinely apply. All tags must be lowercase."""

val AskChefVariantViewModelFactory = object : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
        val recipeAdjustModel = Firebase.vertexAI.generativeModel(
            modelName = "gemini-2.5-flash",
            generationConfig = generationConfig {
                temperature = 0.7f
                maxOutputTokens = 4096
                topP = 0.95f
            },
            systemInstruction = content { text(RECIPE_ADJUST_SYSTEM_INSTRUCTIONS) }
        )

        val jsonGenerativeModel = Firebase.vertexAI.generativeModel(
            modelName = "gemini-2.5-flash-lite",
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
