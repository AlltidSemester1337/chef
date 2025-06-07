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

package com.formulae.chef

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import com.formulae.chef.feature.chat.ChatViewModel
import com.formulae.chef.services.authentication.UserSessionServiceFirebaseImpl
import com.google.auth.oauth2.GoogleCredentials
import com.google.cloud.aiplatform.v1.PredictionServiceClient
import com.google.cloud.aiplatform.v1.PredictionServiceSettings
import com.google.firebase.Firebase
import com.google.firebase.vertexai.type.content
import com.google.firebase.vertexai.type.generationConfig
import com.google.firebase.vertexai.vertexAI
import org.json.JSONObject
import java.io.InputStream
import java.lang.String
import java.nio.charset.Charset


private const val DERIVE_RECIPE_JSON_SYSTEM_INSTRUCTIONS =
    """If the text content contains one or more recipes, output the following example JSON format:\n" +
            "recipes": [
                {
                    "title": "Chicken Enchiladas with Creamy Chipotle Sauce",
                    "summary": "This recipe elevates classic chicken enchiladas with a smoky chipotle cream sauce and vibrant fresh ingredients, aligning with the Honest Greens ethos of whole foods and healthy cooking.",
                    "servings": "6-8 enchiladas",
                    "prepTime": "25 minutes",
                    "cookingTime": "35 minutes",
                    "nutrientsPerServing": [
                        {
                            "name": "Calories",
                            "quantity": "380",
                            "unit": "kcal"
                        },
                        {
                            "name": "Protein",
                            "quantity": "28",
                            "unit": "g"
                        },
                        {
                            "name": "Carbohydrates",
                            "quantity": "30",
                            "unit": "g"
                        },
                        {
                            "name": "Fat",
                            "quantity": "12",
                            "unit": "g"
                        },
                        {
                            "name": "Fiber",
                            "quantity": "4",
                            "unit": "g"
                        }
                    ],
                    "ingredients": [
                        {
                            "name": "olive oil",
                            "quantity": "30",
                            "unit": "ml"
                        },
                        {
                            "name": "medium onion",
                            "quantity": "150",
                            "unit": "g"
                        },
                        {
                            "name": "garlic",
                            "quantity": "5",
                            "unit": "g"
                        },
                        {
                            "name": "chili powder",
                            "quantity": "1",
                            "unit": "tsp"
                        },
                        {
                            "name": "cumin",
                            "quantity": "1/2",
                            "unit": "tsp"
                        },
                        {
                            "name": "smoked paprika",
                            "quantity": "1/4",
                            "unit": "tsp"
                        },
                        {
                            "name": "salt",
                            "quantity": "1/2",
                            "unit": "tsp"
                        },
                        {
                            "name": "black pepper",
                            "quantity": "1/2",
                            "unit": "tsp"
                        },
                        {
                            "name": "boneless, skinless chicken breasts",
                            "quantity": "500",
                            "unit": "g"
                        },
                        {
                            "name": "diced tomatoes",
                            "quantity": "400",
                            "unit": "g"
                        },
                        {
                            "name": "chicken broth",
                            "quantity": "120",
                            "unit": "ml"
                        },
                        {
                            "name": "light cream",
                            "quantity": "60",
                            "unit": "ml"
                        },
                        {
                            "name": "chipotle peppers",
                            "quantity": "1-2",
                            "unit": "each"
                        },
                        {
                            "name": "tomato sauce",
                            "quantity": "120",
                            "unit": "ml"
                        },
                        {
                            "name": "water",
                            "quantity": "60",
                            "unit": "ml"
                        },
                        {
                            "name": "chipotle pepper",
                            "quantity": "1"
                        },
                        {
                            "name": "adobo sauce",
                            "quantity": "15",
                            "unit": "g"
                        },
                        {
                            "name": "Greek yogurt (or vegan alternative)",
                            "quantity": "200",
                            "unit": "ml"
                        },
                        {
                            "name": "lime juice",
                            "quantity": "1",
                            "unit": "tbsp"
                        },
                        {
                            "name": "corn tortillas",
                            "quantity": "6-8"
                        },
                        {
                            "name": "shredded Monterey Jack cheese (or Mexican cheese blend)",
                            "quantity": "120",
                            "unit": "g"
                        },
                        {
                            "name": "fresh cilantro for garnish"
                        }
                    ],
                    "difficulty": "EASY",
                    "instructions": [
                        "Prepare the Chicken Filling:** Heat 15 ml olive oil in a large skillet over medium heat. Add 2 medium onions and 5g garlic and cook until softened, about 5 minutes. Stir in 1 tsp chili powder, 1/2 tsp cumin, 1/4 tsp smoked paprika, 1/4 tsp salt, and 1/4 tsp pepper. Cook for 1 minute more.",
                        "Add 500 g shredded chicken, 400 g diced tomatoes, 120 ml chicken broth, 60 ml light cream, and 1 minced chipotle pepper to the skillet. Bring to a simmer and cook for 5-7 minutes, stirring occasionally, until the sauce has thickened slightly.",
                        "Prepare the Creamy Chipotle Sauce:** In a small bowl, whisk together 120 ml tomato sauce, 60 ml water, 15 ml olive oil, 1 minced chipotle pepper, 15 g adobo sauce, 200 ml Greek yogurt, 1 tbsp lime juice, 1/4 tsp salt, and 1/4 tsp pepper. Taste and adjust seasoning as needed. You might prefer more chipotle for a spicier kick.",
                        "Assemble the Enchiladas:** Preheat oven to 180°C. Warm 6-8 corn tortillas slightly (either in a dry skillet or microwave) to make them more pliable. Fill each tortilla with about 2 tablespoons of the chicken filling, sprinkle with 120 g cheese, and roll up tightly.",
                        "Place the enchiladas seam-down in a lightly greased 9x13 inch baking dish. Pour the creamy chipotle sauce evenly over the enchiladas, ensuring they are fully coated.",
                        "Bake for 20-25 minutes, or until the cheese is melted and bubbly, and the sauce is heated through.",
                        "Garnish with fresh cilantro and serve immediately."
                    ],
                    "tipsAndTricks": "Optional additions: Serve with a dollop of plain Greek yogurt or a side of fresh salsa."
                }
            ]
            "If any of the fields are not applicable, do not add them to the JSON output."""

