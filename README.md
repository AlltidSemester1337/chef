# Chef

![App Icon](vertexai/app/src/main/res/mipmap-xxxhdpi/logo_round.webp)

A personal cooking assistant / recipe generator and collection browsing Android app (Kotlin) to
suggest recipes for cooking. Built on Google firebase and vertexai platform.

## Required setup

The following files are **gitignored** and must be obtained from the team before building:

| File | Purpose |
|------|---------|
| `local.properties` | Must contain `firebaseDbUrl` and `phoenixApiKey` |
| `vertexai/app/google-services.json` | Firebase / GCP service account credentials |
| `vertexai/app/src/main/assets/gcp.json` | Vertex AI (Gemini) credentials |
| `vertexai/app/src/main/assets/imagen-google-services.json` | Imagen image generation credentials |
| `vertexai/app/src/main/assets/chat_system_prompt.txt` | Main chat system prompt — **required at runtime; app crashes on launch without it** |

Build and run the app from the `vertexai/app` module. JDK 17 is required — prefix Gradle commands with `JAVA_HOME=/path/to/jdk-17` if your system default differs.

### Building

```bash
JAVA_HOME=/home/kalle/.jdks/jdk-17.0.12 ./gradlew :vertexai:app:assembleDebug
```

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
- 3.2.0 (Current) - Multiple recipe suggestions with card grid and images in chat, also major updates for Claude driven development
- 4.0.0 - Restructured Recipe data format, added more information. Also complete redesign of the
  app!

Up next: Refer to Linear

## Demos

[Demos](https://www.youtube.com/playlist?list=PL3z3ETRVg-c4teb_hZ8OzLwx7ySJKoB1Q)

## How to make contributions?

Fork or reach out to authors humlekottekonsult@gmail.com

## Support, feature request, question etc

This project is currently in closed beta and owned as well as currently operated and maintained
by [Humlekotte Konsultbolag](https://www.humlekotte.nu). Any questions reach out via email
humlekottekonsult@gmail.com
