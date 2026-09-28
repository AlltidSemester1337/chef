package com.formulae.chef

/**
 * Where the user was right before the chat tab was shown, so the chat's close (X) can return
 * there (#61).
 *
 * With the #56 tab model the back stack is always `[home]` or `[home, <tab>]`, so the entry under
 * chat is always `home` and a plain `popBackStack()` can't tell where the user came from; the
 * origin is tracked on every destination change instead.
 *
 * @property route base route of the tab the user was on ([ChefRoutes.HOME] or [ChefRoutes.COLLECTION])
 * @property recipeFromHomeId set when that screen was a recipe opened from Home: such an entry is
 * transient (never saved on tab switch), so it has to be re-opened rather than restored.
 */
internal data class ChatOrigin(val route: String, val recipeFromHomeId: String? = null)

/** What closing the chat screen should do to get the user back to where they came from. */
internal sealed interface ChatCloseAction {
    /** Switch back to a bottom-bar tab, restoring its saved state (e.g. an open recipe). */
    data class SwitchToTab(val route: String) : ChatCloseAction

    /** Re-open the recipe the user had opened from Home (Back from it returns Home). */
    data class ReopenRecipeFromHome(val recipeId: String) : ChatCloseAction
}

private val chatOriginTabs = setOf(ChefRoutes.HOME, ChefRoutes.COLLECTION)

/**
 * Next tracked [ChatOrigin] after navigating to [currentBaseRoute]. Kept unchanged while the chat
 * itself is shown; cleared for non-tab destinations such as sign-in.
 */
internal fun nextChatOrigin(
    currentBaseRoute: String,
    currentRecipeFromHomeId: String?,
    previous: ChatOrigin?
): ChatOrigin? = when (currentBaseRoute) {
    ChefRoutes.GENERATE -> previous
    in chatOriginTabs -> ChatOrigin(
        route = currentBaseRoute,
        recipeFromHomeId = currentRecipeFromHomeId?.takeIf {
            currentBaseRoute == ChefRoutes.COLLECTION && it.isNotBlank()
        }
    )
    else -> null
}

internal fun resolveChatCloseAction(origin: ChatOrigin?): ChatCloseAction {
    val recipeFromHomeId = origin?.recipeFromHomeId
    return if (recipeFromHomeId != null) {
        ChatCloseAction.ReopenRecipeFromHome(recipeFromHomeId)
    } else {
        ChatCloseAction.SwitchToTab(origin?.route ?: ChefRoutes.HOME)
    }
}
