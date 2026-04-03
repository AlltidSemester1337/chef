package com.formulae.chef.feature.chat.ui

private val MARKDOWN_CHARS = Regex("[*#`_~]")

/** Hard byte-length limit enforced by the GCP TTS API. */
const val TTS_HARD_LIMIT = 4500

/** Maximum response length for which the speaker button is shown / auto-play fires. */
const val TTS_DISPLAY_THRESHOLD = 1500

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
