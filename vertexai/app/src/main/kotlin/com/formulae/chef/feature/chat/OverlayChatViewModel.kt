package com.formulae.chef.feature.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.formulae.chef.feature.chat.ui.ChatMessage
import com.formulae.chef.feature.chat.ui.Participant
import com.formulae.chef.feature.model.Recipe
import com.google.firebase.vertexai.GenerativeModel
import com.google.firebase.vertexai.type.content
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class OverlayChatViewModel(
    private val defaultChatModel: GenerativeModel,
    private val recipeContextModel: GenerativeModel
) : ViewModel() {

    private val _uiState: MutableStateFlow<ChatUiState> = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private var chat = defaultChatModel.startChat()
    private var contextInitialized = false

    fun initWithRecipeContext(recipe: Recipe) {
        if (contextInitialized) return
        contextInitialized = true

        val contextText = buildRecipeContextText(recipe)
        val silentUserContent = content(role = "user") { text(contextText) }
        val silentModelContent = content(role = "model") { text("Understood, I have the recipe details.") }
        chat = recipeContextModel.startChat(
            history = listOf(silentUserContent, silentModelContent)
        )
        _uiState.value.addMessage(
            ChatMessage(
                text = "I've loaded **${recipe.title}** — what would you like to know?",
                participant = Participant.MODEL
            )
        )
    }

    fun sendMessage(userMessage: String) {
        _uiState.value.addMessage(
            ChatMessage(text = userMessage, participant = Participant.USER, isPending = true)
        )
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val response = chat.sendMessage(userMessage)
                _uiState.value.replaceLastPendingMessage()
                response.text?.let { modelResponse ->
                    _uiState.value.addMessage(
                        ChatMessage(text = modelResponse, participant = Participant.MODEL)
                    )
                }
            } catch (e: Exception) {
                _uiState.value.replaceLastPendingMessage()
                _uiState.value.addMessage(
                    ChatMessage(
                        text = e.localizedMessage ?: "An error occurred",
                        participant = Participant.ERROR
                    )
                )
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun reset() {
        contextInitialized = false
        _uiState.value = ChatUiState()
        chat = defaultChatModel.startChat()
    }

    companion object {
        fun buildRecipeContextText(recipe: Recipe): String = buildString {
            append("Recipe: ${recipe.title}\n\n")
            if (recipe.summary.isNotBlank()) append("${recipe.summary.replace("\\n", "\n")}\n\n")
            if (recipe.ingredients.isNotEmpty()) {
                append("Ingredients:\n")
                recipe.ingredients.forEach { ing ->
                    append("- ${ing.quantity.orEmpty()} ${ing.unit.orEmpty()} ${ing.name.orEmpty()}".trim())
                    append("\n")
                }
                append("\n")
            }
            if (recipe.instructions.isNotEmpty()) {
                append("Instructions:\n")
                recipe.instructions.forEachIndexed { i, step -> append("${i + 1}. $step\n") }
            }
            recipe.tipsAndTricks?.takeIf { it.isNotBlank() }?.let { append("\nTips: $it") }
        }
    }
}
