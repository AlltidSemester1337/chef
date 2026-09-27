# Play Store listing — Chef

This folder holds everything needed to publish Chef on Google Play, starting with the **internal testing** track (CHE-41). The layout follows [fastlane supply](https://docs.fastlane.tools/actions/supply/) (`listings/<locale>/…`), so the upload can be automated later.

| File | Play Console field | Limit |
|---|---|---|
| `listings/en-US/title.txt` | App name | 30 chars |
| `listings/en-US/short_description.txt` | Short description | 80 chars |
| `listings/en-US/full_description.txt` | Full description | 4000 chars |
| `listings/en-US/video_url.txt` | YouTube video | public/unlisted, ads off, not age-restricted |
| `listings/en-US/images/icon.png` | App icon | 512×512 PNG, ≤1 MB |
| `listings/en-US/images/featureGraphic.png` | Feature graphic | 1024×500 PNG/JPEG |
| `listings/en-US/images/phoneScreenshots/*.png` | Phone screenshots | 2–8, 320–3840 px, max 2:1 ratio |
| `data-safety.md` | App content → Data safety | — |
| `../docs/privacy-policy.md` | App content → Privacy policy URL | public URL |
| `../docs/delete-account.md` | Data safety → Delete account URL | public URL |

Public URLs (valid once merged to `main`):
- Privacy policy: https://github.com/AlltidSemester1337/chef/blob/main/docs/privacy-policy.md
- Account deletion: https://github.com/AlltidSemester1337/chef/blob/main/docs/delete-account.md

## 1. One-time: create the upload key

Play App Signing holds the real app signing key. You only keep an *upload key*. Generate it **outside the repo** and back it up (password manager plus offline copy). If you lose it, you can ask Play support to reset it, but that takes days.

```bash
keytool -genkeypair -v -keystore ~/keystores/chef-upload.jks -alias chef-upload -keyalg RSA -keysize 2048 -validity 10000
```

Then add these to `local.properties`:

```properties
releaseStoreFile=/home/<you>/keystores/chef-upload.jks
releaseStorePassword=...
releaseKeyAlias=chef-upload
releaseKeyPassword=...
```

## 2. Build the bundle

```bash
./gradlew :vertexai:app:bundleRelease
jarsigner -verify vertexai/app/build/outputs/bundle/release/app-release.aab
```

Bump `versionCode` in `vertexai/app/build.gradle.kts` for every upload after the first.

## 3. Play Console checklist

**Create app** (All apps → Create app)
- [ ] App name `Chef – AI Cooking Assistant`, default language English (United States) – en-US, **App**, **Free**
- [ ] Accept the Developer Program Policies and US export laws declarations

**App content** (Policy → App content). Complete every card:
- [ ] Privacy policy: the privacy policy URL above
- [ ] App access: **All or some functionality is restricted**. Add instructions plus a reviewer login: an email-verified test account that is on the admin allowlist (`AdminUsers`) so it isn't blocked by the 20-interaction beta quota
- [ ] Ads: **No, my app does not contain ads**
- [ ] Content rating: category **All other app types**. Answer *No* to violence, sexuality, language, controlled substances and gambling. Answer *Yes* to "users can interact/exchange content": users see AI-generated content and recipes saved by others in a shared collection. The expected rating is around PEGI 3 / Everyone, plus an "Users interact" note
- [ ] Target audience: **18+** (AI chat with a shared collection; simplest compliance). Appeals to children: **No**
- [ ] News app: **No**
- [ ] Data safety: follow `data-safety.md`, with the deletion URL above
- [ ] Government apps: **No**. Financial features: **None**. Health: **No**
- [ ] Generative AI: if Play asks, **Yes**. AI-generated text and images, plus the in-app disclaimer (`LegalInfoDialog`)

**Store listing** (Grow users → Store presence → Main store listing)
- [ ] Paste the title and short/full descriptions, and upload the icon, feature graphic, screenshots and video URL
- [ ] App category **Food & Drink**, contact email humlekottekonsult@gmail.com

**Internal testing** (Test and release → Testing → Internal testing)
- [ ] Testers tab → create an email list (up to 100 Google accounts) → save
- [ ] Create new release → opt in to **Play App Signing** (use the Google-generated key) → upload `app-release.aab` → release name `4.0.0 (1)` → release notes → **Save → Review → Start rollout**
- [ ] **Firebase + App Check (required: release builds use Play Integrity):** copy the **App signing key** SHA-256 and the **Upload key** SHA-256 from Play Console → Test and release → App integrity. Add both to Firebase Console → Project settings → Android app `com.formulae.chef` → SHA certificate fingerprints, and register the app-signing SHA-256 under Firebase App Check → Apps → Play Integrity. Also link the Firebase/GCP project under App integrity → Play Integrity API. Without this, App Check rejects the Play-installed build.
- [ ] Copy the **"Join on the web"** opt-in link and share it with testers. After opting in, they install through the Play Store link. Internal releases are usually available within minutes and skip full review

## Later: production

A personal developer account created after Nov 2023 must run a **closed test with at least 12 opted-in testers for 14 consecutive days** before it can apply for production access. Organisation accounts are exempt. This doesn't affect internal testing.
