package com.formulae.chef.services.authentication

import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.UserInfo
import kotlinx.coroutines.flow.Flow

interface UserSessionService {
    val currentUser: Flow<UserInfo?>
    var anonymousSession: Boolean
    suspend fun signInEmailPassword(email: String, password: String): FirebaseUser?
    suspend fun signInUid(uid: String): FirebaseUser?
    suspend fun createUser(email: String, password: String): FirebaseUser
    fun signOut()
    suspend fun deleteUser(uid: String)
}