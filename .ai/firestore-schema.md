# Firestore Schema

Target data model for the RTDB → Firestore migration (CHE-50, "Fas 0"). Until the cut-over the app still runs on RTDB — see `.ai/database-schema.md` for the live schema.

## Database

| Setting | Value | Why |
|---|---|---|
| Project | `idyllic-bloom-425307-r6` | |
| Database ID | `(default)` | Free tier only applies to `(default)` |
| Edition / mode | Standard edition, Native mode | |
| Location | `europe-north2` (Stockholm, regional) | Users are mostly in Sweden: lowest latency and data stays in Sweden. Same price class as `europe-north1`; multi-region (`eur3`) not worth ~2× price at this scale. Cloud Run is available here too, so the backend can be co-located. **Cannot be changed after creation.** |

Sizing assumption: ≤ 100 concurrent users, read-heavy, writes may be slightly slower. Expected cost: within the free tier (50k reads / 20k writes / 20k deletes per day, 1 GiB).

## Conventions

- **Document IDs:** every migrated document keeps its RTDB key (push ID / UID / recipe ID) as its document ID, so existing links and references keep working. New documents use Firestore auto-IDs.
- **No ordering by ID.** RTDB push IDs are chronological; Firestore auto-IDs are random. Anything that needs ordering has an explicit Timestamp field and is sorted on it (server-side `orderBy`, or client-side to avoid composite indexes). During migration the timestamp is decoded from the push ID (first 8 chars = milliseconds since epoch, base-64 with the alphabet `-0-9A-Z_a-z`) when no timestamp field exists.
- **Timestamps:** all time fields are Firestore `Timestamp` (not ISO strings). The app writes client time (converted from its ISO strings via `FirestoreTime`). Migration parses the existing mixed ISO formats (`…+00:00` with microseconds from Python, `…Z` from Kotlin).
- **Field names** are unchanged from RTDB (incl. snake_case collection names) to minimise model churn.
- **Quantities stay strings** (`"1/2"`, `"1 (400g)"`, `"250-300"`); parsing is the consumer's job (e.g. the backend's shopping list).
- **Embedded `id`:** `recipes` and `lists` keep the duplicated `id` field in the document data (as today). Do not combine this with `@DocumentId` on the same property — the Android SDK throws on read when both exist.

## Tree

```
recipes/{recipeId}
└── variants/{variantId}
recipe_of_the_month/{pushId}
video_generation_history/{recipeId}
users/{uid}
├── chat_history/{messageId}
├── liked_messages/{messageId}
└── lists/{listId}
```

---

## `recipes/{recipeId}`

```
Recipe {
  id:                  string           // == document ID
  uid:                 string?          // owner UID; ABSENT = orphaned (owner "removed" it via removeRecipeUid). Never null: Android's Recipe.uid is non-null, so toObject() would throw
  title:               string
  summary:             string           // may be ""
  difficulty:          "EASY" | "MEDIUM"
  cookingTime:         string           // human-readable, may be ""
  prepTime:            string           // may be ""
  servings:            string
  imageUrl:            string           // Firebase Storage HTTPS URL
  videoUrl:            string?          // set by rotw-job when featured
  isFavourite:         boolean          // true = visible in community view
  copyId:              string?          // set when this recipe is a copy of another user's recipe (source recipe ID)
  tipsAndTricks:       string           // may be ""
  tags:                string[]         // lowercase; see tag categories in database-schema.md
  ingredients:         { name: string, quantity: string, unit: string }[]
  instructions:        string[]
  nutrientsPerServing: { name: string, quantity: string, unit: string }[]
  updatedAt:           Timestamp
}
```

Queries:
- My recipes: `where uid == <uid>`
- All recipes (`loadAllRecipes`, used by home + collection): whole collection, filtered client-side. Narrow to `where isFavourite == true` if read volume ever matters.
- By tag: `where tags array-contains <tag>` (max one `array-contains` per query).
- Single recipe: `doc(recipeId)`.

Sorting is done client-side, newest `updatedAt` first (the collection is small). No composite indexes needed.

### `recipes/{recipeId}/variants/{variantId}`

Was `recipe_variants/{recipeId}/{variantId}` in RTDB. No owner field — ownership is the parent recipe's `uid`.

