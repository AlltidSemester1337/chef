package com.formulae.chef.feature.chat

/**
 * A recipe detected in a plain-text (markdown) chat response, summarised for a compact card.
 *
 * @property intro text Chef wrote before the recipe (markdown, may be blank)
 * @property title recipe name, markdown stripped; null when the response has no heading
 * @property description short text between the title and the recipe details, markdown stripped
 * @property details known metadata lines ("Prep time: 20 minutes", "Servings: 4" …), markdown stripped
 * @property ingredientCount number of ingredient list items
 * @property stepCount number of numbered instruction steps
 * @property body full recipe text after the title (markdown), shown when the card is expanded
 */
data class RecipeTextPreview(
    val intro: String,
    val title: String?,
    val description: String,
    val details: List<String>,
    val ingredientCount: Int,
    val stepCount: Int,
    val body: String
)

private val MARKDOWN_EMPHASIS = Regex("[#*_`]")
private val INGREDIENTS_HEADING = Regex("(?i)^ingredients\\b[^.!?]{0,30}$")
private val INSTRUCTIONS_HEADING =
    Regex("(?i)^(instructions|method|directions|steps|preparation)\\b[^.!?]{0,30}$")
private val METADATA_LINE =
    Regex("(?i)^(difficulty|prep(aration)? time|cook(ing)? time|total time|servings|serves|yield)\\s*:.*")
private val LIST_ITEM = Regex("^\\s*([-*•]|\\d+[.)])\\s+\\S.*")
private val NUMBERED_ITEM = Regex("^\\s*\\d+[.)]\\s+\\S.*")
private val FULLY_BOLD_LINE = Regex("^\\*\\*[^*]+\\*\\*:?$")
private val POST_INSTRUCTIONS_SECTION =
    Regex("(?i)^((approximate|approx\\.?)\\s+)?(nutrition|tips|notes|storage|serving suggestions|variations)\\b.*")
private const val MAX_TITLE_LOOKBACK_LINES = 8
private const val MAX_TITLE_LENGTH = 80

private fun String.plain(): String = replace(MARKDOWN_EMPHASIS, "").trim()

/** Drops bold/italic markers but keeps `-`/`*` bullets so list items can still be recognised. */
private fun String.unbold(): String = replace("**", "").replace("__", "")

/** A markdown heading (`## …`) or a line that is entirely bold (`**…**`). */
private fun String.isHeadingLine(): Boolean {
    val trimmed = trim()
    return trimmed.startsWith("#") || FULLY_BOLD_LINE.matches(trimmed)
}

/**
 * Where the numbered steps end: a new markdown heading (e.g. a second recipe) or a known
 * post-instructions section such as `**Approximate nutrition per serving:**` or `**Tips:**`.
 * Sub-headings inside the steps (e.g. `**For the sauce:**`) do not end them.
 */
private fun String.isEndOfSteps(): Boolean {
    val trimmed = trim()
    if (trimmed.startsWith("#")) return true
    return trimmed.startsWith("**") && !LIST_ITEM.matches(trimmed.unbold()) &&
        POST_INSTRUCTIONS_SECTION.matches(trimmed.plain())
}

/**
 * Detects a recipe-shaped chat response (an ingredients section followed by an instructions
 * section, as the chat system prompt asks Chef to format recipes) and extracts what a compact
 * card needs. Returns null for regular conversational messages.
 */
fun parseRecipeText(text: String): RecipeTextPreview? {
    val lines = text.lines()
    val ingredientsIdx = lines.indexOfFirst { INGREDIENTS_HEADING.matches(it.plain()) }
    if (ingredientsIdx == -1) return null
    val instructionsIdx = (ingredientsIdx + 1 until lines.size)
        .firstOrNull { INSTRUCTIONS_HEADING.matches(lines[it].plain()) } ?: return null

    val ingredientCount = (ingredientsIdx + 1 until instructionsIdx)
        .count { LIST_ITEM.matches(lines[it].unbold()) }
    val stepsEnd = (instructionsIdx + 1 until lines.size)
        .firstOrNull { lines[it].isEndOfSteps() } ?: lines.size
    val stepCount = (instructionsIdx + 1 until stepsEnd).count { NUMBERED_ITEM.matches(lines[it].unbold()) }
    if (ingredientCount == 0 || stepCount == 0) return null

    // Walk up from the ingredients heading over the metadata block (blank lines allowed).
    var detailsStart = ingredientsIdx
    val details = mutableListOf<String>()
    var i = ingredientsIdx - 1
    while (i >= 0) {
        val plain = lines[i].plain()
        when {
            plain.isEmpty() -> Unit
            METADATA_LINE.matches(plain) -> {
                details.add(0, plain)
                detailsStart = i
            }
            else -> break
        }
        i--
    }

    // Look a few lines further up for the recipe title; lines in between are its description.
    val titleIdx = (detailsStart - 1 downTo maxOf(0, detailsStart - MAX_TITLE_LOOKBACK_LINES))
        .firstOrNull { idx ->
            val line = lines[idx]
            val plain = line.plain()
            line.isHeadingLine() && plain.isNotEmpty() && plain.length <= MAX_TITLE_LENGTH &&
                !plain.endsWith(":") && !METADATA_LINE.matches(plain) &&
                !INGREDIENTS_HEADING.matches(plain)
        }

    val recipeStart = titleIdx ?: detailsStart
    val description = if (titleIdx != null) {
        (titleIdx + 1 until detailsStart).map { lines[it].plain() }.filter { it.isNotEmpty() }.joinToString(" ")
    } else {
        ""
    }
    val bodyStart = if (titleIdx != null) titleIdx + 1 else recipeStart

    return RecipeTextPreview(
        intro = lines.subList(0, recipeStart).joinToString("\n").trim(),
        title = titleIdx?.let { lines[it].plain() },
        description = description,
        details = details,
        ingredientCount = ingredientCount,
        stepCount = stepCount,
        body = lines.subList(bodyStart, lines.size).joinToString("\n").trim()
    )
}
