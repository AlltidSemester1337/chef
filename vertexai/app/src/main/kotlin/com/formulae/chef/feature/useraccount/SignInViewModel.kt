/*
 * Copyright 2023 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.formulae.chef.feature.useraccount

import android.app.Application
import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import com.formulae.chef.R
import com.formulae.chef.services.authentication.UserSessionService
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SignInViewModel(
    private val userSessionService: UserSessionService,
    private val navController: NavController,
    application: Application
) : AndroidViewModel(application) {
    val email = MutableStateFlow("")
    val password = MutableStateFlow("")
    val isSignUpMode = MutableStateFlow(false)

    fun onToggleSignUpMode(enabled: Boolean) {
        isSignUpMode.value = enabled
    }

    fun onPrimaryCtaClick() {
        if (isSignUpMode.value) onSignUpClick() else onSignInClick()
    }

    private fun onSignInClick() {
        viewModelScope.launch {
            try {
                val emailInput = email.value
                val passwordInput = password.value
                val user = withContext(Dispatchers.IO) {
                    userSessionService.signInEmailPassword(emailInput.trim(), passwordInput.trim())
                }
                Log.d("SignInViewModel", "User result: $user") // Debug log
                // This line should be reached if signInEmailPassword returns successfully.
                if (user != null) {
                    Log.d("SignInViewModel", "Navigating to home") // Ensure this runs
                    navController.navigate("home") {
                        popUpTo("signIn") { inclusive = true }
                        launchSingleTop = true
                    }
                } else {
                    val context: Context = getApplication<Application>().applicationContext
                    Log.d("SignInViewModel", "User is null, invalid credentials")
                    Toast.makeText(
                        context,
                        "Invalid credentials, please try again.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } catch (e: Exception) {
                // Log the exception for debugging
                Log.e("SignInViewModel", "Error signing in", e)
            }
        }
    }

    fun onSkipSignInClick() {
        userSessionService.anonymousSession = true
        navController.navigate("home")
    }

    fun updateEmail(newEmail: String) {
        email.value = newEmail
    }

    fun updatePassword(newPassword: String) {
        password.value = newPassword
    }

    private fun onSignUpClick() {
        viewModelScope.launch {
            val context: Context = getApplication<Application>().applicationContext
            try {
                val emailInput = email.value
                val passwordInput = password.value
                withContext(Dispatchers.IO) {
                    userSessionService.createUser(emailInput.trim(), passwordInput.trim())
                }
                navController.navigate("home")
            } catch (e: FirebaseAuthUserCollisionException) {
                Log.e("SignInViewModel", "Account already exists", e)
                Toast.makeText(
                    context,
                    context.getString(R.string.account_already_exists_error),
                    Toast.LENGTH_SHORT
                ).show()
            } catch (e: Exception) {
                Log.e("SignInViewModel", "Error creating account", e)
                Toast.makeText(
                    context,
                    context.getString(R.string.create_account_generic_error),
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }
}
