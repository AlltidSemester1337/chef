package com.formulae.chef.services.persistence

interface BetaQuotaRepository {
    val uid: String

    /** Reads the user's current lifetime beta interaction count without modifying it. */
    suspend fun getCount(): Int

    /** Atomically increments the user's lifetime beta interaction count and returns the new value. */
    suspend fun incrementAndGet(): Int
}
