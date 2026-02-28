# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Chef is a personal cooking assistant Android app (Kotlin/Jetpack Compose) that generates recipes via Google Vertex AI (Gemini models), manages recipe collections, and provides user authentication through Firebase. Instrumentation is done via OpenTelemetry with Phoenix Arize for model evaluation.

## Build & Test Commands

The project requires JDK 17 and Android SDK with platform 36. Three properties must be set in `local.properties`: `firebaseDbUrl`, `phoenixApiKey`, `chefMainChatPromptTemplate`. A valid `google-services.json` is also required.

```bash
# Build the main Chef app
./gradlew :vertexai:app:assembleDebug

# Run unit tests for the main app
./gradlew :vertexai:app:testDebugUnitTest

# Run a single test class
./gradlew :vertexai:app:testDebugUnitTest --tests "com.formulae.chef.feature.model.RecipeTest"

# Run lint module tests
./gradlew :internal:lint:test

# Lint check (ktlint)
./gradlew ktlintCheck

# Check for dependency updates (filters out non-stable and blocklisted)
./gradlew dependencyUpdates
```

## Development Practices

Use pragmatic TDD: write unit tests for models, ViewModels, and business logic where tests add real value. Avoid overly complex or brittle tests — don't mock deeply nested Firebase/AI dependencies just for coverage. UI flows and Firebase integration are covered by E2E testing, not unit tests.

## Architecture

### Module Structure

The primary app is **`:vertexai:app`** — this is where all Chef-specific code lives. Other modules (`auth`, `analytics`, `crash`) are Firebase quickstart samples, not core Chef code. `internal/lint` contains custom lint rules.

### Main App (`vertexai/app/src/main/kotlin/com/formulae/chef/`)

**Entry point:** `MainActivity` — initializes Firebase, AppCheck, OpenTelemetry tracing, then launches Compose UI via `AppNavigation`.

**Navigation:** `AppNavigation` uses Jetpack Navigation Compose with 4 routes: `home`, `chat`, `collection`, `signIn`.

**Feature packages follow a vertical-slice pattern:**
- `feature/chat/` — `ChatViewModel` (AndroidViewModel) handles Gemini chat, recipe derivation from chat messages (extracts JSON), and image generation via Imagen 3.0 through the Vertex AI Prediction API. Uses two generative models: one for chat, one for JSON extraction.
- `feature/collection/` — `CollectionViewModel` (ViewModel) with injected `RecipeRepository`. Manages recipe list display and removal.
- `feature/useraccount/` — `SignInViewModel` handles Firebase email/password auth flow.
- `feature/model/` — `Recipe` (main data model with nested `Ingredient`, `Nutrient`, `Difficulty` enum) and `Recipes` wrapper.

**Services layer:**
- `services/authentication/` — `UserSessionService` interface with `UserSessionServiceFirebaseImpl` (Firebase Auth with `callbackFlow` for auth state).
- `services/persistence/` — `RecipeRepository` interface with `RecipeRepositoryImpl` (Firebase Realtime Database). `ChatHistoryRepository` interface with `ChatHistoryRepositoryImpl` (persists chat to Firebase under `users/{uid}/chat_history`). `FirebaseInstance` singleton for DB access.

**ViewModel creation:** Factory pattern via `GenerativeAiViewModelFactory`, `CollectionViewModelFactory`, `SignInViewModelFactory`. The `GenerativeAiViewModelFactory` reads `gcp.json` and `imagen-google-services.json` from assets to configure the Vertex AI Prediction client.

### Key Design Decisions

- `Recipe.copyOf()` is a custom copy method (not the data class `copy()`) because Firebase deserialization requires mutable `var` fields with `@PropertyName` annotations that don't work correctly with Kotlin's generated `copy()`.
- Chat history persistence uses a custom `Content`/`Part` data class pair in `ChatHistoryRepositoryImpl` as an intermediary for Firebase serialization, then maps to `com.google.firebase.vertexai.type.Content`.
- OpenTelemetry spans wrap all generative AI calls (`generateChatModelResponse`, `generateJsonModelResponse`, `generateImage`) with LLM-specific attributes for Phoenix Arize eval.

### Dependency Version Management

Plugin versions are declared directly in each module's `build.gradle.kts` (not from the version catalog). Library versions for the main app are hardcoded in `vertexai/app/build.gradle.kts`. The version catalog (`gradle/libs.versions.toml`) is used by some modules but the main app doesn't reference it. The root `build.gradle.kts` configures the `ben-manes.versions` plugin to reject non-stable candidates (except Firebase) and blocklisted dependencies.
