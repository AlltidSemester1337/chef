package com.formulae.chef.services.backend

import com.formulae.chef.services.persistence.Content
import kotlinx.coroutines.CompletableDeferred

/**
 * Records every call. Answers with [replies] for the purpose (else [reply]) or throws [failures]
 * for the purpose (else [failure]). [gate] lets a test hold a call in flight.
 */
class FakeAiCompletionService(
    var reply: String = "Model answer",
    var failure: Exception? = null,
    var gate: CompletableDeferred<Unit>? = null
) : AiCompletionService {
    val replies = mutableMapOf<AiPurpose, String>()
    val failures = mutableMapOf<AiPurpose, Exception>()
    val calls = mutableListOf<Pair<AiPurpose, List<Content>>>()

    override suspend fun complete(purpose: AiPurpose, messages: List<Content>): String {
        calls += purpose to messages.toList()
        gate?.await()
        (failures[purpose] ?: failure)?.let { throw it }
        return replies[purpose] ?: reply
    }
}
