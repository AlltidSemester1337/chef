package com.formulae.chef.services.backend

import com.formulae.chef.feature.model.CookingResource
import java.util.Locale
import org.json.JSONObject

/** Pure request/response shaping for `POST /v1/cooking-resources`, kept free of I/O so it is unit tested. */
object CookingResourcesRequests {

    // Same pattern as the backend; anything else is rejected with 422.
    private val LANGUAGE_TAG = Regex("^[A-Za-z]{2,3}(-[A-Za-z0-9]{2,8}){0,3}$")
    private const val FALLBACK_TAG = "en"

    /**
     * BCP 47 tag the backend accepts, e.g. "sv-SE". Unicode extensions such as Android's regional
     * preferences ("-u-mu-celsius") are dropped; a tag that still doesn't fit falls back to the
     * bare language. Without a language ("und") the tag is "en".
     */
    fun languageTag(locale: Locale): String {
        val tag = locale.stripExtensions().toLanguageTag()
        return when {
            locale.language.isEmpty() -> FALLBACK_TAG
            LANGUAGE_TAG.matches(tag) -> tag
            LANGUAGE_TAG.matches(locale.language) -> locale.language
            else -> FALLBACK_TAG
        }
    }

    /**
     * Builds `{"locale": ...}`. The user's preferences are not sent: the backend reads them from
     * Firestore itself.
     */
    fun buildRequestBody(languageTag: String): String = JSONObject().put("locale", languageTag).toString()

    /**
     * Parses `{"resources": [{"title", "url", "type", "description"}]}`. The backend validates
     * the list already; entries without a title or an https URL are still skipped, because the
     * app opens the URLs.
     */
    fun parseResponse(responseBody: String): List<CookingResource> {
        val resources = JSONObject(responseBody).getJSONArray("resources")
        return (0 until resources.length()).mapNotNull { i ->
            val item = resources.optJSONObject(i) ?: return@mapNotNull null
            val resource = CookingResource(
                title = item.optString("title"),
                url = item.optString("url"),
                type = item.optString("type"),
                description = item.optString("description")
            )
            resource.takeIf { it.title.isNotBlank() && it.url.startsWith("https://") }
        }
    }
}
