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

package com.formulae.chef.feature.useraccount.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.formulae.chef.R
import com.formulae.chef.SignInViewModelFactory
import com.formulae.chef.feature.useraccount.SignInViewModel
import com.formulae.chef.services.authentication.UserSessionService
import com.formulae.chef.ui.theme.Purple40

@Composable
internal fun SignInRoute(
    userSessionService: UserSessionService,
    navController: NavController,
    viewModel: SignInViewModel = viewModel(factory = SignInViewModelFactory(userSessionService, navController))
) {
    val email = viewModel.email.collectAsState()
    val password = viewModel.password.collectAsState()

    SignUpScreen(
        email,
        password,
        { viewModel.updateEmail(it) },
        { viewModel.updatePassword(it) },
        { viewModel.onSignInClick() },
        { viewModel.onSignUpClick() }
    ) { viewModel.onSkipSignInClick() }
}

@Composable
private fun SignUpScreen(
    email: State<String>,
    password: State<String>,
    onUpdateEmail: (String) -> Unit,
    onUpdatePassword: (String) -> Unit,
    onSignInClick: () -> Unit,
    onSignUpClick: () -> Unit,
    onSkipSignUpClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        OutlinedTextField(
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp, 4.dp)
                .border(
                    BorderStroke(width = 2.dp, color = Purple40),
                    shape = RoundedCornerShape(50)
                ),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color.Transparent,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent
            ),
            value = email.value,
            onValueChange = onUpdateEmail,
            placeholder = { Text(stringResource(R.string.email)) },
            leadingIcon = { Icon(imageVector = Icons.Default.Email, contentDescription = "Email") }
        )

        OutlinedTextField(
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp, 4.dp)
                .border(
                    BorderStroke(width = 2.dp, color = Purple40),
                    shape = RoundedCornerShape(50)
                ),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color.Transparent,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent
            ),
            value = password.value,
            onValueChange = onUpdatePassword,
            placeholder = { Text(stringResource(R.string.password)) },
            leadingIcon = { Icon(imageVector = Icons.Default.Lock, contentDescription = "Email") },
            visualTransformation = PasswordVisualTransformation()
        )

        Spacer(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        )

        Button(
            onClick = onSignInClick,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp, 0.dp)
        ) {
            Text(
                text = stringResource(R.string.sign_in),
                fontSize = 16.sp,
                modifier = Modifier.padding(0.dp, 6.dp)
            )
        }

        Spacer(
            modifier = Modifier
                .fillMaxWidth()
                .padding(4.dp)
        )

        TextButton(onClick = onSignUpClick) {
            Text(text = stringResource(R.string.sign_up), fontSize = 16.sp)
        }

        Spacer(
            modifier = Modifier
                .fillMaxWidth()
                .padding(4.dp)
        )

        TextButton(onClick = onSkipSignUpClick) {
            Text(text = stringResource(R.string.skip_sign_up), fontSize = 16.sp)
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewSignUpScreen() {
    SignUpScreen(
        email = object : State<String> {
            override val value: String
                get() = "Email"
        },
        password = object : State<String> {
            override val value: String
                get() = "Password"
        },
        onUpdateEmail = { },
        onUpdatePassword = { },
        onSignInClick = { },
        onSignUpClick = { },
        onSkipSignUpClick = { }
    )
}