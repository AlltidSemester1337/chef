package com.formulae.chef

import org.junit.Assert.assertEquals
import org.junit.Test

class ChatCloseNavigationTest {

    @Test
    fun `route before chat follows non-chat destinations`() {
        assertEquals("collection", nextRouteBeforeChat("collection", "home"))
        assertEquals("home", nextRouteBeforeChat("home", null))
    }

    @Test
    fun `route before chat is kept while chat is shown`() {
        assertEquals("collection", nextRouteBeforeChat(CHAT_ROUTE, "collection"))
        assertEquals(null, nextRouteBeforeChat(CHAT_ROUTE, null))
    }

    @Test
    fun `opened from home pops back to home`() {
        assertEquals(ChatCloseAction.PopBackStack, resolveChatCloseAction("home", "home"))
    }

    @Test
    fun `opened from a recipe in collection pops back to it`() {
        assertEquals(ChatCloseAction.PopBackStack, resolveChatCloseAction("collection", "collection"))
    }

    @Test
    fun `opened via bottom bar from collection switches back to collection tab`() {
        // Tab switch pops up to home, so home is under chat on the back stack.
        assertEquals(
            ChatCloseAction.SwitchToTab("collection"),
            resolveChatCloseAction("collection", "home")
        )
    }

    @Test
    fun `unknown origin with a back stack pops`() {
        assertEquals(ChatCloseAction.PopBackStack, resolveChatCloseAction(null, "home"))
    }

    @Test
    fun `no back stack falls back to the tracked tab or home`() {
        assertEquals(ChatCloseAction.SwitchToTab("collection"), resolveChatCloseAction("collection", null))
        assertEquals(ChatCloseAction.SwitchToTab("home"), resolveChatCloseAction(null, null))
    }
}
