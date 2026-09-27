# Play Console — Data safety answers

These answers are derived from the app code as of 4.0.0 (CHE-41). Re-check them whenever a new SDK, endpoint, or stored field is added.

## Overview

| Question | Answer |
|---|---|
| Does your app collect or share any of the required user data types? | **Yes** |
| Is all user data collected by your app encrypted in transit? | **Yes** (Firebase SDKs, Berget.ai, GCP and Phoenix all use HTTPS) |
| Do you provide a way for users to request that their data is deleted? | **Yes**. In-app deletion and the web page `docs/delete-account.md` |

> "Shared" in Play's sense excludes transfers to **service providers** that process data on your behalf. Berget.ai, Google Cloud and Arize Phoenix act as processors, so the data below is **collected, not shared**.

## Data types

| Category → type | Collected | Shared | Optional? | Purposes |
|---|---|---|---|---|
| Personal info → **Email address** | Yes | No | Required (to use AI features) | Account management, App functionality |
| Personal info → **User IDs** (Firebase UID) | Yes | No | Required | Account management, App functionality |
| Messages → **Other in-app messages** (chat with the AI) | Yes | No | Optional | App functionality, Personalization |
| App activity → **Other user-generated content** (saved recipes, lists, variants, liked replies) | Yes | No | Optional | App functionality |
| App activity → **App interactions** | Yes | No | Required | Analytics |
| App info and performance → **Diagnostics** (AI request traces) | Yes | No | Required | Analytics (quality evaluation) |
| Device or other IDs → **Device or other IDs** (Firebase Analytics app-instance ID) | Yes | No | Required | Analytics |
| Audio → Voice or sound recordings | **No**. Speech-to-text runs through the device's `SpeechRecognizer` service, and only text reaches Chef | — | — | — |

Not collected: location, financial info, health info, photos/videos, files, calendar, contacts, web browsing, installed apps.

**Ephemeral processing:** not applicable to the above; data is stored.
