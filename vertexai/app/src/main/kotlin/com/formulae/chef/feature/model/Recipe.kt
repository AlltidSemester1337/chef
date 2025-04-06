package com.formulae.chef.feature.model

import com.google.firebase.database.PropertyName

data class Recipe(
    var id: String? = null,
    var uid: String = "",
    var title: String = "",
    var summary: String = "",
    var ingredients: String = "",
    var instructions: String = "",
    var imageUrl: String? = null,
    var updatedAt: String = "",
    @get:PropertyName("isFavourite")
    @set:PropertyName("isFavourite")
    var isFavourite: Boolean = false,
    var copyId: String? = null

)