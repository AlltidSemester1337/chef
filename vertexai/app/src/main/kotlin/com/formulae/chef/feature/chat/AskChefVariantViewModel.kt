package com.formulae.chef.feature.chat

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.formulae.chef.buildRecipeContextText
import com.formulae.chef.feature.model.Recipe
import com.formulae.chef.feature.model.Recipes
import com.formulae.chef.services.backend.AiCompletionService
import com.formulae.chef.services.backend.AiPurpose
import com.formulae.chef.services.backend.BackendException
import com.formulae.chef.services.persistence.Content
import com.formulae.chef.services.persistence.Part
import com.google.gson.Gson
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AskChefVariantViewModel(
    private val aiService: AiCompletionService
) : ViewModel() {
    sealed class State {
        object Idle : State()
        object Loading : State()
        data class Success(val recipe: Recipe) : State()
        object Error : State()
        object QuotaExceeded : State()
        object EmailVerificationRequired : State()
    }

    private val _state = MutableStateFlow<State>(State.Idle)
    val state: StateFlow<State> = _state.asStateFlow()

    /**
     * Adjusts [recipe] per [userRequest] (counts against the beta quota on the backend), then
     * extracts the adjusted recipe as JSON.
     */
    fun adjustRecipe(recipe: Recipe, userRequest: String) {
        viewModelScope.launch {
            _state.value = State.Loading
            _state.value = try {
                val adjustedText = complete(AiPurpose.RECIPE_ADJUST, buildAdjustPrompt(recipe, userRequest))
                val jsonText = complete(AiPurpose.DERIVE_RECIPE_JSON, adjustedText)
                val recipes = Gson().fromJson(jsonText, Recipes::class.java).recipes
                if (recipes.isEmpty()) throw IllegalStateException("No recipe in JSON response")
                State.Success(recipes.first())
            } catch (e: CancellationException) {
                throw e
            } catch (e: BackendException.QuotaExceeded) {
                State.QuotaExceeded
            } catch (e: BackendException.EmailVerificationRequired) {
                State.EmailVerificationRequired
            } catch (e: Exception) {
                Log.e(TAG, "adjustRecipe failed", e)
                State.Error
            }
        }
    }

    private suspend fun complete(purpose: AiPurpose, prompt: String): String =
        aiService.complete(purpose, listOf(Content(role = "user", parts = listOf(Part(prompt)))))

    fun reset() {
        _state.value = State.Idle
    }

    companion object {
        private const val TAG = "AskChefVariantViewModel"

        fun buildAdjustPrompt(recipe: Recipe, userRequest: String): String = buildString {
            append(buildRecipeContextText(recipe))
            append("\n\nUser request: $userRequest\n\n")
            append(
                "Please apply the requested modification to this recipe and return the complete " +
                    "updated recipe, including title, summary, all ingredients with quantities and " +
                    "units, numbered instructions, difficulty, timing, and any tips."
            )
        }
    }
}
