package com.formulae.chef.services.authentication

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.tasks.await


class UserSessionServiceFirebaseImpl : UserSessionService {

    override val currentUser: Flow<FirebaseUser?>
        get() = callbackFlow {
            val listener =
                FirebaseAuth.AuthStateListener { auth ->
                    this.trySend(auth.currentUser)
                }
            Firebase.auth.addAuthStateListener(listener)
            awaitClose { Firebase.auth.removeAuthStateListener(listener) }
        }

    override var anonymousSession: Boolean = false

    override suspend fun signInEmailPassword(email: String, password: String): FirebaseUser? {
        try {
            Firebase.auth.signInWithEmailAndPassword(email, password).await()
        } catch (e: FirebaseAuthInvalidCredentialsException) {
            Log.d("UserSessionServiceFirebaseImpl", "Invalid credentials")
            return null
        }
        return currentUser.firstOrNull()
    }

    override suspend fun signInUid(uid: String): FirebaseUser? {
        Firebase.auth.signInWithCustomToken(uid).await()
        return currentUser.firstOrNull()
    }

    override suspend fun createUser(email: String, password: String): FirebaseUser {
        Firebase.auth.createUserWithEmailAndPassword(email, password).await()
        return currentUser.first()!!
    }

    override fun signOut() {
        anonymousSession = false
        Firebase.auth.signOut()
    }

    override suspend fun deleteUser(uid: String) {
        Firebase.auth.currentUser!!.delete().await()
    }


}