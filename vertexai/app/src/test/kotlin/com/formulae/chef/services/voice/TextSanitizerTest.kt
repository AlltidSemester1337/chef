package com.formulae.chef.services.voice

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

    // splitIntoSentences

    @Test
    fun splitIntoSentences_singleSentence() {
        val result = "Hello world.".splitIntoSentences()
        assertEquals(listOf("Hello world."), result)
    }

    @Test
    fun splitIntoSentences_multipleSentencesOnOneLine() {
        val result = "Mix well. Add flour. Stir again.".splitIntoSentences()
        assertEquals(listOf("Mix well.", "Add flour.", "Stir again."), result)
    }

    @Test
    fun splitIntoSentences_splitsOnExclamationAndQuestion() {
        val result = "Stir quickly! Is it done? Yes it is.".splitIntoSentences()
        assertEquals(listOf("Stir quickly!", "Is it done?", "Yes it is."), result)
    }

    @Test
    fun splitIntoSentences_splitsOnNewlines() {
        val result = "First sentence.\nSecond sentence.".splitIntoSentences()
        assertEquals(listOf("First sentence.", "Second sentence."), result)
    }

    @Test
    fun splitIntoSentences_appendsPeriodWhenMissing() {
        val result = "This has no punctuation".splitIntoSentences()
        assertEquals(listOf("This has no punctuation."), result)
    }

    @Test
    fun splitIntoSentences_stripsMarkdown() {
        val result = "**Bold sentence.**".splitIntoSentences()
        assertEquals(listOf("Bold sentence."), result)
    }

    @Test
    fun splitIntoSentences_filtersBlankLines() {
        val result = "First.\n\n\nSecond.".splitIntoSentences()
        assertEquals(listOf("First.", "Second."), result)
    }

    @Test
    fun splitIntoSentences_doesNotSplitDecimalNumbers() {
        val result = "Add 3.5 cups of flour.".splitIntoSentences()
        assertEquals(listOf("Add 3.5 cups of flour."), result)
    }

    @Test
    fun splitIntoSentences_emptyStringReturnsEmptyList() {
        val result = "".splitIntoSentences()
        assertEquals(emptyList<String>(), result)
    }

    @Test
    fun splitIntoSentences_blankStringReturnsEmptyList() {
        val result = "   ".splitIntoSentences()
        assertEquals(emptyList<String>(), result)
    }

    @Test
    fun splitIntoSentences_longSentenceTruncatedToHardLimit() {
        val longSentence = "word ".repeat(TTS_HARD_LIMIT)
        val result = longSentence.splitIntoSentences()
        result.forEach { chunk ->
            assertTrue("Chunk exceeds TTS_HARD_LIMIT: ${chunk.length}", chunk.length <= TTS_HARD_LIMIT)
        }
    }

    // helpers for readability

    private fun sanitizeMarkdown(s: String) = s.sanitizeMarkdown()
}
