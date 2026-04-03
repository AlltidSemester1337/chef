package com.formulae.chef.feature.chat.ui

private val MARKDOWN_CHARS = Regex("[*#`_~]")
private const val TTS_MAX_CHARS = 4500
const val TTS_BUTTON_MAX_CHARS = 1500

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
            if (trimmed.last() in ".!?") trimmed else "$trimmed."
        }
        .replace(Regex(" {2,}"), " ")
        .take(TTS_MAX_CHARS)
        .trim()
