package com.formulae.chef.feature.chat

import com.formulae.chef.feature.model.LikedMessage
import com.formulae.chef.services.persistence.LikedMessagesRepository
import java.time.ZoneOffset
import java.time.ZonedDateTime

/**
 * In-memory cache of the user's liked chat responses, kept in sync with [LikedMessagesRepository].
 *
 * Entries are `(firebaseKey, message)` pairs so an un-like can delete exactly the persisted node(s)
 * that were created for that response text.
 */
class LikedMessagesStore(
    private val repository: LikedMessagesRepository,
    private val now: () -> String = { ZonedDateTime.now(ZoneOffset.UTC).toString() }
) {
    var entries: List<Pair<String, LikedMessage>> = emptyList()
        private set

    suspend fun load() {
        entries = repository.loadLikedMessages()
    }

    fun likedTexts(): Set<String> = entries.map { (_, msg) -> msg.text }.toSet()

    /** Persists the liked/un-liked state of a model response identified by its [text]. */
    fun setLiked(text: String, liked: Boolean) {
        val matching = entries.filter { (_, msg) -> msg.text == text }
        if (liked) {
            if (matching.isNotEmpty()) return
            val key = repository.saveLikedMessage(text) ?: ""
            entries = entries + (key to LikedMessage(text = text, likedAt = now()))
        } else {
            if (matching.isEmpty()) return
            val ids = matching.map { it.first }.filter { it.isNotBlank() }
            if (ids.isNotEmpty()) repository.deleteMessages(ids)
            entries = entries - matching.toSet()
        }
    }

    /**
     * Deletes entries that were folded into the preferences summary during compaction. Only the
     * given entries are dropped, so likes added while compaction was running are kept.
     */
    fun deleteCompacted(compacted: List<Pair<String, LikedMessage>>) {
        val ids = compacted.map { it.first }.filter { it.isNotBlank() }
        if (ids.isNotEmpty()) repository.deleteMessages(ids)
        entries = entries - compacted.toSet()
    }
}
