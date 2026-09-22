package com.formulae.chef.feature.chat

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.formulae.chef.buildRecipeContextText
import com.formulae.chef.feature.chat.ui.ChatMessage
import com.formulae.chef.feature.chat.ui.Participant
import com.formulae.chef.feature.model.Recipe
import com.formulae.chef.services.BetaQuotaService
import com.formulae.chef.services.MAX_CHAT_INPUT_LENGTH
import com.formulae.chef.services.QuotaResult
import com.formulae.chef.services.ai.BergetChatCompletionService
import com.formulae.chef.services.ai.BergetModelConfig
import com.formulae.chef.services.persistence.Content
import com.formulae.chef.services.persistence.Part
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class OverlayChatViewModel(
    private val chatCompletionService: BergetChatCompletionService,
    private val defaultConfig: BergetModelConfig,
    private val recipeContextConfig: BergetModelConfig,
    private val betaQuotaService: BetaQuotaService
) : ViewModel() {

    private val _uiState: MutableStateFlow<ChatUiState> = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _quotaExceeded = MutableStateFlow(false)
    val quotaExceeded: StateFlow<Boolean> = _quotaExceeded.asStateFlow()

    fun onQuotaExceededDialogDismissed() {
        _quotaExceeded.value = false
    }

    private val _emailVerificationRequired = MutableStateFlow(false)
    val emailVerificationRequired: StateFlow<Boolean> = _emailVerificationRequired.asStateFlow()

    fun onEmailVerificationRequiredDialogDismissed() {
        _emailVerificationRequired.value = false
    }

    private var activeConfig = defaultConfig
    private var history: List<Content> = emptyList()
    private var contextInitialized = false

    fun initWithRecipeContext(recipe: Recipe) {
        if (contextInitialized) return
        contextInitialized = true

        val contextText = buildRecipeContextText(recipe)
        val silentUserContent = Content(role = "user", parts = listOf(Part(contextText)))
        val silentModelContent = Content(
            role = "model",
            parts = listOf(Part("Understood, I have the recipe details."))
        )
        activeConfig = recipeContextConfig
        history = listOf(silentUserContent, silentModelContent)
        _uiState.value.addMessage(
            ChatMessage(
                text = "I've loaded ${recipe.title} — what would you like to know?",
                participant = Participant.MODEL
            )
        )
    }

    fun sendMessage(userMessage: String) {
        val truncatedMessage = userMessage.take(MAX_CHAT_INPUT_LENGTH)
        _uiState.value.addMessage(
            ChatMessage(text = truncatedMessage, participant = Participant.USER, isPending = true)
        )
        viewModelScope.launch {
            when (betaQuotaService.checkAndRecordInteraction()) {
                QuotaResult.Blocked -> {
                    _uiState.value.replaceLastPendingMessage()
                    _quotaExceeded.value = true
                    return@launch
                }
                QuotaResult.EmailVerificationRequired -> {
                    _uiState.value.replaceLastPendingMessage()
                    _emailVerificationRequired.value = true
                    return@launch
                }
                QuotaResult.Allowed -> Unit
            }
            _isLoading.value = true
            try {
                val newUserContent = Content(role = "user", parts = listOf(Part(truncatedMessage)))
                val modelResponse = chatCompletionService.createChatCompletion(
                    activeConfig,
                    history + newUserContent
                )
                _uiState.value.replaceLastPendingMessage()
                history = history + newUserContent +
                    Content(role = "model", parts = listOf(Part(modelResponse)))
                _uiState.value.addMessage(
                    ChatMessage(text = modelResponse, participant = Participant.MODEL)
                )
            } catch (e: Exception) {
                Log.e("OverlayChatViewModel", "Failed to generate chat response", e)
                _uiState.value.replaceLastPendingMessage()
                _uiState.value.addMessage(
                    ChatMessage(
                        text = "Sorry, something went wrong. Please try again.",
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
        activeConfig = defaultConfig
        history = emptyList()
    }
}
