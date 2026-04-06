package com.formulae.chef.feature.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.formulae.chef.feature.model.Recipe
import com.formulae.chef.feature.model.Recipes
import com.google.firebase.vertexai.GenerativeModel
import com.google.firebase.vertexai.type.content
import com.google.gson.Gson
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AskChefVariantViewModel(
    private val recipeAdjustModel: GenerativeModel,
    private val jsonGenerativeModel: GenerativeModel
) : ViewModel() {

    sealed class State {
        object Idle : State()
        object Loading : State()
        data class Success(val recipe: Recipe) : State()
        object Error : State()
    }

    private val _state = MutableStateFlow<State>(State.Idle)
    val state: StateFlow<State> = _state.asStateFlow()

    fun adjustRecipe(recipe: Recipe, userRequest: String) {
        viewModelScope.launch {
            _state.value = State.Loading
            try {
                val prompt = buildAdjustPrompt(recipe, userRequest)
                val adjustedText = recipeAdjustModel
                    .generateContent(content { text(prompt) })
                    .text ?: throw Exception("Empty response from adjust model")

                val jsonText = jsonGenerativeModel
                    .generateContent(content { text(adjustedText) })
                    .text ?: throw Exception("Empty response from JSON model")

                val recipes = Gson().fromJson(jsonText, Recipes::class.java).recipes
                if (recipes.isEmpty()) throw Exception("No recipe in JSON response")

                _state.value = State.Success(recipes.first())
            } catch (e: Exception) {
                _state.value = State.Error
            }
        }
    }

    fun reset() {
        _state.value = State.Idle
    }

    companion object {
        fun buildAdjustPrompt(recipe: Recipe, userRequest: String): String = buildString {
            append(OverlayChatViewModel.buildRecipeContextText(recipe))
            append("\n\nUser request: $userRequest\n\n")
            append(
                "Please apply the requested modification to this recipe and return the complete " +
                    "updated recipe, including title, summary, all ingredients with quantities and " +
                    "units, numbered instructions, difficulty, timing, and any tips."
            )
        }
    }
}
