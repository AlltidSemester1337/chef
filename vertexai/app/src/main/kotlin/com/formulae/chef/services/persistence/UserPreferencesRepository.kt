package com.formulae.chef.services.persistence

import com.formulae.chef.feature.model.UserPreferences

interface UserPreferencesRepository {
    val uid: String

    suspend fun loadPreferences(): UserPreferences?

    suspend fun savePreferences(preferences: UserPreferences)
}
