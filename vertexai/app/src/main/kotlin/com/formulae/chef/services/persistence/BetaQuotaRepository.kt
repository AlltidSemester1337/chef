package com.formulae.chef.services.persistence

interface BetaQuotaRepository {
    val uid: String

    /** Atomically increments the user's lifetime beta interaction count and returns the new value. */
    suspend fun incrementAndGet(): Int
}
