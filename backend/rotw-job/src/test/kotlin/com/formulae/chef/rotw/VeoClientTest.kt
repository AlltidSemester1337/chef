package com.formulae.chef.rotw

import com.formulae.chef.rotw.service.extractOperationName
import com.formulae.chef.rotw.service.extractVideoBytes
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject

class VeoClientTest {

    private fun jsonObject(raw: String): JsonObject = Json.parseToJsonElement(raw) as JsonObject

    @Test
    fun `extractOperationName returns name from a successful LRO response`() {
        val response = jsonObject(
            """{ "name": "projects/p/locations/l/operations/12345" }"""
        )

        val operationName = extractOperationName(response)

        assertEquals("projects/p/locations/l/operations/12345", operationName)
    }

    @Test
    fun `extractOperationName surfaces the API error instead of a generic message`() {
        val response = jsonObject(
            """{ "error": { "code": 404, "message": "Publisher Model not found", "status": "NOT_FOUND" } }"""
        )

        val exception = assertFailsWith<IllegalStateException> { extractOperationName(response) }

        assertContains(exception.message.orEmpty(), "Publisher Model not found")
    }

    @Test
    fun `extractOperationName fails loudly with the raw response when name is missing`() {
        val response = jsonObject("""{ "unexpected": "shape" }""")

        val exception = assertFailsWith<IllegalStateException> { extractOperationName(response) }

        assertContains(exception.message.orEmpty(), "unexpected")
    }

    @Test
    fun `extractVideoBytes decodes base64 video bytes from a completed operation`() {
        val encoded = java.util.Base64.getEncoder().encodeToString(byteArrayOf(1, 2, 3, 4))
        val status = jsonObject(
            """{ "done": true, "response": { "videos": [ { "bytesBase64Encoded": "$encoded" } ] } }"""
        )

        val bytes = extractVideoBytes(status)

        assertEquals(listOf<Byte>(1, 2, 3, 4), bytes.toList())
    }

    @Test
    fun `extractVideoBytes surfaces the API error for a failed completed operation`() {
        val status = jsonObject(
            """{ "done": true, "error": { "code": 8, "message": "Quota exceeded", "status": "RESOURCE_EXHAUSTED" } }"""
        )

        val exception = assertFailsWith<IllegalStateException> { extractVideoBytes(status) }

        assertContains(exception.message.orEmpty(), "Quota exceeded")
    }
}
