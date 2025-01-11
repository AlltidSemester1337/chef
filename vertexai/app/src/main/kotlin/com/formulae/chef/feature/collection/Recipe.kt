package com.formulae.chef.feature.collection

data class Recipe(
    val id: String? = null,
    val title: String,
    val description: String,
    val ingredients: List<String>,
    val instructions: List<String>,
    val tips: List<String> = emptyList(),
    val imageUrl: String? = null
)
