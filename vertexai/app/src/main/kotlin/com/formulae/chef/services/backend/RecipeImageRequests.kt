package com.formulae.chef.services.backend

import org.json.JSONObject

/** Pure request/response shaping for `POST /v1/images/recipe`, kept free of I/O so it is unit tested. */
object RecipeImageRequests {

    /** The backend rejects longer recipe texts with 422. */
    const val MAX_RECIPE_LENGTH = 8_000

    /**
     * Builds `{"recipe": ...}` from a plain-text recipe, trimmed and cut to [MAX_RECIPE_LENGTH].
     * No other fields: the backend owns prompt and model, and rejects unknown fields with 422.
     *
     * @throws IllegalArgumentException for a blank recipe (the backend would reject it with 422).
     */
    fun buildRequestBody(recipeText: String): String {
        val recipe = recipeText.trim().take(MAX_RECIPE_LENGTH)
        require(recipe.isNotEmpty()) { "Recipe text is blank" }
        return JSONObject().put("recipe", recipe).toString()
    }

    /** Extracts `imageUrl` from a 200 response body `{"imageUrl": "https://..."}`. */
    fun parseResponse(responseBody: String): String = JSONObject(responseBody).getString("imageUrl")
}
