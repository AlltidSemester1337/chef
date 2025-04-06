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
    "If the text content contains one or more recipes, output the following example JSON format:\n" +
            "\"recipes\": [{   \"title\": \"Chicken Enchiladas with Creamy Chipotle Sauce\",   \"summary\": \"\\n\\nThis recipe elevates classic chicken enchiladas with a smoky chipotle cream sauce and vibrant fresh ingredients, aligning with the Honest Greens ethos of whole foods and healthy cooking.\\n\\n**Yields:** 6-8 enchiladas\\n**Prep time:** 25 minutes\\n**Cook time:** 35 minutes\\n\\n\\n**Nutritional Information per serving (approximate):** *These values are estimates and will vary based on specific ingredients used.*\\n\\n* Calories: 380 kcal\\n* Protein: 28g\\n* Carbohydrates: 30g\\n* Fat: 12g\\n* Fiber: 4g\\n\\n\\n\",   \"ingredients\": \"**For the Chicken Filling:**\\n\\n* 15ml olive oil\\n* 1 medium onion (150g), finely chopped\\n* 2 cloves garlic (5g), minced\\n* 1 tsp chili powder\\n* 1/2 tsp cumin\\n* 1/4 tsp smoked paprika\\n* 1/4 tsp salt\\n* 1/4 tsp black pepper\\n* 500g boneless, skinless chicken breasts, cooked and shredded (you can use rotisserie chicken for convenience)\\n* 400g can diced tomatoes, undrained\\n* 120ml chicken broth (low sodium preferred)\\n* 60ml light cream (or full-fat coconut milk for a richer, vegan option)\\n* 1-2 chipotle peppers in adobo sauce, finely minced (adjust to your spice preference)\\n\\n\\n**For the Creamy Chipotle Sauce:**\\n\\n* 120ml tomato sauce (passata or homemade for better flavor)\\n* 60ml water\\n* 1 tbsp (15ml) olive oil\\n* 1 chipotle pepper in adobo sauce, finely minced (or more, to taste)\\n* 1 tbsp (15g) adobo sauce from the chipotle can\\n* 200ml Greek yogurt (full-fat for creaminess) or vegan alternative\\n* 1 tbsp lime juice\\n* 1/4 tsp salt\\n* 1/4 tsp black pepper\\n\\n\\n**For Assembling:**\\n\\n* 6-8 corn tortillas (choose low sodium options where possible)\\n* 120g shredded Monterey Jack cheese (or a blend of Mexican cheeses)\\n* Fresh cilantro, chopped (for garnish)\\n\\n\\n\",\n" +
            "   \"instructions\": \"1. **Prepare the Chicken Filling:** Heat olive oil in a large skillet over medium heat. Add onion and garlic and cook until softened, about 5 minutes. Stir in chili powder, cumin, smoked paprika, salt, and pepper. Cook for 1 minute more.2. Add shredded chicken, diced tomatoes, chicken broth, light cream, and minced chipotle peppers to the skillet. Bring to a simmer and cook for 5-7 minutes, stirring occasionally, until the sauce has thickened slightly.3. **Prepare the Creamy Chipotle Sauce:** In a small bowl, whisk together tomato sauce, water, olive oil, minced chipotle pepper, adobo sauce, Greek yogurt, lime juice, salt, and pepper. Taste and adjust seasoning as needed. You might prefer more chipotle for a spicier kick.4. **Assemble the Enchiladas:** Preheat oven to 180\\\\u00b0C. Warm tortillas slightly (either in a dry skillet or microwave) to make them more pliable. Fill each tortilla with about 2 tablespoons of the chicken filling, sprinkle with cheese, and roll up tightly.5. Place the enchiladas seam-down in a lightly greased 9x13 inch baking dish. Pour the creamy chipotle sauce evenly over the enchiladas, ensuring they are fully coated.6. Bake for 20-25 minutes, or until the cheese is melted and bubbly, and the sauce is heated through.7. Garnish with fresh cilantro and serve immediately. Optional additions: Serve with a dollop of plain Greek yogurt or a side of fresh salsa.\\n**Tips & Variations:*** For a vegetarian version, substitute the chicken with 200g cooked black beans or a mixture of cooked vegetables like bell peppers, zucchini, and mushrooms.\\n* Add a sprinkle of cotija cheese for a tangy topping.\\n* Adjust the amount of chipotle peppers to control the spice level. Start with less and add more to taste.\\n* For a gluten-free option, use corn tortillas or gluten-free wraps.Enjoy your delicious and healthy Chicken Enchiladas with Creamy Chipotle Sauce!\\n\",\n" +
            "  }] \n" +
            "Please include notes and variations at the end of instruction steps in the same property as well as nutritional values at the end in the same property as summary."

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
