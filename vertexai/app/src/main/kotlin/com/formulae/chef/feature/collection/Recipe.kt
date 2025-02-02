package com.formulae.chef.feature.collection

data class Recipe(
    val title: String,
    val summary: String,
    val ingredients: String,
    val instructions: String,
    val imageUrl: String? = null,
    val updatedAt: String
)
