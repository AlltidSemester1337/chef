package com.formulae.chef.services.backend

import java.io.IOException
import kotlinx.coroutines.test.runTest
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** URLs, bodies, headers and error handling of [ChefBackendClient] against a fake transport. */
class ChefBackendClientUnitTest {

    private class Request(val url: String, val headers: Map<String, String>, val body: String)

    private val requests = mutableListOf<Request>()
    private val responses = ArrayDeque<BackendResponse>()
    private val tokenRefreshes = mutableListOf<Boolean>()

    private val client = ChefBackendClient(
        baseUrl = "https://backend.example/",
        tokenProvider = { forceRefresh ->
            tokenRefreshes += forceRefresh
            mapOf("Authorization" to "Bearer id-$forceRefresh", "X-Firebase-AppCheck" to "app-check")
        },
        transport = { url, headers, body, _ ->
            requests += Request(url, headers, body)
            responses.removeFirstOrNull() ?: throw IOException("No response queued")
        }
    )

    private fun respond(status: Int, body: String) {
        responses += BackendResponse(status, body.toByteArray())
    }

    @Test
    fun generateRecipeImage_postsRecipeAndReturnsUrl() = runTest {
        respond(200, """{"imageUrl":"https://img.example/recipes%2Fa.jpg?alt=media"}""")

        val url = client.generateRecipeImage("Recipe: Pancakes")

        assertEquals("https://img.example/recipes%2Fa.jpg?alt=media", url)
        val request = requests.single()
        assertEquals("https://backend.example/v1/images/recipe", request.url)
        assertEquals("Recipe: Pancakes", JSONObject(request.body).getString("recipe"))
        assertEquals("Bearer id-false", request.headers["Authorization"])
        assertEquals("app-check", request.headers["X-Firebase-AppCheck"])
    }

    @Test
    fun fetchCookingResources_postsLocaleAndReturnsResources() = runTest {
        respond(200, """{"resources":[{"title":"T","url":"https://t.example","type":"blog","description":"D"}]}""")

        val resources = client.fetchCookingResources("sv-SE")

        assertEquals(listOf("T"), resources.map { it.title })
        val request = requests.single()
        assertEquals("https://backend.example/v1/cooking-resources", request.url)
        assertEquals("sv-SE", JSONObject(request.body).getString("locale"))
    }

    @Test(expected = BackendException.RateLimited::class)
    fun generateRecipeImage_dailyBudgetUsedUp_throwsRateLimited() = runTest {
        respond(429, """{"detail":"rate_limited"}""")
        client.generateRecipeImage("Recipe: Pancakes")
    }

    @Test(expected = BackendException.QuotaExceeded::class)
    fun fetchCookingResources_quotaExceeded_throwsQuotaExceeded() = runTest {
        respond(429, """{"detail":"quota_exceeded"}""")
        client.fetchCookingResources("sv-SE")
    }

    @Test(expected = BackendException.EmailVerificationRequired::class)
    fun generateRecipeImage_unverifiedEmail_throwsEmailVerificationRequired() = runTest {
        respond(403, """{"detail":"email_verification_required"}""")
        client.generateRecipeImage("Recipe: Pancakes")
    }

    @Test(expected = BackendException.Upstream::class)
    fun generateRecipeImage_upstreamError_throwsUpstream() = runTest {
        respond(502, """{"detail":"upstream_error"}""")
        client.generateRecipeImage("Recipe: Pancakes")
    }

    @Test
    fun unauthorized_retriesOnceWithRefreshedTokens() = runTest {
        respond(401, """{"detail":"invalid_token"}""")
        respond(200, """{"resources":[]}""")

        val resources = client.fetchCookingResources("sv-SE")

        assertTrue(resources.isEmpty())
        assertEquals(listOf(false, true), tokenRefreshes)
        assertEquals("Bearer id-true", requests.last().headers["Authorization"])
    }

    @Test
    fun unauthorizedTwice_throwsUnauthorized() = runTest {
        respond(401, "")
        respond(401, "")

        val result = runCatching { client.generateRecipeImage("Recipe: Pancakes") }

        assertTrue(result.exceptionOrNull() is BackendException.Unauthorized)
        assertEquals(2, requests.size)
    }
}
