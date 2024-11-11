# Chef

A personal cooking assistant app to suggest recipes for cooking. Built on vertexai chat and
firebase. Requires integration towards
vertexai using google-services.json credentials for SA in order to run.
Also requires a firebaseDbUrl to be set in local.properties when building the app to run using
ChatHistoryRealtimeDatabasePersistence.

Run the app from vertexai/app module.

Features:

- 1.0.x - Initial release, chat history persisted locally
- 1.1.x - Chat history persisted on Realtime Database
- 1.2.x - Search function in chat (delivered by toggling results not working)
- 1.3.x - UI improvements, remove menu, add logos etc
- ... (design updates from Bobo?) - Fix proper toggling of both search bar and toggling results?
- 2.0.x - Integrate fine-tuned model from historical data (requires at least 100 approved recipe
  suggestions) for chatbot instead of general purpose / reinforced learning model

Old desc below:

## Samples

You can open each of the following samples as an Android Studio project, and run
them on a mobile device or a virtual device (AVD). When doing so you need to
add each sample app you wish to try to a Firebase project on the [Firebase
console](https://console.firebase.google.com). You can add multiple sample apps
to the same Firebase project. There's no need to create separate projects for
each app.

To add a sample app to a Firebase project, use the `applicationId` value specified
in the `app/build.gradle` file of the app as the Android package name. Download
the generated `google-services.json` file, and copy it to the `app/` directory of
the sample you wish to run.

- [Analytics](analytics/README.md)
- [Auth](auth/README.md)
- [Crash](crash/README.md)
- [Database](database/README.md)
- [Vertex AI](vertexai/README.md)

## How to make contributions?

Fork or reach out to author.
