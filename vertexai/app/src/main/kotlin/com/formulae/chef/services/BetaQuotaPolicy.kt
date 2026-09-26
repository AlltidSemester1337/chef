package com.formulae.chef.services

import com.formulae.chef.services.authentication.AdminUsers

const val BETA_INTERACTION_LIMIT = 20

sealed class QuotaResult {
    object Allowed : QuotaResult()
    object Blocked : QuotaResult()
    object EmailVerificationRequired : QuotaResult()
}

/**
 * Pure decision logic for CHE-39's open-beta interaction quota, kept separate from
 * [BetaQuotaService] so it can be unit tested without a real UserSessionService/Firebase.
 */
object BetaQuotaPolicy {

    sealed class Classification {
        object BlockedNoQuota : Classification()
        object AllowedUnlimited : Classification()
        object NeedsEmailVerification : Classification()
        object NeedsInteractionCount : Classification()
    }

    fun classify(
        anonymousSession: Boolean,
        uid: String?,
        email: String?,
        emailVerified: Boolean
    ): Classification =
        when {
            anonymousSession || uid == null -> Classification.BlockedNoQuota
            AdminUsers.isAdmin(email) -> Classification.AllowedUnlimited
            !emailVerified -> Classification.NeedsEmailVerification
            else -> Classification.NeedsInteractionCount
        }

    fun resultForInteractionCount(count: Int): QuotaResult =
        if (count > BETA_INTERACTION_LIMIT) QuotaResult.Blocked else QuotaResult.Allowed
}
