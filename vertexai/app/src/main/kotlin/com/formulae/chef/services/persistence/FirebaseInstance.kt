package com.formulae.chef.services.persistence

import com.google.firebase.Firebase
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.firestore

object FirebaseInstance {
    /** The `(default)` Firestore database (europe-north1). Schema: `.ai/firestore-schema.md`. */
    val firestore: FirebaseFirestore by lazy { Firebase.firestore }
}
