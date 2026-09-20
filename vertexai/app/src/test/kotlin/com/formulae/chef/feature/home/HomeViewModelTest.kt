package com.formulae.chef.feature.home

import com.formulae.chef.services.persistence.CachedCookingResources
import java.time.ZoneOffset
import java.time.ZonedDateTime
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeViewModelTest {

    @Test
    fun `blank cache updatedAt is stale`() {
        val cached = CachedCookingResources(updatedAt = "")

        assertTrue(HomeViewModel.isStale(cached, preferencesUpdatedAt = null))
    }

    @Test
    fun `cache older than 7 days is stale`() {
        val cached = CachedCookingResources(
            updatedAt = ZonedDateTime.now(ZoneOffset.UTC).minusDays(8).toString()
        )

        assertTrue(HomeViewModel.isStale(cached, preferencesUpdatedAt = null))
    }

    @Test
    fun `preferences updated after cache is stale`() {
        val cacheTime = ZonedDateTime.now(ZoneOffset.UTC).minusDays(1)
        val cached = CachedCookingResources(updatedAt = cacheTime.toString())
        val preferencesUpdatedAt = ZonedDateTime.now(ZoneOffset.UTC).toString()

        assertTrue(HomeViewModel.isStale(cached, preferencesUpdatedAt))
    }

    @Test
    fun `fresh cache with older preferences is not stale`() {
        val cacheTime = ZonedDateTime.now(ZoneOffset.UTC).minusHours(1)
        val cached = CachedCookingResources(updatedAt = cacheTime.toString())
        val preferencesUpdatedAt = ZonedDateTime.now(ZoneOffset.UTC).minusDays(2).toString()

        assertFalse(HomeViewModel.isStale(cached, preferencesUpdatedAt))
    }
}
