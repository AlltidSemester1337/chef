package com.formulae.chef.services.persistence

import com.google.firebase.Timestamp
import java.time.Instant
import java.time.ZonedDateTime

/**
 * Domain models keep timestamps as ISO-8601 strings; Firestore stores them as [Timestamp].
 * Models expose a `@PropertyName` accessor pair that converts through here (see e.g. `Recipe.updatedAtTimestamp`).
 */
object FirestoreTime {
    /** Accepts `…Z`, `…+00:00` (incl. µs) and `…Z[UTC]`. Blank or unparseable → null (field is omitted). */
    fun toTimestamp(iso: String?): Timestamp? {
        if (iso.isNullOrBlank()) return null
        val instant = runCatching { ZonedDateTime.parse(iso).toInstant() }.getOrNull() ?: return null
        return Timestamp(instant.epochSecond, instant.nano)
    }

    /** [Timestamp] → `Instant.toString()` form (`…Z`), or "" when absent. */
    fun toIso(timestamp: Timestamp?): String =
        timestamp?.let { Instant.ofEpochSecond(it.seconds, it.nanoseconds.toLong()).toString() } ?: ""
}
