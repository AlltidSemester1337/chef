package com.formulae.chef.services.persistence

import android.util.Log
import com.google.firebase.firestore.DocumentSnapshot

/**
 * Converts each item on its own, so one malformed document cannot fail a whole list (e.g. a recipe
 * with a wrong field type would otherwise break the collection for every user). Items whose
 * conversion throws are reported via [onSkipped] and left out; `null` results are left out silently.
 */
internal inline fun <S, T : Any> List<S>.mapSkippingInvalid(
    idOf: (S) -> String,
    onSkipped: (id: String, e: Exception) -> Unit,
    convert: (S) -> T?
): List<T> = mapNotNull { item ->
    try {
        convert(item)
    } catch (e: Exception) {
        onSkipped(idOf(item), e)
        null
    }
}

/** [DocumentSnapshot.toObject] per document; malformed documents are logged by ID and skipped. */
internal fun <T : Any> List<DocumentSnapshot>.toObjectsSkippingInvalid(clazz: Class<T>): List<T> =
    mapSkippingInvalid(
        idOf = DocumentSnapshot::getId,
        onSkipped = { id, e -> logSkipped(clazz, id, e) },
        convert = { it.toObject(clazz) }
    )

/** Single-document variant of [toObjectsSkippingInvalid]: a malformed document reads as absent. */
internal fun <T : Any> DocumentSnapshot.toObjectOrNull(clazz: Class<T>): T? =
    try {
        toObject(clazz)
    } catch (e: Exception) {
        logSkipped(clazz, id, e)
        null
    }

// Document ID and the mapper's message (field path and types) only — never field values.
private fun logSkipped(clazz: Class<*>, id: String, e: Exception) {
    Log.w("Firestore", "Skipping malformed ${clazz.simpleName} document $id", e)
}
