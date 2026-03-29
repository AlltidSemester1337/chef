# Implement Figma Redesign

By using the design screens located in the Figma file Chef UI and the page Final UI, we want to redesign the existing app screens. Ignore any Figma design specifications that aren't covered by the current code.

## Implementation plan

### Overview

The redesign transforms the app from a basic button-driven layout to a polished, design-system-driven experience with a bottom navigation bar, recipe card grids, a redesigned chat interface, and consistent use of the Figma design tokens. The scope covers the 5 screens that exist in both the current code and the Figma designs: **Home**, **Chat/Generate**, **Recipe Detail**, **Collections**, and **Sign Up/Sign In**.

Figma screens that have **no corresponding code** (onboarding flow, list management dialogs, community recipes browse, etc.) are out of scope per the user story.

---

### Step 0: Theme & Design System Foundation

**Files:** `ui/theme/Color.kt`, `ui/theme/Theme.kt`, `ui/theme/Typography.kt`, `ui/theme/Type.kt`, `ui/theme/Shape.kt` (new)

Before touching any screen, align the theme with the Figma design tokens defined in `figma.md`:

1. **Colors** — Wire the custom app colors already declared in `Color.kt` (`BackgroundColor`, `BackgroundAccent`, `TextPrimary`, `TextSecondary`, `TextSupporting`) into the Material3 `lightColorScheme` in `Theme.kt`. Add the missing Accent color `#C45234` (terracotta) as `primary`. Map terracotta shades: `50 → #FFF7F5`, `100 → #FFF1ED`, `200 → #F9D8CF`, `600 → #C45234`, `700 → #A9462C`, `800 → #8D3C27`. Set `background` to `#FFFDFC`, `surface` to `#FFFFFF`, `onBackground`/`onSurface` to `TextPrimary`.
2. **Typography** — Activate the existing `AppTypography` from `Typography.kt` (currently marked TODO) and wire it into the theme. Delete the duplicate minimal `Type.kt`. Ensure all 6 type slots (`headlineLarge`, `labelMedium`, `bodyLarge`, `bodyMedium`, `labelLarge`, `bodySmall`) match Figma specs.
3. **Shapes** — Create `Shape.kt` defining `Shapes(small = RoundedCornerShape(8.dp), medium = RoundedCornerShape(12.dp), large = RoundedCornerShape(16.dp))` and apply to theme.
4. **Accent color** — Define a `ChefColors` object or extend `ColorScheme` extension properties for terracotta shades that aren't standard Material3 slots, so screens can reference `MaterialTheme.chefColors.terracotta600` etc.

**Acceptance:** App compiles and existing screens automatically pick up the new color scheme, typography, and shapes from theme.

---

### Step 1: Bottom Navigation Bar

**Files:** `AppNavigation.kt`, new shared composable `ui/components/ChefNavigationBar.kt`

The Figma design replaces the current HomeScreen button menu with a persistent 3-tab bottom navigation bar (Home, Generate, Collections). This is the most structural change.

1. Create `ChefNavigationBar` composable using Material3 `NavigationBar` with 3 `NavigationBarItem`s:
   - **Home** — home icon (Material `Icons.Outlined.Home`)
   - **Generate** — chef hat / cooking icon (use Material `Icons.Outlined.Restaurant` or custom)
   - **Collections** — bookmark icon (Material `Icons.Outlined.Bookmarks`)
   - Selected item: bold Figtree label, terracotta/700 color. Unselected: medium Figtree, grey/700.
   - Container color: terracotta/50 (`#FFF7F5`).
2. Wrap `AppNavigation`'s `NavHost` in a `Scaffold` with `bottomBar = { ChefNavigationBar(...) }`.
3. Update navigation routes: `home`, `chat` (renamed to `generate`), `collection` (renamed to `collections`). Keep `signIn` as a standalone route without the bottom bar.
4. Remove the 3 navigation buttons from `HomeScreen` — they are replaced by the bottom nav.

**Acceptance:** Tapping bottom nav items navigates between Home, Generate (chat), and Collections screens. Bottom nav persists across these 3 screens. Sign-in screen shows without bottom nav.

---

### Step 2: Home Screen Redesign

**Files:** `HomeScreen.kt`

Transform from centered buttons to a scrollable feed layout matching Figma's "Home first visit" design:

