# Multiple recipe suggestions with images in grid chat reply

We want to redesign the way Chef replies when giving one or more recipe suggestions (refer to JSON output in response).
- When there is 0 recipe suggestions in response, output only text.
- When there is one or more recipe suggestions in the response, update the code to immediately create new Recipe entities.
- Update Recipe entity creation so that first all text details are populated, then images are generated and appended. Run multiple recipe creations in parallel.
- Update the chat frontend to create a grid view where titles and images are displayed. Maximum 3 cards per row in the grid, then start a new row.
- Display titles and show spinners while images are loading on the cards.
- When the user interacts by clicking on a recipe card, navigate to the recipe details screen (reuse DetailScreen.kt). Back button returns the user to chat.

## Decisions

- **Recipe persistence**: Recipes are in-memory only during the session. Starring a card saves it to Firebase. Imagen is called per card to display the image, but nothing is stored unless starred.
- **Starring from grid**: Since recipe details are already derived, starring skips re-derivation and calls `RecipeRepositoryImpl.saveRecipe()` directly.
- **Back navigation**: `DetailRoute` gets a `backRoute: String` parameter so both collection and chat can reuse it.
- **Text streaming**: Deferred to a separate story.
- **Chat history**: After app restart, replies that showed recipe grids fall back to plain text. Reconstructing grid state from persisted history is out of scope.

## Implementation plan

### Step 1 — Extend `ChatMessage` to carry recipes

Add `recipes: List<Recipe>` and `starredRecipeIds: Set<String>` to `ChatMessage`. When `recipes` is non-empty the UI renders a grid instead of a text bubble. `isStarred` (single boolean) is replaced by `starredRecipeIds` for per-card state.

File: `feature/chat/ui/ChatMessage.kt`

### Step 2 — Parallelize and decouple recipe + image creation in `ChatViewModel`

- Split `createRecipeFromJson()` into two steps: text-only (fast) and image (slow).
- Make `createImageForRecipe` a proper `suspend` fun — remove `runBlocking`.
- After receiving a model reply, run JSON extraction once via `deriveRecipesFromMessage()`.
- Build all text-complete `Recipe` objects first, emit them to the UI immediately (cards show title + spinner).
- Then launch image generation in parallel with `coroutineScope { recipes.map { async { ... } }.awaitAll() }` and update each recipe as its image resolves.
- Expose a `MutableStateFlow<List<Recipe>>` per pending message to feed live image updates to the UI.

File: `feature/chat/ChatViewModel.kt`

### Step 3 — Auto-detect and branch on model response

In `sendMessage()`, after getting the model reply:
- Attempt to call `deriveRecipesFromMessage()` on the response.
- If 1+ recipes are returned: add a `ChatMessage(recipes = ...)` to the UI state.
- If 0 recipes (or extraction fails): add a plain text `ChatMessage` as today.

File: `feature/chat/ChatViewModel.kt`

### Step 4 — Starring from grid saves directly to Firebase

Add `onRecipeStarredFromGrid(recipe: Recipe)` to `ChatViewModel`:
- Sets `uid`, `isFavourite = true` on the in-memory recipe.
- Calls `_recipeRepositoryImpl.saveRecipe(recipe)` directly (no re-derivation).
- Updates `starredRecipeIds` in the matching `ChatMessage`.

File: `feature/chat/ChatViewModel.kt`

### Step 5 — Chat UI: recipe grid composable

In `ChatBubbleItem`, if `chatMessage.recipes.isNotEmpty()`:
- Render a recipe grid: chunk recipes into rows of 3, each row a `Row`, each card a custom `RecipeSuggestionCard`.
- `RecipeSuggestionCard`: title text + `AsyncImage` (Coil) with `CircularProgressIndicator` placeholder + star `IconButton`.
- Wire `onRecipeClick: (Recipe) -> Unit` and `onStarClick: (Recipe) -> Unit`.

File: `feature/chat/ui/ChatScreen.kt`

### Step 6 — Navigation: chat → detail → back to chat

- Add `backRoute: String` parameter to `DetailRoute` in `DetailScreen.kt` (replacing the hardcoded `"collection"`).
- Update the existing call site in `CollectionScreen.kt` to pass `backRoute = "collection"`.
- Pass `navController` into `ChatRoute` (update `AppNavigation.kt` and `ChatScreen.kt`).
- Add a `selectedRecipeFromChat: StateFlow<Recipe?>` to `ChatViewModel`.
- In `AppNavigation`, add composable `"chatRecipeDetail"` that reads `selectedRecipeFromChat` from `ChatViewModel` and renders `DetailRoute(backRoute = "chat")`.
- On recipe card click: set `selectedRecipeFromChat` in ViewModel, then `navController.navigate("chatRecipeDetail")`.

Files: `feature/collection/ui/DetailScreen.kt`, `feature/chat/ui/ChatScreen.kt`, `feature/chat/ChatViewModel.kt`, `AppNavigation.kt`

### Files to change summary

| File | Change |
|---|---|
| `feature/chat/ui/ChatMessage.kt` | Add `recipes`, `starredRecipeIds` fields; remove single `isStarred` |
| `feature/chat/ChatUiState.kt` | Add `updateRecipeStarred()` helper |
| `feature/chat/ChatViewModel.kt` | Auto-detect recipes; parallel image gen; selectedRecipeFromChat state; onRecipeStarredFromGrid |
| `feature/chat/ui/ChatScreen.kt` | RecipeSuggestionCard composable; grid layout; navController param |
| `feature/collection/ui/DetailScreen.kt` | Add `backRoute: String` to `DetailRoute` |
| `feature/collection/ui/CollectionScreen.kt` | Pass `backRoute = "collection"` to `DetailRoute` |
| `AppNavigation.kt` | Pass navController to ChatRoute; add `chatRecipeDetail` route |
