package com.formulae.chef.feature.model

import com.formulae.chef.services.persistence.FirestoreTime
import com.google.firebase.Timestamp
import com.google.firebase.firestore.Exclude
import com.google.firebase.firestore.PropertyName

data class UserPreferences(
    var summary: String = "",
    @get:Exclude
    @set:Exclude
    var updatedAt: String = ""
) {
    /** Firestore mapping of [updatedAt], stored as a Timestamp. */
    @get:PropertyName("updatedAt")
    @set:PropertyName("updatedAt")
    var updatedAtTimestamp: Timestamp?
        get() = FirestoreTime.toTimestamp(updatedAt)
        set(value) {
            updatedAt = FirestoreTime.toIso(value)
        }
}
