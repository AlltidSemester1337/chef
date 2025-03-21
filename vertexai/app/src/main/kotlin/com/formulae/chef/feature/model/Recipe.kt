package com.formulae.chef.feature.model

data class Recipe(
    var id: String? = null,
    var uid: String = "",
    var title: String = "",
    var summary: String = "",
    var ingredients: String = "",
    var instructions: String = "",
    var imageUrl: String? = null,
    var updatedAt: String = "",
    var isFavourite: Boolean = false,
    var copyId: String? = null

)