```
RecipeVariant {
  id:                  string        // == document ID
  label:               string
  createdAt:           Timestamp
  isPinned:            boolean
  title:               string
  summary:             string
  servings:            string?
  prepTime:            string?
  cookingTime:         string?
  nutrientsPerServing: Nutrient[]?
  ingredients:         Ingredient[]
  difficulty:          "EASY" | "MEDIUM"?
  instructions:        string[]
  tipsAndTricks:       string?
}
```

Deleting a recipe does **not** delete its `variants` subcollection (same orphaning behaviour as RTDB today). A collection-group query on `variants` is available to the backend.

---

## `recipe_of_the_month/{pushId}`

Written only by `rotw-job` (Admin SDK). The job picks a *random* not-yet-featured favourite; the app shows the *latest* record.

```
RecipeOfTheMonth {
  recipeId:    string
  recipeTitle: string
  videoUrl:    string
  monthOf:     string      // "YYYY-MM"
  createdAt:   Timestamp
}
```

Latest: `orderBy createdAt desc, limit 1`.

## `video_generation_history/{recipeId}`

Written only by `rotw-job`. Existence of the document = recipe has been featured. Never deleted.

```
{ selectedAt: Timestamp | null }   // null for entries migrated from RTDB (it only stored `true`)
```

---

## `users/{uid}`

One document per user. Single-object data lives as map fields on this document (one read on load).

```
User {
  betaInteractionCount: number                 // absent = 0; may only increase by exactly 1 (rules); client increments in a transaction (returns the new value)
  preferences: {                               // absent until first detected
    summary:   string
    updatedAt: Timestamp
  }?
  cooking_resources: {                         // absent until first fetched
    resources: { title: string, url: string, type: string, description: string }[]
    updatedAt: Timestamp
  }?
}
```

### `users/{uid}/chat_history/{messageId}`

```
ChatMessage {
  role:      "user" | "model"
  parts:     { text: string }[]
  createdAt: Timestamp          // NEW — required for ordering; migrated from push-ID timestamp
}
```

Last 20: `orderBy createdAt, limitToLast 20`.

### `users/{uid}/liked_messages/{messageId}`

```
LikedMessage {
  text:    string
  likedAt: Timestamp
}
```

### `users/{uid}/lists/{listId}`

```
RecipeList {
  id:        string      // == document ID
  name:      string
  recipeIds: string[]    // ordered; add/remove with arrayUnion / arrayRemove (atomic, no read-modify-write)
  createdAt: Timestamp   // storage-only (not on the Kotlin model); lists are shown in creation order. Migrated from push-ID time
}
```

---

## Security semantics (implemented in `firestore.rules`, pass 2)

| Path | Read | Write |
|---|---|---|
| `recipes/{id}` | any signed-in user | create: `uid` must be caller (or absent/null); update/delete: only current owner; owner may remove `uid` (orphan) |
| `recipes/{id}/variants/{v}` | any signed-in user | only the parent recipe's owner (`get()` on parent) |
| `recipe_of_the_month`, `video_generation_history` | public | nobody (Admin SDK bypasses rules) |
| `users/{uid}` and subcollections | owner only | owner only; `betaInteractionCount` may only change by +1 (absent → 1); user doc may not be deleted |

## Indexes

Only Firestore's automatic single-field indexes are used. They cover every query above (`uid ==`, `isFavourite ==`, `tags array-contains`, `orderBy createdAt`). No composite indexes and no index exemptions are configured.

Both can be added later without downtime or data migration (Firestore backfills the index in the background). Add a composite index only when a query filters on one field and sorts on another server-side; add exemptions for large unqueried fields (`instructions`, `chat_history.parts`) only if index storage/write cost ever becomes noticeable.

## Reserved for the backend / MCP server (not part of Fas 0)

- `search_recipes` — tag/flag filters first. Firestore has no full-text search. Ingredient search needs a derived, normalised field (e.g. `ingredientNames: string[]`) because `array-contains` on `ingredients` only matches an entire `{name, quantity, unit}` object.
- Vector search — optional `embedding: Vector` on `recipes` + vector index, queried with `findNearest`. Adds ~4–12 kB per recipe read; decide placement when the backend is built.
- `plan_week` — `users/{uid}/meal_plans/{isoWeek}` (e.g. `2026-W41`); deterministic ID makes re-runs idempotent.
- `generate_shopping_list` — `users/{uid}/shopping_lists/{isoWeek}`.
- Beta quota — move `betaInteractionCount` enforcement into the backend (server-side increment + check). Rules can only guarantee "+1 per write", not that the client actually calls it.
