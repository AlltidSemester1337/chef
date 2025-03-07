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
import com.formulae.chef.services.authentication.UserSessionService
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

    fun onSignInClick() {
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

    fun onSignUpClick() {
        viewModelScope.launch {
            val email = email.value
            val password = password.value
            userSessionService.createUser(email.trim(), password.trim())
            navController.navigate("home")
        }
    }


}
