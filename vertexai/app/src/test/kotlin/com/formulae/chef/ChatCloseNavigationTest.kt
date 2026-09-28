package com.formulae.chef

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ChatCloseNavigationTest {

    // --- nextChatOrigin ---

    @Test
    fun `origin follows the tab destinations`() {
        assertEquals(ChatOrigin(ChefRoutes.HOME), nextChatOrigin(ChefRoutes.HOME, null, null))
        assertEquals(
            ChatOrigin(ChefRoutes.COLLECTION),
            nextChatOrigin(ChefRoutes.COLLECTION, null, ChatOrigin(ChefRoutes.HOME))
        )
    }

    @Test
    fun `origin records a recipe opened from Home`() {
        assertEquals(
            ChatOrigin(ChefRoutes.COLLECTION, recipeFromHomeId = "r1"),
            nextChatOrigin(ChefRoutes.COLLECTION, "r1", ChatOrigin(ChefRoutes.HOME))
        )
    }

    @Test
    fun `blank recipe id is not a recipe from Home`() {
        assertEquals(ChatOrigin(ChefRoutes.COLLECTION), nextChatOrigin(ChefRoutes.COLLECTION, "", null))
    }

    @Test
    fun `origin is kept while chat is shown`() {
        val origin = ChatOrigin(ChefRoutes.COLLECTION, recipeFromHomeId = "r1")
        assertEquals(origin, nextChatOrigin(ChefRoutes.GENERATE, null, origin))
        assertNull(nextChatOrigin(ChefRoutes.GENERATE, null, null))
    }

    @Test
    fun `non-tab destinations clear the origin`() {
        assertNull(nextChatOrigin(ChefRoutes.SIGN_IN, null, ChatOrigin(ChefRoutes.COLLECTION)))
    }

    // --- resolveChatCloseAction ---

    @Test
    fun `chat opened from Home tab returns to Home`() {
        assertEquals(
            ChatCloseAction.SwitchToTab(ChefRoutes.HOME),
            resolveChatCloseAction(ChatOrigin(ChefRoutes.HOME))
        )
    }

    @Test
    fun `chat opened from Collections (bottom bar or a Collections recipe) switches back to Collections`() {
        // The Collections tab's saved state (list or open recipe) is restored by navigateToTab.
        assertEquals(
            ChatCloseAction.SwitchToTab(ChefRoutes.COLLECTION),
            resolveChatCloseAction(ChatOrigin(ChefRoutes.COLLECTION))
        )
    }

    @Test
    fun `chat opened from a recipe opened on Home re-opens that recipe`() {
        // That entry is discarded (not saved) when leaving it, so it can't be restored as a tab.
        assertEquals(
            ChatCloseAction.ReopenRecipeFromHome("r1"),
            resolveChatCloseAction(ChatOrigin(ChefRoutes.COLLECTION, recipeFromHomeId = "r1"))
        )
    }

    @Test
    fun `unknown origin falls back to Home`() {
        assertEquals(ChatCloseAction.SwitchToTab(ChefRoutes.HOME), resolveChatCloseAction(null))
    }
}
