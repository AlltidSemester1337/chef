package com.formulae.chef.services.voice

private val MARKDOWN_CHARS = Regex("[*#`_~]")

/** Hard byte-length limit enforced by the GCP TTS API. */
const val TTS_HARD_LIMIT = 4500

fun String.sanitizeMarkdown(): String =
    replace(MARKDOWN_CHARS, "")
        .replace(Regex("\n{3,}"), "\n\n")
        .trim()

fun String.sanitizeForTts(): String =
    sanitizeMarkdown()
        .lines()
        .filter { it.isNotBlank() }
        .joinToString(" ") { line ->
            val trimmed = line.trim()
            if (trimmed.endsWith('.') || trimmed.endsWith('!') || trimmed.endsWith('?')) trimmed else "$trimmed."
        }
        .replace(Regex(" {2,}"), " ")
        .take(TTS_HARD_LIMIT)
        .trim()

/**
 * Splits text into TTS-ready sentence chunks. Each chunk is markdown-stripped,
 * punctuation-terminated, and safe to pass directly to [GcpTextToSpeechService.synthesize].
 * Splitting on sentence boundaries allows chunked streaming playback: the first sentence
 * can be synthesized and played while subsequent sentences are still being synthesized.
 */
fun String.splitIntoSentences(): List<String> =
    sanitizeMarkdown()
        .lines()
        .map { it.trim() }
        .filter { it.isNotBlank() }
        .flatMap { line ->
            line.split(Regex("(?<=[.!?])\\s+"))
                .map { it.trim() }
                .filter { it.isNotBlank() }
        }
        .map { sentence ->
            val cleaned = sentence.replace(Regex(" {2,}"), " ")
            val hasPunctuation = cleaned.endsWith('.') || cleaned.endsWith('!') || cleaned.endsWith('?')
            val punctuated = if (hasPunctuation) cleaned else "$cleaned."
            punctuated.truncateAtWordBoundary(TTS_HARD_LIMIT)
        }

private fun String.truncateAtWordBoundary(limit: Int): String {
    if (length <= limit) return this
    val cut = lastIndexOf(' ', limit)
    return if (cut > 0) substring(0, cut).trimEnd() + "." else take(limit)
}
