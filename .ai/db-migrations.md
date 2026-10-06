# Firebase Realtime Database Migrations

This file documents schema migrations applied to the Chef Firebase Realtime Database, and the process used to perform them.

---

## Migration Process

The database has no formal migration framework. The standard process is:

1. **Backup first** — export the full database before any changes (see CLAUDE.md for the `firebase database:get` command). Backups go in `db-backups/` (gitignored).
2. **Prepare a migration file** — edit a copy of the backup JSON to apply the schema change. Store migration files in `db-backups/` next to the backup (gitignored, never committed). **Never** put them under `vertexai/app/src/main/assets/` — assets are shipped verbatim in the APK/AAB, and the `verifyAssets` Gradle task fails the build if anything but `chat_system_prompt.txt` is there (CHE-49).
3. **Validate locally** — review the diff between the backup and the migration file to confirm only intended changes are present.
4. **Upload manually** — the user imports the migration file via the Firebase Console (`Import JSON` on the Realtime Database page). This overwrites the entire database, so the backup in step 1 is critical.
5. **Document here** — add an entry below describing what changed, why, and the file used.

> **Note:** Backup and migration files are full database snapshots and contain user data (`users/{uid}` incl. `chat_history` and `preferences`). Keep them local, and delete them once the migration is verified.

---

## Migration Log

### 2026-04 — Add `tags` to all existing recipes (CHE-12)

**File:** `chef-db-backup-04-2026.json` (deleted 2026-10-06 in CHE-49; it had been packaged into the APK by mistake)
**Scope:** All 49 recipes in the `recipes` node.
**Change:** Added a `tags: string[]` field to every recipe. New recipes going forward have tags generated automatically by the Gemini JSON extraction model at save time (see `DERIVE_RECIPE_JSON_SYSTEM_INSTRUCTIONS` in `GenerativeAiViewModelFactory.kt`).

**How tags were applied retroactively:**

A Python script read the backup JSON and assigned tags to each recipe based on title and ingredient analysis. Tags follow four categories: main ingredient, cuisine, effort level, and season/occasion. All tags are lowercase.

The script logic for each recipe:
- **Main ingredient** — inferred from title keywords (`chicken`, `beef`, `lamb`, `pork`, `fish`) and ingredient list. Vegetarian/vegan where no meat or fish is present.
- **Cuisine** — inferred from dish name and characteristic spice/ingredient patterns (e.g. gochujang → `korean`, garam masala → `indian`, tagliatelle → `italian`).
- **Effort** — parsed from `cookingTime` field: ≤25 min → `under 30 minutes`, ≤60 min → `under 1 hour`, ~1–2 hrs → `1-2 hours`, 2+ hrs → `slow cook`.
- **Occasion** — `weeknight` applied to quick, single-pan, or stir-fry dishes.

Notable cases:
- *Healthier Moussaka* — tagged `lamb` and `beef` because the recipe specifies "ground lamb or beef".
- *West African Peanut Stew* — tagged `beef` (recipe uses beef stew meat despite the name suggesting vegetarian).
- *Elevated Hummus with Spiced Beef* — tagged `beef` and `middle eastern` (not `vegetarian` despite hummus base).
- *Fluffy Coconut Rice* — tagged `vegetarian` and `vegan` as a standalone side dish with no animal products.

**How to reproduce / update:**

```python
# Load the backup, assign tags, write back
import json

TAGS = {
    "<recipe-push-id>": ["tag1", "tag2", ...],
    # ...
}

with open('path/to/backup.json') as f:
    data = json.load(f)

for rid, tags in TAGS.items():
    data['recipes'][rid]['tags'] = tags

with open('path/to/output.json', 'w') as f:
    json.dump(data, f, indent=2, ensure_ascii=False)
```

Then import `output.json` via the Firebase Console → Realtime Database → (kebab menu) → Import JSON.
