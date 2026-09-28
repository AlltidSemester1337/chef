package com.formulae.chef.feature.chat

import com.formulae.chef.feature.model.LikedMessage
import com.formulae.chef.services.persistence.LikedMessagesRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LikedMessagesStoreTest {

    private class FakeLikedMessagesRepository(
        initial: List<Pair<String, LikedMessage>> = emptyList()
    ) : LikedMessagesRepository {
        val stored = initial.toMutableList()
        val deletedIds = mutableListOf<String>()
        private var nextKey = 0

        override suspend fun loadLikedMessages(): List<Pair<String, LikedMessage>> = stored.toList()

        override fun saveLikedMessage(text: String): String {
            val key = "key-${nextKey++}"
            stored += key to LikedMessage(text = text, likedAt = "now")
            return key
        }

        override fun deleteMessages(ids: List<String>) {
            deletedIds += ids
            stored.removeAll { it.first in ids }
        }
    }

    private fun store(repo: LikedMessagesRepository) = LikedMessagesStore(repo, now = { "now" })

    @Test
    fun `liking a message saves it and marks it liked`() {
        val repo = FakeLikedMessagesRepository()
        val store = store(repo)

        store.setLiked("Great recipe", liked = true)

        assertTrue("Great recipe" in store.likedTexts())
        assertEquals(listOf("Great recipe"), repo.stored.map { it.second.text })
    }

    @Test
    fun `un-liking a just-liked message removes it from cache and repository (issue 63)`() {
        val repo = FakeLikedMessagesRepository()
        val store = store(repo)

        store.setLiked("Great recipe", liked = true)
        store.setLiked("Great recipe", liked = false)

        assertFalse("Great recipe" in store.likedTexts())
        assertTrue(store.entries.isEmpty())
        assertTrue(repo.stored.isEmpty())
        assertEquals(listOf("key-0"), repo.deletedIds)
    }

    @Test
    fun `un-liking a message loaded from the repository deletes it by its key`() = runTest {
        val repo = FakeLikedMessagesRepository(
            listOf(
                "persisted-1" to LikedMessage(text = "Old favourite", likedAt = "then"),
                "persisted-2" to LikedMessage(text = "Keep me", likedAt = "then")
            )
        )
        val store = store(repo)
        store.load()

        store.setLiked("Old favourite", liked = false)

        assertEquals(listOf("persisted-1"), repo.deletedIds)
        assertEquals(setOf("Keep me"), store.likedTexts())
    }

    @Test
    fun `liking an already liked message does not save a duplicate`() {
        val repo = FakeLikedMessagesRepository()
        val store = store(repo)

        store.setLiked("Great recipe", liked = true)
        store.setLiked("Great recipe", liked = true)

        assertEquals(1, repo.stored.size)
        assertEquals(1, store.entries.size)
    }

    @Test
    fun `un-liking a message that is not cached is a no-op`() {
        val repo = FakeLikedMessagesRepository()
        val store = store(repo)

        store.setLiked("Never liked", liked = false)

        assertTrue(repo.deletedIds.isEmpty())
        assertTrue(store.entries.isEmpty())
    }

    @Test
    fun `can like again after un-liking`() {
        val repo = FakeLikedMessagesRepository()
        val store = store(repo)

        store.setLiked("Great recipe", liked = true)
        store.setLiked("Great recipe", liked = false)
        store.setLiked("Great recipe", liked = true)

        assertEquals(setOf("Great recipe"), store.likedTexts())
        assertEquals(listOf("key-1"), repo.stored.map { it.first })
    }

    @Test
    fun `deleteCompacted only drops the compacted entries`() {
        val repo = FakeLikedMessagesRepository()
        val store = store(repo)
        store.setLiked("A", liked = true)
        val compacted = store.entries.toList()
        store.setLiked("B", liked = true)

        store.deleteCompacted(compacted)

        assertEquals(setOf("B"), store.likedTexts())
        assertEquals(listOf("key-0"), repo.deletedIds)
    }
}
