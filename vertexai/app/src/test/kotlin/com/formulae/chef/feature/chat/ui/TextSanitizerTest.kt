package com.formulae.chef.feature.chat.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TextSanitizerTest {

    // sanitizeMarkdown

    @Test
    fun sanitizeMarkdown_stripsAsterisks() {
        assertEquals("bold text", "**bold text**".sanitizeMarkdown())
    }

    @Test
    fun sanitizeMarkdown_stripsHashes() {
        assertEquals("Title", "## Title".sanitizeMarkdown())
    }

    @Test
    fun sanitizeMarkdown_stripsBackticks() {
        assertEquals("code", "`code`".sanitizeMarkdown())
    }

    @Test
    fun sanitizeMarkdown_stripsUnderscoresAndTildes() {
        assertEquals("text", "_text_".sanitizeMarkdown())
        assertEquals("text", "~~text~~".sanitizeMarkdown())
    }

    @Test
    fun sanitizeMarkdown_preservesSingleNewlines() {
        val input = "line one\nline two"
        assertTrue(sanitizeMarkdown(input).contains("\n"))
    }

    @Test
    fun sanitizeMarkdown_collapseExcessiveBlankLines() {
        val input = "line one\n\n\n\nline two"
        assertEquals("line one\n\nline two", input.sanitizeMarkdown())
    }

    @Test
    fun sanitizeMarkdown_trims() {
        assertEquals("hello", "  hello  ".sanitizeMarkdown())
    }

    // sanitizeForTts

    @Test
    fun sanitizeForTts_stripsMarkdownAndFlattensNewlines() {
        val input = "**Ingredients:**\n_Flour_\n`Eggs`"
        val result = input.sanitizeForTts()
        assertFalse(result.contains("*"))
        assertFalse(result.contains("_"))
        assertFalse(result.contains("`"))
        assertFalse(result.contains("\n"))
    }

    @Test
    fun sanitizeForTts_appendsPeriodToLinesLackingPunctuation() {
        val result = "Hello\nWorld".sanitizeForTts()
        // Both lines should end with a period before being joined
        assertTrue(result.contains("Hello."))
        assertTrue(result.contains("World."))
    }

    @Test
    fun sanitizeForTts_doesNotDoublePunctuate() {
        val result = "Hello.\nWorld!".sanitizeForTts()
        assertFalse(result.contains(".."))
        assertFalse(result.contains("!."))
    }

    @Test
    fun sanitizeForTts_truncatesAtHardLimit() {
        val longText = "a".repeat(TTS_HARD_LIMIT + 500)
        val result = longText.sanitizeForTts()
        assertTrue(result.length <= TTS_HARD_LIMIT)
    }

    @Test
    fun sanitizeForTts_shortTextUnchangedByTruncation() {
        val short = "What a great recipe."
        val result = short.sanitizeForTts()
        assertTrue(result.length <= TTS_HARD_LIMIT)
        assertTrue(result.isNotBlank())
    }

    // TTS_DISPLAY_THRESHOLD constant

    @Test
    fun ttsDisplayThreshold_isLessThanHardLimit() {
        assertTrue(TTS_DISPLAY_THRESHOLD < TTS_HARD_LIMIT)
    }

    // helpers for readability

    private fun sanitizeMarkdown(s: String) = s.sanitizeMarkdown()
}
