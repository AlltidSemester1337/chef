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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.formulae.chef.services.authentication.UserSessionServiceFirebaseImpl
import com.formulae.chef.services.persistence.RecipeRepositoryImpl
import com.formulae.chef.ui.theme.GenerativeAISample
import com.google.firebase.Firebase
import com.google.firebase.appcheck.appCheck
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory
import com.google.firebase.initialize
import io.opentelemetry.api.common.AttributeKey
import io.opentelemetry.api.common.Attributes
import io.opentelemetry.exporter.otlp.http.trace.OtlpHttpSpanExporter
import io.opentelemetry.sdk.OpenTelemetrySdk
import io.opentelemetry.sdk.resources.Resource
import io.opentelemetry.sdk.trace.SdkTracerProvider
import io.opentelemetry.sdk.trace.export.BatchSpanProcessor

class MainActivity : ComponentActivity() {

    init {
        Firebase.initialize(context = this)
        Firebase.appCheck.installAppCheckProviderFactory(
            DebugAppCheckProviderFactory.getInstance(),
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val recipeRepository = RecipeRepositoryImpl()

        this.actionBar?.hide()

        // Initialize the OTLP exporter
        val spanExporter = OtlpHttpSpanExporter.builder()
            .addHeader("Authorization", "Bearer ${BuildConfig.phoenixApiKey}")
            .addHeader("api_key", BuildConfig.phoenixApiKey)
            .setEndpoint("https://app.phoenix.arize.com/v1/traces")
            .build()

        val resource = Resource.create(
            Attributes.builder()
                .put(AttributeKey.stringKey("service.name"), "Chef-Android")
                .put(AttributeKey.stringKey("project.name"), "Chef-Android")
                .put(AttributeKey.stringKey("openinference.project.name"), "Chef-Android")
                .build()
        )

        // Initialize the OpenTelemetry SDK
        val tracerProvider = SdkTracerProvider.builder()
            .addSpanProcessor(BatchSpanProcessor.builder(spanExporter).build())
            .setResource(resource)
            .build()

        OpenTelemetrySdk.builder()
            .setTracerProvider(tracerProvider)
            .buildAndRegisterGlobal()

        val userSessionService = UserSessionServiceFirebaseImpl()

        setContent {
            GenerativeAISample {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppNavigation(recipeRepository, userSessionService)
                }
            }
        }
    }
}
