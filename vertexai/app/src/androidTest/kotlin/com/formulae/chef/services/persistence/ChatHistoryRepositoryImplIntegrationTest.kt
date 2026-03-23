package com.formulae.chef.services.persistence

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.vertexai.type.TextPart
import com.google.firebase.vertexai.type.content
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Integration tests for [ChatHistoryRepositoryImpl] against the Firebase Realtime Database emulator.
 *
 * Prerequisites:
 *   - Firebase Emulator Suite running: `firebase emulators:start --only database`
 *   - An Android emulator or connected device
 */
@RunWith(AndroidJUnit4::class)
class ChatHistoryRepositoryImplIntegrationTest {

    private lateinit var testDatabase: FirebaseDatabase
    private lateinit var repository: ChatHistoryRepositoryImpl

    private val testUid = "test-chat-user"

    @Before
    fun setup() {
        testDatabase = FirebaseDatabase.getInstance("http://10.0.2.2:9000?ns=chef-integration-test")
        testDatabase.useEmulator("10.0.2.2", 9000)
        repository = ChatHistoryRepositoryImpl(uid = testUid, database = testDatabase)
    }

    @After
    fun tearDown() = runBlocking {
        testDatabase.getReference("/").removeValue().await()
    }

    @Test
    fun loadChatHistoryLastTwentyEntries_returnsEmptyListWhenNoHistory() = runBlocking {
        val history = repository.loadChatHistoryLastTwentyEntries()
        assertTrue(history.isEmpty())
    }

    @Test
    fun saveNewEntries_andLoad_preservesRoleAndText() = runBlocking {
        val userEntry = content(role = "user") { text("What can I cook tonight?") }
        val modelEntry = content(role = "model") { text("Here are some ideas...") }

        repository.saveNewEntries(listOf(userEntry, modelEntry))

        val loaded = waitForEntries(count = 2)
        assertEquals(2, loaded.size)
        assertEquals("user", loaded[0].role)
        assertEquals("What can I cook tonight?", (loaded[0].parts.first() as TextPart).text)
        assertEquals("model", loaded[1].role)
        assertEquals("Here are some ideas...", (loaded[1].parts.first() as TextPart).text)
    }

    @Test
    fun loadChatHistoryLastTwentyEntries_returnsAtMostTwentyEntries() = runBlocking {
        val entries = (1..25).map { i ->
            content(role = if (i % 2 == 0) "model" else "user") { text("Message $i") }
        }
        repository.saveNewEntries(entries)

        val loaded = waitForEntries(count = 20)
        assertEquals(20, loaded.size)
    }

    @Test
    fun loadChatHistoryLastTwentyEntries_returnsLastEntries_notFirst() = runBlocking {
        val entries = (1..25).map { i ->
            content(role = "user") { text("Message $i") }
        }
        repository.saveNewEntries(entries)

        val loaded = waitForEntries(count = 20)
        // The last 20 of 25 messages should be messages 6–25
        val texts = loaded.map { (it.parts.first() as TextPart).text }
        assertTrue("Message 1" !in texts)
        assertTrue("Message 25" in texts)
    }

    @Test
    fun saveNewEntries_multipleCallsAppend() = runBlocking {
        repository.saveNewEntries(listOf(content(role = "user") { text("First") }))
        repository.saveNewEntries(listOf(content(role = "model") { text("Second") }))

        val loaded = waitForEntries(count = 2)
        assertEquals(2, loaded.size)
    }

    /** Polls until [count] entries are visible in the DB, to account for async writes. */
    private suspend fun waitForEntries(count: Int): List<com.google.firebase.vertexai.type.Content> {
        repeat(10) {
            val entries = repository.loadChatHistoryLastTwentyEntries()
            if (entries.size >= count) return entries
            kotlinx.coroutines.delay(200)
        }
        return repository.loadChatHistoryLastTwentyEntries()
    }
}
