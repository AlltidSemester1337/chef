package com.formulae.chef.feature.chat

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.formulae.chef.buildRecipeContextText
import com.formulae.chef.feature.model.Recipe
import com.formulae.chef.feature.model.Recipes
import com.formulae.chef.services.ai.BergetChatCompletionService
import com.formulae.chef.services.ai.BergetModelConfig
import com.formulae.chef.services.persistence.Content
import com.formulae.chef.services.persistence.Part
import com.formulae.chef.services.telemetry.LlmSpanAttributes
import com.formulae.chef.services.telemetry.getTracer
import com.formulae.chef.services.telemetry.withParentSpan
import com.google.gson.Gson
import io.opentelemetry.api.trace.SpanKind
import io.opentelemetry.api.trace.StatusCode
import io.opentelemetry.context.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AskChefVariantViewModel(
    private val chatCompletionService: BergetChatCompletionService,
    private val recipeAdjustConfig: BergetModelConfig,
    private val jsonConfig: BergetModelConfig
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
                val resultRecipe = withParentSpan("adjustRecipeFlow") {
                    val prompt = buildAdjustPrompt(recipe, userRequest)
                    val adjustedText = generateInstrumented(
                        spanName = "adjustRecipe",
                        config = recipeAdjustConfig,
                        prompt = prompt
                    )

                    val jsonText = generateInstrumented(
                        spanName = "extractRecipeJson",
                        config = jsonConfig,
                        prompt = adjustedText
                    )

                    val recipes = Gson().fromJson(jsonText, Recipes::class.java).recipes
                    if (recipes.isEmpty()) throw Exception("No recipe in JSON response")

                    recipes.first()
                }

                _state.value = State.Success(resultRecipe)
            } catch (e: Exception) {
                Log.e(TAG, "adjustRecipe failed", e)
                _state.value = State.Error
            }
        }
    }

    private suspend fun generateInstrumented(
        spanName: String,
        config: BergetModelConfig,
        prompt: String
    ): String {
        val span = getTracer().spanBuilder(spanName)
            .setSpanKind(SpanKind.CLIENT)
            .setAllAttributes(LlmSpanAttributes.input(spanName, config.model, prompt))
            .startSpan()
        return try {
            Context.current().with(span).makeCurrent().use {
                val result = chatCompletionService.createChatCompletion(
                    config,
                    listOf(Content(role = "user", parts = listOf(Part(prompt))))
                )
                span.setAllAttributes(LlmSpanAttributes.output(result))
                span.setStatus(StatusCode.OK)
                result
            }
        } catch (e: Exception) {
            span.recordException(e)
            span.setStatus(StatusCode.ERROR)
            throw e
        } finally {
            span.end()
        }
    }

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
