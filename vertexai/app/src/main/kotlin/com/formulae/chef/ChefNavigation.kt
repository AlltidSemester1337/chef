package com.formulae.chef

import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavController

/**
 * Route constants and back-stack helpers shared by [AppNavigation] and the screens that navigate
 * between the bottom-bar tabs (#56).
 *
 * Invariant: the back stack is always `[home]` or `[home, <tab>]`. Every cross-tab navigation goes
 * through [navigateToTab] / [navigateToCollection] / [navigateToChat] so no screen is ever pushed on
 * top of another tab. Pushing e.g. `generate` on top of `collection` made the saved tab state
 * contain both screens, so tapping Home (or Collections) later restored the chat instead of the tab
 * the user asked for.
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

    /**
     * A Collections entry opened on a specific recipe from Home is a transient "recipe from Home"
     * view, not part of the Collections tab: Back returns to Home and leaving it never saves its
     * state, so the Collections tab keeps whatever the user had there before.
     */
    fun isRecipeOpenedFromHome(route: String?, recipeIdArg: String?): Boolean =
        route == COLLECTION_PATTERN && !recipeIdArg.isNullOrBlank()
}

private fun NavBackStackEntry.isRecipeOpenedFromHome(): Boolean = ChefRoutes.isRecipeOpenedFromHome(
    destination.route,
    arguments?.getString(ChefRoutes.ARG_RECIPE_ID)
)

/** Pops a transient recipe-from-Home entry without saving it, so it is never restored later. */
private fun NavController.discardRecipeOpenedFromHome() {
    if (currentBackStackEntry?.isRecipeOpenedFromHome() == true) {
        popBackStack()
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
    discardRecipeOpenedFromHome()
    if (route == ChefRoutes.HOME) {
        if (currentBackStackEntry?.destination?.route == ChefRoutes.HOME) return
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
 * Opens the Collections tab with explicit arguments.
 *
 * With a [recipeId] (recipe tapped on Home) the recipe is opened as a transient entry on top of
 * Home: the Collections tab's saved state is left untouched and restored on the next tab visit.
 * With only a [tab] (Home's "All saved recipes" / "Community collection" links) any previously
 * saved Collections state is discarded so the requested tab wins over a restored entry.
 */
fun NavController.navigateToCollection(tab: String? = null, recipeId: String? = null) {
    if (recipeId.isNullOrBlank()) {
        clearBackStack(ChefRoutes.COLLECTION_PATTERN)
    }
    navigate(ChefRoutes.collection(tab = tab, recipeId = recipeId)) {
        popUpTo(ChefRoutes.HOME) { saveState = recipeId.isNullOrBlank() }
        launchSingleTop = true
    }
}

/**
 * Back from a Collections recipe: a recipe opened from Home returns to Home; a recipe opened
 * inside Collections runs [closeRecipe] to go back to the list.
 */
fun NavController.navigateBackFromRecipe(closeRecipe: () -> Unit) {
    if (currentBackStackEntry?.isRecipeOpenedFromHome() == true) {
        navigateToTab(ChefRoutes.HOME)
    } else {
        closeRecipe()
    }
}

/**
 * Opens the chat tab ("Chat with Chef" on a recipe). With a [recipeId] the chat is primed with
 * that recipe (#60): the saved chat entry is dropped so the fresh entry gets the new argument
 * (restoreState would otherwise bring back the old entry and its old arguments).
 */
fun NavController.navigateToChat(recipeId: String? = null) {
    if (recipeId.isNullOrBlank()) {
        navigateToTab(ChefRoutes.GENERATE)
        return
    }
    discardRecipeOpenedFromHome()
    clearBackStack(CHAT_ROUTE_PATTERN)
    navigate(chatRouteForRecipe(recipeId)) {
        popUpTo(ChefRoutes.HOME) { saveState = true }
        launchSingleTop = true
    }
}
