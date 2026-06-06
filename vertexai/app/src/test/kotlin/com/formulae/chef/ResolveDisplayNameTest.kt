package com.formulae.chef

import org.junit.Assert.assertEquals
import org.junit.Test

class ResolveDisplayNameTest {

    @Test
    fun `returns first name when displayName is set`() {
        assertEquals("Jane", resolveDisplayName("Jane Doe", null))
    }

    @Test
    fun `returns single-token displayName as-is`() {
        assertEquals("Jane", resolveDisplayName("Jane", null))
    }

    @Test
    fun `falls back to capitalised email prefix when displayName is null`() {
        assertEquals("Jane", resolveDisplayName(null, "jane@example.com"))
    }

    @Test
    fun `falls back to capitalised email prefix when displayName is blank`() {
        assertEquals("Jane", resolveDisplayName("  ", "jane@example.com"))
    }

    @Test
    fun `returns there when both displayName and email are null`() {
        assertEquals("there", resolveDisplayName(null, null))
    }

    @Test
    fun `returns there when displayName is blank and email is null`() {
        assertEquals("there", resolveDisplayName("  ", null))
    }
}
