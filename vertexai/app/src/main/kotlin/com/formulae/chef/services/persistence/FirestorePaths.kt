package com.formulae.chef.services.persistence

import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore

/** Collection/field names from `.ai/firestore-schema.md`. */
internal object FirestorePaths {
    const val RECIPES = "recipes"
    const val VARIANTS = "variants"
    const val RECIPE_OF_THE_MONTH = "recipe_of_the_month"
    const val USERS = "users"
    const val CHAT_HISTORY = "chat_history"
    const val LIKED_MESSAGES = "liked_messages"
    const val LISTS = "lists"

    // Single-object user data lives as map fields on users/{uid}.
    const val PREFERENCES = "preferences"
    const val COOKING_RESOURCES = "cooking_resources"
    const val BETA_INTERACTION_COUNT = "betaInteractionCount"
}

internal fun FirebaseFirestore.userDoc(uid: String): DocumentReference =
    collection(FirestorePaths.USERS).document(uid)

internal fun FirebaseFirestore.userCollection(uid: String, name: String): CollectionReference =
    userDoc(uid).collection(name)
