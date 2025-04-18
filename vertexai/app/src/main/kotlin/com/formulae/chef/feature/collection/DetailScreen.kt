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

package com.formulae.chef.feature.collection

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import com.formulae.chef.feature.model.Recipe

@Composable
internal fun DetailRoute(
    collectionViewModel: CollectionViewModel,
    recipe: Recipe,
    navController: NavController
) {

    val onToggleCookingModeClick = collectionViewModel::onToggleCookingMode

    BackHandler {
        navController.navigate("collection")
    }

    CreateDetailScreen(
        recipe,
        onToggleCookingModeClick,
    )
}

@Composable
private fun CreateDetailScreen(
    recipe: Recipe,
    onToggleCookingModeClick: () -> Unit,
) {
    var showIngredients by remember { mutableStateOf(true) }
    val scrollState = rememberScrollState()
    val hasImage = recipe.imageUrl?.isNotEmpty() ?: false
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState),
    ) {

        Text(text = recipe.title, style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(8.dp))

        // Conditional Display
        if (hasImage) {
            Image(
                painter = rememberAsyncImagePainter(recipe.imageUrl),
                contentDescription = "Recipe Image",
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .clip(RoundedCornerShape(8.dp))
            )
            //Button(
            //    onClick = onToggleCookingModeClick,
            //    modifier = Modifier
            //        .fillMaxWidth()
            //) {
            //    Text("Start cooking!")
            //}
        }

        Text(text = recipe.summary.replace("\\n", "\n"), style = MaterialTheme.typography.bodyMedium)
        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Button(
                onClick = { showIngredients = true },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (showIngredients) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface
                )
            ) {
                Text("Ingredients")
            }

            Button(
                onClick = { showIngredients = false },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (!showIngredients) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface
                )
            ) {
                Text("Instructions")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Conditional Display
        if (showIngredients) {
            Text(text = "Ingredients", style = MaterialTheme.typography.headlineSmall)
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = recipe.ingredients.replace("\\n", "\n"), style = MaterialTheme.typography.bodyMedium)
            Spacer(modifier = Modifier.height(8.dp))
        } else {
            Text(text = "Instructions", style = MaterialTheme.typography.headlineSmall)
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = recipe.instructions.replace("\\n", "\n"), style = MaterialTheme.typography.bodyMedium)
        }

        // Centered Share Button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 30.dp),
            horizontalArrangement = Arrangement.Center
        ) {
            Button(
                onClick = {
                    clipboardManager.setText(AnnotatedString("https://humlekotte.nu/chef-web/recipe/?id=${recipe.id}"))
                    Toast.makeText(
                        context,
                        "Recipe URL copied to clipboard",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            ) {
                Text("Share Recipe")
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewCreateDetailScreen() {
    CreateDetailScreen(
        Recipe(
            title = "West African Peanut Stew (Peanut Butter Stew)",
            summary = "This recipe features flavorful Lebanese-style kafta kebabs, cooked to juicy perfection, served with a vibrant harissa yogurt sauce and a medley of roasted vegetables.\\n\\n**Yields:** 4 servings\\n\\n**Nutritional Information per serving (approximate):**\\n\\n* Calories: 550 kcal\\n* Protein: 30g\\n* Carbohydrates: 40g\\n* Fat: 25g\\n\\n\\n", // Shortened for readability
            ingredients = "**Ingredients:**\\n\\n**For the Kafta Kebabs:**\\n\\n* 500g ground lamb or a mix of ground lamb and beef\\n* 1 large onion, finely chopped\\n* 2 cloves garlic, minced\\n* 1/2 cup (20g)  packed fresh parsley, finely chopped\\n* 1/4 cup (10g) packed fresh mint, finely chopped\\n* 1 tbsp (15g) ground cumin\\n* 1 tsp (5g) ground coriander\\n* 1/2 tsp (2.5g) allspice\\n* 1/4 tsp cayenne pepper (optional, adjust to taste)\\n* Salt and pepper to taste\\n\\n**For the Harissa Yogurt Sauce:**\\n\\n* 250g plain Greek yogurt\\n* 1 tbsp (15g) harissa paste (adjust to taste)\\n* 1 tbsp (15ml) lemon juice\\n* 1/4 tsp salt\\n\\n**For the Roasted Vegetables:**\\n\\n* 1 large sweet potato, peeled and cubed\\n* 1 red bell pepper, chopped\\n* 1/2 cup broccoli florets\\n* 1 tbsp olive oil\\n* 1/2 tsp salt\\n* 1/4 tsp black pepper\\n\\n\\n",
            instructions = "1. **Prepare the Kafta:** In a large bowl, combine the ground lamb (or mix), onion, garlic, parsley, mint, cumin, coriander, allspice, cayenne pepper (if using), salt, and pepper. Gently mix with your hands until well combined. Do not overmix.\\n2. **Shape the Kebabs:** Divide the mixture into 4 equal portions. Shape each portion into a long kebab, about 10-15cm in length.  Alternatively, make small meatballs and skip the kebab step. \\n3. **Prepare the Harissa Yogurt Sauce:** In a bowl, combine the yogurt, harissa paste, lemon juice, and salt. Mix well.\\n4. **Prepare the Vegetables:** Preheat oven to 200\\u00b0C (400\\u00b0F). Toss the sweet potato, bell pepper, and broccoli with olive oil, salt, and pepper.  Spread in a single layer on a baking sheet lined with parchment paper. \\n5. **Roast the Vegetables:** Bake the vegetables for 20-25 minutes, or until tender and slightly caramelized. \\n6. **Cook the Kebabs:**  While the vegetables are roasting, heat a grill pan or skillet over medium-high heat. Add the kebabs to the pan and cook for 4-5 minutes per side, or until browned and cooked through. (Cooking time will vary depending on thickness).  Alternatively, grill them outdoors on the BBQ or cook in the oven (if small meatballs have been prepared).\\n7. **Serve:** Serve the kebabs with the harissa yogurt sauce and the roasted vegetables.\\n**Tips:***  For a milder flavour, reduce or omit the cayenne pepper. \\n* Feel free to use other vegetables for roasting, such as zucchini, carrots, or Brussels sprouts.\\n*  Serve the kebabs with pita bread, hummus, or tabbouleh for a complete Lebanese-style meal.Enjoy your delicious and flavorful Lebanese Kafta Kebabs!\\n",
            imageUrl = "https://storage.googleapis.com/idyllic-bloom-425307-r6.firebasestorage.app/recipes/71204b99-36e5-419d-8fed-8fba949bd3d4"
        ),
        onToggleCookingModeClick = {}
    )

}


