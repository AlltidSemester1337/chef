package com.formulae.chef.feature.model

import com.formulae.chef.services.persistence.FirestoreTime
import com.google.firebase.Timestamp
import com.google.firebase.firestore.Exclude
import com.google.firebase.firestore.PropertyName

data class LikedMessage(
    var text: String = "",
    @get:Exclude
    @set:Exclude
    var likedAt: String = ""
) {
    /** Firestore mapping of [likedAt], stored as a Timestamp. */
    @get:PropertyName("likedAt")
    @set:PropertyName("likedAt")
    var likedAtTimestamp: Timestamp?
        get() = FirestoreTime.toTimestamp(likedAt)
        set(value) {
            likedAt = FirestoreTime.toIso(value)
        }
}
