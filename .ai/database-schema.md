# Firebase Realtime Database Schema

Derived from `vertexai/app/src/main/assets/idyllic-bloom-425307-r6-default-rtdb-export.json`.

## Root Structure

```
ROOT
├── recipes                  — all recipe objects, keyed by Firebase push ID
├── recipe_of_the_month      — monthly featured recipe entries, keyed by Firebase push ID
├── video_generation_history — permanent set of recipe IDs that have been featured; never deleted
└── users                    — user profiles keyed by Firebase Auth UID
```

---

## `recipes` Node

**Path:** `recipes/{pushId}`
Each recipe is a top-level child with a Firebase push ID as key.

```
Recipe {
  id:                 string        // Firebase push ID (same as the node key)
  uid:                string        // Firebase Auth UID of the owner
  title:              string        // Recipe name
  summary:            string        // Long description with yield and nutrition overview (may be empty "")
  difficulty:         string        // Enum: "EASY" | "MEDIUM"
  cookingTime:        string        // e.g. "20-25 minutes", "1.5-2 hours", or "" if unset
  prepTime:           string        // e.g. "20 minutes", or "" if unset
  servings:           string        // e.g. "4 servings", "2 portions"
  imageUrl:           string        // Firebase Storage HTTPS URL
  videoUrl:           string?       // Firebase Storage HTTPS URL to Recipe of the Month video; absent or null if not selected
  isFavourite:        boolean
  tipsAndTricks:      string        // Free-text tips (may be empty "")
  updatedAt:          string        // ISO 8601 timestamp, e.g. "2025-02-12T13:58:18.650875+00:00"
  ingredients:        Ingredient[]
  instructions:       string[]      // Ordered instruction steps
  nutrientsPerServing: Nutrient[]
  tags:               string[]      // Optional. Auto-generated at save time. Lowercase descriptive tags. See Tag Categories below.
}
```

### `Ingredient`

```
Ingredient {
  name:     string    // Full description, e.g. "ground lamb or a mix of ground lamb and beef"
  quantity: string    // Numeric or fractional, e.g. "500", "1/2", "1 (400g)"
  unit:     string    // e.g. "g", "ml", "tsp", "tbsp", or "" when unit-less
}
```

### `Nutrient`

```
Nutrient {
  name:     string    // e.g. "Calories", "Protein", "Carbohydrates", "Fat", "Fiber", "Sugar"
  quantity: string    // e.g. "550", "30", "250-300"
  unit:     string    // e.g. "kcal", "g", or ""
}
```

---

## `users` Node

**Path:** `users/{uid}`
Each user is keyed by their Firebase Auth UID.

```
User {
  chat_history:    { [pushId]: ChatMessage }    // Map of chat messages keyed by Firebase push ID
  liked_messages:  { [pushId]: LikedMessage }   // Map of liked AI responses keyed by Firebase push ID
  lists:           { [pushId]: RecipeList }     // Map of user-created recipe lists keyed by Firebase push ID
}
```

### `ChatMessage`

**Path:** `users/{uid}/chat_history/{pushId}`

```
ChatMessage {
  role:  string        // Enum: "user" | "model"
  parts: MessagePart[]
}
```

### `MessagePart`

```
MessagePart {
  text: string    // Message content; can be long markdown-formatted AI response
}
```

### `LikedMessage`

**Path:** `users/{uid}/liked_messages/{pushId}`

```
LikedMessage {
  text:     string    // The model message text that was liked
  likedAt:  string    // ISO 8601 timestamp
}
```

### `RecipeList`

**Path:** `users/{uid}/lists/{pushId}`
User-created named lists for organizing recipes (CHE-13).

```
RecipeList {
  id:        string      // Firebase push ID (same as node key)
  name:      string      // User-provided list name, e.g. "Work week planning"
  recipeIds: string[]    // Ordered list of recipe IDs belonging to this list
}
```

---

## Complete Tree

