package com.formulae.chef.services.voice

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.formulae.chef.BuildConfig
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Integration tests for GcpTextToSpeechService against the live GCP Cloud TTS API.
 *
 * Requires a connected device/emulator and gcpTtsApiKey set in local.properties.
 * Tests are skipped automatically if no API key is configured.
 */
@RunWith(AndroidJUnit4::class)
class GcpTextToSpeechServiceTest {

    private lateinit var service: GcpTextToSpeechService

    @Before
    fun setUp() {
        assumeTrue(
            "Skipping: gcpTtsApiKey not configured in local.properties",
            BuildConfig.gcpTtsApiKey.isNotBlank()
        )
        service = GcpTextToSpeechService(BuildConfig.gcpTtsApiKey)
    }

    @Test
    fun synthesize_returnsNonEmptyAudioBytes() = runTest {
        val result = service.synthesize("Hello Chef, what shall we cook today?")
        assertNotNull(result)
        assertTrue("Expected non-empty audio bytes", result.isNotEmpty())
        // MP3 files start with 0xFF 0xFB or ID3 tag (0x49 0x44 0x33)
        assertTrue(
            "Expected MP3 audio format",
            (result[0] == 0xFF.toByte() && result[1] == 0xFB.toByte()) ||
                (result[0] == 0x49.toByte() && result[1] == 0x44.toByte())
        )
    }

    @Test(expected = GcpTtsException::class)
    fun synthesize_withBadApiKey_throwsGcpTtsException() = runTest {
        val badKeyService = GcpTextToSpeechService("invalid-api-key")
        badKeyService.synthesize("This should fail")
    }
}
