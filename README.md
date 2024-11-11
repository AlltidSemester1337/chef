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
- 1.4.x - UI improvements, remove menu, add logos etc
- 1.5.x - Fix proper toggling of both search bar and toggling results
- ... (design updates from Bobo?)
- 2.0.x - Integrate fine-tuned model from historical data (requires at least 100 approved recipe
  suggestions) for chatbot instead of general purpose / reinforced learning model

## How to make contributions?

Fork or reach out to author.
