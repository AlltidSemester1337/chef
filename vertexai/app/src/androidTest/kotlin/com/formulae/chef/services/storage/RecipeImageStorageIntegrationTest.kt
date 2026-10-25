package com.formulae.chef.services.storage

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.formulae.chef.services.persistence.FirestoreEmulator.signInAsNewUser
import com.google.firebase.FirebaseApp
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageException
import java.util.UUID
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import org.junit.runner.RunWith

/**
 * [RecipeImageStorage] against the Storage + Auth emulators with the production `storage.rules`.
 *
 * Prerequisites: `npx firebase emulators:start --only firestore,auth,storage --project <project_id>`
 * from firestore-tools/ (see FirestoreEmulator), and an Android emulator.
 */
@RunWith(AndroidJUnit4::class)
class RecipeImageStorageIntegrationTest {

    private val storage: FirebaseStorage by lazy {
        val projectId = FirebaseApp.getInstance().options.projectId
        FirebaseStorage.getInstance("gs://$projectId.firebasestorage.app").apply { useEmulator(HOST, 9199) }
    }

    private val jpeg = byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte(), 0xD9.toByte())

    @Test
    fun upload_storesJpegAtUuidName_andReturnsTokenlessDownloadUrl() = runBlocking {
        signInAsNewUser()

        val url = RecipeImageStorage(storage).upload(jpeg)

        val match = Regex("""/o/recipes%2F([0-9a-f-]{36})\.jpg\?alt=media$""").find(url)
        assertTrue("unexpected URL $url", match != null)
        val ref = storage.reference.child("recipes/${match!!.groupValues[1]}.jpg")
        assertEquals("image/jpeg", ref.metadata.await().contentType)
    }

    // Release 4.2.0 uploads with putBytes() and no metadata. The emulator then stores
    // application/octet-stream (production infers image/jpeg from the name); the rules accept it
    // for <uuid>.jpg during the transition, so the released app keeps uploading.
    @Test
    fun uploadWithoutMetadata_asReleasedAppDoes_isAccepted() = runBlocking {
        signInAsNewUser()
        val ref = storage.reference.child("recipes/${UUID.randomUUID()}.jpg")

        ref.putBytes(jpeg).await()

        assertTrue(ref.metadata.await().sizeBytes > 0)
    }

    @Test
    fun uploadToExistingName_isDenied() = runBlocking {
        signInAsNewUser()
        val ref = storage.reference.child("recipes/${UUID.randomUUID()}.jpg")
        ref.putBytes(jpeg).await()

        try {
            ref.putBytes(jpeg).await()
            fail("overwrite should be denied")
        } catch (e: StorageException) {
            assertEquals(StorageException.ERROR_NOT_AUTHORIZED, e.errorCode)
        }
    }

    private companion object {
        const val HOST = "10.0.2.2"
    }
}
