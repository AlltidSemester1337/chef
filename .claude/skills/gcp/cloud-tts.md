---
name: cloud-tts
description: Reference when working with GCP Cloud Text-to-Speech API (voice synthesis, TTS errors, API key setup).
---

# GCP Cloud Text-to-Speech API

## Setup

1. Enable the API in GCP Console: **APIs & Services → Library → search "Cloud Text-to-Speech API" → Enable**
2. Add `gcpTtsApiKey=<key>` to `local.properties`
3. API key propagation after enabling/restricting a key can take **up to 5 minutes**
4. If the key has service restrictions, `texttospeech.googleapis.com` must be in the allowed list

## Authentication

Always pass the API key as a **request header**, not a URL query parameter (avoids server-side logging of the key):

```kotlin
connection.setRequestProperty("X-goog-api-key", apiKey)
```

## Voice

Current voice: `en-US-Chirp3-HD-Aoede` (Chirp 3 HD — high quality, conversational)

## Known API Limits

| Limit | Value | Handling in Chef |
|---|---|---|
| Total input per request | 5 000 bytes | `sanitizeForTts()` hard-truncates at `TTS_HARD_LIMIT` chars |
| Per-sentence length | Undocumented; long lines without punctuation fail | `sanitizeForTts()` appends `.` to each line to create sentence breaks |
| TTS button visibility | n/a | Speaker button hidden and auto-play skipped for responses > `TTS_DISPLAY_THRESHOLD` chars |

## Common HTTP Errors

| Code | Reason | Fix |
|---|---|---|
| 403 `API_KEY_SERVICE_BLOCKED` | API not enabled or key restricted | Enable Cloud TTS API; check key restrictions |
| 400 `input.text longer than 5000 bytes` | Total text too long | Enforce `TTS_HARD_LIMIT` before calling `synthesize()` |
| 400 `sentences that are too long` | Single "sentence" exceeds internal limit | Ensure `sanitizeForTts()` adds period breaks between lines |

## Text Sanitization (`TextSanitizer.kt`)

Two functions in `feature/chat/ui/TextSanitizer.kt`:

- `String.sanitizeMarkdown()` — strips `* # \` _ ~`, collapses 3+ blank lines. Used for display text in chat bubbles.
- `String.sanitizeForTts()` — calls `sanitizeMarkdown()`, then adds period breaks per line, flattens newlines to spaces, truncates to `TTS_HARD_LIMIT`. Used before every `ttsService.synthesize()` call.

## Integration Testing

See `src/androidTest/.../services/voice/GcpTextToSpeechServiceTest.kt`. Tests are skipped automatically when `gcpTtsApiKey` is blank in `BuildConfig`.

## Future Work

- CHE-25: Lower TTS latency (sentence-chunked playback or gRPC streaming)
- CHE-26: Voice button on recipe detail screen