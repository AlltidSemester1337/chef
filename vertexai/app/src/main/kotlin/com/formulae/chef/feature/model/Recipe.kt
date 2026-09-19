package com.formulae.chef.feature.model

import com.google.firebase.database.PropertyName
import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.annotations.JsonAdapter
import java.lang.reflect.Type

data class Recipe(
    var id: String? = null,
    var uid: String = "",
    var title: String = "",
    var summary: String = "",
    var servings: String? = "",
    var prepTime: String? = null,
    var cookingTime: String? = null,
    var nutrientsPerServing: List<Nutrient>? = listOf(),
    var ingredients: List<Ingredient> = listOf(),
    var difficulty: Difficulty? = Difficulty.EASY,
    var instructions: List<String> = listOf(),
    @JsonAdapter(TipsAndTricksDeserializer::class)
    var tipsAndTricks: String? = null,

    var imageUrl: String? = null,
    var updatedAt: String = "",
    @get:PropertyName("isFavourite")
    @set:PropertyName("isFavourite")
    var isFavourite: Boolean = false,
    var copyId: String? = null,
    var tags: List<String> = emptyList()
) {

    fun copyOf(
        id: String? = this.id,
        uid: String = this.uid,
        title: String = this.title,
        summary: String = this.summary,
        servings: String? = this.servings,
        prepTime: String? = this.prepTime,
        cookingTime: String? = this.cookingTime,
        nutrientsPerServing: List<Nutrient>? = this.nutrientsPerServing,
        ingredients: List<Ingredient> = this.ingredients,
        difficulty: Difficulty? = this.difficulty,
        instructions: List<String> = this.instructions,
        tipsAndTricks: String? = this.tipsAndTricks,
        imageUrl: String? = this.imageUrl,
        updatedAt: String = this.updatedAt,
        isFavourite: Boolean = this.isFavourite,
        copyId: String? = this.copyId,
        tags: List<String> = this.tags
    ): Recipe {
        return Recipe(
            id,
            uid,
            title,
            summary,
            servings,
            prepTime,
            cookingTime,
            nutrientsPerServing,
            ingredients,
            difficulty,
            instructions,
            tipsAndTricks,
            imageUrl,
            updatedAt,
            isFavourite,
            copyId,
            tags
        )
    }
}

/**
 * Some LLM providers return `tipsAndTricks` as a JSON array of tip strings instead of the
 * documented single bullet-formatted string (e.g. Mistral Small 3.2 via Berget.ai). Accept both
 * shapes and normalize to the "- " per-line format [parseTips] expects.
 */
private class TipsAndTricksDeserializer : JsonDeserializer<String?> {
    override fun deserialize(json: JsonElement?, typeOfT: Type?, context: JsonDeserializationContext?): String? {
        if (json == null || json.isJsonNull) return null
        return if (json.isJsonArray) {
            json.asJsonArray.joinToString("\n") { "- ${it.asString.trim()}" }
        } else {
            json.asString
        }
    }
}

data class Ingredient(
    var name: String? = "",
    var quantity: String? = "",
    var unit: String? = ""
)

data class Nutrient(
    var name: String? = "",
    var quantity: String? = "",
    var unit: String? = ""
)

enum class Difficulty {
    EASY, MEDIUM, HARD
}

fun Recipe.parsedServingsCount(): Int? =
    Regex("""\d+""").find(servings ?: "")?.value?.toIntOrNull()
