package com.formulae.chef.services

import com.formulae.chef.services.authentication.UserSessionService
import com.formulae.chef.services.persistence.BetaQuotaRepository
import com.formulae.chef.services.persistence.BetaQuotaRepositoryImpl
import kotlinx.coroutines.flow.first

/**
 * Wraps [BetaQuotaPolicy] with the real session/Firebase dependencies. Called once per costly AI
 * interaction (main chat, overlay chat, recipe-variant adjust) before making the AI call.
 */
class BetaQuotaService(
    private val userSessionService: UserSessionService,
    private val repositoryFactory: (uid: String) -> BetaQuotaRepository = { uid -> BetaQuotaRepositoryImpl(uid) }
) {
    suspend fun checkAndRecordInteraction(): QuotaResult {
        val anonymousSession = userSessionService.anonymousSession
        val user = if (anonymousSession) null else userSessionService.currentUser.first()

        val classification = BetaQuotaPolicy.classify(
            anonymousSession,
            user?.uid,
            user?.email,
            user?.isEmailVerified == true
        )
        return when (classification) {
            BetaQuotaPolicy.Classification.BlockedNoQuota -> QuotaResult.Blocked
            BetaQuotaPolicy.Classification.AllowedUnlimited -> QuotaResult.Allowed
            BetaQuotaPolicy.Classification.NeedsEmailVerification ->
                QuotaResult.EmailVerificationRequired
            BetaQuotaPolicy.Classification.NeedsInteractionCount -> {
                val count = repositoryFactory(user!!.uid).incrementAndGet()
                BetaQuotaPolicy.resultForInteractionCount(count)
            }
        }
    }
}
