package com.formulae.chef.feature.chat

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.formulae.chef.ModelConfig
import com.formulae.chef.buildRecipeContextText
import com.formulae.chef.feature.model.Recipe
import com.formulae.chef.feature.model.Recipes
import com.google.firebase.vertexai.GenerativeModel
import com.google.firebase.vertexai.type.content
import com.google.gson.Gson
import io.opentelemetry.api.GlobalOpenTelemetry
import io.opentelemetry.api.trace.Span
import io.opentelemetry.api.trace.StatusCode
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
                val adjustedText = generateInstrumented(
                    spanName = "adjustRecipe",
                    modelName = ModelConfig.CHAT_MODEL,
                    prompt = prompt
                ) { recipeAdjustModel.generateContent(content { text(it) }).text }
                    ?: throw Exception("Empty response from adjust model")

                val jsonText = generateInstrumented(
                    spanName = "extractRecipeJson",
                    modelName = ModelConfig.LITE_MODEL,
                    prompt = adjustedText
                ) { jsonGenerativeModel.generateContent(content { text(it) }).text }
                    ?: throw Exception("Empty response from JSON model")

                val recipes = Gson().fromJson(jsonText, Recipes::class.java).recipes
                if (recipes.isEmpty()) throw Exception("No recipe in JSON response")

                _state.value = State.Success(recipes.first())
            } catch (e: Exception) {
                Log.e(TAG, "adjustRecipe failed", e)
                _state.value = State.Error
            }
        }
    }

    private suspend fun generateInstrumented(
        spanName: String,
        modelName: String,
        prompt: String,
        call: suspend (String) -> String?
    ): String? {
        val tracer = GlobalOpenTelemetry.getTracer("com.formulae.chef")
        val span: Span = tracer.spanBuilder(spanName)
            .setAttribute("operation.name", spanName)
            .setAttribute("llm.model_name", modelName)
            .setAttribute("llm.input_messages.0.message.role", "user")
            .setAttribute("llm.input_messages.0.message.content", prompt)
            .startSpan()
        return try {
            io.opentelemetry.context.Context.current().with(span).makeCurrent().use {
                val result = call(prompt)
                span.setAttribute("llm.output_messages.0.message.role", "model")
                span.setAttribute("llm.output_messages.0.message.content", result ?: "")
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
