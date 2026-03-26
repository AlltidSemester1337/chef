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
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.formulae.chef.feature.chat.ui.ChatMessage
import com.formulae.chef.feature.chat.ui.Participant
import com.formulae.chef.feature.model.Recipe
import com.formulae.chef.feature.model.Recipes
import com.formulae.chef.feature.model.UserPreferences
import com.formulae.chef.services.authentication.UserSessionService
import com.formulae.chef.services.persistence.ChatHistoryRepository
import com.formulae.chef.services.persistence.ChatHistoryRepositoryImpl
import com.formulae.chef.services.persistence.RecipeRepositoryImpl
import com.formulae.chef.services.persistence.UserPreferencesRepository
import com.formulae.chef.services.persistence.UserPreferencesRepositoryImpl
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.UserInfo
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import com.google.firebase.vertexai.Chat
import com.google.firebase.vertexai.GenerativeModel
import com.google.firebase.vertexai.type.Content
import com.google.firebase.vertexai.type.GenerateContentResponse
import android.graphics.Bitmap
import com.google.firebase.vertexai.type.ImagePart
import java.io.ByteArrayOutputStream
import com.google.firebase.vertexai.type.TextPart
import com.google.firebase.vertexai.type.asTextOrNull
import com.google.firebase.vertexai.type.content
import com.google.gson.Gson
import io.opentelemetry.api.GlobalOpenTelemetry
import io.opentelemetry.api.trace.Span
import io.opentelemetry.api.trace.StatusCode
import io.opentelemetry.api.trace.Tracer
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
    chatGenerativeModel: GenerativeModel,
    jsonGenerativeModel: GenerativeModel,
    private val preferencesGenerativeModel: GenerativeModel,
    private val compactionGenerativeModel: GenerativeModel,
    imageGenerativeModel: GenerativeModel,
    application: Application,
    userSessionService: UserSessionService,
    private val applicationScope: CoroutineScope
) : AndroidViewModel(application) {
    private val _recipeRepositoryImpl = RecipeRepositoryImpl()
    private val _projectId = FirebaseApp.getInstance().options.projectId

    private val _imageGenerativeModel = imageGenerativeModel
    private val _userSessionService = userSessionService
    private val _jsonGenerativeModel = jsonGenerativeModel

    private val _chatHistory: MutableStateFlow<List<Content>> = MutableStateFlow(emptyList())
    private val _uiState: MutableStateFlow<ChatUiState> = MutableStateFlow(ChatUiState())

    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _selectedRecipeFromChat = MutableStateFlow<Recipe?>(null)
    val selectedRecipeFromChat: StateFlow<Recipe?> = _selectedRecipeFromChat.asStateFlow()

    private lateinit var chat: Chat
    private var _currentUser: UserInfo? = null
    private lateinit var _chatHistoryPersistenceImpl: ChatHistoryRepository
    private lateinit var _userPreferencesRepository: UserPreferencesRepository
    private var _cachedPreferences: UserPreferences? = null

    init {
        viewModelScope.launch {
            _isLoading.value = true
            _currentUser = _userSessionService.currentUser.first()
            if (_currentUser == null) {
                Log.e("ChatViewModel", "No authenticated user, aborting chat init")
                _isLoading.value = false
                return@launch
            }
            _chatHistoryPersistenceImpl = ChatHistoryRepositoryImpl(_currentUser!!.uid)
            _userPreferencesRepository = UserPreferencesRepositoryImpl(_currentUser!!.uid)

            val historyDeferred = async { initializeChatHistory() }
            val prefsDeferred = async { loadUserPreferences() }
            val persistedHistory = historyDeferred.await()
            val prefs = prefsDeferred.await()
            _cachedPreferences = prefs

            val fullHistory = buildChatHistoryWithPreferences(persistedHistory, prefs)
            _chatHistory.value = fullHistory
            chat = chatGenerativeModel.startChat(history = fullHistory)
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

    private fun updateUiStateMessages(history: List<Content>) {
        _uiState.value = ChatUiState(
            history.map { content ->
                ChatMessage(
                    text = content.parts.first().asTextOrNull() ?: "",
                    participant = if (content.role == "user") Participant.USER else Participant.MODEL,
                    isPending = false
                )
            }
        )
    }

    fun sendMessage(userMessage: String) {
        val newUserContent = content(role = "user") { text(userMessage) }
        _uiState.value.addMessage(
            ChatMessage(
                text = userMessage,
                participant = Participant.USER,
                isPending = true
            )
        )

        viewModelScope.launch {
            try {
                val response = generateModelResponseInstrumented(
                    prompt = userMessage,
                    responseFunction = chat::sendMessage,
                    spanName = "generateChatModelResponse"
                )
                _uiState.value.replaceLastPendingMessage()

                response.text?.let { modelResponse ->
                    val newModelContent = content(role = "model") { text(modelResponse) }
                    _chatHistory.value += newUserContent
                    _chatHistory.value += newModelContent
                    _chatHistoryPersistenceImpl.saveNewEntries(listOf(newUserContent, newModelContent))

                    launch { detectAndSavePreferences(userMessage) }

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
                }
            } catch (e: Exception) {
                _uiState.value.replaceLastPendingMessage()
                _uiState.value.addMessage(
                    ChatMessage(
                        text = e.localizedMessage ?: e.message ?: "An error occurred",
                        participant = Participant.ERROR
                    )
                )
            }
        }
    }

    private suspend fun detectAndSavePreferences(userMessage: String) {
        try {
            val currentPrefs = _cachedPreferences
            val prompt = if (currentPrefs?.summary?.isNotBlank() == true) {
                "Previously known preferences: ${currentPrefs.summary}\n\nUser message: $userMessage"
            } else {
                "User message: $userMessage"
            }
            val response = preferencesGenerativeModel.generateContent(content { text(prompt) })
            val responseText = response.text ?: return
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
        try {
            val allEntries = _chatHistoryPersistenceImpl.loadAllEntries()
            val entriesToCompact = selectEntriesToCompact(allEntries)
            if (entriesToCompact.isEmpty()) return

            val transcript = entriesToCompact.joinToString("\n") { (_, entryContent) ->
                "${entryContent.role}: ${entryContent.parts.filterIsInstance<TextPart>().firstOrNull()?.text ?: ""}"
            }
            val currentPrefs = _cachedPreferences
            val prompt = buildString {
                if (currentPrefs?.summary?.isNotBlank() == true) {
                    append("Existing preferences: ${currentPrefs.summary}\n\n")
                }
                append("Chat transcript to summarize:\n$transcript")
            }
            val response = compactionGenerativeModel.generateContent(content { text(prompt) })
            val newSummary = response.text?.trim() ?: return
            if (newSummary.isNotBlank()) {
                val updatedPrefs = UserPreferences(
                    summary = newSummary,
                    updatedAt = ZonedDateTime.now(ZoneOffset.UTC).toString()
                )
                _userPreferencesRepository.savePreferences(updatedPrefs)
                _cachedPreferences = updatedPrefs
            }
            _chatHistoryPersistenceImpl.deleteEntries(entriesToCompact.map { it.first })
            Log.d("ChatViewModel", "Compacted ${entriesToCompact.size} entries into preferences")
        } catch (e: Exception) {
            Log.e("ChatViewModel", "Compaction failed (non-critical)", e)
        }
    }

    private suspend fun extractRecipeDetailsFromMessage(messageText: String): List<Recipe> {
        val recipesJsonText = generateModelResponseInstrumented(
            prompt = messageText,
            responseFunction = { prompt ->
                _jsonGenerativeModel.generateContent(content { text(prompt) })
            },
            spanName = "generateJsonModelResponse"
        ).text!!
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
            recipes.map { recipe ->
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
        val gcsUri = generateImageInstrumented(recipe)
        val storagePath = gcsUri.removePrefix("gs://$_projectId.firebasestorage.app/")
        val downloadUrl = Firebase.storage("gs://$_projectId.firebasestorage.app/")
            .reference.child(storagePath).downloadUrl.await()
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
                val recipeToSave = recipe.copyOf(
                    uid = _currentUser?.uid ?: "",
                    isFavourite = true
                )
                withContext(Dispatchers.IO) {
                    _recipeRepositoryImpl.saveRecipe(recipeToSave)
                }
                _uiState.value.updateRecipeStarred(messageId, recipeId, isStarred = true)
                Toast.makeText(context, "Recipe saved to collection.", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Log.e("FirebaseSave", "Failed to save recipe", e)
                Toast.makeText(
                    context,
                    "Failed to save recipe: ${e.localizedMessage}",
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
                    "Failed to save recipe: ${e.localizedMessage}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private suspend fun deriveRecipesFromMessage(messageText: String): List<Recipe> {
        val recipes = extractRecipeDetailsFromMessage(messageText)
        return recipes.map { recipe ->
            val imageUrl = try {
                createImageForRecipeAsync(recipe.toString())
            } catch (e: Exception) {
                Log.e("ChatViewModel", "Failed to generate image for ${recipe.title}", e)
                null
            }
            recipe.copyOf(imageUrl = imageUrl)
        }
    }

    private suspend fun generateModelResponseInstrumented(
        prompt: String,
        responseFunction: suspend (String) -> GenerateContentResponse,
        spanName: String
    ): GenerateContentResponse {
        val tracer: Tracer = getTracer()
        val span: Span = tracer.spanBuilder(spanName)
            .setAttribute("operation.name", "generateChatModelResponse")
            .setAttribute("llm.model_name", "gemini-2.5-flash")
            .setAttribute("llm.input_messages.0.message.role", "user")
            .setAttribute("llm.input_messages.0.message.content", prompt)
            .startSpan()

        var response: GenerateContentResponse? = null
        try {
            io.opentelemetry.context.Context.current().with(span).makeCurrent().use {
                response = responseFunction(prompt)
                span.setAttribute("llm.output_messages.0.message.role", "model")
                span.setAttribute("llm.output_messages.0.message.content", response?.text ?: "")
            }
        } catch (e: Exception) {
            span.recordException(e)
            span.setStatus(StatusCode.ERROR)
            throw e
        } finally {
            span.end()
        }
        return response!!
    }

    private fun getTracer(): Tracer {
        return GlobalOpenTelemetry.getTracer("com.formulae.chef")
    }

    private suspend fun generateImageInstrumented(recipe: String): String {
        val prompt = IMAGE_PROMPT_TEMPLATE + recipe
        val span = getTracer().spanBuilder("generateImage")
            .setAttribute("operation.name", "generateImage")
            .setAttribute("llm.model_name", "vertexai/gemini-flash-image")
            .setAttribute("llm.input_messages.0.message.role", "user")
            .setAttribute("llm.input_messages.0.message.content", prompt)
            .startSpan()

        try {
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

            span.setAttribute("llm.output_messages.0.message.role", "model")
            span.setAttribute("llm.output_messages.0.message.content", gcsUri)

            return gcsUri
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
            prefs: UserPreferences?
        ): List<Content> {
            if (prefs == null || prefs.summary.isBlank()) return history
            val syntheticUser = content(role = "user") {
                text("My food preferences and context: ${prefs.summary}")
            }
            val syntheticModel = content(role = "model") {
                text("Understood, I'll keep these preferences in mind throughout our conversation.")
            }
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
