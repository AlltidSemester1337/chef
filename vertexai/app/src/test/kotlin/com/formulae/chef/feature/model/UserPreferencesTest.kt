package com.formulae.chef.feature.model

import org.junit.Assert.assertEquals
import org.junit.Test

class UserPreferencesTest {
    @Test
    fun defaultConstructor_hasEmptyFields() {
        val prefs = UserPreferences()
        assertEquals("", prefs.summary)
        assertEquals("", prefs.updatedAt)
    }

    @Test
    fun fields_areMutable() {
        val prefs = UserPreferences()
        prefs.summary = "prefers metric"
        prefs.updatedAt = "2026-01-01T00:00:00Z"
        assertEquals("prefers metric", prefs.summary)
        assertEquals("2026-01-01T00:00:00Z", prefs.updatedAt)
    }

    @Test
    fun constructor_setsProvidedValues() {
        val prefs = UserPreferences(summary = "no fish", updatedAt = "2026-01-01T00:00:00Z")
        assertEquals("no fish", prefs.summary)
        assertEquals("2026-01-01T00:00:00Z", prefs.updatedAt)
    }
}
