package com.formulae.chef.services.backend

import org.json.JSONException
import org.json.JSONObject

/** Errors from chef-backend that callers handle differently. Network failures stay `IOException`s. */
sealed class BackendException(message: String) : Exception(message) {
    /** 429: beta quota used up. Also returned for anonymous users, who have no quota. */
    class QuotaExceeded : BackendException("Beta quota exceeded")

    /** 429 `rate_limited`: a per-user daily budget (e.g. recipe images) is used up. */
    class RateLimited : BackendException("Daily budget for this request type used up")

    /** 403: the user must verify their email address first. */
    class EmailVerificationRequired : BackendException("Email verification required")

    /** 401: missing or rejected ID / App Check token. */
    class Unauthorized : BackendException("Backend rejected the credentials")

    /** 5xx: the backend or the AI/TTS provider behind it failed. */
    class Upstream(val status: Int) : BackendException("Backend failed with HTTP $status")

    /** Any other status (e.g. 422) means the app sent a request the backend does not accept. */
    class UnexpectedResponse(val status: Int, detail: String?) :
        BackendException("Unexpected HTTP $status from backend: $detail")

    companion object {
        /** Maps a non-2xx response to the matching exception. [body] may be empty or non-JSON. */
        fun fromResponse(status: Int, body: String?): BackendException {
            val detail = parseDetail(body)
            return when {
                status == 429 && detail == "rate_limited" -> RateLimited()
                status == 429 -> QuotaExceeded()
                status == 403 && detail == "email_verification_required" -> EmailVerificationRequired()
                status == 401 -> Unauthorized()
                status >= 500 -> Upstream(status)
                else -> UnexpectedResponse(status, detail)
            }
        }

        // FastAPI errors look like {"detail": "..."}. Validation errors (422) have a list instead,
        // which echoes the request input (user content), so only string details are kept.
        private fun parseDetail(body: String?): String? {
            if (body.isNullOrBlank()) return null
            return try {
                JSONObject(body).opt("detail") as? String
            } catch (e: JSONException) {
                null
            }
        }
    }
}
