# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.
Before any actions are taken in new sessions, all documentation and instructions in ./.ai/ folder MUST be read.
No files in this directory are allowed to edit unless explicitly instructed in prompt.

- `.ai/database-schema.md` — Firebase Realtime Database schema (derived from the DB export). Reference this whenever working with data models, Firebase persistence, or the structure of `recipes` / `users` nodes.

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

# Lint check (ktlint)
./gradlew ktlintCheck

# Check for dependency updates (filters out non-stable and blocklisted)
./gradlew dependencyUpdates
```

## Database Schema Changes

**Before making any changes to the Firebase Realtime Database schema, a backup of the current database state MUST be created first.**

Run the following Firebase CLI command to export the full database to a timestamped local backup file:

```bash
firebase database:get / --project $PROJECT_ID > db-backups/backup-$(date +%Y%m%d-%H%M%S).json
```

Backup files are stored in `db-backups/` (gitignored — never committed). Verify the backup file is non-empty before proceeding with any schema changes.

**This step is mandatory — no schema changes may proceed without a confirmed backup.**

## Development Practices

Use pragmatic TDD: write unit tests for models, ViewModels, and business logic where tests add real value. Avoid overly complex or brittle tests — don't mock deeply nested Firebase/AI dependencies just for coverage. UI flows and Firebase integration are covered by E2E testing, not unit tests.

**Tests are mandatory alongside implementation.** For every new feature or change, tests covering the new logic must be included in the same session — never deferred. Specifically:
- New data model fields → add assertions to the existing model test class
- New state management methods (e.g. in `*UiState`) → add or extend a `*UiStateTest` class
- New ViewModel logic (where injectable/fakeable) → extend or create a `*ViewModelTest` with fake dependencies
- Skip tests only for code that has deep Firebase/AI constructor dependencies that cannot be injected (e.g. `ChatViewModel`, `SignInViewModel`)

## Architecture

Use abstractions such as service and UI layers to adhere to SRP and keep business logic cleanly separated from other responsibilities.

### Module Structure

The primary app is **`:vertexai:app`** — this is where all Chef-specific code lives.

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

## ADB Device Interaction

App package name: `com.formulae.chef`

Useful ADB commands for this project:

```bash
# Check connected devices
adb devices

# Take a screenshot (saves locally)
adb exec-out screencap -p > /tmp/chef-screen.png

# Stream logcat filtered to the app (live)
adb logcat -v time | grep "com\.formulae\.chef"

# Collect recent logs (last 200 lines, app + errors/warnings)
adb logcat -d -v time | grep -E "com\.formulae\.chef|E/|W/" | tail -200

# Check crash buffer
adb logcat -d -b crash | tail -100

# Clear logcat buffer
adb logcat -c

# Check if app process is running
adb shell pidof com.formulae.chef

# Dump UI hierarchy (for layout inspection)
adb shell uiautomator dump /sdcard/ui.xml && adb pull /sdcard/ui.xml /tmp/chef-ui.xml

# Start the app
adb shell am start -n com.formulae.chef/.MainActivity

# Force-stop the app
adb shell am force-stop com.formulae.chef

# Clear app data
adb shell pm clear com.formulae.chef

# Install a debug build
adb install -r vertexai/app/build/outputs/apk/debug/app-debug.apk
```

Use `/adb-troubleshoot` to run a guided troubleshooting session against the connected device.

## Integration Tests

Integration tests for Firebase services (Realtime Database, Authentication) live in `vertexai/app/src/androidTest/`. They require the **Firebase Emulator Suite** to be running locally.

### Setup

```bash
# Install Firebase CLI (if not installed)
npm install -g firebase-tools

# Start emulators (from project root)
firebase emulators:start --only database,auth
```

Default emulator ports: `9000` (Realtime Database), `9099` (Authentication).

### Running

```bash
# Requires a connected Android device or running Android emulator
./gradlew :vertexai:app:connectedAndroidTest
```

### AI Model API Calls

Vertex AI (Gemini) and Imagen API regression testing requires real GCP credentials and incurs API costs. These are covered by manual E2E testing sessions, not automated integration tests.

## Context Hub (chub)

`chub` is a CLI for searching and retrieving LLM-optimized docs and skills. Use `/chub` to look up external API docs or skills relevant to the current task.

Contributors can install to enable the chub skill:

```bash
npm install -g @aisuite/chub
chub update   # download the registry/m
```

### Dependency Version Management

Plugin versions are declared directly in `build.gradle.kts`. Library versions for the main app are hardcoded in `vertexai/app/build.gradle.kts`. The root `build.gradle.kts` configures the `ben-manes.versions` plugin to reject non-stable candidates (except Firebase) and blocklisted dependencies.
