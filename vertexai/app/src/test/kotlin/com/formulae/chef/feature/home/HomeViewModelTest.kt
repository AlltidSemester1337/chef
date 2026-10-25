package com.formulae.chef.feature.home

import com.formulae.chef.feature.model.CookingResource
import com.formulae.chef.feature.model.UserPreferences
import com.formulae.chef.services.authentication.UserSessionService
import com.formulae.chef.services.backend.BackendException
import com.formulae.chef.services.backend.CookingResourcesService
import com.formulae.chef.services.persistence.CachedCookingResources
import com.formulae.chef.services.persistence.CookingResourcesRepository
import com.formulae.chef.services.persistence.UserPreferencesRepository
import com.google.firebase.auth.FirebaseUser
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private val freshResources = listOf(CookingResource("Fresh", "https://fresh.example", "blog", "New"))
    private val cachedResources = listOf(CookingResource("Cached", "https://cached.example", "website", "Old"))

    private val service = FakeCookingResourcesService(freshResources)
    private val resourcesRepository = FakeCookingResourcesRepository()
    private var preferences: UserPreferences? = null

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // The session is anonymous, so init does nothing and each test drives loadOrGenerateResources.
    private fun makeViewModel() = HomeViewModel(
        userSessionService = AnonymousSession(),
        cookingResourcesService = service,
        preferencesRepositoryFactory = { FakePreferencesRepository(preferences) },
        resourcesRepositoryFactory = { resourcesRepository },
        locale = { Locale.forLanguageTag("sv-SE-u-mu-celsius") }
    )

    private fun cacheOf(resources: List<CookingResource>, ageDays: Long) = CachedCookingResources(
        resources = resources,
        updatedAt = ZonedDateTime.now(ZoneOffset.UTC).minusDays(ageDays).toString()
    )

    @Test
    fun `fresh cache is shown without calling the backend`() = runTest(testDispatcher) {
        resourcesRepository.cached = cacheOf(cachedResources, ageDays = 1)
        val viewModel = makeViewModel()

        viewModel.loadOrGenerateResources("user-1")

        assertEquals(cachedResources, viewModel.uiState.value.resources)
        assertFalse(viewModel.uiState.value.isLoading)
        assertTrue(service.calls.isEmpty())
    }

    @Test
    fun `without cache resources are fetched with the locale tag and saved`() = runTest(testDispatcher) {
        val viewModel = makeViewModel()

        viewModel.loadOrGenerateResources("user-1")

        assertEquals(listOf("sv-SE"), service.calls)
        assertEquals(freshResources, viewModel.uiState.value.resources)
        assertEquals(freshResources, resourcesRepository.saved?.resources)
        assertTrue(resourcesRepository.saved!!.updatedAt.isNotBlank())
    }

    @Test
    fun `stale cache is replaced by fetched resources`() = runTest(testDispatcher) {
        resourcesRepository.cached = cacheOf(cachedResources, ageDays = 8)
        val viewModel = makeViewModel()

        viewModel.loadOrGenerateResources("user-1")

        assertEquals(1, service.calls.size)
        assertEquals(freshResources, viewModel.uiState.value.resources)
        assertEquals(freshResources, resourcesRepository.saved?.resources)
    }

    @Test
    fun `newer preferences make a fresh cache stale`() = runTest(testDispatcher) {
        resourcesRepository.cached = cacheOf(cachedResources, ageDays = 1)
        preferences = UserPreferences(summary = "Vegetarian", updatedAt = ZonedDateTime.now(ZoneOffset.UTC).toString())
        val viewModel = makeViewModel()

        viewModel.loadOrGenerateResources("user-1")

        assertEquals(1, service.calls.size)
        assertEquals(freshResources, viewModel.uiState.value.resources)
    }

    @Test
    fun `empty result is not cached`() = runTest(testDispatcher) {
        service.result = emptyList()
        val viewModel = makeViewModel()

        viewModel.loadOrGenerateResources("user-1")

        assertTrue(viewModel.uiState.value.resources.isEmpty())
        assertFalse(viewModel.uiState.value.isLoading)
        assertNull(resourcesRepository.saved)
    }

    @Test
    fun `backend failure falls back to the stale cache`() = runTest(testDispatcher) {
        resourcesRepository.cached = cacheOf(cachedResources, ageDays = 8)
        service.failure = BackendException.QuotaExceeded()
        val viewModel = makeViewModel()

        viewModel.loadOrGenerateResources("user-1")

        assertEquals(cachedResources, viewModel.uiState.value.resources)
        assertFalse(viewModel.uiState.value.isLoading)
        assertNull(resourcesRepository.saved)
    }

    @Test
    fun `backend failure without cache shows nothing`() = runTest(testDispatcher) {
        service.failure = BackendException.Upstream(502)
        val viewModel = makeViewModel()

        viewModel.loadOrGenerateResources("user-1")

        assertTrue(viewModel.uiState.value.resources.isEmpty())
        assertFalse(viewModel.uiState.value.isLoading)
    }

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

private class FakeCookingResourcesService(var result: List<CookingResource>) : CookingResourcesService {
    var failure: Exception? = null
    val calls = mutableListOf<String>()

    override suspend fun fetchCookingResources(languageTag: String): List<CookingResource> {
        calls += languageTag
        failure?.let { throw it }
        return result
    }
}

private class FakeCookingResourcesRepository : CookingResourcesRepository {
    var cached: CachedCookingResources? = null
    var saved: CachedCookingResources? = null

    override suspend fun load(): CachedCookingResources? = cached

    override suspend fun save(cached: CachedCookingResources) {
        saved = cached
    }
}

private class FakePreferencesRepository(private val preferences: UserPreferences?) : UserPreferencesRepository {
    override val uid: String = "user-1"
    override suspend fun loadPreferences(): UserPreferences? = preferences
    override suspend fun savePreferences(preferences: UserPreferences) = Unit
}

private class AnonymousSession : UserSessionService {
    override val currentUser: Flow<FirebaseUser?> = flowOf(null)
    override var anonymousSession: Boolean = true
    override suspend fun signInEmailPassword(email: String, password: String): FirebaseUser? = null
    override suspend fun signInUid(uid: String): FirebaseUser? = null
    override suspend fun createUser(email: String, password: String): FirebaseUser = error("Not used")
    override fun signOut() = Unit
    override suspend fun deleteUser(uid: String) = Unit
}
