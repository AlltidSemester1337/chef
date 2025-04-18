package com.formulae.chef.services.persistence

import com.formulae.chef.BuildConfig
import com.google.firebase.FirebaseApp
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase

object FirebaseInstance {
    val database by lazy {
        Firebase.database(
            FirebaseApp.getInstance(),
            BuildConfig.firebaseDbUrl
        )
    }
}
