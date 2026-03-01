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
import com.formulae.chef.services.authentication.UserSessionService
import com.formulae.chef.services.persistence.ChatHistoryRepository
import com.formulae.chef.services.persistence.ChatHistoryRepositoryImpl
import com.formulae.chef.services.persistence.RecipeRepositoryImpl
import com.google.cloud.aiplatform.v1.EndpointName
import com.google.cloud.aiplatform.v1.PredictRequest
import com.google.cloud.aiplatform.v1.PredictionServiceClient
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.UserInfo
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import com.google.firebase.vertexai.Chat
import com.google.firebase.vertexai.GenerativeModel
import com.google.firebase.vertexai.type.Content
import com.google.firebase.vertexai.type.GenerateContentResponse
import com.google.firebase.vertexai.type.asTextOrNull
import com.google.firebase.vertexai.type.content
import com.google.gson.Gson
import com.google.protobuf.Value
import com.google.protobuf.util.JsonFormat
import io.opentelemetry.api.GlobalOpenTelemetry
import io.opentelemetry.api.trace.Span
import io.opentelemetry.api.trace.StatusCode
import io.opentelemetry.api.trace.Tracer
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
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.util.UUID

private const val IMAGE_PROMPT_TEMPLATE =
    "As a professional photographer specializing in 100mm Macro lens natural lighting food photography, create a photorealistic, colorful, visually appealing image of a single serving for the following recipe: "

class ChatViewModel(
    chatGenerativeModel: GenerativeModel,
    jsonGenerativeModel: GenerativeModel,
    predictionServiceClient: PredictionServiceClient?,
    location: String,
    application: Application,
    userSessionService: UserSessionService
) : AndroidViewModel(application) {
    private val _recipeRepositoryImpl = RecipeRepositoryImpl()
    private val _projectId = FirebaseApp.getInstance().options.projectId

    private val _imagenEndpointName =
        EndpointName.ofProjectLocationPublisherModelName(
            _projectId, location, "google", "imagen-4.0-generate-001"
        )
    private val _predictionServiceClient = predictionServiceClient
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

    init {
        viewModelScope.launch {
            _isLoading.value = true
            _currentUser = _userSessionService.currentUser.first()
            _chatHistoryPersistenceImpl = ChatHistoryRepositoryImpl(_currentUser!!.uid)
            _chatHistory.value = initializeChatHistory()
            chat = chatGenerativeModel.startChat(history = _chatHistory.value)
            _isLoading.value = false
            updateUiStateMessages(_chatHistory.value)
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
                    try {
                        val imageUrl = createImageForRecipeAsync(recipe.toString())
                        val currentRecipes = _uiState.value.messages
                            .firstOrNull { it.id == messageId }?.recipes ?: return@async
                        val updatedRecipes = currentRecipes.map { r ->
                            if (r.id == recipe.id) r.copyOf(imageUrl = imageUrl) else r
                        }
                        _uiState.value.updateMessageRecipes(messageId, updatedRecipes)
                    } catch (e: Exception) {
                        Log.e("ChatViewModel", "Image generation failed for ${recipe.title}", e)
                    }
                }
            }.awaitAll()
        }
    }

    private suspend fun createImageForRecipeAsync(recipe: String): String {
        val gcsUri = withContext(Dispatchers.IO) { generateImageInstrumented(recipe) }
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

    private fun generateImageInstrumented(recipe: String): String {
        val gson = Gson()
        val prompt = IMAGE_PROMPT_TEMPLATE + recipe
        val span = getTracer().spanBuilder("generateImage")
            .setAttribute("operation.name", "generateImage")
            .setAttribute("llm.model_name", "vertexai/imagen4")
            .setAttribute("llm.input_messages.0.message.role", "model")
            .setAttribute("llm.input_messages.0.message.content", prompt)
            .startSpan()

        try {
            io.opentelemetry.context.Context.current().with(span).makeCurrent().use {
                val instancesJson = gson.toJson(mapOf("prompt" to prompt))
                val instances = jsonToValue(instancesJson)

                val paramsJson = gson.toJson(
                    mapOf(
                        "sampleCount" to 1,
                        "aspectRatio" to "4:3",
                        "storageUri" to "gs://$_projectId.firebasestorage.app/recipes",
                        "outputOptions" to mapOf("mimeType" to "image/jpeg")
                    )
                )

                val parameters = jsonToValue(paramsJson)
                span.setAttribute("llm.invocation_parameters", paramsJson)

                val predictRequest = PredictRequest.newBuilder()
                    .setEndpoint(_imagenEndpointName.toString())
                    .addAllInstances(listOf(instances))
                    .setParameters(parameters)
                    .build()

                val response = _predictionServiceClient!!.predict(predictRequest)
                val gcsUri = response.predictionsList[0].structValue.getFieldsOrThrow("gcsUri").stringValue

                span.setAttribute("llm.output_messages.0.message.role", "model")
                span.setAttribute("llm.output_messages.0.message.content", gcsUri)

                return gcsUri
            }
        } catch (e: Exception) {
            span.recordException(e)
            span.setStatus(StatusCode.ERROR)
            throw e
        } finally {
            span.end()
        }
    }

    private fun jsonToValue(json: String): Value {
        val builder = Value.newBuilder()
        JsonFormat.parser().merge(json, builder)
        return builder.build()
    }
}
