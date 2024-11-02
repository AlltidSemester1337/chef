package com.formulae.chef.services.persistence

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.google.common.reflect.TypeToken
import com.google.firebase.vertexai.type.Content
import com.google.firebase.vertexai.type.Part
import com.google.firebase.vertexai.type.TextPart
import com.google.firebase.vertexai.type.asTextOrNull
import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import java.lang.reflect.Type
import com.google.gson.GsonBuilder

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

private const val PREFS_NAME = "chat_prefs"
private const val KEY_CHAT_HISTORY = "chat_history"
private val gson = GsonBuilder()
    .registerTypeAdapter(Content::class.java, ContentInstanceCreator())
    .registerTypeAdapter(Part::class.java, PartInstanceCreator())
    .create()

class ChatHistoryFilePersistence(context: Context) : ChatHistoryPersistence {
    private val _context = context

    override fun saveChatHistory(history: List<Content>) {
        val sharedPreferences: SharedPreferences = _context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        val type = object : TypeToken<List<Content>>() {}.type
        val historyJson = gson.toJson(history, type)
        Log.d("ChatViewModel", "save history: ${history.reversed().first().parts.first().asTextOrNull()}")
        Log.d("ChatViewModel", "save historyJson: ${historyJson}")
        editor.putString(KEY_CHAT_HISTORY, historyJson)
        editor.apply()
    }

    override fun loadChatHistory(): List<Content> {
        val sharedPreferences: SharedPreferences = _context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val historyJson = sharedPreferences.getString(KEY_CHAT_HISTORY, null)
        return if (historyJson != null) {
            val type = object : TypeToken<List<Content>>() {}.type
            val result: List<Content> = gson.fromJson(historyJson, type)
            Log.d("ChatViewModel", "load historyJson: ${historyJson}")
            Log.d("ChatViewModel", "result: ${result}")
            Log.d("ChatViewModel", "result first element: ${result.first().parts.first().asTextOrNull()}")
            result
        } else {
            emptyList()
        }
    }
}