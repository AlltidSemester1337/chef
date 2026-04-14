package com.formulae.chef.feature.home

import com.formulae.chef.feature.model.CookingResource
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeUiStateTest {

    @Test
    fun `default state has empty resources and is not loading`() {
        val state = HomeUiState()

        assertTrue(state.resources.isEmpty())
        assertFalse(state.isLoading)
    }

    @Test
    fun `state with resources exposes them correctly`() {
        val resources = listOf(
            CookingResource(title = "Köket.se", url = "https://koket.se", type = "website"),
            CookingResource(title = "Gordon Ramsay", url = "https://youtube.com/@gordonramsay", type = "youtube")
        )

        val state = HomeUiState(resources = resources)

        assertEquals(2, state.resources.size)
        assertEquals("Köket.se", state.resources[0].title)
        assertEquals("Gordon Ramsay", state.resources[1].title)
    }

    @Test
    fun `loading state has isLoading true`() {
        val state = HomeUiState(isLoading = true)

        assertTrue(state.isLoading)
        assertTrue(state.resources.isEmpty())
    }

    @Test
    fun `copy preserves unchanged fields`() {
        val resources = listOf(CookingResource(title = "Köket.se", url = "https://koket.se", type = "website"))
        val state = HomeUiState(resources = resources, isLoading = false)

        val loading = state.copy(isLoading = true)

        assertTrue(loading.isLoading)
        assertEquals(1, loading.resources.size)
    }
}
