package com.formulae.chef.feature.collection

data class Recipe(
    val id: String,
    val title: String,
    val ingredients: List<String>,
    val instructions: String
)