package com.formulae.chef.services.persistence

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.formulae.chef.services.persistence.FirestoreEmulator.awaitCondition
import com.formulae.chef.services.persistence.FirestoreEmulator.signInAsNewUser
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/** [ChatHistoryRepositoryImpl] against the Firestore emulator with production rules. See [FirestoreEmulator]. */
@RunWith(AndroidJUnit4::class)
class ChatHistoryRepositoryImplIntegrationTest {

    private suspend fun newRepository() =
        ChatHistoryRepositoryImpl(uid = signInAsNewUser(), firestore = FirestoreEmulator.firestore)

    private suspend fun ChatHistoryRepositoryImpl.awaitEntries(count: Int) =
        awaitCondition({ loadChatHistoryLastTwentyEntries() }) { it.size >= count }

    @Test
    fun loadChatHistoryLastTwentyEntries_returnsEmptyListWhenNoHistory() = runBlocking {
        assertTrue(newRepository().loadChatHistoryLastTwentyEntries().isEmpty())
    }

    @Test
    fun saveNewEntries_andLoad_preservesRoleTextAndOrder() = runBlocking {
        val repository = newRepository()
        repository.saveNewEntries(
            listOf(
                Content(role = "user", parts = listOf(Part("What can I cook tonight?"))),
                Content(role = "model", parts = listOf(Part("Here are some ideas...")))
            )
        )

        val loaded = repository.awaitEntries(2)
        assertEquals(listOf("user", "model"), loaded.map { it.role })
        assertEquals("What can I cook tonight?", loaded[0].parts.first().text)
        assertEquals("Here are some ideas...", loaded[1].parts.first().text)
    }

    @Test
    fun loadChatHistoryLastTwentyEntries_returnsTheLastTwentyInOrder() = runBlocking {
        val repository = newRepository()
        repository.saveNewEntries((1..25).map { Content(role = "user", parts = listOf(Part("Message $it"))) })

        awaitCondition({ repository.loadAllEntries() }) { it.size == 25 }
        val texts = repository.loadChatHistoryLastTwentyEntries().map { it.parts.first().text }
        assertEquals((6..25).map { "Message $it" }, texts)
    }

    @Test
    fun saveNewEntries_multipleCallsAppendInOrder() = runBlocking {
        val repository = newRepository()
        repository.saveNewEntries(listOf(Content(role = "user", parts = listOf(Part("First")))))
        repository.awaitEntries(1)
        repository.saveNewEntries(listOf(Content(role = "model", parts = listOf(Part("Second")))))

        assertEquals(listOf("First", "Second"), repository.awaitEntries(2).map { it.parts.first().text })
    }

    @Test
    fun deleteEntries_removesOnlyTheGivenEntries() = runBlocking {
        val repository = newRepository()
        repository.saveNewEntries((1..3).map { Content(role = "user", parts = listOf(Part("M$it"))) })
        val all = awaitCondition({ repository.loadAllEntries() }) { it.size == 3 }

        repository.deleteEntries(all.take(2).map { it.first })

        assertEquals(listOf("M3"), repository.loadAllEntries().map { it.second.parts.first().text })
    }
}
