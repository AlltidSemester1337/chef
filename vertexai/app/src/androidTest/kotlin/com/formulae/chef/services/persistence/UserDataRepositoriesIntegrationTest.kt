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

    @Test
    fun betaQuota_incrementsByOneFromZero() = runBlocking {
        val quota = BetaQuotaRepositoryImpl(uid = signInAsNewUser(), firestore = firestore)
        assertEquals(1, quota.incrementAndGet())
        assertEquals(2, quota.incrementAndGet())
        assertEquals(3, quota.incrementAndGet())
    }

    // Regression: a non-merge set() of preferences/cooking resources would drop betaInteractionCount,
    // which the rules reject — and would reset the quota if they didn't.
    @Test
    fun savingPreferencesAndResources_keepsTheBetaCount() = runBlocking {
        val uid = signInAsNewUser()
        val quota = BetaQuotaRepositoryImpl(uid = uid, firestore = firestore)
        quota.incrementAndGet()
        quota.incrementAndGet()

        UserPreferencesRepositoryImpl(uid = uid, firestore = firestore)
            .savePreferences(UserPreferences(summary = "x", updatedAt = "2026-10-08T07:24:00Z"))
        CookingResourcesRepositoryImpl(uid = uid, firestore = firestore)
            .save(CachedCookingResources(resources = emptyList(), updatedAt = "2026-10-08T07:24:00Z"))

        assertEquals(3, quota.incrementAndGet())
        assertEquals("x", UserPreferencesRepositoryImpl(uid = uid, firestore = firestore).loadPreferences()!!.summary)
    }
}
