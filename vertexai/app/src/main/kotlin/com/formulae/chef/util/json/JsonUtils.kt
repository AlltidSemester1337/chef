package com.formulae.chef.util.json

import com.google.firebase.vertexai.type.Content
import com.google.firebase.vertexai.type.Part
import com.google.firebase.vertexai.type.TextPart
import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import java.lang.reflect.Type

class ContentInstanceCreator : JsonDeserializer<Content> {
    override fun deserialize(json: JsonElement, typeOfT: Type, context: JsonDeserializationContext): Content {
        val jsonObject = json.asJsonObject
        val parts = jsonObject.getAsJsonArray("parts").map { context.deserialize<Part>(it, Part::class.java) }

        return Content(role = jsonObject.getAsJsonPrimitive("role").asString, parts = parts)
    }
}

class PartInstanceCreator : JsonDeserializer<Part> {
    override fun deserialize(json: JsonElement, typeOfT: Type, context: JsonDeserializationContext): Part {
        val jsonObject = json.asJsonObject
        val text = jsonObject.get("text").asString
        return TextPart(text)
    }
}