1. **Header section** — "Hi, {username}!" as `headlineLarge`, settings icon button (top-right). Below: description text (`bodyLarge`) + "Generate personalized recipes" filled button (terracotta/600, full width, rounded 8dp).
2. **Decorative divider** — A wavy decorative element below the header (use a simple `Divider` or decorative drawable if available).
3. **"Your saved recipes" section** — Section title (`labelMedium` semibold) + "All saved recipes" link (terracotta/600, wavy underline). Horizontal row of 2 recipe cards (see shared RecipeCard component, step 6).
4. **"What's cooking?" section** — Section title + "Community collection" link. 2-column grid of recipe cards (6 cards shown in Figma). Use `LazyVerticalGrid` or FlowRow with 2 columns.
5. **Bottom description text** — Italic medium text about chatting with Chef.
6. **FAB** — Floating action button (circular, terracotta/600, chef hat logo) in bottom-right corner above the nav bar. Tapping navigates to Generate/chat.

**Data:** The "Your saved recipes" section pulls from the existing `RecipeRepository`. The community section also uses existing recipe data (no new data source needed). The user greeting uses `UserSessionService`.

**Acceptance:** Home screen shows greeting, CTA button, saved recipes row, community recipe grid, FAB. Scrollable. Tapping a recipe card navigates to recipe detail.

---

### Step 3: Chat/Generate Screen Redesign

**Files:** `feature/chat/ui/ChatScreen.kt`

Redesign the chat screen to match the Figma "Generate" / "Message history" designs:

1. **Top bar** — Replace default with custom top bar: Chef logo (24dp circular), "Chat with Chef" title (`headlineLarge` 20sp ExtraBold Italic), bookmark icon button (right). Use Material3 `TopAppBar` or custom `Row`.
2. **Message info text** — "Chef saves up to 50 messages" centered below top bar (bodySmall, grey/600).
3. **User messages** — Right-aligned bubbles with `BackgroundAccent` (`#F9D8CF` / terracotta/200) background, 8dp rounded corners, 8dp padding. Text in `bodyLarge` Figtree Medium.
4. **Model messages** — Left-aligned, no bubble background (plain text on screen background). Text in `bodyLarge` Figtree Medium.
5. **Recipe cards in chat** — When a recipe is generated, show an inline recipe card (horizontal layout: image left 140dp wide, title + rating + servings right) with `BackgroundAccent` (`#FFF1ED`) background, 8dp rounded. Include a bookmark icon overlay on the image.
6. **Message input bar** — Bottom bar with rounded container, shadow on top. Bookmark/attachment icon left, text placeholder "Ask Chef for recipes, tips & tricks...", no separate send button (send on keyboard action). Background white, rounded top corners 8dp.

**Acceptance:** Chat shows redesigned bubbles, top bar, recipe cards inline, and bottom input bar matching Figma.

---

### Step 4: Recipe Detail Screen Redesign

**Files:** `feature/collection/ui/DetailScreen.kt`

Redesign to match Figma's "Recipe - Ingredients" and "Recipe - Instructions" screens:

1. **Top bar** — Back arrow (left), recipe title (truncated, center), bookmark icon + share icon (right). Overlay on top of the hero image.
2. **Hero image** — Full-width image at top, no rounded corners on top, extends behind status bar area.
3. **Recipe title** — Large, centered, bold italic text below image. Difficulty badge (e.g. "E" for Easy) as a small chip/tag.
4. **Rating & servings row** — Star rating display (5 stars, filled/half/empty), numeric rating, servings count with person icon, "Rate recipe" link.
5. **Prep time & difficulty** — Row with clock icon + prep/cook time, difficulty icon + difficulty level.
6. **Description** — Recipe summary text (`bodyLarge`).
7. **Ingredients/Instructions tab** — Segmented button/tab row with `BackgroundAccent` background container, rounded 8dp. Selected tab: white background, bold italic terracotta/800 text. Unselected: transparent, medium italic text.
8. **Ingredients list** — Serving adjuster row (count with +/- buttons). Ingredient items as simple text rows with checkboxes.
9. **Instructions list** — Numbered steps with checkbox for each. Bold quantities inline. "Start timer" links for timed steps.
10. **Tags section** — "Featured in lists" expandable section with list chips. "Tags" section with tag chips (icon + label).
11. **"Rate recipe" button** — Full-width terracotta/600 filled button at bottom.
12. **Chat CTA** — "Got a question...? Chat with Chef anytime!" italic text with link at very bottom.

**Acceptance:** Recipe detail shows hero image, metadata, tabbed ingredients/instructions, tags, and rating button matching Figma.

---

### Step 5: Collections Screen Redesign

**Files:** `feature/collection/ui/CollectionScreen.kt`

