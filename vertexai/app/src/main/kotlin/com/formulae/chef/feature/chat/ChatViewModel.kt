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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.time.ZoneOffset
import java.time.ZonedDateTime

private const val IMAGE_PROMPT_TEMPLATE =
    "As a professional photographer specializing in 100mm Macro lens natural lightning food photography, please create a photorealistic, colorful, visually appealing image for use in a recipe collection webpage of a single serving for the following recipe: "

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
            _projectId, location, "google", "imagen-3.0-generate-002"
        )
    private val _predictionServiceClient = predictionServiceClient
    private val _userSessionService = userSessionService
    private val _jsonGenerativeModel = jsonGenerativeModel

    private val _chatHistory: MutableStateFlow<List<Content>> = MutableStateFlow(emptyList())
    private val _uiState: MutableStateFlow<ChatUiState> =
        MutableStateFlow(
            ChatUiState()
        )

    val uiState: StateFlow<ChatUiState> =
        _uiState.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private lateinit var chat: Chat
    private var _currentUser: UserInfo? = null
    private lateinit var _chatHistoryPersistenceImpl: ChatHistoryRepository

    init {
        viewModelScope.launch {
            _isLoading.value = true
            _currentUser = _userSessionService.currentUser.first()
            _chatHistoryPersistenceImpl = ChatHistoryRepositoryImpl(_currentUser!!.uid)
            _chatHistory.value = initializeChatHistory()
            chat = chatGenerativeModel.startChat(
                history = _chatHistory.value
            )
            _isLoading.value = false
            _chatHistory.collect { history -> updateUiStateMessages(history) }
        }
    }

    private suspend fun initializeChatHistory(): List<Content> {
        var result: List<Content> = emptyList()

        try {
            result = _chatHistoryPersistenceImpl.loadChatHistoryLastTwentyEntries()
        } catch (e: Exception) {
            Log.e("FirebaseDB", "Error fetching chat history", e)
        }

        return result
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
                    updateUiStateMessages(_chatHistory.value)
                    _chatHistoryPersistenceImpl.saveNewEntries(listOf(newUserContent, newModelContent))
                }
            } catch (e: Exception) {
                _uiState.value.replaceLastPendingMessage()
                _uiState.value.addMessage(
                    ChatMessage(
                        text = e.localizedMessage,
                        participant = Participant.ERROR
                    )
                )
            }
        }
    }

    private suspend fun generateModelResponseInstrumented(
        prompt: String,
        responseFunction: suspend (String) -> GenerateContentResponse,
        spanName: String
    ): GenerateContentResponse {
        // Obtain the global OpenTelemetry tracer
        val tracer: Tracer = getTracer()

        // Start a new span
        val span: Span = tracer.spanBuilder(spanName)
            .setAttribute("operation.name", "generateChatModelResponse")
            .setAttribute("llm.model_name", "gemini-2.0-flash")
            .setAttribute("llm.input_messages.0.message.role", "user")
            .setAttribute("llm.input_messages.0.message.content", prompt)
            .startSpan()

        // Initialize the response variable
        var response: GenerateContentResponse? = null

        // Use a try-finally block to ensure the span is ended properly
        try {
            // Set the span in the current context
            io.opentelemetry.context.Context.current().with(span).makeCurrent().use {
                // Call the original method logic
                response = responseFunction(prompt)

                // Add the output as an attribute
                span.setAttribute("llm.output_messages.0.message.role", "model")
                span.setAttribute("llm.output_messages.0.message.content", response?.text ?: "")
            }
        } catch (e: Exception) {
            // Record any exceptions thrown during the method execution
            span.recordException(e)
            span.setStatus(StatusCode.ERROR)
            throw e
        } finally {
            // End the span
            span.end()
        }

        // Return the response
        return response!!
    }

    private fun getTracer(): Tracer {
        return GlobalOpenTelemetry.getTracer("com.formulae.chef")
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
                val newRecipes = withContext(Dispatchers.Default) {
                    deriveRecipesFromMessage(message.text)
                }

                if (_currentUser != null) {
                    newRecipes.forEach { it.uid = _currentUser!!.uid }
                }

                newRecipes.forEach { it.isFavourite = true }

                withContext(Dispatchers.IO) {
                    newRecipes.forEach(_recipeRepositoryImpl::saveRecipe)
                }
                // Update UI
                _uiState.value.updateStarredMessage(message, isStarred = true)

                // Show toast
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
        val recipesJsonText = generateModelResponseInstrumented(
            prompt = messageText,
            responseFunction = { prompt ->
                _jsonGenerativeModel.generateContent(content { text(prompt) })
            },
            spanName = "generateJsonModelResponse"
        ).text!!
        val gson = Gson()
        val recipes = gson.fromJson(recipesJsonText.replace("```json", "").replace("```", ""), Recipes::class.java)
        return recipes.recipes.map(this::createRecipeFromJson)
    }

    private fun createRecipeFromJson(recipe: Recipe): Recipe {
        val title = recipe.title.replace("##", "").trim()
        val summary = recipe.summary.replace("##", "").trim()
        val ingredients = recipe.ingredients
        val instructions = recipe.instructions

        if (title.isEmpty() || summary.isEmpty() || ingredients.isEmpty() || instructions.isEmpty()) {
            throw Exception("Failed to derive details from recipe: $recipe")
        }
        var imageUrl: String? = null
        try {
            imageUrl = createImageForRecipe(recipe.toString())
        } catch (e: Exception) {
            Log.e("FirebaseSave", "Failed to generate image for recipe", e)
        }

        val updatedAt = ZonedDateTime.now(ZoneOffset.UTC).toString()
        return Recipe(
            title = title,
            summary = summary,
            ingredients = ingredients,
            instructions = instructions,
            imageUrl = imageUrl,
            updatedAt = updatedAt
        )
    }


    private fun createImageForRecipe(recipe: String): String {
        val gcsUri = generateImageInstrumented(recipe)
        val storagePath = gcsUri.removePrefix("gs://$_projectId.firebasestorage.app/")
        val downloadUrl = runBlocking {
            Firebase.storage("gs://$_projectId.firebasestorage.app/").reference.child(storagePath).downloadUrl.await()
        }
        return "https://" + downloadUrl.host + downloadUrl.encodedPath + "?alt=media"
    }

    private fun generateImageInstrumented(recipe: String): String {
        val gson = Gson()
        val prompt = IMAGE_PROMPT_TEMPLATE + recipe
        val span = getTracer().spanBuilder("generateImage")
            .setAttribute("operation.name", "generateImage")
            .setAttribute("llm.model_name", "vertexai/imagen3")
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
                span.setAttribute("llm.invocation_parameters", paramsJson) // Capture parameters

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

    // Converts JSON string to Protobuf Value
    private fun jsonToValue(json: String): Value {
        val builder = Value.newBuilder()
        JsonFormat.parser().merge(json, builder)
        return builder.build()
    }


}
