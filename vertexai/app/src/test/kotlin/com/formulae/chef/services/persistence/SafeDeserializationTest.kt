package com.formulae.chef.services.persistence

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SafeDeserializationTest {
    private data class Doc(val id: String, val value: String)

    private val docs = listOf(Doc("a", "1"), Doc("bad", "x"), Doc("c", "3"))

    private fun convert(doc: Doc): Int = doc.value.toInt()

    @Test
    fun mapSkippingInvalid_skipsDocumentsThatFailToConvert() {
        val result = docs.mapSkippingInvalid(Doc::id, onSkipped = { _, _ -> }, convert = ::convert)

        assertEquals(listOf(1, 3), result)
    }

    @Test
    fun mapSkippingInvalid_reportsTheSkippedDocumentId() {
        val skipped = mutableListOf<Pair<String, Exception>>()

        docs.mapSkippingInvalid(Doc::id, onSkipped = { id, e -> skipped += id to e }, convert = ::convert)

        assertEquals(listOf("bad"), skipped.map { it.first })
        assertTrue(skipped.single().second is NumberFormatException)
    }

    @Test
    fun mapSkippingInvalid_dropsNullConversionsSilently() {
        var skippedCount = 0

        val result = docs.mapSkippingInvalid(
            Doc::id,
            onSkipped = { _, _ -> skippedCount++ },
            convert = { if (it.id == "a") null else it.id }
        )

        assertEquals(listOf("bad", "c"), result)
        assertEquals(0, skippedCount)
    }

    @Test
    fun mapSkippingInvalid_keepsOrder() {
        val many = (1..20).map { Doc("d$it", if (it % 5 == 0) "x" else "$it") }

        val result = many.mapSkippingInvalid(Doc::id, onSkipped = { _, _ -> }, convert = ::convert)

        assertEquals((1..20).filter { it % 5 != 0 }, result)
    }
}