val GenerativeViewModelFactory = object : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(
        viewModelClass: Class<T>,
        extras: CreationExtras
    ): T {
        val config = generationConfig {
            temperature = 2f
            maxOutputTokens = 8192
            topP = 0.95f
        }

        val application = extras[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as Application

        return with(viewModelClass) {
            when {
                isAssignableFrom(ChatViewModel::class.java) -> {
                    // Initialize a GenerativeModel with the `gemini-flash` AI model for chat
                    val chatGenerativeModel = Firebase.vertexAI.generativeModel(
                        modelName = "gemini-1.5-flash-002",
                        generationConfig = config,
                        systemInstruction = content { text(BuildConfig.chefMainChatPromptTemplate) }
                    )

                    val jsonGenerativeModel = Firebase.vertexAI.generativeModel(
                        modelName = "gemini-2.0-flash-lite-001",
                        generationConfig = config,
                        systemInstruction = content { text(DERIVE_RECIPE_JSON_SYSTEM_INSTRUCTIONS) }
                    )


                    val settingsStream: InputStream =
                        application.applicationContext.assets.open("gcp.json")
                    val configString = settingsStream.bufferedReader(Charset.defaultCharset()).use { it.readText() }
                    val location = JSONObject(configString).getString("location")

                    val credentialsStream: InputStream =
                        application.applicationContext.assets.open("imagen-google-services.json")
                    val credentials = GoogleCredentials.fromStream(credentialsStream)
                        .createScoped(listOf("https://www.googleapis.com/auth/cloud-platform"))

                    val endpoint = String.format("$location-aiplatform.googleapis.com:443")

                    val predictionServiceSettings: PredictionServiceSettings =
                        PredictionServiceSettings.newBuilder()
                            .setCredentialsProvider { credentials }
                            .setEndpoint(endpoint)
                            .build()

                    val predictionServiceClient = PredictionServiceClient.create(predictionServiceSettings)

                    val userSessionService = UserSessionServiceFirebaseImpl()

                    ChatViewModel(
                        chatGenerativeModel = chatGenerativeModel,
                        jsonGenerativeModel = jsonGenerativeModel,
                        predictionServiceClient = predictionServiceClient,
                        location = location,
                        application = application,
                        userSessionService = userSessionService
                    )
                }

                else ->
                    throw IllegalArgumentException("Unknown ViewModel class: ${viewModelClass.name}")
            }
        } as T
    }
}
