package com.formulae.chef.feature.home

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.formulae.chef.feature.model.CookingResource
import com.formulae.chef.services.authentication.UserSessionService
import com.formulae.chef.services.persistence.CachedCookingResources
import com.formulae.chef.services.persistence.CookingResourcesRepository
import com.formulae.chef.services.persistence.CookingResourcesRepositoryImpl
import com.formulae.chef.services.persistence.UserPreferencesRepository
import com.formulae.chef.services.persistence.UserPreferencesRepositoryImpl
import com.google.firebase.vertexai.GenerativeModel
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.util.Locale
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

private const val TAG = "HomeViewModel"
private const val CACHE_MAX_AGE_DAYS = 7L

class HomeViewModel(
    private val userSessionService: UserSessionService,
    private val resourcesGenerativeModel: GenerativeModel,
    private val preferencesRepositoryFactory: (String) -> UserPreferencesRepository = { uid ->
        UserPreferencesRepositoryImpl(uid)
    },
    private val resourcesRepositoryFactory: (String) -> CookingResourcesRepository = { uid ->
        CookingResourcesRepositoryImpl(uid)
    }
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val user = userSessionService.currentUser.first { it != null }
            if (user != null) {
                loadOrGenerateResources(user.uid)
            }
        }
    }

    private suspend fun loadOrGenerateResources(uid: String) {
        _uiState.value = _uiState.value.copy(isLoading = true)
        try {
            val prefsRepo = preferencesRepositoryFactory(uid)
            val resourcesRepo = resourcesRepositoryFactory(uid)

            val preferences = prefsRepo.loadPreferences()
            val cached = resourcesRepo.load()

            if (cached != null && !isStale(cached, preferences?.updatedAt)) {
                Log.d(TAG, "Using cached cooking resources (${cached.resources.size} items)")
                _uiState.value = HomeUiState(resources = cached.resources, isLoading = false)
                return
            }

            Log.d(TAG, "Generating fresh cooking resources via Gemini")
            val generated = generateResources(preferences?.summary.orEmpty())
            if (generated.isNotEmpty()) {
                val newCache = CachedCookingResources(
                    resources = generated,
                    updatedAt = ZonedDateTime.now(ZoneOffset.UTC).toString()
                )
                resourcesRepo.save(newCache)
                _uiState.value = HomeUiState(resources = generated, isLoading = false)
            } else {
                _uiState.value = HomeUiState(isLoading = false)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load/generate cooking resources", e)
            _uiState.value = HomeUiState(isLoading = false)
        }
    }

    private fun isStale(cached: CachedCookingResources, preferencesUpdatedAt: String?): Boolean {
        if (cached.updatedAt.isBlank()) return true
        return try {
            val cacheTime = ZonedDateTime.parse(cached.updatedAt)
            val maxAge = ZonedDateTime.now(ZoneOffset.UTC).minusDays(CACHE_MAX_AGE_DAYS)
            if (cacheTime.isBefore(maxAge)) return true

            if (!preferencesUpdatedAt.isNullOrBlank()) {
                val prefsTime = ZonedDateTime.parse(preferencesUpdatedAt)
                prefsTime.isAfter(cacheTime)
            } else {
                false
            }
        } catch (e: Exception) {
            Log.w(TAG, "Could not parse timestamps for staleness check, treating as stale", e)
            true
        }
    }

    private suspend fun generateResources(preferencesSummary: String): List<CookingResource> {
        val locale = Locale.getDefault()
        val prompt = buildString {
            append("User locale: ${locale.displayLanguage} (${locale.country})\n")
            if (preferencesSummary.isNotBlank()) {
                append("User cooking preferences: $preferencesSummary\n")
            }
            append("Recommend 5-8 personalized cooking resources for this user.")
        }
        return try {
            val response = resourcesGenerativeModel.generateContent(prompt)
            val json = response.text ?: return emptyList()
            val type = object : TypeToken<List<CookingResource>>() {}.type
            Gson().fromJson(json, type) ?: emptyList()
        } catch (e: Exception) {
            Log.e(TAG, "Gemini resource generation failed", e)
            emptyList()
        }
    }
}
