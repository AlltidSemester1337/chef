package com.formulae.chef.services.persistence

import android.content.Context
import android.content.SharedPreferences
import com.formulae.chef.util.json.ContentInstanceCreator
import com.formulae.chef.util.json.PartInstanceCreator
import com.google.common.reflect.TypeToken
import com.google.firebase.vertexai.type.Content
import com.google.firebase.vertexai.type.Part
import com.google.gson.GsonBuilder

private const val PREFS_NAME = "chat_prefs"
private const val KEY_CHAT_HISTORY = "chat_history"
private val gson = GsonBuilder()
    .registerTypeAdapter(Content::class.java, ContentInstanceCreator())
    .registerTypeAdapter(Part::class.java, PartInstanceCreator())
    .create()

class ChatHistoryRepositoryFileImpl(context: Context) : ChatHistoryRepository {
    private val _context = context

    //override fun saveChatHistory(history: List<Content>) {
    //    val sharedPreferences: SharedPreferences = _context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    //    val editor = sharedPreferences.edit()
    //    val type = object : TypeToken<List<Content>>() {}.type
    //    val historyJson = gson.toJson(history, type)
    //    editor.putString(KEY_CHAT_HISTORY, historyJson)
    //    editor.apply()
    //}

    override fun saveNewEntries(newEntries: List<Content>) {
        TODO("Not yet implemented")
    }

    override suspend fun loadChatHistory(): List<Content> {
        val sharedPreferences: SharedPreferences = _context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val historyJson = sharedPreferences.getString(KEY_CHAT_HISTORY, null)
        return if (historyJson != null) {
            val type = object : TypeToken<List<Content>>() {}.type
            val result: List<Content> = gson.fromJson(historyJson, type)
            result
        } else {
            emptyList()
        }
    }

}