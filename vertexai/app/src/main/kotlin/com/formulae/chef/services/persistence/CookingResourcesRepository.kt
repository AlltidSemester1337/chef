package com.formulae.chef.services.persistence

import com.formulae.chef.feature.model.CookingResource

data class CachedCookingResources(
    var resources: List<CookingResource> = emptyList(),
    var updatedAt: String = ""
)

interface CookingResourcesRepository {
    suspend fun load(): CachedCookingResources?
    suspend fun save(cached: CachedCookingResources)
}