```
ROOT
├── recipes (object)
│   └── {pushId} (object)
│       ├── id:                 string
│       ├── uid:                string
│       ├── title:              string
│       ├── summary:            string
│       ├── difficulty:         "EASY" | "MEDIUM"
│       ├── cookingTime:        string
│       ├── prepTime:           string
│       ├── servings:           string
│       ├── imageUrl:           string
│       ├── isFavourite:        boolean
│       ├── tipsAndTricks:      string
│       ├── updatedAt:          string (ISO 8601)
│       ├── ingredients:        array
│       │   └── [n]: { name: string, quantity: string, unit: string }
│       ├── instructions:       array
│       │   └── [n]: string
│       ├── nutrientsPerServing: array
│       │   └── [n]: { name: string, quantity: string, unit: string }
│       └── tags:               array (optional)
│           └── [n]: string
│
└── users (object)
    └── {uid} (object)
        ├── chat_history (object)
        │   └── {pushId} (object)
        │       ├── role:  "user" | "model"
        │       └── parts: array
        │           └── [n]: { text: string }
        ├── liked_messages (object)
        │   └── {pushId} (object)
        │       ├── text:     string   // model message text the user liked
        │       └── likedAt:  string   // ISO 8601 timestamp
        └── lists (object)
            └── {pushId} (object)
                ├── id:        string   // same as pushId
                ├── name:      string   // user-provided list name
                └── recipeIds: array
                    └── [n]: string    // recipe push ID
```

### Tag Categories

Tags are generated automatically by the JSON extraction model at recipe save time. All tags are lowercase. Four categories are used:

| Category | Examples |
|---|---|
| Main ingredient | `chicken`, `beef`, `lamb`, `pork`, `fish`, `vegetarian`, `vegan` |
| Cuisine | `italian`, `indian`, `moroccan`, `middle eastern`, `french`, `korean`, `thai`, `japanese`, `mexican`, `spanish`, `indonesian`, `peruvian`, `south american`, `west african`, `mediterranean`, `greek`, `asian` |
| Effort / time | `under 30 minutes`, `under 1 hour`, `1-2 hours`, `slow cook` |
| Season / occasion | `weeknight`, `christmas`, `easter`, `summer`, `spring`, `baking` |

Tags are optional and absent on recipes created before CHE-12 unless retroactively migrated (see `.ai/db-migrations.md`).

---

## `recipe_of_the_month` Node

**Path:** `recipe_of_the_month/{pushId}`
Written by the `rotw-job` Cloud Run job on the first Sunday of each month (CHE-29).

```
RecipeOfTheMonth {
  recipeId:    string    // push ID of the selected recipe in `recipes`
  recipeTitle: string    // recipe title (denormalised for fast home screen render)
  videoUrl:    string    // Firebase Storage HTTPS URL, e.g. videos/rotw/2026-04.mp4
  monthOf:     string    // "YYYY-MM", e.g. "2026-04"
  createdAt:   string    // ISO 8601 timestamp
}
```

---

## `video_generation_history` Node

**Path:** `video_generation_history/{recipeId}: true`
Permanent record of every recipe ID that has been selected for a Recipe of the Month video.
Written by `rotw-job` at generation time. **Never deleted** — ensures the same recipe is never
featured twice, even if `recipe_of_the_month` entries are cleaned up.

---

## Field Type Reference

| Field | Type | Notes                                                                         |
|---|---|-------------------------------------------------------------------------------|
| `difficulty` | string (enum) | "EASY" or "MEDIUM"                                                            |
| `isFavourite` | boolean |                                                                               |
| `id` | string | Firebase push ID, duplicated inside the object                                |
| `uid` | string | Firebase Auth UID                                                             |
| `updatedAt` | string | ISO 8601 with timezone offset                                                 |
| `cookingTime`, `prepTime` | string | Human-readable duration or empty string                                       |
| `quantity` (ingredient/nutrient) | string | Numeric or fractional; stored as string not number                            |
| `unit` (ingredient/nutrient) | string | May be empty string                                                           |
| `role` (chat) | string (enum) | "user" or "model"                                                             |
| `imageUrl` | string | `https://storage.googleapis.com/{PROJECT_ID}.firebasestorage.app/recipes/...` |
| `videoUrl` | string? | Firebase Storage URL to ROTW video; absent/null on recipes not selected       |
| `tags` | string[] | Optional; absent on pre-CHE-12 recipes. All values lowercase. |

---

## Notes

- **Arrays:** `ingredients`, `instructions`, `nutrientsPerServing`, and `parts` are stored as JSON arrays (not Firebase push-ID maps).
- **`chat_history`:** Stored as a push-ID keyed map, not an array.
- **Quantities as strings:** Both ingredient quantities and nutrient amounts are strings (not numbers), allowing fractional notation like `"1/2"`.
- **Recipes are global:** The `recipes` node is a flat collection; ownership is tracked via the `uid` field on each recipe.
- **Empty strings:** Several string fields (`cookingTime`, `prepTime`, `summary`, `tipsAndTricks`, `unit`) may be empty strings rather than absent.
