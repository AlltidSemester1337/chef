import java.util.Properties

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

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.gms.google-services")
}

val localProperties = Properties()
val localPropertiesFile = rootProject.file("local.properties")

if (localPropertiesFile.exists()) {
    localProperties.load(localPropertiesFile.inputStream())
}

// Base URL of chef-backend (Cloud Run), which proxies the AI and TTS calls. Not a secret, but
// project-specific, so it lives in local.properties / CI secrets rather than in the repo.
val chefBackendUrl: String = localProperties.getProperty("chefBackendUrl", "")

// Upload-key signing for Play releases. Optional: when any key is missing, release builds are left
// unsigned so debug builds and CI keep working without the keystore.
val releaseSigningKeys = listOf("releaseStoreFile", "releaseStorePassword", "releaseKeyAlias", "releaseKeyPassword")
val hasReleaseSigning = releaseSigningKeys.all { !localProperties.getProperty(it).isNullOrBlank() }

android {
    namespace = "com.formulae.chef"
    compileSdk = 36

    defaultConfig {
        buildConfigField(
            "String",
            "chefBackendUrl",
            "\"${chefBackendUrl}\""
        )

        applicationId = "com.formulae.chef"
        minSdk = 26
        targetSdk = 36
        versionCode = 5
        versionName = "4.2.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }

    signingConfigs {
        if (hasReleaseSigning) {
            create("release") {
                storeFile = rootProject.file(localProperties.getProperty("releaseStoreFile"))
                storePassword = localProperties.getProperty("releaseStorePassword")
                keyAlias = localProperties.getProperty("releaseKeyAlias")
                keyPassword = localProperties.getProperty("releaseKeyPassword")
            }
        } else {
            logger.info("Release signing not configured; set $releaseSigningKeys in local.properties")
        }
    }

    buildTypes {
        release {
            if (hasReleaseSigning) {
                signingConfig = signingConfigs.getByName("release")
            }
            isMinifyEnabled = true
            isShrinkResources = true
            isDebuggable = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            isDebuggable = true
        }
    }

    // Lets ViewModel unit tests run code that logs via android.util.Log (returns 0 instead of throwing).
    testOptions {
        unitTests.isReturnDefaultValues = true
    }

    packaging {
        resources {
            excludes += "META-INF/DEPENDENCIES"
            excludes += "META-INF/INDEX.LIST"
        }
    }
}

// Everything under src/*/assets is shipped verbatim in the APK/AAB and is readable by anyone with
// the app (CHE-49). Fail the build if anything other than allowlisted files lands there, so DB
// exports, service-account keys and other local-only files can never be packaged again.
// System prompts are chosen by chef-backend, so the app ships no assets at all.
val allowedAssets = emptySet<String>()
val assetDirs = fileTree("src") { include("*/assets/**") }
val verifyAssets by tasks.registering {
    description = "Fails if non-allowlisted files or credentials are present in asset directories."
    inputs.files(assetDirs)
    doLast {
        val offending = assetDirs.files.filter { file ->
            file.name !in allowedAssets || file.readText().contains("\"private_key\"")
        }
        if (offending.isNotEmpty()) {
            throw GradleException(
                "Files in assets are packaged into the APK and must not contain secrets or data dumps. " +
                    "Move these out of src/*/assets (DB backups belong outside the repo):\n" +
                    offending.joinToString("\n") { "  - ${it.relativeTo(projectDir)}" }
            )
        }
    }
}
tasks.named("preBuild") { dependsOn(verifyAssets) }

// Firestore and Gson (de)serialize model classes by reflection. When R8 renames or strips such a
// class, only the release build fails, at runtime (CHE-55, CHE-69). Convention: reflection-serialized
// data classes live in feature/model (kept by proguard-rules.pro) or are annotated @Keep. This task
// checks the convention in the sources, and its result in R8's mapping.txt: each such class keeps its
// name, its no-arg constructor and its field names. Runs after every release minification.
val mainKotlinSources = fileTree("src/main/kotlin") { include("**/*.kt") }
val releaseMapping = layout.buildDirectory.file("outputs/mapping/release/mapping.txt")
val verifyReleaseModelsKept by tasks.registering {
    description = "Fails if a reflection-serialized model class lost its name, constructor or fields in R8."
    inputs.files(mainKotlinSources)
    inputs.file(releaseMapping)
    doLast {
        val srcRoot = file("src/main/kotlin")
        val dataClass = Regex("""^((?:@\w+\s+)*)(?:(?:private|internal)\s+)?data class (\w+)""", RegexOption.MULTILINE)
        val reflectiveUse = Regex("""(?:toObjects?|fromJson|\.get)\([^\n]*?(\w+)::class\.java|TypeToken<(?:\w+<)?(\w+)>""")
        val declared = mutableSetOf<String>()
        val kept = mutableMapOf<String, String>() // simple name -> FQN
        val used = mutableSetOf<String>()
        mainKotlinSources.files.forEach { source ->
            val text = source.readText()
            val pkg = Regex("""^package ([\w.]+)""", RegexOption.MULTILINE).find(text)?.groupValues?.get(1) ?: ""
            val inModelPackage = source.relativeTo(srcRoot).invariantSeparatorsPath
                .startsWith("com/formulae/chef/feature/model/")
            dataClass.findAll(text).forEach { match ->
                val name = match.groupValues[2]
                declared += name
                if (inModelPackage || "@Keep" in match.groupValues[1]) kept[name] = "$pkg.$name"
            }
            reflectiveUse.findAll(text).forEach { match ->
                used += match.groupValues.drop(1).first { it.isNotEmpty() }
            }
        }

        val problems = mutableListOf<String>()
        (used intersect declared).filterNot { it in kept }.forEach {
            problems += "$it is (de)serialized by reflection but is neither in feature/model nor @Keep"
        }
        val mapping = releaseMapping.get().asFile.readLines()
        val fieldLine = Regex("""^\s+[\w.$\[\]]+ ([\w$]+) -> ([\w$]+)$""")
        for (fqn in kept.values.sorted()) {
            val start = mapping.indexOfFirst { it.startsWith("$fqn -> ") }
            if (start < 0) {
                problems += "$fqn was removed by R8"
                continue
            }
            if (mapping[start] != "$fqn -> $fqn:") problems += "$fqn was renamed (${mapping[start]})"
            // Member lines are indented; R8 also puts "# {...}" metadata comments between them.
            val members = mapping.drop(start + 1).takeWhile { it.startsWith(" ") || it.startsWith("#") }
            if (members.none { "void <init>()" in it }) problems += "$fqn lost its no-arg constructor"
            members.mapNotNull { fieldLine.find(it) }
                .filter { it.groupValues[1] != it.groupValues[2] }
                .forEach { problems += "$fqn field ${it.groupValues[1]} was renamed to ${it.groupValues[2]}" }
        }
        if (problems.isNotEmpty()) {
            throw GradleException(
                "Release build would break Firestore/Gson (de)serialization. Move the class to feature/model " +
                    "or annotate it with @Keep:\n" + problems.joinToString("\n") { "  - $it" }
            )
        }
        logger.lifecycle("verifyReleaseModelsKept: ${kept.size} model classes intact after R8")
    }
}
tasks.matching { it.name == "minifyReleaseWithR8" }.configureEach { finalizedBy(verifyReleaseModelsKept) }
verifyReleaseModelsKept.configure { mustRunAfter("minifyReleaseWithR8") }

// A release without the backend URL would build fine but every AI and voice feature would fail.
tasks.matching { it.name == "preReleaseBuild" }.configureEach {
    doFirst {
        if (chefBackendUrl.isBlank()) {
            throw GradleException("chefBackendUrl must be set in local.properties for release builds.")
        }
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.17.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.10.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.10.0")
    implementation("androidx.activity:activity-compose:1.10.0")
    implementation("androidx.navigation:navigation-compose:2.9.7")
    implementation("androidx.preference:preference-ktx:1.2.1")
    implementation("com.google.code.gson:gson:2.13.2")

    implementation(platform("androidx.compose:compose-bom:2026.02.01"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-core")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("io.coil-kt:coil-compose:2.7.0")

    implementation(platform("com.google.firebase:firebase-bom:33.7.0"))
    implementation("com.google.firebase:firebase-analytics")
    implementation("com.google.firebase:firebase-auth")
    implementation("com.google.firebase:firebase-appcheck-playintegrity")
    implementation("com.google.firebase:firebase-firestore")
    implementation("com.google.firebase:firebase-appcheck-debug")

    implementation("androidx.media3:media3-exoplayer:1.4.1")
    implementation("androidx.media3:media3-ui:1.4.1")

    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.9.0")
    testImplementation("org.json:json:20231013")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
    androidTestImplementation(platform("androidx.compose:compose-bom:2026.02.01"))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    androidTestImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.9.0")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}
