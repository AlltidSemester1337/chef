package com.formulae.chef

internal const val CHAT_ROUTE = "generate"

/** What closing the chat screen should do to get the user back to where they came from. */
internal sealed interface ChatCloseAction {
    /** The previous back-stack entry is the screen the user came from (e.g. Home, a recipe). */
    data object PopBackStack : ChatCloseAction

    /** The user reached chat via a bottom-bar tab switch, which pops back to `home`; switch back. */
    data class SwitchToTab(val route: String) : ChatCloseAction
}

/**
 * Tracks the last base route shown before the chat screen. Updated on every destination change;
 * keeps the previous value while the chat itself is shown.
 */
internal fun nextRouteBeforeChat(currentBaseRoute: String, routeBeforeChat: String?): String? =
    if (currentBaseRoute == CHAT_ROUTE) routeBeforeChat else currentBaseRoute

/**
 * Bottom-bar navigation pops everything above `home` (saving its state), so after a tab switch the
 * back stack under chat is `home` even if the user was on Collections. In that case we switch back
 * to the tab they came from (restoring its saved state) instead of popping to `home`.
 */
internal fun resolveChatCloseAction(routeBeforeChat: String?, previousBackStackBaseRoute: String?): ChatCloseAction =
    when {
        previousBackStackBaseRoute != null &&
            (routeBeforeChat == null || routeBeforeChat == previousBackStackBaseRoute) -> ChatCloseAction.PopBackStack
        else -> ChatCloseAction.SwitchToTab(routeBeforeChat ?: "home")
    }
