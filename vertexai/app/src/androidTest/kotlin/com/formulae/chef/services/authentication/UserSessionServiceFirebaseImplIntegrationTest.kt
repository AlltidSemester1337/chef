package com.formulae.chef.services.authentication

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Integration tests for [UserSessionServiceFirebaseImpl] against the Firebase Auth emulator.
 *
 * Prerequisites:
 *   - Firebase Emulator Suite running: `firebase emulators:start --only auth`
 *   - An Android emulator or connected device
 *
 * Each test creates fresh credentials and cleans up afterward to stay isolated.
 */
@RunWith(AndroidJUnit4::class)
class UserSessionServiceFirebaseImplIntegrationTest {

    private lateinit var service: UserSessionServiceFirebaseImpl
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()

    private val testEmail = "integration-test@chef.test"
    private val testPassword = "testPassword123!"

    companion object {
        private var emulatorConfigured = false
    }

    @Before
    fun setup() {
        if (!emulatorConfigured) {
            auth.useEmulator("10.0.2.2", 9099)
            emulatorConfigured = true
        }
        service = UserSessionServiceFirebaseImpl()
        runBlocking { signOutAndCleanup() }
    }

    @After
    fun tearDown() = runBlocking {
        signOutAndCleanup()
    }

    @Test
    fun createUser_createsAndSignsInNewUser() = runBlocking {
        val user = service.createUser(testEmail, testPassword)

        assertNotNull(user)
        assertEquals(testEmail, user.email)
    }

    @Test
    fun signInEmailPassword_returnsUserForValidCredentials() = runBlocking {
        service.createUser(testEmail, testPassword)
        service.signOut()

        val user = service.signInEmailPassword(testEmail, testPassword)

        assertNotNull(user)
        assertEquals(testEmail, user?.email)
    }

    @Test
    fun signInEmailPassword_returnsNullForInvalidCredentials() = runBlocking {
        service.createUser(testEmail, testPassword)
        service.signOut()

        val user = service.signInEmailPassword(testEmail, "wrongPassword!")

        assertNull(user)
    }

    @Test
    fun signOut_clearsCurrentUser() = runBlocking {
        service.createUser(testEmail, testPassword)

        service.signOut()

        val user = service.currentUser.first()
        assertNull(user)
    }

    @Test
    fun currentUser_emitsSignedInUserAfterSignIn() = runBlocking {
        service.createUser(testEmail, testPassword)

        val user = service.currentUser.first()

        assertNotNull(user)
        assertEquals(testEmail, user?.email)
    }

    @Test
    fun currentUser_emitsNullWhenSignedOut() = runBlocking {
        val user = service.currentUser.first()
        assertNull(user)
    }

    @Test
    fun signOut_setsAnonymousSessionToFalse() = runBlocking {
        service.createUser(testEmail, testPassword)
        service.anonymousSession = true

        service.signOut()

        assertEquals(false, service.anonymousSession)
    }

    private suspend fun signOutAndCleanup() {
        try {
            auth.currentUser?.delete()?.await()
        } catch (_: Exception) {
            // User might not exist — ignore
        }
        auth.signOut()
    }
}
