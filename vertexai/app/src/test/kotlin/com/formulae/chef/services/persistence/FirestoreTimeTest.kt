package com.formulae.chef.services.persistence

import com.google.firebase.Timestamp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class FirestoreTimeTest {
    // 2025-02-12T13:58:18Z
    private val seconds = 1_739_368_698L

    @Test
    fun toTimestamp_parsesKotlinUtcFormat() {
        val ts = FirestoreTime.toTimestamp("2025-02-12T13:58:18.650Z")!!
        assertEquals(seconds, ts.seconds)
        assertEquals(650_000_000, ts.nanoseconds)
    }

    @Test
    fun toTimestamp_parsesPythonOffsetWithMicroseconds() {
        val ts = FirestoreTime.toTimestamp("2025-02-12T13:58:18.650875+00:00")!!
        assertEquals(seconds, ts.seconds)
        assertEquals(650_875_000, ts.nanoseconds)
    }

    @Test
    fun toTimestamp_parsesZonedSuffixAndNonUtcOffset() {
        assertEquals(seconds, FirestoreTime.toTimestamp("2025-02-12T13:58:18Z[UTC]")!!.seconds)
        assertEquals(seconds, FirestoreTime.toTimestamp("2025-02-12T14:58:18+01:00")!!.seconds)
    }

    @Test
    fun toTimestamp_returnsNullForBlankOrGarbage() {
        assertNull(FirestoreTime.toTimestamp(""))
        assertNull(FirestoreTime.toTimestamp(null))
        assertNull(FirestoreTime.toTimestamp("igår"))
    }

    @Test
    fun toIso_formatsAsInstantString() {
        assertEquals("2025-02-12T13:58:18.650Z", FirestoreTime.toIso(Timestamp(seconds, 650_000_000)))
        assertEquals("2025-02-12T13:58:18Z", FirestoreTime.toIso(Timestamp(seconds, 0)))
    }

    @Test
    fun toIso_returnsEmptyForNull() {
        assertEquals("", FirestoreTime.toIso(null))
    }

    @Test
    fun roundTrip_preservesInstant() {
        val iso = "2026-10-08T07:24:00.123456Z"
        assertEquals(iso, FirestoreTime.toIso(FirestoreTime.toTimestamp(iso)))
    }
}
