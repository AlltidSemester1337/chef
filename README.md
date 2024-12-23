# Chef

![App Icon](vertexai/app/src/main/res/mipmap-xxxhdpi/ic_launcher_new_round.webp)

A personal cooking assistant Android app (Kotlin) to suggest recipes for cooking. Built on vertexai chat and
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

Planned upcoming features / requests / ideas:
Collections view with recipe details and images, also options to filter / search and categorize

Web? https://reflex.dev/ 

RAG? Langchain graphchain? (cost efficient w options for scalability), consider [context caching](https://ai.google.dev/gemini-api/docs/caching?lang=python) or chatGTP memory etc?) 

Quick intro and few shot prompting for new users? (sign up / register flow)

User seed / username based sessions and sharing / collaboration / community features? (minimal at least a copy recipe text)

## Demo 1.4 release

[Demo 1.4](https://youtube.com/shorts/N_3rSULhudQ?feature=share)

## How to make contributions?

Fork or reach out to authors humlekottekonsult@gmail.com

## Support, feature request, question etc
This project is owned and currently operated and maintained by [Humlekotte Konsultbolag](https://www.humlekotte.nu). Any questions reach out via email humlekottekonsult@gmail.com
