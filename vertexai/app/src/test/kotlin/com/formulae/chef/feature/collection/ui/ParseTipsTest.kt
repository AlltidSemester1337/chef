package com.formulae.chef.feature.collection.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ParseTipsTest {

    @Test
    fun `bullet lines are split into separate tips`() {
        val input = "- Store in airtight container\n- Reheat gently on low heat\n- Substitute chicken for lamb"

        val result = parseTips(input)

        assertEquals(3, result.size)
        assertEquals("Store in airtight container", result[0])
        assertEquals("Reheat gently on low heat", result[1])
        assertEquals("Substitute chicken for lamb", result[2])
    }

    @Test
    fun `multi-line tip is joined into one entry`() {
        val input = "- Store leftovers in an airtight container\n  in the fridge for up to 3 days\n- Reheat gently"

        val result = parseTips(input)

        assertEquals(2, result.size)
        assertEquals("Store leftovers in an airtight container in the fridge for up to 3 days", result[0])
        assertEquals("Reheat gently", result[1])
    }

    @Test
    fun `fallback - no bullets renders whole string as single tip`() {
        val input = "For milder flavor reduce cayenne pepper. Use zucchini instead of listed vegetables."

        val result = parseTips(input)

        assertEquals(1, result.size)
        assertEquals(input.trim(), result[0])
    }

    @Test
    fun `empty string returns empty list`() {
        val result = parseTips("")

        assertTrue(result.isEmpty())
    }

    @Test
    fun `blank-only string returns empty list`() {
        val result = parseTips("   \n  \n ")

        assertTrue(result.isEmpty())
    }

    @Test
    fun `single bullet tip is parsed correctly`() {
        val input = "- Use room temperature butter for best results"

        val result = parseTips(input)

        assertEquals(1, result.size)
        assertEquals("Use room temperature butter for best results", result[0])
    }

    @Test
    fun `blank lines between bullets are ignored`() {
        val input = "- First tip\n\n- Second tip\n\n- Third tip"

        val result = parseTips(input)

        assertEquals(3, result.size)
        assertEquals("First tip", result[0])
        assertEquals("Second tip", result[1])
        assertEquals("Third tip", result[2])
    }
}