Redesign to match Figma's "Collections - Your recipes" screen:

1. **Title** — "Collections" as `headlineLarge` (ExtraBold Italic) at top.
2. **Tab group** — Segmented control with "Saved recipes" (active, bold italic) and "Community recipes" tabs. `BackgroundAccent` container with white highlight on selected tab, rounded 8dp.
3. **Search bar** — White background, terracotta/200 border, search icon left, placeholder "Search recipes....", 40dp height, 8dp rounded.
4. **"Your lists" section** — Title + "New list" link (terracotta/600). List items in a vertical list: each row has thumbnail image (65x48, 8dp rounded), title (SemiBold 16sp) + subtitle (Regular 14sp, recipe count), and a 3-dot menu icon button on the right. *Note: list management CRUD is out of scope — only display the existing flat recipe list grouped as "lists" if data supports it, otherwise skip this section.*
5. **"Your saved recipes" section** — Title with count. 2-column grid of recipe cards with bookmark icon overlay (top-right of image). Cards match the shared RecipeCard component.
6. **FAB** — Same Chef FAB as Home screen, bottom-right.

**Acceptance:** Collections screen shows tabs, search, recipe grid matching Figma. Existing toggle (My Favourites / Browse All) maps to the new tab design.

---

### Step 6: Shared Components

**Files:** New `ui/components/` package

Extract reusable components used across multiple screens:

1. **`RecipeCard`** — Vertical card: image top (BackgroundAccent background, rounded top 8dp), title bottom (bold italic 14sp, BackgroundAccent background, rounded bottom 8dp). Fixed width ~165dp. Optional bookmark icon overlay. Used in Home, Collections, Chat.
2. **`RecipeCardHorizontal`** — Horizontal card for chat inline: image left (140dp), text + metadata right. Used in Generate/Chat screen.
3. **`ChefFab`** — Circular FAB with chef logo, terracotta/600, elevation shadow. Used in Home, Collections.
4. **`SectionHeader`** — Row with title text left + link text right (terracotta/600, wavy underline). Used in Home, Collections.
5. **`ChefTopBar`** — Reusable top app bar with optional back nav, title, and action icons.
6. **`SegmentedTabRow`** — The BackgroundAccent container with rounded white-highlighted tab selector. Used in Collections, Recipe Detail.

---

### Step 7: Sign Up / Sign In Screen Polish

**Files:** `feature/useraccount/ui/SignInScreen.kt`, `feature/useraccount/ui/components/InputField.kt`

The sign-up screen is already the closest to the Figma design. Polish remaining issues:

1. **Layout** — Center the logo (48dp circular) and "Welcome to Chef!" title. Remove the two overlapping carrot background images and replace with the single decorative vector (the large swoopy carrot shape behind the form).
2. **"Sign in" link** — Top-right, terracotta/600 with wavy underline decoration. Already partially implemented.
3. **Input fields** — Fix the `InputField` component: ensure the border color is terracotta/200 (`#F9D8CF`), no purple outline (addresses the existing TODO about "purple shit"). Remove the `OutlinedTextField`'s default border and use only the custom border.
4. **"Create account" button** — Full-width, terracotta/600, 8dp rounded, 44dp height. Already mostly correct.
5. **Fix positioning** — Remove the absolute `Box` positioning TODOs and use a proper `Column` layout with consistent spacing (24dp between field groups).

**Acceptance:** Sign-up screen matches Figma with clean centered layout, proper input fields, and no purple outlines.

---

### Implementation order & dependencies

| Order | Step | Depends on | Effort |
|-------|------|-----------|--------|
| 1 | Step 0: Theme foundation | — | Small |
| 2 | Step 6: Shared components | Step 0 | Medium |
| 3 | Step 1: Bottom navigation | Step 0 | Medium |
| 4 | Step 2: Home screen | Steps 1, 6 | Large |
| 5 | Step 5: Collections screen | Steps 1, 6 | Large |
| 6 | Step 4: Recipe detail | Steps 0, 6 | Large |
| 7 | Step 3: Chat/Generate screen | Steps 1, 6 | Large |
| 8 | Step 7: Sign-up polish | Step 0 | Small |

### Out of scope

The following Figma screens/features have no current code counterpart and are excluded:
- Onboarding flow (screens 1-4)
- List management (create/edit/delete lists, add recipe to list dialogs)
- Community recipes browse as separate data source
- Chat overlay modal
- Rating system backend
- Timer functionality in recipe instructions
- Menu/options dropdown overlays
