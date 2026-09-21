package com.formulae.chef.services

import com.formulae.chef.services.authentication.UserSessionService
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.UserInfo
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

class FakeUserInfo(
    private val uid: String,
    private val email: String?
) : UserInfo {
    override fun getUid(): String = uid
    override fun getEmail(): String? = email
    override fun getProviderId(): String = "fake"
    override fun getDisplayName(): String? = null
    override fun getPhotoUrl(): android.net.Uri? = null
    override fun getPhoneNumber(): String? = null
    override fun isEmailVerified(): Boolean = true
}

class FakeUserSessionService(
    override var anonymousSession: Boolean = false,
    user: UserInfo? = null
) : UserSessionService {
    override val currentUser: Flow<UserInfo?> = flowOf(user)

    override suspend fun signInEmailPassword(email: String, password: String): FirebaseUser? = null
    override suspend fun signInUid(uid: String): FirebaseUser? = null
    override suspend fun createUser(email: String, password: String): FirebaseUser =
        throw UnsupportedOperationException("not needed for tests")
    override fun signOut() {}
    override suspend fun deleteUser(uid: String) {}
}
