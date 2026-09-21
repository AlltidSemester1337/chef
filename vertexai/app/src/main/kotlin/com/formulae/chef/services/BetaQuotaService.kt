package com.formulae.chef.services

import com.formulae.chef.services.authentication.AdminUsers
import com.formulae.chef.services.authentication.UserSessionService
import com.formulae.chef.services.persistence.BetaQuotaRepository
import com.formulae.chef.services.persistence.BetaQuotaRepositoryImpl
import com.google.firebase.auth.UserInfo
import kotlinx.coroutines.flow.first

/**
 * Wraps [BetaQuotaPolicy] with the real session/Firebase dependencies. [checkQuota] gates a costly
 * AI interaction (main chat, overlay chat, recipe-variant adjust) up front, without spending quota.
 * [recordInteraction] should only be called after that AI call succeeds, so a failed/errored
 * request never permanently consumes part of a user's lifetime allotment.
 */
class BetaQuotaService(
    private val userSessionService: UserSessionService,
    private val repositoryFactory: (uid: String) -> BetaQuotaRepository = { uid -> BetaQuotaRepositoryImpl(uid) }
) {
    private suspend fun resolveUser(): UserInfo? {
        if (userSessionService.anonymousSession) return null
        return userSessionService.currentUser.first()
    }

    suspend fun checkQuota(): QuotaResult {
        val anonymousSession = userSessionService.anonymousSession
        val user = if (anonymousSession) null else resolveUser()

        return when (BetaQuotaPolicy.classify(anonymousSession, user?.uid, user?.email)) {
            BetaQuotaPolicy.Classification.BlockedNoQuota -> QuotaResult.Blocked
            BetaQuotaPolicy.Classification.AllowedUnlimited -> QuotaResult.Allowed
            BetaQuotaPolicy.Classification.NeedsInteractionCount -> {
                val count = repositoryFactory(user!!.uid).getCount()
                BetaQuotaPolicy.resultForInteractionCount(count)
            }
        }
    }

    suspend fun recordInteraction() {
        val user = resolveUser() ?: return
        if (AdminUsers.isAdmin(user.email)) return
        repositoryFactory(user.uid).incrementAndGet()
    }
}
