/*
 * Copyright 2023 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.formulae.chef.feature.chat

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.formulae.chef.feature.chat.ui.ChatMessage
import com.formulae.chef.feature.chat.ui.Participant
import com.formulae.chef.feature.model.LikedMessage
import com.formulae.chef.feature.model.Recipe
import com.formulae.chef.feature.model.Recipes
import com.formulae.chef.feature.model.UserPreferences
import com.formulae.chef.services.BetaQuotaService
import com.formulae.chef.services.MAX_CHAT_INPUT_LENGTH
import com.formulae.chef.services.MAX_IMAGES_PER_MESSAGE
import com.formulae.chef.services.QuotaResult
import com.formulae.chef.services.ai.BergetChatCompletionService
import com.formulae.chef.services.ai.BergetModelConfig
import com.formulae.chef.services.authentication.UserSessionService
import com.formulae.chef.services.persistence.ChatHistoryRepository
import com.formulae.chef.services.persistence.ChatHistoryRepositoryImpl
import com.formulae.chef.services.persistence.Content
import com.formulae.chef.services.persistence.LikedMessagesRepository
import com.formulae.chef.services.persistence.LikedMessagesRepositoryImpl
import com.formulae.chef.services.persistence.Part
import com.formulae.chef.services.persistence.RecipeRepositoryImpl
import com.formulae.chef.services.persistence.UserPreferencesRepository
import com.formulae.chef.services.persistence.UserPreferencesRepositoryImpl
import com.formulae.chef.services.telemetry.LlmSpanAttributes
import com.formulae.chef.services.telemetry.getTracer
import com.formulae.chef.services.telemetry.withParentSpan
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.UserInfo
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import com.google.firebase.vertexai.GenerativeModel
import com.google.firebase.vertexai.type.ImagePart
import com.google.firebase.vertexai.type.content
import com.google.gson.Gson
import io.opentelemetry.api.trace.SpanKind
import io.opentelemetry.api.trace.StatusCode
import java.io.ByteArrayOutputStream
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.util.UUID
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

private val IMAGE_PROMPT_TEMPLATE =
    "As a professional photographer specializing in 100mm Macro lens natural lighting food photography, " +
        "create a photorealistic, colorful, visually appealing image of a single serving for the following recipe: "

private data class PreferenceDetectionResult(
    val detected: Boolean = false,
    val updatedSummary: String = ""
)

class ChatViewModel(
    private val chatCompletionService: BergetChatCompletionService,
    private val chatConfig: BergetModelConfig,
    private val jsonConfig: BergetModelConfig,
    private val preferencesConfig: BergetModelConfig,
    private val compactionConfig: BergetModelConfig,
    imageGenerativeModel: GenerativeModel,
    application: Application,
    userSessionService: UserSessionService,
    private val applicationScope: CoroutineScope,
    private val betaQuotaService: BetaQuotaService = BetaQuotaService(userSessionService)
) : AndroidViewModel(application) {
    private val _recipeRepositoryImpl = RecipeRepositoryImpl()
    private val _projectId = FirebaseApp.getInstance().options.projectId

    private val _imageGenerativeModel = imageGenerativeModel
    private val _userSessionService = userSessionService

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

    private val _chatHistory: MutableStateFlow<List<Content>> = MutableStateFlow(emptyList())
    private val _uiState: MutableStateFlow<ChatUiState> = MutableStateFlow(ChatUiState())

    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _selectedRecipeFromChat = MutableStateFlow<Recipe?>(null)
    val selectedRecipeFromChat: StateFlow<Recipe?> = _selectedRecipeFromChat.asStateFlow()

    private var _currentUser: UserInfo? = null
    private lateinit var _chatHistoryPersistenceImpl: ChatHistoryRepository
    private lateinit var _userPreferencesRepository: UserPreferencesRepository
    private lateinit var _likedMessagesRepository: LikedMessagesRepository
    private var _cachedPreferences: UserPreferences? = null
    private var _cachedLikedMessages: List<Pair<String, LikedMessage>> = emptyList()

    init {
        viewModelScope.launch {
            _isLoading.value = true
            _currentUser = _userSessionService.currentUser.first()
            if (_currentUser == null) {
                Log.e("ChatViewModel", "No authenticated user, aborting chat init")
                _isLoading.value = false
                return@launch
            }
            val uid = _currentUser!!.uid
            _chatHistoryPersistenceImpl = ChatHistoryRepositoryImpl(uid)
            _userPreferencesRepository = UserPreferencesRepositoryImpl(uid)
            _likedMessagesRepository = LikedMessagesRepositoryImpl(uid)

            val historyDeferred = async { initializeChatHistory() }
            val prefsDeferred = async { loadUserPreferences() }
            val collectionTitlesDeferred = async { loadCollectionTitles(uid) }
            val likedMessagesDeferred = async { loadLikedMessages() }

            val persistedHistory = historyDeferred.await()
            val prefs = prefsDeferred.await()
            val collectionTitles = collectionTitlesDeferred.await()
            val likedMessages = likedMessagesDeferred.await()

            _cachedPreferences = prefs
            _cachedLikedMessages = likedMessages

            val fullHistory = buildChatHistoryWithPreferences(persistedHistory, prefs, collectionTitles)
            _chatHistory.value = fullHistory
            _isLoading.value = false
            updateUiStateMessages(persistedHistory)
        }
    }

    private suspend fun initializeChatHistory(): List<Content> {
        return try {
            _chatHistoryPersistenceImpl.loadChatHistoryLastTwentyEntries()
        } catch (e: Exception) {
            Log.e("FirebaseDB", "Error fetching chat history", e)
            emptyList()
        }
    }

    private suspend fun loadUserPreferences(): UserPreferences? {
        return try {
            _userPreferencesRepository.loadPreferences()
        } catch (e: Exception) {
            Log.e("ChatViewModel", "Error loading user preferences", e)
            null
        }
    }

    private suspend fun loadCollectionTitles(uid: String): List<String> {
        return try {
            _recipeRepositoryImpl.loadUserRecipes(uid)
                .filter { it.isFavourite }
                .map { it.title }
        } catch (e: Exception) {
            Log.w("ChatViewModel", "Failed to load collection titles (non-critical)", e)
            emptyList()
        }
    }

    private suspend fun loadLikedMessages(): List<Pair<String, LikedMessage>> {
        return try {
            _likedMessagesRepository.loadLikedMessages()
        } catch (e: Exception) {
            Log.w("ChatViewModel", "Failed to load liked messages (non-critical)", e)
            emptyList()
        }
    }

    private fun updateUiStateMessages(history: List<Content>) {
        val likedTexts = _cachedLikedMessages.map { (_, msg) -> msg.text }.toSet()
        _uiState.value = ChatUiState(
            history.map { content ->
                val text = content.parts.firstOrNull()?.text ?: ""
                ChatMessage(
                    text = text,
                    participant = if (content.role == "user") Participant.USER else Participant.MODEL,
                    isPending = false,
                    isLiked = content.role == "model" && text in likedTexts
                )
            }
        )
    }

    fun sendMessage(userMessage: String) {
        val truncatedMessage = userMessage.take(MAX_CHAT_INPUT_LENGTH)
        val newUserContent = Content(role = "user", parts = listOf(Part(truncatedMessage)))
        _uiState.value.addMessage(
            ChatMessage(
                text = truncatedMessage,
                participant = Participant.USER,
                isPending = true
            )
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
            try {
                val modelResponse = generateModelResponseInstrumented(
                    config = chatConfig,
                    spanName = "generateChatModelResponse",
                    messages = _chatHistory.value + newUserContent
                )
                _uiState.value.replaceLastPendingMessage()

                val newModelContent = Content(role = "model", parts = listOf(Part(modelResponse)))
                _chatHistory.value += newUserContent
                _chatHistory.value += newModelContent
                _chatHistoryPersistenceImpl.saveNewEntries(listOf(newUserContent, newModelContent))

                launch { detectAndSavePreferences(truncatedMessage) }

                val extractedRecipes = try {
                    extractRecipeDetailsFromMessage(modelResponse)
                } catch (e: Exception) {
                    Log.w("ChatViewModel", "Recipe extraction failed, showing plain text", e)
                    emptyList()
                }

                if (extractedRecipes.isNotEmpty()) {
                    val messageId = UUID.randomUUID().toString()
                    _uiState.value.addMessage(
                        ChatMessage(
                            id = messageId,
                            participant = Participant.MODEL,
                            recipes = extractedRecipes
                        )
                    )
                    launch { generateImagesForMessage(messageId, extractedRecipes) }
                } else {
                    _uiState.value.addMessage(
                        ChatMessage(
                            text = modelResponse,
                            participant = Participant.MODEL
                        )
                    )
                }
            } catch (e: Exception) {
                Log.e("ChatViewModel", "Failed to generate chat response", e)
                _uiState.value.replaceLastPendingMessage()
                _uiState.value.addMessage(
                    ChatMessage(
                        text = "Sorry, something went wrong. Please try again.",
                        participant = Participant.ERROR
                    )
                )
            }
        }
    }

    fun onMessageLiked(message: ChatMessage) {
        if (message.isLiked) return
        _likedMessagesRepository.saveLikedMessage(message.text)
        val liked = LikedMessage(
            text = message.text,
            likedAt = ZonedDateTime.now(ZoneOffset.UTC).toString()
        )
        _cachedLikedMessages = _cachedLikedMessages + Pair("", liked)
        _uiState.value.updateMessageLiked(message.id, isLiked = true)
    }

    private suspend fun detectAndSavePreferences(userMessage: String) {
        try {
            val currentPrefs = _cachedPreferences
            val prompt = if (currentPrefs?.summary?.isNotBlank() == true) {
                "Previously known preferences: ${currentPrefs.summary}\n\nUser message: $userMessage"
            } else {
                "User message: $userMessage"
            }
            val responseText = chatCompletionService.createChatCompletion(
                preferencesConfig,
                listOf(Content(role = "user", parts = listOf(Part(prompt))))
            )
            val gson = Gson()
            val result = gson.fromJson(responseText, PreferenceDetectionResult::class.java)
            if (result.detected && result.updatedSummary.isNotBlank()) {
                val updated = UserPreferences(
                    summary = result.updatedSummary,
                    updatedAt = ZonedDateTime.now(ZoneOffset.UTC).toString()
                )
                _userPreferencesRepository.savePreferences(updated)
                _cachedPreferences = updated
                Log.d("ChatViewModel", "Preferences updated: ${result.updatedSummary}")
            }
        } catch (e: Exception) {
            Log.w("ChatViewModel", "Preference detection failed (non-critical)", e)
        }
    }

    fun onNavigateAway() {
        applicationScope.launch {
            runCompactionIfNeeded()
        }
    }

    private suspend fun runCompactionIfNeeded() {
        if (_currentUser == null) return
        try {
            val allEntries = _chatHistoryPersistenceImpl.loadAllEntries()
            val entriesToCompact = selectEntriesToCompact(allEntries)
            if (entriesToCompact.isEmpty()) return

            val transcript = entriesToCompact.joinToString("\n") { (_, entryContent) ->
                "${entryContent.role}: ${entryContent.parts.firstOrNull()?.text ?: ""}"
            }
            val currentPrefs = _cachedPreferences
            val likedToCompact = _cachedLikedMessages
            val prompt = buildString {
                if (currentPrefs?.summary?.isNotBlank() == true) {
                    append("Existing preferences: ${currentPrefs.summary}\n\n")
                }
                if (likedToCompact.isNotEmpty()) {
                    val likedText = likedToCompact.joinToString("\n---\n") { (_, msg) -> msg.text }
                    append("Responses the user has explicitly liked:\n$likedText\n\n")
                }
                append("Chat transcript to summarize:\n$transcript")
            }
            val newSummary = chatCompletionService.createChatCompletion(
                compactionConfig,
                listOf(Content(role = "user", parts = listOf(Part(prompt))))
            ).trim()
            if (newSummary.isNotBlank()) {
                val updatedPrefs = UserPreferences(
                    summary = newSummary,
                    updatedAt = ZonedDateTime.now(ZoneOffset.UTC).toString()
                )
                _userPreferencesRepository.savePreferences(updatedPrefs)
                _cachedPreferences = updatedPrefs
                _chatHistoryPersistenceImpl.deleteEntries(entriesToCompact.map { it.first })
                val likedIdsToDelete = likedToCompact.map { it.first }.filter { it.isNotBlank() }
                if (likedIdsToDelete.isNotEmpty()) {
                    _likedMessagesRepository.deleteMessages(likedIdsToDelete)
                }
                _cachedLikedMessages = emptyList()
                Log.d("ChatViewModel", "Compacted ${entriesToCompact.size} entries into preferences")
            }
        } catch (e: Exception) {
            Log.e("ChatViewModel", "Compaction failed (non-critical)", e)
        }
    }

    private suspend fun extractRecipeDetailsFromMessage(messageText: String): List<Recipe> {
        val recipesJsonText = generateModelResponseInstrumented(
            config = jsonConfig,
            spanName = "generateJsonModelResponse",
            messages = listOf(Content(role = "user", parts = listOf(Part(messageText))))
        )
        val gson = Gson()
        val recipesWrapper = gson.fromJson(recipesJsonText, Recipes::class.java)
        return recipesWrapper.recipes.map { recipe ->
            val title = recipe.title.replace("##", "").trim()
            val summary = recipe.summary.replace("##", "").trim()
            val ingredients = recipe.ingredients
            val instructions = recipe.instructions
            if (title.isEmpty() || summary.isEmpty() || ingredients.isEmpty() || instructions.isEmpty()) {
                throw Exception("Failed to derive details from recipe: $recipe")
            }
            recipe.copyOf(
                id = UUID.randomUUID().toString(),
                title = title,
                summary = summary,
                updatedAt = ZonedDateTime.now(ZoneOffset.UTC).toString()
            )
        }
    }

    private suspend fun generateImagesForMessage(messageId: String, recipes: List<Recipe>) {
        coroutineScope {
            recipes.take(MAX_IMAGES_PER_MESSAGE).map { recipe ->
                async {
                    val recipeId = recipe.id ?: return@async
                    try {
                        val imageUrl = createImageForRecipeAsync(recipe.toString())
                        _uiState.value.updateRecipeImage(messageId, recipeId, imageUrl)
                    } catch (e: Exception) {
                        Log.e("ChatViewModel", "Image generation failed for ${recipe.title}", e)
                        _uiState.value.markRecipeImageFailed(messageId, recipeId)
                    }
                }
            }.awaitAll()
        }
    }

    private suspend fun createImageForRecipeAsync(recipe: String): String {
        Log.d("ChatViewModel", "Starting image generation pipeline")
        val gcsUri = generateImageInstrumented(recipe)
        Log.d("ChatViewModel", "Image generated and uploaded, fetching download URL")
        val storagePath = gcsUri.removePrefix("gs://$_projectId.firebasestorage.app/")
        val downloadUrl = Firebase.storage("gs://$_projectId.firebasestorage.app/")
            .reference.child(storagePath).downloadUrl.await()
        Log.d("ChatViewModel", "Download URL obtained successfully")
        return "https://" + downloadUrl.host + downloadUrl.encodedPath + "?alt=media"
    }

    fun onRecipeSelectedFromChat(recipe: Recipe) {
        _selectedRecipeFromChat.value = recipe
    }

    fun clearSelectedRecipe() {
        _selectedRecipeFromChat.value = null
    }

    fun onRecipeStarredFromGrid(messageId: String, recipe: Recipe) {
        val context: Context = getApplication<Application>().applicationContext
        viewModelScope.launch {
            try {
                val recipeId = recipe.id ?: run {
                    Log.e("ChatViewModel", "Recipe has no ID, cannot star")
                    return@launch
                }
                val message = _uiState.value.messages.find { it.id == messageId }
                val isCurrentlyStarred = message?.starredRecipeIds?.contains(recipeId) == true
                if (isCurrentlyStarred) {
                    withContext(Dispatchers.IO) {
                        _recipeRepositoryImpl.removeRecipe(recipeId)
                    }
                    _uiState.value.updateRecipeStarred(messageId, recipeId, isStarred = false)
                    Toast.makeText(context, "Recipe removed from collection.", Toast.LENGTH_SHORT).show()
                } else {
                    val recipeToSave = recipe.copyOf(
                        uid = _currentUser?.uid ?: "",
                        isFavourite = true
                    )
                    withContext(Dispatchers.IO) {
                        _recipeRepositoryImpl.saveRecipe(recipeToSave)
                    }
                    _uiState.value.updateRecipeStarred(messageId, recipeId, isStarred = true)
                    Toast.makeText(context, "Recipe saved to collection.", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Log.e("FirebaseSave", "Failed to update recipe", e)
                Toast.makeText(
                    context,
                    "Failed to update recipe. Please try again.",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    fun onRecipeStarred(message: ChatMessage) {
        val context: Context = getApplication<Application>().applicationContext
        if (!message.isStarred) {
            saveRecipe(context, message)
        }
    }

    private fun saveRecipe(context: Context, message: ChatMessage) {
        viewModelScope.launch {
            try {
                val newRecipes = deriveRecipesFromMessage(message.text)

                if (_currentUser != null) {
                    newRecipes.forEach { it.uid = _currentUser!!.uid }
                }
                newRecipes.forEach { it.isFavourite = true }

                withContext(Dispatchers.IO) {
                    newRecipes.forEach(_recipeRepositoryImpl::saveRecipe)
                }
                _uiState.value.updateStarredMessage(message, isStarred = true)

                Toast.makeText(
                    context,
                    "Recipe saved successfully, view details in collection.",
                    Toast.LENGTH_SHORT
                ).show()
            } catch (e: Exception) {
                Log.e("FirebaseSave", "Failed to save recipe", e)
                Toast.makeText(
                    context,
                    "Failed to save recipe. Please try again.",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private suspend fun deriveRecipesFromMessage(messageText: String): List<Recipe> =
        withParentSpan("deriveRecipesFromMessage") {
            val recipes = extractRecipeDetailsFromMessage(messageText)
            recipes.map { recipe ->
                val imageUrl = try {
                    createImageForRecipeAsync(recipe.toString())
                } catch (e: Exception) {
                    Log.e(
                        "ChatViewModel",
                        "Image generation pipeline failed for '${recipe.title}' " +
                            "[${e.javaClass.simpleName}: ${e.message}]",
                        e
                    )
                    null
                }
                recipe.copyOf(imageUrl = imageUrl)
            }
        }

    private suspend fun generateModelResponseInstrumented(
        config: BergetModelConfig,
        spanName: String,
        messages: List<Content>
    ): String {
        val prompt = messages.lastOrNull()?.parts?.firstOrNull()?.text ?: ""
        val span = getTracer().spanBuilder(spanName)
            .setSpanKind(SpanKind.CLIENT)
            .setAllAttributes(LlmSpanAttributes.input(spanName, config.model, prompt))
            .startSpan()

        return try {
            io.opentelemetry.context.Context.current().with(span).makeCurrent().use {
                val response = chatCompletionService.createChatCompletion(config, messages)
                span.setAllAttributes(LlmSpanAttributes.output(response))
                span.setStatus(StatusCode.OK)
                response
            }
        } catch (e: Exception) {
            span.recordException(e)
            span.setStatus(StatusCode.ERROR)
            throw e
        } finally {
            span.end()
        }
    }

    private suspend fun generateImageInstrumented(recipe: String): String {
        val prompt = IMAGE_PROMPT_TEMPLATE + recipe
        val modelName = "vertexai/gemini-flash-image"
        val span = getTracer().spanBuilder("generateImage")
            .setSpanKind(SpanKind.CLIENT)
            .setAllAttributes(LlmSpanAttributes.input("generateImage", modelName, prompt))
            .startSpan()

        return try {
            io.opentelemetry.context.Context.current().with(span).makeCurrent().use {
                val response = _imageGenerativeModel.generateContent(content { text(prompt) })
                val imagePart = response.candidates?.firstOrNull()?.content?.parts
                    ?.filterIsInstance<ImagePart>()
                    ?.firstOrNull()
                    ?: throw IllegalStateException("No image data in response from gemini-2.5-flash-image")

                val outputStream = ByteArrayOutputStream()
                imagePart.image.compress(Bitmap.CompressFormat.JPEG, 90, outputStream)
                val imageBytes = outputStream.toByteArray()
                val imagePath = "recipes/${UUID.randomUUID()}.jpg"

                Firebase.storage("gs://$_projectId.firebasestorage.app/")
                    .reference.child(imagePath)
                    .putBytes(imageBytes)
                    .await()

                val gcsUri = "gs://$_projectId.firebasestorage.app/$imagePath"

                span.setAllAttributes(LlmSpanAttributes.output(gcsUri))
                span.setStatus(StatusCode.OK)

                gcsUri
            }
        } catch (e: Exception) {
            span.recordException(e)
            span.setStatus(StatusCode.ERROR)
            throw e
        } finally {
            span.end()
        }
    }

    companion object {
        fun buildChatHistoryWithPreferences(
            history: List<Content>,
            prefs: UserPreferences?,
            collectionTitles: List<String> = emptyList()
        ): List<Content> {
            val hasSummary = prefs != null && prefs.summary.isNotBlank()
            val hasTitles = collectionTitles.isNotEmpty()
            if (!hasSummary && !hasTitles) return history

            val contextText = buildString {
                if (hasSummary) {
                    append("My food preferences and context: ${prefs!!.summary}")
                }
                if (hasTitles) {
                    if (hasSummary) append("\n\n")
                    append("Recipes I have saved to my collection: ${collectionTitles.joinToString(", ")}")
                }
            }

            val syntheticUser = Content(role = "user", parts = listOf(Part(contextText)))
            val syntheticModel = Content(
                role = "model",
                parts = listOf(Part("Understood, I'll keep these preferences in mind throughout our conversation."))
            )
            return listOf(syntheticUser, syntheticModel) + history
        }

        fun selectEntriesToCompact(
            allEntries: List<Pair<String, Content>>,
            keepLast: Int = 20
        ): List<Pair<String, Content>> {
            if (allEntries.size <= keepLast) return emptyList()
            return allEntries.dropLast(keepLast)
        }
    }
}
