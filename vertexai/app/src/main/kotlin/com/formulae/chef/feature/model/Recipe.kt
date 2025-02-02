package com.formulae.chef.feature.model

data class Recipe(
    val id: String? = null,
    val title: String,
    val summary: String,
    val ingredients: String,
    val instructions: String,
    val imageUrl: String? = null,
    val updatedAt: String
)