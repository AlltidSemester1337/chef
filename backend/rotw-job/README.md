# rotw-job — Recipe of the Month Video Generator

A Cloud Run Job that runs monthly to:
1. Pick a random favourite recipe that has never been featured before
2. Generate an 8-second cinematic food video (with audio) via **Google Veo 3.1 Lite** (Vertex AI, GA)
3. Upload the video to **Firebase Storage** under `videos/rotw/YYYY-MM-{recipeId}.mp4`
4. Write a record to the `recipe_of_the_month` RTDB node
5. Update `videoUrl` on the selected recipe in the `recipes` RTDB node
6. Mark the recipe as used in `video_generation_history` so it is never selected again

## Prerequisites

- JDK 17
- A GCP project with the **Vertex AI API** enabled
- A Firebase project (Realtime Database + Storage)
- A service account with the following roles:
  - `roles/aiplatform.user` — for Veo 3.1 video generation
  - `roles/firebase.admin` — for RTDB reads/writes
  - `roles/storage.objectAdmin` — for Firebase Storage uploads
- The service account key exported as a JSON file (for local runs)

## Environment variables

| Variable | Required | Description |
|---|---|---|
| `GOOGLE_APPLICATION_CREDENTIALS` | Yes | Absolute path to service account key JSON |
| `GCP_PROJECT_ID` | Yes | GCP project ID (e.g. `my-project-123`) |
| `FIREBASE_DB_URL` | Yes | Firebase RTDB URL (e.g. `https://my-project-default-rtdb.firebaseio.com`) |
| `FIREBASE_STORAGE_BUCKET` | Yes | Firebase Storage bucket (e.g. `my-project.appspot.com`) |
| `GCP_LOCATION` | Yes | Vertex AI region (e.g. `us-central1`) |

## Running locally

```bash
# From the repo root
export GOOGLE_APPLICATION_CREDENTIALS=/path/to/service-account.json
export GCP_PROJECT_ID=my-project-123
export GCP_LOCATION=us-central1
export FIREBASE_DB_URL=https://my-project-default-rtdb.firebaseio.com
export FIREBASE_STORAGE_BUCKET=my-project.appspot.com

./gradlew :backend:rotw-job:run
```

> **Note:** A real run generates a video via Veo 3.1 Lite (GA model, with audio), which costs roughly $0.40/video depending on current billing — see "Current approximate cost" below. Only run against production credentials when you intend to produce an actual video.

## Running tests (no API calls, no cost)

```bash
./gradlew :backend:rotw-job:test
```

The test suite covers `selectRecipe()` (recipe selection logic) and `GeminiPromptBuilder` (prompt construction). Both are pure functions with no external dependencies.

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
  --set-env-vars "GCP_PROJECT_ID=$GCP_PROJECT_ID,GCP_LOCATION=$GCP_REGION,FIREBASE_DB_URL=...,FIREBASE_STORAGE_BUCKET=..." \
  --set-secrets "GOOGLE_APPLICATION_CREDENTIALS=rotw-sa-key:latest"
```

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

## Firebase RTDB schema written by this job

```
recipe_of_the_month/
  {pushId}/
    recipeId:    string   # ID of the selected recipe
    recipeTitle: string
    videoUrl:    string   # Firebase Storage HTTPS download URL, e.g. videos/rotw/2026-04-abc123.mp4
    monthOf:     string   # "YYYY-MM"
    createdAt:   string   # ISO-8601 timestamp

video_generation_history/
  {recipeId}: true        # permanent; never deleted
```

The `recipes/{id}/videoUrl` field is also updated on the selected recipe so the Android app can show the video in the recipe detail screen.

## Current approximate cost

| Item | Cost |
|---|---|
| Veo 3.1 Lite video generation (8s, with audio, 720p) | ~$0.40 per run (unverified — check actual billing) |
| Firebase Storage (video retained indefinitely) | ~$0.026/GB/month |
| Cloud Run Job execution | negligible |
| Cloud Scheduler | free tier |

**Note:** `veo-3.1-lite-generate-001` is GA on Vertex AI as of April 2026. Verify the first real run's cost against actual GCP billing.
