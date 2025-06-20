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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.formulae.chef.R
import com.formulae.chef.SignInViewModelFactory
import com.formulae.chef.feature.useraccount.SignInViewModel
import com.formulae.chef.feature.useraccount.ui.components.InputField
import com.formulae.chef.services.authentication.UserSessionService
import com.formulae.chef.ui.theme.AppTypography
import com.formulae.chef.ui.theme.BackgroundColor

// TODO: Implement further alignment with design either Figma Pro or manually, collaborate w Bob!
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
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight()
            .background(BackgroundColor),
        contentAlignment = Alignment.BottomCenter
    ) {
        TextButton(
            onClick = onSignInClick,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp, 0.dp)
        ) {

            // Sign in link
            Text(
                text = stringResource(R.string.sign_in),
                color = Color(0xFFC45234),
                textAlign = TextAlign.Right,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                //TODO: Should be waved underline if possible?
                textDecoration = TextDecoration.Underline
            )
        }
        // Background decoration SVG
        // TODO: Fix weird right crop? Also not 100% sure size and position.
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(x = 0.dp, y = 100.dp)
                .width(230.dp)
                .height(350.dp)
                .clip(RoundedCornerShape(16.dp)) // Outer clipping
        ) {
            // Background color using shape of the image
            Image(
                painter = painterResource(id = R.drawable.carrot),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .matchParentSize()
                    .clip(RoundedCornerShape(16.dp)) // Clip outer bounds
            )

            // Foreground PNG with transparent shape preserved
            Image(
                painter = painterResource(id = R.drawable.carrot),
                contentDescription = stringResource(R.string.carrot),
                modifier = Modifier
                    .matchParentSize()
                    .clip(RoundedCornerShape(16.dp)),
                contentScale = ContentScale.Crop
            )

            // Logo/Avatar
            Image(
                painter = painterResource(id = R.drawable.logo),
                contentDescription = stringResource(R.string.app_name),
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .size(48.dp)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop
            )

            Text(
                text = stringResource(R.string.welcome_message),
                style = AppTypography.headlineLarge,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .width(343.dp)
                    .offset(x = 0.dp, y = 50.dp)
            )
        }




        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight()
                .offset(x = 10.dp, y = 200.dp),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            InputField(
                label = stringResource(R.string.email),
                placeholder = "name@email.com",
                value = email.value,
                onValueChange = onUpdateEmail
            )

            //TODO: Double check this spacing with Figma design, generated code miss?
            Spacer(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp)
            )

            InputField(
                label = stringResource(R.string.password),
                placeholder = stringResource(R.string.password_placeholder),
                supportingText = stringResource(R.string.password_supporting_text),
                value = password.value,
                onValueChange = onUpdatePassword,
                visualTransformation = PasswordVisualTransformation()
            )


            // Can be re-enabled to support anonymous sessions (decided by design to remove for now)
            //TextButton(onClick = onSkipSignUpClick) {
            //    Text(text = stringResource(R.string.skip_sign_up), fontSize = 16.sp)
            //}


        }
        // Button section
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .width(343.dp)
                .padding(bottom = 10.dp),
            verticalArrangement = Arrangement.Bottom,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Create account button
            Button(
                onClick = onSignUpClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFC45234)
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = stringResource(R.string.create_account),
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    lineHeight = 20.sp
                )
            }
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