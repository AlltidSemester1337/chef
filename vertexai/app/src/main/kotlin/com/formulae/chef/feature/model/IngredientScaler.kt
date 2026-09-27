package com.formulae.chef.feature.model

import java.math.BigDecimal
import java.math.RoundingMode
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.roundToLong

/**
 * Display-time scaling of ingredient quantities when the user adjusts the number of servings.
 *
 * Quantities are stored as free-form strings (see `.ai/database-schema.md`), e.g. `"500"`, `"0.5"`,
 * `"1/2"`, `"1 1/2"`, `"2-3"` or `"1 (400g)"`. Only the leading amount (or both ends of a range)
 * is scaled — any trailing text is preserved verbatim. Quantities that cannot be parsed
 * (e.g. `"to taste"`, `"a pinch"`) are returned unchanged. Nothing here is ever persisted.
 */
object IngredientScaler {

    private const val UNICODE_FRACTIONS = "½⅓⅔¼¾⅛⅜⅝⅞"
    private val unicodeFractionValues = mapOf(
        '½' to 1.0 / 2,
        '⅓' to 1.0 / 3,
        '⅔' to 2.0 / 3,
        '¼' to 1.0 / 4,
        '¾' to 3.0 / 4,
        '⅛' to 1.0 / 8,
        '⅜' to 3.0 / 8,
        '⅝' to 5.0 / 8,
        '⅞' to 7.0 / 8
    )

    // Order matters: most specific alternatives first.
    private const val NUMBER =
        """\d+\s+\d+/\d+|\d+/\d+|\d+\s*[$UNICODE_FRACTIONS]|\d+(?:[.,]\d+)?|[.,]\d+|[$UNICODE_FRACTIONS]"""

    private val quantityRegex = Regex(
        """^\s*($NUMBER)(?:(\s*(?:-|–|—|to)\s*)($NUMBER))?(.*)$""",
        RegexOption.IGNORE_CASE
    )

    private val mixedFractionRegex = Regex("""^(\d+)\s+(\d+)/(\d+)$""")
    private val fractionRegex = Regex("""^(\d+)/(\d+)$""")
    private val wholeWithUnicodeRegex = Regex("""^(\d+)\s*([$UNICODE_FRACTIONS])$""")

    /** Denominators tried when formatting a result as a kitchen-friendly fraction. */
    private val friendlyDenominators = listOf(2, 3, 4, 8)
    private const val FRACTION_TOLERANCE = 0.02
    private const val WHOLE_TOLERANCE = 0.01

    /**
     * Ratio between [targetServings] and [originalServings]. Returns `1.0` when either is unknown
     * or non-positive, so callers can always apply the result safely.
     */
    fun multiplier(originalServings: Int?, targetServings: Int?): Double {
        if (originalServings == null || targetServings == null) return 1.0
        if (originalServings <= 0 || targetServings <= 0) return 1.0
        return targetServings.toDouble() / originalServings.toDouble()
    }

    /** Scales the leading amount of [quantity] by [multiplier], preserving any trailing text. */
    fun scaleQuantity(quantity: String?, multiplier: Double): String? {
        if (quantity.isNullOrBlank() || multiplier == 1.0 || multiplier <= 0.0) return quantity
        val match = quantityRegex.matchEntire(quantity) ?: return quantity
        val (firstToken, separator, secondToken, suffix) = match.destructured

        // Guard against partially-matched numbers such as "1.2.3" or "1/2/3".
        if (suffix.firstOrNull()?.let { it.isDigit() || it in "./," } == true) return quantity

        val first = parseNumber(firstToken) ?: return quantity
        val useFractions = usesFractionNotation(firstToken) || usesFractionNotation(secondToken)
        val scaledFirst = format(first * multiplier, useFractions)

        if (secondToken.isEmpty()) return scaledFirst + suffix

        val second = parseNumber(secondToken) ?: return quantity
        return scaledFirst + separator + format(second * multiplier, useFractions) + suffix
    }

    /** Returns [ingredients] with quantities scaled by [multiplier]; names and units are untouched. */
    fun scaleIngredients(ingredients: List<Ingredient>, multiplier: Double): List<Ingredient> {
        if (multiplier == 1.0) return ingredients
        return ingredients.map { it.copy(quantity = scaleQuantity(it.quantity, multiplier)) }
    }

    internal fun parseNumber(token: String): Double? {
        val t = token.trim()
        if (t.isEmpty()) return null
        mixedFractionRegex.matchEntire(t)?.let { m ->
            val (whole, num, den) = m.destructured
            val d = den.toDouble()
            return if (d == 0.0) null else whole.toDouble() + num.toDouble() / d
        }
        fractionRegex.matchEntire(t)?.let { m ->
            val (num, den) = m.destructured
            val d = den.toDouble()
            return if (d == 0.0) null else num.toDouble() / d
        }
        wholeWithUnicodeRegex.matchEntire(t)?.let { m ->
            val (whole, frac) = m.destructured
            return whole.toDouble() + (unicodeFractionValues[frac.first()] ?: return null)
        }
        if (t.length == 1) unicodeFractionValues[t.first()]?.let { return it }
        return t.replace(',', '.').toDoubleOrNull()
    }

    private fun usesFractionNotation(token: String): Boolean =
        token.contains('/') || token.any { it in UNICODE_FRACTIONS }

    internal fun format(value: Double, preferFractions: Boolean): String {
        val nearestWhole = value.roundToLong()
        if (abs(value - nearestWhole) < WHOLE_TOLERANCE && nearestWhole > 0) return nearestWhole.toString()
        if (preferFractions) formatAsFraction(value)?.let { return it }
        return formatAsDecimal(value)
    }

    private fun formatAsFraction(value: Double): String? {
        val whole = floor(value).toLong()
        val remainder = value - whole
        for (den in friendlyDenominators) {
            val num = (remainder * den).roundToLong()
            if (num in 1 until den && abs(remainder - num.toDouble() / den) < FRACTION_TOLERANCE) {
                val gcd = gcd(num, den.toLong())
                val fraction = "${num / gcd}/${den / gcd}"
                return if (whole > 0) "$whole $fraction" else fraction
            }
        }
        return null
    }

    private fun formatAsDecimal(value: Double): String {
        val scale = when {
            value >= 100 -> 0
            value >= 10 -> 1
            else -> 2
        }
        val rounded = BigDecimal(value).setScale(scale, RoundingMode.HALF_UP).stripTrailingZeros()
        // Never round a positive amount down to zero.
        val nonZero = if (rounded.signum() == 0) {
            BigDecimal(value).setScale(2, RoundingMode.UP).stripTrailingZeros()
        } else {
            rounded
        }
        return nonZero.toPlainString()
    }

    private tailrec fun gcd(a: Long, b: Long): Long = if (b == 0L) a else gcd(b, a % b)
}

/**
 * Returns a display copy of this recipe with ingredient quantities scaled from the recipe's own
 * servings count to [targetServings]. Returns `this` unchanged when no scaling applies.
 */
fun Recipe.scaledToServings(targetServings: Int?): Recipe {
    val multiplier = IngredientScaler.multiplier(parsedServingsCount(), targetServings)
    if (multiplier == 1.0) return this
    return copyOf(ingredients = IngredientScaler.scaleIngredients(ingredients, multiplier))
}
