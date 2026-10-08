package com.formulae.chef.services.persistence

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await

class BetaQuotaRepositoryImpl(
    override val uid: String,
    private val firestore: FirebaseFirestore = FirebaseInstance.firestore
) : BetaQuotaRepository {

    // Transaction (not FieldValue.increment) so the new value is returned atomically.
    // Rules only accept an unchanged count or exactly +1; merge keeps the other user fields.
    override suspend fun incrementAndGet(): Int {
        val userRef = firestore.userDoc(uid)
        return firestore.runTransaction { transaction ->
            val current = transaction.get(userRef).getLong(FirestorePaths.BETA_INTERACTION_COUNT) ?: 0L
            val next = current + 1
            transaction.set(userRef, mapOf(FirestorePaths.BETA_INTERACTION_COUNT to next), SetOptions.merge())
            next.toInt()
        }.await()
    }
}
