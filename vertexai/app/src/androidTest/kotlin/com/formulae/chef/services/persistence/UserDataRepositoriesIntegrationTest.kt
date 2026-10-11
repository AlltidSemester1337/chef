package com.formulae.chef.services.persistence

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.formulae.chef.feature.model.CookingResource
import com.formulae.chef.feature.model.UserPreferences
import com.formulae.chef.services.persistence.FirestoreEmulator.signInAsNewUser
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Repositories that share the `users/{uid}` document (preferences, cooking resources, beta quota),
 * against the Firestore emulator with production rules. See [FirestoreEmulator].
 */
@RunWith(AndroidJUnit4::class)
class UserDataRepositoriesIntegrationTest {
    private val firestore get() = FirestoreEmulator.firestore

    @Test
    fun preferences_roundTrip() = runBlocking {
        val prefs = UserPreferencesRepositoryImpl(uid = signInAsNewUser(), firestore = firestore)
        assertNull(prefs.loadPreferences())

        prefs.savePreferences(UserPreferences(summary = "Gillar chili", updatedAt = "2026-10-08T07:24:00Z"))

        val loaded = prefs.loadPreferences()!!
        assertEquals("Gillar chili", loaded.summary)
        assertEquals("2026-10-08T07:24:00Z", loaded.updatedAt)
    }

    @Test
    fun cookingResources_roundTrip() = runBlocking {
        val repository = CookingResourcesRepositoryImpl(uid = signInAsNewUser(), firestore = firestore)
        assertNull(repository.load())

        val resource = CookingResource(title = "Serious Eats", url = "https://x", type = "site", description = "d")
        repository.save(CachedCookingResources(resources = listOf(resource), updatedAt = "2026-10-08T07:24:00Z"))

        val loaded = repository.load()!!
        assertEquals(listOf(resource), loaded.resources)
        assertEquals("2026-10-08T07:24:00Z", loaded.updatedAt)
    }

    // Regression: both live as map fields on users/{uid}. A non-merge set() of one would drop the
    // other, and the server-owned betaInteractionCount with it (which the rules reject).
    @Test
    fun savingPreferencesAndResources_keepsTheOtherField() = runBlocking {
        val uid = signInAsNewUser()
        val preferences = UserPreferencesRepositoryImpl(uid = uid, firestore = firestore)
        val resources = CookingResourcesRepositoryImpl(uid = uid, firestore = firestore)

        preferences.savePreferences(UserPreferences(summary = "x", updatedAt = "2026-10-08T07:24:00Z"))
        resources.save(CachedCookingResources(resources = emptyList(), updatedAt = "2026-10-08T07:24:00Z"))

        assertEquals("x", preferences.loadPreferences()!!.summary)
        assertEquals("2026-10-08T07:24:00Z", resources.load()!!.updatedAt)
    }
}
