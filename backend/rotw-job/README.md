# rotw-job — Recipe of the Month Video Generator

A Cloud Run Job that runs monthly to:
1. Pick a random favourite recipe that has never been featured before
2. Generate an 8-second cinematic food video (with audio) via **Google Veo 3.1 Lite** (Vertex AI, GA)
3. Upload the video to **Firebase Storage** under `videos/rotw/YYYY-MM-{recipeId}.mp4`
4. Write a document to the `recipe_of_the_month` Firestore collection
5. Update `videoUrl` on the selected `recipes/{id}` document
6. Mark the recipe as used in `video_generation_history` so it is never selected again

## Prerequisites

- JDK 17
- A GCP project with the **Vertex AI API** enabled
- A Firebase project with Firestore (`(default)` database, europe-north2) and Storage
- A service account (attached to the Cloud Run Job — no key file) with the following roles:
  - `roles/aiplatform.user` — for Veo 3.1 video generation
  - `roles/datastore.user` — for Firestore reads/writes (the Admin SDK bypasses `firestore.rules`)
  - `roles/storage.objectAdmin` — for Firebase Storage uploads
- Locally: Application Default Credentials via `gcloud auth application-default login` (no service-account key files)

## Environment variables

| Variable | Required | Description |
|---|---|---|
| `GCP_PROJECT_ID` | Yes | GCP project ID (e.g. `my-project-123`) |
| `FIREBASE_STORAGE_BUCKET` | Yes | Firebase Storage bucket (e.g. `my-project.appspot.com`) |
| `GCP_LOCATION` | Yes | Vertex AI region (e.g. `us-central1`) |
| `FIRESTORE_EMULATOR_HOST` | No | e.g. `127.0.0.1:8080` — point Firestore at the emulator instead of production |

Credentials and the Firestore project come from ADC — there is no key-file variable.

## Running locally

```bash
# From the repo root
gcloud auth application-default login   # once
export GCP_PROJECT_ID=my-project-123
export GCP_LOCATION=us-central1
export FIREBASE_STORAGE_BUCKET=my-project.appspot.com

./gradlew :backend:rotw-job:run
```

> **Note:** A real run generates a video via Veo 3.1 Lite (GA model, with audio), which costs roughly $0.40/video depending on current billing — see "Current approximate cost" below. Only run against production credentials when you intend to produce an actual video.

## Running tests (no API calls, no cost)

```bash
./gradlew :backend:rotw-job:test
```

The test suite covers `selectRecipe()` (recipe selection logic), `GeminiPromptBuilder` (prompt construction) and the Firestore document mapping — all pure, no external dependencies.

`FirebaseAdminServiceEmulatorTest` exercises the Firestore reads/writes against the emulator; it is skipped unless `FIRESTORE_EMULATOR_HOST` is set:

```bash
cd firestore-tools && npx firebase emulators:exec --only firestore --project demo-rotw \
  "cd .. && ./gradlew :backend:rotw-job:test --rerun"
```

## Building and pushing the container image

The project uses [Jib](https://github.com/GoogleContainerTools/jib) for containerisation — no local Docker daemon required.

```bash
export GCP_PROJECT_ID=my-project-123

# Build and push to Google Artifact Registry / Container Registry
./gradlew :backend:rotw-job:jib
```

This produces image `gcr.io/$GCP_PROJECT_ID/rotw-job:latest`.

## Deploying as a Cloud Run Job

```bash
export GCP_REGION=us-central1

gcloud run jobs create rotw-job \
  --image gcr.io/$GCP_PROJECT_ID/rotw-job \
  --region $GCP_REGION \
  --service-account rotw-job@$GCP_PROJECT_ID.iam.gserviceaccount.com \
  --set-env-vars "GCP_PROJECT_ID=$GCP_PROJECT_ID,GCP_LOCATION=$GCP_REGION,FIREBASE_STORAGE_BUCKET=..."
```

The job authenticates as its attached service account via ADC — do not mount key files or secrets for credentials.

### Scheduled execution

The job is intended to run on the **first Sunday of each month at 22:00 UTC**. Standard cron
(which Cloud Scheduler uses) applies OR semantics when both day-of-month and day-of-week are
restricted, so a single cron expression can't express "Nth weekday of month" — `"0 22 1-7 * 0"`
would actually fire every day 1st–7th *and* every Sunday. Instead, the scheduler triggers the
job **daily** at 22:00 UTC, and `RecipeOfTheMonthJob.execute()` checks `isFirstSundayOfMonth()`
first and exits immediately (no Firebase reads, no Veo calls, no cost) on every other day:

```bash
gcloud scheduler jobs create http rotw-monthly \
  --schedule "0 22 * * *" \
  --uri "https://$GCP_REGION-run.googleapis.com/apis/run.googleapis.com/v1/namespaces/$GCP_PROJECT_ID/jobs/rotw-job:run" \
  --oauth-service-account-email rotw-scheduler@$GCP_PROJECT_ID.iam.gserviceaccount.com \
  --location $GCP_REGION
```

## Firestore data written by this job

See `.ai/firestore-schema.md` for the full model.

```
recipe_of_the_month/{autoId}
  recipeId:    string     # ID of the selected recipe
  recipeTitle: string
  videoUrl:    string     # Firebase Storage HTTPS URL, e.g. videos/rotw/2026-04-abc123.mp4
  monthOf:     string     # "YYYY-MM"
  createdAt:   Timestamp  # the app shows the latest by createdAt

video_generation_history/{recipeId}
  selectedAt:  Timestamp  # permanent; never deleted (null for entries migrated from RTDB)
```

The `recipes/{id}.videoUrl` field is also updated on the selected recipe so the Android app can show the video in the recipe detail screen.

## Current approximate cost

| Item | Cost |
|---|---|
| Veo 3.1 Lite video generation (8s, with audio, 720p) | ~$0.40 per run (unverified — check actual billing) |
| Firebase Storage (video retained indefinitely) | ~$0.026/GB/month |
| Cloud Run Job execution | negligible |
| Cloud Scheduler | free tier |

**Note:** `veo-3.1-lite-generate-001` is GA on Vertex AI as of April 2026. Verify the first real run's cost against actual GCP billing.
