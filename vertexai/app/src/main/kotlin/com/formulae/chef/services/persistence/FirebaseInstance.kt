package com.formulae.chef.services.persistence

import com.formulae.chef.BuildConfig
import com.google.firebase.ktx.Firebase
import com.google.firebase.FirebaseApp
import com.google.firebase.database.ktx.database

object FirebaseInstance {
    val database by lazy {
        Firebase.database(
            FirebaseApp.getInstance(),
            BuildConfig.firebaseDbUrl
        )
    }
}
