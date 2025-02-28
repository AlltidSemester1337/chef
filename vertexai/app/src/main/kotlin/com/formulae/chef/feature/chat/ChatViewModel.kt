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
import com.formulae.chef.services.persistence.ChatHistoryRepositoryImpl
import com.formulae.chef.services.persistence.RecipeRepositoryImpl
import com.google.cloud.aiplatform.v1.EndpointName
import com.google.cloud.aiplatform.v1.PredictRequest
import com.google.cloud.aiplatform.v1.PredictionServiceClient
import com.google.firebase.FirebaseApp
import com.google.firebase.vertexai.Chat
import com.google.firebase.vertexai.GenerativeModel
import com.google.firebase.vertexai.type.Content
import com.google.firebase.vertexai.type.asTextOrNull
import com.google.firebase.vertexai.type.content
import com.google.gson.Gson
import com.google.protobuf.Value
import com.google.protobuf.util.JsonFormat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ChatViewModel(
    generativeModel: GenerativeModel,
    predictionServiceClient: PredictionServiceClient?,
    location: String,
    application: Application
) : AndroidViewModel(application) {
    private val _chatHistoryPersistenceImpl = ChatHistoryRepositoryImpl()
    private val _recipeRepositoryImpl = RecipeRepositoryImpl()
    private val _projectId = FirebaseApp.getInstance().options.projectId

    private val _imagenEndpointName =
        EndpointName.ofProjectLocationPublisherModelName(
            _projectId, location, "google", "imagen-3.0-generate-002"
        )
    private val _predictionServiceClient = predictionServiceClient

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

    init {
        viewModelScope.launch {
            _isLoading.value = true
            _chatHistory.value = initializeChatHistory()
            chat = generativeModel.startChat(
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
                val response = chat.sendMessage(userMessage)

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

    fun onRecipeStarred(message: ChatMessage) {
        val context: Context = getApplication<Application>().applicationContext
        if (message.isStarred) {
            removeRecipe(context, message)
        } else {
            saveRecipe(context, message)
        }
    }

    fun removeRecipe(context: Context, message: ChatMessage) {
        viewModelScope.launch {
            try {
                // TODO Implement search/delete by title
                //_persistenceImpl.deleteRecipe(recipeData)
                Log.d("CHEF", "CHEF:::::RECIPE_DELETED")
                // Update UI
                _uiState.value.updateStarredMessage(message, isStarred = false)

                // Show toast
                //Toast.makeText(
                //    context,
                //    "Recipe removed from collection successfully.",
                //    Toast.LENGTH_SHORT
                //).show()

            } catch (e: Exception) {
                Log.e("FirebaseDelete", "Failed to remove recipe", e)
                Toast.makeText(
                    context,
                    "Failed to remove recipe: ${e.localizedMessage}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    fun saveRecipe(context: Context, message: ChatMessage) {
        viewModelScope.launch {
            try {
                val newRecipe = withContext(Dispatchers.Default) {
                    deriveRecipeFromMessage(message.text)
                }

                withContext(Dispatchers.IO) {
                    _recipeRepositoryImpl.saveRecipe(newRecipe)
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

    private fun deriveRecipeFromMessage(answer: String): Recipe {
        val title = deriveRecipeTitle(answer)
        val summary = deriveRecipeSummary(answer, title = title)
        val ingredients = deriveRecipeIngredients(answer, title = title, summary = summary)
        val instructions = deriveRecipeInstructions(answer)

        if (title.isEmpty() || summary.isEmpty() || ingredients.isEmpty() || instructions.isEmpty()) {
            throw Exception("Failed to derive details from recipe: $answer")
        }
        var imageUrl: String? = null
        try {
            imageUrl = createImageForRecipe(answer)
        } catch (e: Exception) {
            Log.e("FirebaseSave", "Failed to generate image for recipe", e)
        }

        val updatedAt = java.time.ZonedDateTime.now(java.time.ZoneOffset.UTC).toString()
        return Recipe(
            title = title,
            summary = summary,
            ingredients = ingredients,
            instructions = instructions,
            imageUrl = imageUrl,
            updatedAt = updatedAt
        )
    }

    private fun deriveRecipeTitle(answer: String): String {
        val title = answer.split("\n\n")[0].replace("##", "").trim()
        return title
    }

    private fun deriveRecipeSummary(answer: String, title: String): String {
        val summary =
            answer.split("**Ingredients:**")[0].replace(title, "").replace("##", "").trim()
        return summary
    }

    private fun deriveRecipeIngredients(answer: String, title: String, summary: String): String {
        val ingredients =
            answer.split("**Instructions:**")[0].replace(title, "").replace(summary, "").replace("**Ingredients:**", "")
                .replace("##", "").trim()
        return ingredients
    }

    private fun deriveRecipeInstructions(answer: String): String {
        val instructions = answer.split("**Instructions:**")[1].replace("\n\n", "")
        return instructions
    }

    private fun createImageForRecipe(answer: String): String {
        val gson = Gson()
        val prompt =
            "As a professional photographer specializing in 100mm Macro lens natural lightning food photography, please create a photorealistic, colorful, visually appealing image for use in a recipe collection webpage of a single serving for the following recipe: $answer"
        val instancesJson = gson.toJson(mapOf("prompt" to prompt))
        val instances = jsonToValue(instancesJson)

        val paramsJson = gson.toJson(
            mapOf(
                "sampleCount" to 1,
                "aspectRatio" to "4:3",
                "storageUri" to "gs://$_projectId.firebasestorage.app/recipes",
                "outputOptions" to
                        mapOf(
                            "mimeType" to "image/jpeg",
                        )
            )
        )

        val parameters = jsonToValue(paramsJson)

        val predictRequest = PredictRequest.newBuilder()
            .setEndpoint(_imagenEndpointName.toString())
            .addAllInstances(listOf(instances))
            .setParameters(parameters)
            .build()
        val response = _predictionServiceClient!!.predict(predictRequest)
        val gcsUri = response.predictionsList[0].structValue.getFieldsOrThrow("gcsUri").stringValue
        return convertFirebaseStorageUriToPublicUrl(gcsUri)
    }

    // Converts JSON string to Protobuf Value
    fun jsonToValue(json: String): Value {
        val builder = Value.newBuilder()
        JsonFormat.parser().merge(json, builder)
        return builder.build()
    }

    fun convertFirebaseStorageUriToPublicUrl(gcsUri: String): String {
        require(gcsUri.startsWith("gs://")) { "Not a valid GCS URI" }
        val path = gcsUri.removePrefix("gs://").split("/", limit = 2)
        val bucket = path[0]
        val objectPath = path[1].replace("/", "%2F") // URL encode "/"
        return "https://firebasestorage.googleapis.com/v0/b/$bucket/o/$objectPath?alt=media"
    }


}
