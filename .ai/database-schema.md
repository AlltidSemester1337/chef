# Firebase Realtime Database Schema

Derived from `vertexai/app/src/main/assets/idyllic-bloom-425307-r6-default-rtdb-export.json`.

## Root Structure

```
ROOT
├── recipes    — all recipe objects, keyed by Firebase push ID
└── users      — user profiles keyed by Firebase Auth UID
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
  isFavourite:        boolean
  tipsAndTricks:      string        // Free-text tips (may be empty "")
  updatedAt:          string        // ISO 8601 timestamp, e.g. "2025-02-12T13:58:18.650875+00:00"
  ingredients:        Ingredient[]
  instructions:       string[]      // Ordered instruction steps
  nutrientsPerServing: Nutrient[]
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
  chat_history: { [pushId]: ChatMessage }   // Map of chat messages keyed by Firebase push ID
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
│       └── nutrientsPerServing: array
│           └── [n]: { name: string, quantity: string, unit: string }
│
└── users (object)
    └── {uid} (object)
        └── chat_history (object)
            └── {pushId} (object)
                ├── role:  "user" | "model"
                └── parts: array
                    └── [n]: { text: string }
```

---

## Field Type Reference

| Field | Type | Notes |
|---|---|---|
| `difficulty` | string (enum) | "EASY" or "MEDIUM" |
| `isFavourite` | boolean | |
| `id` | string | Firebase push ID, duplicated inside the object |
| `uid` | string | Firebase Auth UID |
| `updatedAt` | string | ISO 8601 with timezone offset |
| `cookingTime`, `prepTime` | string | Human-readable duration or empty string |
| `quantity` (ingredient/nutrient) | string | Numeric or fractional; stored as string not number |
| `unit` (ingredient/nutrient) | string | May be empty string |
| `role` (chat) | string (enum) | "user" or "model" |
| `imageUrl` | string | `https://storage.googleapis.com/idyllic-bloom-425307-r6.firebasestorage.app/recipes/...` |

---

## Notes

- **Arrays:** `ingredients`, `instructions`, `nutrientsPerServing`, and `parts` are stored as JSON arrays (not Firebase push-ID maps).
- **`chat_history`:** Stored as a push-ID keyed map, not an array.
- **Quantities as strings:** Both ingredient quantities and nutrient amounts are strings (not numbers), allowing fractional notation like `"1/2"`.
- **Recipes are global:** The `recipes` node is a flat collection; ownership is tracked via the `uid` field on each recipe.
- **Empty strings:** Several string fields (`cookingTime`, `prepTime`, `summary`, `tipsAndTricks`, `unit`) may be empty strings rather than absent.
