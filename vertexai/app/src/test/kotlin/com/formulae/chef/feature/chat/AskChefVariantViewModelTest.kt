package com.formulae.chef.feature.chat

import com.formulae.chef.feature.model.Ingredient
import com.formulae.chef.feature.model.Recipe
import com.formulae.chef.services.backend.AiPurpose
import com.formulae.chef.services.backend.BackendException
import com.formulae.chef.services.backend.FakeAiCompletionService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AskChefVariantViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val ai = FakeAiCompletionService()
    private lateinit var viewModel: AskChefVariantViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        viewModel = AskChefVariantViewModel(ai)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private val adjustedJson = """{"recipes":[{"title":"Vegetarian Carbonara","summary":"No meat"}]}"""

    // --- adjustRecipe ---

    @Test
    fun adjustRecipe_adjustsThenExtractsJson() = runTest(testDispatcher) {
        ai.replies[AiPurpose.RECIPE_ADJUST] = "Adjusted recipe text"
        ai.replies[AiPurpose.DERIVE_RECIPE_JSON] = adjustedJson

        viewModel.adjustRecipe(sampleRecipe, "make it vegetarian")
        advanceUntilIdle()

        assertEquals(listOf(AiPurpose.RECIPE_ADJUST, AiPurpose.DERIVE_RECIPE_JSON), ai.calls.map { it.first })
        val adjustPrompt = ai.calls[0].second.single().parts.single().text
        assertTrue(adjustPrompt.contains("Pasta Carbonara"))
        assertTrue(adjustPrompt.contains("make it vegetarian"))
        assertEquals("Adjusted recipe text", ai.calls[1].second.single().parts.single().text)
        val state = viewModel.state.value as AskChefVariantViewModel.State.Success
        assertEquals("Vegetarian Carbonara", state.recipe.title)
    }

    @Test
    fun adjustRecipe_quotaExceeded_skipsExtractionAndReportsQuota() = runTest(testDispatcher) {
        ai.failures[AiPurpose.RECIPE_ADJUST] = BackendException.QuotaExceeded()

        viewModel.adjustRecipe(sampleRecipe, "make it vegetarian")
        advanceUntilIdle()

        assertEquals(AskChefVariantViewModel.State.QuotaExceeded, viewModel.state.value)
        assertEquals(1, ai.calls.size)
    }

    @Test
    fun adjustRecipe_emailVerificationRequired_reportsVerification() = runTest(testDispatcher) {
        ai.failures[AiPurpose.RECIPE_ADJUST] = BackendException.EmailVerificationRequired()

        viewModel.adjustRecipe(sampleRecipe, "make it vegetarian")
        advanceUntilIdle()

        assertEquals(AskChefVariantViewModel.State.EmailVerificationRequired, viewModel.state.value)
    }

    @Test
    fun adjustRecipe_extractionFails_reportsError() = runTest(testDispatcher) {
        ai.failures[AiPurpose.DERIVE_RECIPE_JSON] = BackendException.Upstream(502)

        viewModel.adjustRecipe(sampleRecipe, "make it vegetarian")
        advanceUntilIdle()

        assertEquals(AskChefVariantViewModel.State.Error, viewModel.state.value)
    }

    @Test
    fun adjustRecipe_invalidJson_reportsError() = runTest(testDispatcher) {
        ai.replies[AiPurpose.DERIVE_RECIPE_JSON] = "not json"

        viewModel.adjustRecipe(sampleRecipe, "make it vegetarian")
        advanceUntilIdle()

        assertEquals(AskChefVariantViewModel.State.Error, viewModel.state.value)
    }

    @Test
    fun adjustRecipe_noRecipesInJson_reportsError() = runTest(testDispatcher) {
        ai.replies[AiPurpose.DERIVE_RECIPE_JSON] = """{"recipes":[]}"""

        viewModel.adjustRecipe(sampleRecipe, "make it vegetarian")
        advanceUntilIdle()

        assertEquals(AskChefVariantViewModel.State.Error, viewModel.state.value)
    }

    @Test
    fun reset_returnsToIdle() = runTest(testDispatcher) {
        ai.failure = BackendException.Upstream(502)
        viewModel.adjustRecipe(sampleRecipe, "make it vegetarian")
        advanceUntilIdle()

        viewModel.reset()

        assertEquals(AskChefVariantViewModel.State.Idle, viewModel.state.value)
    }

    // --- buildAdjustPrompt ---

    private val sampleRecipe = Recipe(
        title = "Pasta Carbonara",
        summary = "Classic Italian pasta dish.",
        ingredients = listOf(
            Ingredient(name = "spaghetti", quantity = "400", unit = "g"),
            Ingredient(name = "guanciale", quantity = "150", unit = "g"),
            Ingredient(name = "eggs", quantity = "4", unit = "")
        ),
        instructions = listOf(
            "Boil pasta in salted water.",
            "Fry guanciale until crispy.",
            "Mix eggs and cheese, combine with pasta off heat."
        ),
        tipsAndTricks = "Use pasta water to loosen the sauce."
    )

    @Test
    fun buildAdjustPrompt_includesRecipeTitle() {
        val result = AskChefVariantViewModel.buildAdjustPrompt(sampleRecipe, "make it vegetarian")
        assertTrue(result.contains("Pasta Carbonara"))
    }

    @Test
    fun buildAdjustPrompt_includesUserRequest() {
        val result = AskChefVariantViewModel.buildAdjustPrompt(sampleRecipe, "make it vegetarian")
        assertTrue(result.contains("make it vegetarian"))
    }

    @Test
    fun buildAdjustPrompt_includesIngredients() {
        val result = AskChefVariantViewModel.buildAdjustPrompt(sampleRecipe, "reduce spice")
        assertTrue(result.contains("spaghetti"))
        assertTrue(result.contains("guanciale"))
    }

    @Test
    fun buildAdjustPrompt_includesInstructions() {
        val result = AskChefVariantViewModel.buildAdjustPrompt(sampleRecipe, "reduce spice")
        assertTrue(result.contains("Boil pasta"))
    }

    @Test
    fun buildAdjustPrompt_includesModificationInstruction() {
        val result = AskChefVariantViewModel.buildAdjustPrompt(sampleRecipe, "make it vegan")
        assertTrue(result.contains("modify") || result.contains("modification") || result.contains("updated"))
    }

    @Test
    fun buildAdjustPrompt_withEmptyRequest_stillContainsRecipe() {
        val result = AskChefVariantViewModel.buildAdjustPrompt(sampleRecipe, "")
        assertTrue(result.contains("Pasta Carbonara"))
        assertFalse(result.isBlank())
    }

    @Test
    fun buildAdjustPrompt_userRequestLabelPresent() {
        val result = AskChefVariantViewModel.buildAdjustPrompt(sampleRecipe, "reduce salt")
        assertTrue(result.contains("User request:"))
        assertTrue(result.contains("reduce salt"))
    }
}
