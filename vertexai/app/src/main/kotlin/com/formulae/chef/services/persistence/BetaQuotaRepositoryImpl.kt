package com.formulae.chef.services.persistence

import android.util.Log
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.MutableData
import com.google.firebase.database.Transaction
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.tasks.await

class BetaQuotaRepositoryImpl(
    override val uid: String,
    private val database: FirebaseDatabase = FirebaseInstance.database
) : BetaQuotaRepository {
    private val _quotaKey = "users/$uid/betaInteractionCount"

    override suspend fun getCount(): Int {
        val snapshot = database.getReference(_quotaKey).get().await()
        return snapshot.getValue(Int::class.java) ?: 0
    }

    override suspend fun incrementAndGet(): Int {
        return suspendCancellableCoroutine { continuation ->
            database.getReference(_quotaKey).runTransaction(object : Transaction.Handler {
                override fun doTransaction(currentData: MutableData): Transaction.Result {
                    val currentCount = currentData.getValue(Int::class.java) ?: 0
                    currentData.value = currentCount + 1
                    return Transaction.success(currentData)
                }

                override fun onComplete(
                    error: DatabaseError?,
                    committed: Boolean,
                    currentData: DataSnapshot?
                ) {
                    if (error != null) {
                        Log.e("BetaQuotaRepo", "Error incrementing beta interaction count", error.toException())
                        continuation.resumeWithException(error.toException())
                        return
                    }
                    if (!committed) {
                        Log.e("BetaQuotaRepo", "Transaction did not commit for $_quotaKey")
                        continuation.resumeWithException(IllegalStateException("Transaction not committed"))
                        return
                    }
                    continuation.resume(currentData?.getValue(Int::class.java) ?: 0)
                }
            })
        }
    }
}
