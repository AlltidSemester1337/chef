package com.formulae.chef.services

import org.junit.Assert.assertEquals
import org.junit.Test

class BetaQuotaPolicyTest {

    @Test
    fun `anonymous session is blocked without checking uid`() {
        val result = BetaQuotaPolicy.classify(anonymousSession = true, uid = null, email = null)
        assertEquals(BetaQuotaPolicy.Classification.BlockedNoQuota, result)
    }

    @Test
    fun `no uid is blocked`() {
        val result = BetaQuotaPolicy.classify(
            anonymousSession = false,
            uid = null,
            email = "someone@example.com"
        )
        assertEquals(BetaQuotaPolicy.Classification.BlockedNoQuota, result)
    }

    @Test
    fun `admin email is allowed unlimited`() {
        val result = BetaQuotaPolicy.classify(
            anonymousSession = false,
            uid = "uid-1",
            email = "humlekottekonsult@gmail.com"
        )
        assertEquals(BetaQuotaPolicy.Classification.AllowedUnlimited, result)
    }

    @Test
    fun `non-admin signed-in user needs interaction count`() {
        val result = BetaQuotaPolicy.classify(
            anonymousSession = false,
            uid = "uid-1",
            email = "someone@example.com"
        )
        assertEquals(BetaQuotaPolicy.Classification.NeedsInteractionCount, result)
    }

    @Test
    fun `count at limit is allowed`() {
        assertEquals(QuotaResult.Allowed, BetaQuotaPolicy.resultForInteractionCount(BETA_INTERACTION_LIMIT))
    }

    @Test
    fun `count below limit is allowed`() {
        assertEquals(QuotaResult.Allowed, BetaQuotaPolicy.resultForInteractionCount(1))
    }

    @Test
    fun `count over limit is blocked`() {
        assertEquals(
            QuotaResult.Blocked,
            BetaQuotaPolicy.resultForInteractionCount(BETA_INTERACTION_LIMIT + 1)
        )
    }
}
