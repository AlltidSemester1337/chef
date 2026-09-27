package com.formulae.chef

import androidx.navigation.NavController

/**
 * Route constants and back-stack helpers shared by [AppNavigation] and the screens that navigate
 * between the bottom-bar tabs (#56).
 *
 * Invariant: the back stack is always `[home]` or `[home, <tab>]`. Every cross-tab navigation goes
 * through [navigateToTab] / [navigateToCollection] so no screen is ever pushed on top of another
 * tab. Pushing e.g. `generate` on top of `collection` made the saved tab state contain both
 * screens, so tapping Home (or Collections) later restored the chat instead of the tab the user
 * asked for.
 */
object ChefRoutes {
    const val HOME = "home"
    const val GENERATE = "generate"
    const val COLLECTION = "collection"
    const val SIGN_IN = "signIn"

    const val ARG_TAB = "tab"
    const val ARG_RECIPE_ID = "recipeId"

    /** Destination pattern registered in the NavHost for the Collections tab. */
    const val COLLECTION_PATTERN = "$COLLECTION?$ARG_TAB={$ARG_TAB}&$ARG_RECIPE_ID={$ARG_RECIPE_ID}"

    /** Builds a concrete Collections route, optionally opening a tab and/or a recipe detail. */
    fun collection(tab: String? = null, recipeId: String? = null): String {
        val params = buildList {
            tab?.let { add("$ARG_TAB=$it") }
            recipeId?.let { add("$ARG_RECIPE_ID=$it") }
        }
        return if (params.isEmpty()) COLLECTION else "$COLLECTION?${params.joinToString("&")}"
    }
}

/**
 * Switches to a bottom-bar tab ([ChefRoutes.HOME], [ChefRoutes.GENERATE], [ChefRoutes.COLLECTION]).
 *
 * Home is the root of the back stack, so switching to it just pops everything above it; it must
 * never `restoreState`, because a non-inclusive `popUpTo(home) { saveState = true }` also files the
 * popped stack under the `home` key, and restoring that re-pushes the screen the user was leaving.
 * The other tabs save/restore their state as usual so e.g. an open recipe survives a tab switch.
 */
fun NavController.navigateToTab(route: String) {
    if (route == ChefRoutes.HOME) {
        if (!popBackStack(ChefRoutes.HOME, inclusive = false, saveState = true)) {
            // Home is not on the back stack (should not happen since it is the start destination).
            navigate(ChefRoutes.HOME) { launchSingleTop = true }
        }
        return
    }
    navigate(route) {
        popUpTo(ChefRoutes.HOME) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

/**
 * Opens the Collections tab with explicit arguments (a specific tab and/or recipe). Any previously
 * saved Collections state is discarded so the new arguments win over a restored entry.
 */
fun NavController.navigateToCollection(tab: String? = null, recipeId: String? = null) {
    clearBackStack(ChefRoutes.COLLECTION_PATTERN)
    navigate(ChefRoutes.collection(tab = tab, recipeId = recipeId)) {
        popUpTo(ChefRoutes.HOME) { saveState = true }
        launchSingleTop = true
    }
}
