package com.formulae.chef.services.backend

import com.formulae.chef.feature.model.CookingResource
import java.util.Locale
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CookingResourcesRequestsTest {

    @Test
    fun languageTag_languageAndRegion() {
        assertEquals("sv-SE", CookingResourcesRequests.languageTag(Locale.forLanguageTag("sv-SE")))
    }

    @Test
    fun languageTag_dropsUnicodeExtensions() {
        // Android 14+ adds regional preferences (temperature unit, first day of week) as extensions.
        val locale = Locale.forLanguageTag("en-SE-u-fw-mon-mu-celsius")

        assertEquals("en-SE", CookingResourcesRequests.languageTag(locale))
    }

    @Test
    fun languageTag_keepsScript() {
        assertEquals("zh-Hant-TW", CookingResourcesRequests.languageTag(Locale.forLanguageTag("zh-Hant-TW")))
    }

    @Test
    fun languageTag_tagTheBackendRejects_fallsBackToLanguage() {
        // Private-use subtags ("-x-...") don't fit the backend's pattern.
        assertEquals("de", CookingResourcesRequests.languageTag(Locale.forLanguageTag("de-DE-x-lvariant-a")))
    }

    @Test
    fun languageTag_emptyLocale_fallsBackToEnglish() {
        assertEquals("en", CookingResourcesRequests.languageTag(Locale.ROOT))
    }

    @Test
    fun buildRequestBody_hasOnlyTheLocale() {
        val json = JSONObject(CookingResourcesRequests.buildRequestBody("sv-SE"))

        assertEquals(setOf("locale"), json.keys().asSequence().toSet())
        assertEquals("sv-SE", json.getString("locale"))
    }

    @Test
    fun parseResponse_returnsResources() {
        val body = """{"resources":[{"title":"Köket","url":"https://www.koket.se","type":"website",""" +
            """"description":"Svenska recept."}]}"""

        assertEquals(
            listOf(CookingResource("Köket", "https://www.koket.se", "website", "Svenska recept.")),
            CookingResourcesRequests.parseResponse(body)
        )
    }

    @Test
    fun parseResponse_emptyList() {
        assertTrue(CookingResourcesRequests.parseResponse("""{"resources":[]}""").isEmpty())
    }

    @Test
    fun parseResponse_skipsEntriesWithoutTitleOrHttpsUrl() {
        val body = """{"resources":[""" +
            """{"title":"Plain http","url":"http://example.com","type":"website","description":""},""" +
            """{"title":"Script","url":"javascript:alert(1)","type":"website","description":""},""" +
            """{"title":"","url":"https://example.com","type":"website","description":""},""" +
            """"not an object",""" +
            """{"title":"Ok","url":"https://example.com","type":"blog","description":"d"}]}"""

        assertEquals(listOf("Ok"), CookingResourcesRequests.parseResponse(body).map { it.title })
    }
}
