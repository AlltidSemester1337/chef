package com.formulae.chef.feature.model

import com.google.firebase.Timestamp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
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

    @Test
    fun updatedAtTimestamp_mapsIsoStringBothWays() {
        val prefs = UserPreferences(updatedAt = "2026-01-01T00:00:00Z")
        assertEquals(1_767_225_600L, prefs.updatedAtTimestamp!!.seconds)

        prefs.updatedAtTimestamp = Timestamp(1_767_225_660L, 0)
        assertEquals("2026-01-01T00:01:00Z", prefs.updatedAt)
    }

    @Test
    fun updatedAtTimestamp_isNullWhenUpdatedAtBlank() {
        assertNull(UserPreferences().updatedAtTimestamp)
    }
}
