# Chef

![App Icon](vertexai/app/src/main/res/mipmap-xxxhdpi/ic_launcher_new_round.webp)

A personal cooking assistant / recipe generator and collection browsing Android app (Kotlin) to
suggest recipes for cooking. Built on Google firebase and vertexai platform.
Requires integration towards vertexai using google-services.json credentials for SA in order to run.
Also requires firebaseDbUrl, phoenixApiKey (instrumentation) and chefMainChatPromptTemplate
properties to be set in local.properties when building the app.

Build and run the app from vertexai/app module.

Features:

- 1.0.x - Initial release, chat history persisted locally
- 1.1.x - Chat history persisted on Realtime Database
- 1.2.x - Search function in chat (delivered by toggling results not working)
- 1.4.x - UI improvements, remove menu, add logos etc
- 1.5.x - Added recipe collection / favourite feature
- 1.6.x - Generate image and save / load for recipes in recipe collection
- 1.6.1 enable sharing URL (copy)
- 1.6.2 search / filter recipes in collection. This replaces search functionality in chat.
- 2.0.0 - Users and sessions
- 3.0.0 - Recipe collection updates and browsing
- 3.1.0 - Instrumentation via OTEL and Phoenix Arize (for use in model eval primarily)

## Demos

[Demos](https://www.youtube.com/playlist?list=PL3z3ETRVg-c4teb_hZ8OzLwx7ySJKoB1Q)

## How to make contributions?

Fork or reach out to authors humlekottekonsult@gmail.com

## Support, feature request, question etc

This project is currently in closed beta and owned as well as currently operated and maintained
by [Humlekotte Konsultbolag](https://www.humlekotte.nu). Any questions reach out via email
humlekottekonsult@gmail.com
