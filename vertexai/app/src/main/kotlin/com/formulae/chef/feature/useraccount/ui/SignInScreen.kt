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

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.formulae.chef.R
import com.formulae.chef.SignInViewModelFactory
import com.formulae.chef.feature.useraccount.SignInViewModel
import com.formulae.chef.feature.useraccount.ui.components.InputField
import com.formulae.chef.services.authentication.UserSessionService
import com.formulae.chef.ui.theme.AppTypography
import com.formulae.chef.ui.theme.BackgroundColor
import com.formulae.chef.ui.theme.Terracotta600

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
    )
}

@Composable
private fun SignUpScreen(
    email: State<String>,
    password: State<String>,
    onUpdateEmail: (String) -> Unit,
    onUpdatePassword: (String) -> Unit,
    onSignInClick: () -> Unit,
    onSignUpClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundColor)
    ) {
        // Background decoration — static, not affected by keyboard
        Image(
            painter = painterResource(id = R.drawable.carrot),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .offset(x = 77.dp, y = 117.dp)
                .width(288.dp)
                .height(386.dp)
                .rotate(10.89f)
                .clip(RoundedCornerShape(16.dp))
        )

        // Outer column shrinks with the keyboard; button is always below the fields
        Column(
            modifier = Modifier
                .fillMaxSize()
                .imePadding()
                .navigationBarsPadding()
        ) {
            // Scrollable content — takes all remaining space above the button
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(105.dp))

                Image(
                    painter = painterResource(id = R.drawable.logo),
                    contentDescription = stringResource(R.string.app_name),
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = stringResource(R.string.welcome_message),
                    style = AppTypography.headlineLarge,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(69.dp))

                InputField(
                    label = stringResource(R.string.email),
                    placeholder = "name@email.com",
                    value = email.value,
                    onValueChange = onUpdateEmail
                )

                Spacer(modifier = Modifier.height(24.dp))

                InputField(
                    label = stringResource(R.string.password),
                    placeholder = stringResource(R.string.password_placeholder),
                    supportingText = stringResource(R.string.password_supporting_text),
                    value = password.value,
                    onValueChange = onUpdatePassword,
                    visualTransformation = PasswordVisualTransformation()
                )
            }

            // Create account button — always anchored below the content, above the keyboard
            Button(
                onClick = onSignUpClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 16.dp)
                    .height(44.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Terracotta600),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = stringResource(R.string.create_account),
                    style = AppTypography.labelLarge,
                    textAlign = TextAlign.Center
                )
            }
        }

        // "Sign in" link — drawn last so it sits above the Column in z-order and receives touches
        TextButton(
            onClick = onSignInClick,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(end = 16.dp, top = 6.dp)
        ) {
            Text(
                text = stringResource(R.string.sign_in),
                style = AppTypography.labelLarge.copy(
                    color = Terracotta600,
                    textDecoration = TextDecoration.Underline
                )
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewSignUpScreen() {
    SignUpScreen(
        email = remember {
            object : State<String> {
                override val value: String get() = "Email"
            }
        },
        password = remember {
            object : State<String> {
                override val value: String get() = "Password"
            }
        },
        onUpdateEmail = { },
        onUpdatePassword = { },
        onSignInClick = { },
        onSignUpClick = { }
    )
}
