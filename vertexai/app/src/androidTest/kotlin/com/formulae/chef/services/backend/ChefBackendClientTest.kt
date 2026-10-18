package com.formulae.chef.services.backend

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.formulae.chef.BuildConfig
import com.formulae.chef.services.persistence.Content
import com.formulae.chef.services.persistence.Part
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Integration tests against the deployed chef-backend: URLs, headers and error mapping.
 *
 * Only rejected credentials are sent, so no AI or TTS call is ever made (no cost). A successful
 * round trip needs a signed-in user and a registered App Check debug token, and is covered by
 * manual E2E testing. Skipped when chefBackendUrl is not set in local.properties.
 */
@RunWith(AndroidJUnit4::class)
class ChefBackendClientTest {

    private val tokenRequests = mutableListOf<Boolean>()
    private lateinit var client: ChefBackendClient

    @Before
    fun setUp() {
        assumeTrue("Skipping: chefBackendUrl not configured", BuildConfig.chefBackendUrl.isNotBlank())
        client = ChefBackendClient(BuildConfig.chefBackendUrl) { forceRefresh ->
            tokenRequests += forceRefresh
            mapOf("Authorization" to "Bearer not-a-token", "X-Firebase-AppCheck" to "not-a-token")
        }
    }

    @Test(expected = BackendException.Unauthorized::class)
    fun complete_withRejectedTokens_throwsUnauthorized() = runTest {
        client.complete(AiPurpose.CHAT, listOf(Content(role = "user", parts = listOf(Part("Hello")))))
    }

    @Test(expected = BackendException.Unauthorized::class)
    fun synthesize_withRejectedTokens_throwsUnauthorized() = runTest {
        client.synthesize("Hello Chef.")
    }

    @Test
    fun unauthorized_retriesOnceWithRefreshedTokens() = runTest {
        runCatching { client.synthesize("Hello Chef.") }
        assertEquals(listOf(false, true), tokenRequests)
    }
}
