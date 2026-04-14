package com.formulae.chef.feature.home

import com.formulae.chef.feature.model.CookingResource

data class HomeUiState(
    val resources: List<CookingResource> = emptyList(),
    val isLoading: Boolean = false
)
