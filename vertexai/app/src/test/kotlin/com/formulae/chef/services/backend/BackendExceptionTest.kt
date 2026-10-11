package com.formulae.chef.services.backend

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BackendExceptionTest {

    @Test
    fun fromResponse_429_isQuotaExceeded() {
        val e = BackendException.fromResponse(429, """{"detail":"quota_exceeded"}""")
        assertTrue(e is BackendException.QuotaExceeded)
    }

    @Test
    fun fromResponse_403WithEmailVerificationDetail_isEmailVerificationRequired() {
        val e = BackendException.fromResponse(403, """{"detail":"email_verification_required"}""")
        assertTrue(e is BackendException.EmailVerificationRequired)
    }

    @Test
    fun fromResponse_403WithOtherDetail_isUnexpected() {
        val e = BackendException.fromResponse(403, """{"detail":"forbidden"}""")
        assertTrue(e is BackendException.UnexpectedResponse)
        assertEquals(403, (e as BackendException.UnexpectedResponse).status)
    }

    @Test
    fun fromResponse_401_isUnauthorized() {
        val e = BackendException.fromResponse(401, """{"detail":"Not authenticated"}""")
        assertTrue(e is BackendException.Unauthorized)
    }

    @Test
    fun fromResponse_502_isUpstream() {
        val e = BackendException.fromResponse(502, """{"detail":"upstream_error"}""")
        assertTrue(e is BackendException.Upstream)
        assertEquals(502, (e as BackendException.Upstream).status)
    }

    @Test
    fun fromResponse_503WithHtmlBody_isUpstream() {
        val e = BackendException.fromResponse(503, "<html>Service Unavailable</html>")
        assertTrue(e is BackendException.Upstream)
    }

    @Test
    fun fromResponse_422_isUnexpected() {
        val e = BackendException.fromResponse(422, """{"detail":[{"msg":"Field required"}]}""")
        assertTrue(e is BackendException.UnexpectedResponse)
    }

    @Test
    fun fromResponse_422_doesNotEchoRequestInputInMessage() {
        val body = """{"detail":[{"msg":"Extra inputs","input":"my secret recipe question"}]}"""
        val e = BackendException.fromResponse(422, body)
        assertFalse(e.message!!.contains("my secret recipe question"))
    }

    @Test
    fun fromResponse_nullOrEmptyBody_isHandled() {
        assertTrue(BackendException.fromResponse(429, null) is BackendException.QuotaExceeded)
        assertTrue(BackendException.fromResponse(500, "") is BackendException.Upstream)
        assertTrue(BackendException.fromResponse(403, null) is BackendException.UnexpectedResponse)
    }
}
