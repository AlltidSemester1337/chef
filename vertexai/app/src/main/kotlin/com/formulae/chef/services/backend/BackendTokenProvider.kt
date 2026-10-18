package com.formulae.chef.services.backend

import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.tasks.await

/** Supplies the headers that authenticate a request to chef-backend. */
fun interface BackendTokenProvider {
    /**
     * @param forceRefresh fetch new tokens instead of the cached ones, e.g. after a 401.
     * @throws BackendException.QuotaExceeded when nobody is signed in (guest session): the backend
     *   gives anonymous users no quota, so the app shows the same dialog without a round trip.
     */
    suspend fun authHeaders(forceRefresh: Boolean): Map<String, String>
}

class FirebaseBackendTokenProvider(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val appCheck: FirebaseAppCheck = FirebaseAppCheck.getInstance()
) : BackendTokenProvider {

    override suspend fun authHeaders(forceRefresh: Boolean): Map<String, String> {
        val user = auth.currentUser ?: throw BackendException.QuotaExceeded()
        val idToken = user.getIdToken(forceRefresh).await().token
            ?: throw BackendException.Unauthorized()
        val appCheckToken = appCheck.getAppCheckToken(forceRefresh).await().token
        return mapOf(
            "Authorization" to "Bearer $idToken",
            "X-Firebase-AppCheck" to appCheckToken
        )
    }
}
