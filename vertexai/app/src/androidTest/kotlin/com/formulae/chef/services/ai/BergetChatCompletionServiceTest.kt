package com.formulae.chef.services.ai

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.formulae.chef.BuildConfig
import com.formulae.chef.services.persistence.Content
import com.formulae.chef.services.persistence.Part
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Integration tests for BergetChatCompletionService against the live Berget.ai API.
 *
 * Requires a connected device/emulator and bergetApiKey set in local.properties.
 * Tests are skipped automatically if no API key is configured.
 */
@RunWith(AndroidJUnit4::class)
class BergetChatCompletionServiceTest {

    private lateinit var service: BergetChatCompletionService

    @Before
    fun setUp() {
        assumeTrue(
            "Skipping: bergetApiKey not configured in local.properties",
            BuildConfig.bergetApiKey.isNotBlank()
        )
        service = BergetChatCompletionService(BuildConfig.bergetApiKey)
    }

    @Test
    fun createChatCompletion_returnsNonBlankResponse() = runTest {
        val config = BergetModelConfig(
            model = BERGET_MISTRAL_SMALL_3_2,
            temperature = 0.2f,
            topP = 0.95f,
            maxTokens = 64
        )
        val result = service.createChatCompletion(
            config,
            listOf(Content(role = "user", parts = listOf(Part("Reply with the single word: pong"))))
        )
        assertNotNull(result)
        assertFalse("Expected non-blank response", result.isBlank())
    }

    @Test(expected = BergetChatCompletionException::class)
    fun createChatCompletion_withBadApiKey_throwsBergetChatCompletionException() = runTest {
        val badKeyService = BergetChatCompletionService("invalid-api-key")
        val config = BergetModelConfig(
            model = BERGET_MISTRAL_SMALL_3_2,
            temperature = 0.2f,
            topP = 0.95f,
            maxTokens = 16
        )
        badKeyService.createChatCompletion(
            config,
            listOf(Content(role = "user", parts = listOf(Part("hello"))))
        )
    }
}
