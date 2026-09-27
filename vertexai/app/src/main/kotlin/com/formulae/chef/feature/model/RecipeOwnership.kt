package com.formulae.chef.feature.model

/**
 * True only when [currentUid] belongs to a signed-in user and matches the recipe's owner uid.
 * A null/blank uid never counts as ownership, so anonymous sessions (or a recipe with an unset
 * uid) can never unlock owner-only actions such as creating/pinning/deleting variants.
 */
fun Recipe.isOwnedBy(currentUid: String?): Boolean = !currentUid.isNullOrBlank() && uid == currentUid
