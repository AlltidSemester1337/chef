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

package com.formulae.chef

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.formulae.chef.services.authentication.UserSessionServiceFirebaseImpl
import com.formulae.chef.services.persistence.RecipeListRepositoryImpl
import com.formulae.chef.services.persistence.RecipeRepositoryImpl
import com.formulae.chef.services.persistence.RecipeVariantRepositoryImpl
import com.formulae.chef.ui.theme.GenerativeAISample
import com.google.firebase.Firebase
import com.google.firebase.appcheck.appCheck
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory
import com.google.firebase.initialize

class MainActivity : ComponentActivity() {

    init {
        Firebase.initialize(context = this)
        // Debug provider keeps local dev builds / directly-installed test builds working without
        // per-device Play Integrity setup. Play Integrity is required for release so App Check
        // provides real abuse protection once Firebase enforces it for AI Logic (Nov 2, 2026).
        val appCheckProviderFactory = if (BuildConfig.DEBUG) {
            DebugAppCheckProviderFactory.getInstance()
        } else {
            PlayIntegrityAppCheckProviderFactory.getInstance()
        }
        Firebase.appCheck.installAppCheckProviderFactory(appCheckProviderFactory)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Let the app own window-inset handling (status/nav bars and the IME) instead of the
        // decor view panning or resizing the window. Combined with adjustResize in the manifest,
        // this is what makes Modifier.imePadding() report real keyboard insets to Compose.
        enableEdgeToEdge()
        val recipeRepository = RecipeRepositoryImpl()
        val recipeListRepository = RecipeListRepositoryImpl()
        val recipeVariantRepository = RecipeVariantRepositoryImpl()

        this.actionBar?.hide()

        // Touches the lazy singleton, initializing the OTLP exporter and registering the global
        // OpenTelemetry SDK exactly once per process.
        ChefTelemetry.tracerProvider

        val userSessionService = UserSessionServiceFirebaseImpl()

        setContent {
            GenerativeAISample {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppNavigation(recipeRepository, recipeListRepository, recipeVariantRepository, userSessionService)
                }
            }
        }
    }

    override fun onStop() {
        super.onStop()
        // Force-export any buffered spans instead of relying on the batch timer, since the
        // process may be killed while backgrounded before the default 5s delay fires.
        ChefTelemetry.flush()
    }
}
