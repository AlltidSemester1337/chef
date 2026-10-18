package com.formulae.chef.feature.chat

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.formulae.chef.buildRecipeContextText
import com.formulae.chef.feature.chat.ui.ChatMessage
import com.formulae.chef.feature.chat.ui.Participant
import com.formulae.chef.feature.model.Recipe
import com.formulae.chef.services.MAX_CHAT_INPUT_LENGTH
import com.formulae.chef.services.backend.AiCompletionService
import com.formulae.chef.services.backend.AiPurpose
import com.formulae.chef.services.backend.BackendException
import com.formulae.chef.services.persistence.Content
import com.formulae.chef.services.persistence.Part
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class OverlayChatViewModel(
    private val aiService: AiCompletionService
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

    private var activePurpose = AiPurpose.OVERLAY_DEFAULT
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
        activePurpose = AiPurpose.OVERLAY_RECIPE_CONTEXT
        history = listOf(silentUserContent, silentModelContent)
        _uiState.value.addMessage(
            ChatMessage(
                text = "I've loaded ${recipe.title} — what would you like to know?",
                participant = Participant.MODEL
            )
        )
    }

    /**
     * Sends [userMessage] (capped at [MAX_CHAT_INPUT_LENGTH]) with the history to [aiService] under
     * [activePurpose]. The backend enforces the beta quota: [BackendException.QuotaExceeded] and
     * [BackendException.EmailVerificationRequired] open the matching dialog, any other failure
     * shows an error message. Only successful exchanges are added to [history].
     */
    fun sendMessage(userMessage: String) {
        val truncatedMessage = userMessage.take(MAX_CHAT_INPUT_LENGTH)
        val userContent = Content(role = "user", parts = listOf(Part(truncatedMessage)))
        _uiState.value.addMessage(
            ChatMessage(text = truncatedMessage, participant = Participant.USER, isPending = true)
        )
        _isLoading.value = true
        viewModelScope.launch {
            try {
                val messages = history + userContent
                val response = aiService.complete(activePurpose, messages)
                history = messages + Content(role = "model", parts = listOf(Part(response)))
                _uiState.value.addMessage(ChatMessage(text = response, participant = Participant.MODEL))
            } catch (e: CancellationException) {
                throw e
            } catch (e: BackendException.QuotaExceeded) {
                _quotaExceeded.value = true
            } catch (e: BackendException.EmailVerificationRequired) {
                _emailVerificationRequired.value = true
            } catch (e: Exception) {
                Log.e(TAG, "Failed to generate chat response", e)
                _uiState.value.addMessage(ChatMessage(text = ERROR_MESSAGE, participant = Participant.ERROR))
            } finally {
                _uiState.value.replaceLastPendingMessage()
                _isLoading.value = false
            }
        }
    }

    fun reset() {
        contextInitialized = false
        _uiState.value = ChatUiState()
        activePurpose = AiPurpose.OVERLAY_DEFAULT
        history = emptyList()
    }

    private companion object {
        const val TAG = "OverlayChatViewModel"
        const val ERROR_MESSAGE = "Sorry, something went wrong. Please try again."
    }
}
