package com.formulae.chef.services

import com.formulae.chef.services.persistence.BetaQuotaRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

private class FakeBetaQuotaRepository(
    override val uid: String,
    initialCount: Int = 0
) : BetaQuotaRepository {
    var count = initialCount
        private set
    var incrementCalls = 0
        private set

    override suspend fun getCount(): Int = count

    override suspend fun incrementAndGet(): Int {
        incrementCalls++
        count++
        return count
    }
}

class BetaQuotaServiceTest {

    private fun serviceWith(
        session: FakeUserSessionService,
        repository: FakeBetaQuotaRepository?
    ): BetaQuotaService {
        var requestedUid: String? = null
        return BetaQuotaService(session) { uid ->
            requestedUid = uid
            requireNotNull(repository) { "Repository should not be requested for uid=$requestedUid" }
        }
    }

    @Test
    fun `anonymous session is blocked without touching repository`() = runTest {
        val session = FakeUserSessionService(anonymousSession = true)
        val service = serviceWith(session, repository = null)

        assertEquals(QuotaResult.Blocked, service.checkQuota())
    }

    @Test
    fun `admin is allowed without touching repository or incrementing`() = runTest {
        val user = FakeUserInfo(uid = "admin-uid", email = "humlekottekonsult@gmail.com")
        val session = FakeUserSessionService(user = user)
        val service = serviceWith(session, repository = null)

        assertEquals(QuotaResult.Allowed, service.checkQuota())
        service.recordInteraction()
    }

    @Test
    fun `non-admin under limit is allowed and does not increment on checkQuota`() = runTest {
        val user = FakeUserInfo(uid = "uid-1", email = "someone@example.com")
        val session = FakeUserSessionService(user = user)
        val repo = FakeBetaQuotaRepository(uid = "uid-1", initialCount = 5)
        val service = serviceWith(session, repo)

        assertEquals(QuotaResult.Allowed, service.checkQuota())
        assertEquals(0, repo.incrementCalls)
    }

    @Test
    fun `non-admin at limit is blocked`() = runTest {
        val user = FakeUserInfo(uid = "uid-1", email = "someone@example.com")
        val session = FakeUserSessionService(user = user)
        val repo = FakeBetaQuotaRepository(uid = "uid-1", initialCount = BETA_INTERACTION_LIMIT)
        val service = serviceWith(session, repo)

        assertEquals(QuotaResult.Blocked, service.checkQuota())
    }

    @Test
    fun `recordInteraction increments the correct uid for a non-admin`() = runTest {
        val user = FakeUserInfo(uid = "uid-1", email = "someone@example.com")
        val session = FakeUserSessionService(user = user)
        val repo = FakeBetaQuotaRepository(uid = "uid-1", initialCount = 0)
        val service = serviceWith(session, repo)

        service.recordInteraction()

        assertEquals(1, repo.incrementCalls)
        assertEquals(1, repo.count)
    }

    @Test
    fun `not signed in is blocked`() = runTest {
        val session = FakeUserSessionService(anonymousSession = false, user = null)
        val service = serviceWith(session, repository = null)

        assertEquals(QuotaResult.Blocked, service.checkQuota())
        assertFalse(session.anonymousSession)
    }
}
