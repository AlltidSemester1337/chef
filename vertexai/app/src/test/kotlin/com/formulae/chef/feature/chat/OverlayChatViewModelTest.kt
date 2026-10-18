package com.formulae.chef.feature.chat

import com.formulae.chef.buildRecipeContextText
import com.formulae.chef.feature.chat.ui.Participant
import com.formulae.chef.feature.model.Ingredient
import com.formulae.chef.feature.model.Recipe
import com.formulae.chef.services.MAX_CHAT_INPUT_LENGTH
import com.formulae.chef.services.backend.AiPurpose
import com.formulae.chef.services.backend.BackendException
import com.formulae.chef.services.backend.FakeAiCompletionService
import com.formulae.chef.services.persistence.Content
import java.io.IOException
import kotlinx.coroutines.CompletableDeferred
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
class OverlayChatViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val ai = FakeAiCompletionService()
    private lateinit var viewModel: OverlayChatViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        viewModel = OverlayChatViewModel(ai)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun texts(messages: List<Content>) = messages.map { it.role to it.parts.joinToString("") { p -> p.text } }

    private fun uiMessages() = viewModel.uiState.value.messages

    // --- sendMessage: success ---

    @Test
    fun sendMessage_withoutRecipe_usesDefaultPurposeAndSendsUserMessage() = runTest(testDispatcher) {
        viewModel.sendMessage("How long do I boil eggs?")
        advanceUntilIdle()

        assertEquals(1, ai.calls.size)
        assertEquals(AiPurpose.OVERLAY_DEFAULT, ai.calls[0].first)
        assertEquals(listOf("user" to "How long do I boil eggs?"), texts(ai.calls[0].second))
    }

    @Test
    fun sendMessage_success_showsUserAndModelMessages() = runTest(testDispatcher) {
        ai.reply = "About 7 minutes."
        viewModel.sendMessage("How long do I boil eggs?")
        advanceUntilIdle()

        val messages = uiMessages()
        assertEquals(2, messages.size)
        assertEquals(Participant.USER, messages[0].participant)
        assertFalse(messages[0].isPending)
        assertEquals(Participant.MODEL, messages[1].participant)
        assertEquals("About 7 minutes.", messages[1].text)
    }

    @Test
    fun sendMessage_success_addsExchangeToHistoryOfNextCall() = runTest(testDispatcher) {
        ai.reply = "First answer"
        viewModel.sendMessage("First question")
        advanceUntilIdle()
        viewModel.sendMessage("Second question")
        advanceUntilIdle()

        assertEquals(
            listOf("user" to "First question", "model" to "First answer", "user" to "Second question"),
            texts(ai.calls[1].second)
        )
    }

    @Test
    fun sendMessage_afterRecipeContext_usesRecipePurposeAndSendsContextFirst() = runTest(testDispatcher) {
        viewModel.initWithRecipeContext(sampleRecipe)
        viewModel.sendMessage("Can I use bacon?")
        advanceUntilIdle()

        val (purpose, messages) = ai.calls.single()
        assertEquals(AiPurpose.OVERLAY_RECIPE_CONTEXT, purpose)
        assertEquals(3, messages.size)
        assertEquals("user", messages[0].role)
        assertTrue(messages[0].parts.first().text.contains("Pasta Carbonara"))
        assertEquals("model", messages[1].role)
        assertEquals("user" to "Can I use bacon?", texts(messages)[2])
    }

    @Test
    fun sendMessage_truncatesToMaxInputLength() = runTest(testDispatcher) {
        viewModel.sendMessage("x".repeat(MAX_CHAT_INPUT_LENGTH + 50))
        advanceUntilIdle()

        val sent = ai.calls.single().second.last().parts.joinToString("") { it.text }
        assertEquals(MAX_CHAT_INPUT_LENGTH, sent.length)
        assertEquals(MAX_CHAT_INPUT_LENGTH, uiMessages()[0].text.length)
    }

    // --- sendMessage: loading state ---

    @Test
    fun sendMessage_isLoadingWhileCallInFlight() = runTest(testDispatcher) {
        val gate = CompletableDeferred<Unit>()
        ai.gate = gate
        viewModel.sendMessage("Question")
        advanceUntilIdle()
        assertTrue(viewModel.isLoading.value)

        gate.complete(Unit)
        advanceUntilIdle()
        assertFalse(viewModel.isLoading.value)
    }

    // --- sendMessage: quota and errors from the backend ---

    @Test
    fun sendMessage_quotaExceeded_opensQuotaDialogWithoutErrorMessage() = runTest(testDispatcher) {
        ai.failure = BackendException.QuotaExceeded()
        viewModel.sendMessage("Question")
        advanceUntilIdle()

        assertTrue(viewModel.quotaExceeded.value)
        assertFalse(viewModel.emailVerificationRequired.value)
        assertFalse(viewModel.isLoading.value)
        assertTrue(uiMessages().none { it.isPending })
        assertTrue(uiMessages().none { it.participant == Participant.ERROR })
    }

    @Test
    fun sendMessage_emailVerificationRequired_opensVerificationDialogWithoutErrorMessage() =
        runTest(testDispatcher) {
            ai.failure = BackendException.EmailVerificationRequired()
            viewModel.sendMessage("Question")
            advanceUntilIdle()

            assertTrue(viewModel.emailVerificationRequired.value)
            assertFalse(viewModel.quotaExceeded.value)
            assertFalse(viewModel.isLoading.value)
            assertTrue(uiMessages().none { it.isPending })
            assertTrue(uiMessages().none { it.participant == Participant.ERROR })
        }

    @Test
    fun sendMessage_upstreamError_showsErrorMessage() = runTest(testDispatcher) {
        ai.failure = BackendException.Upstream(502)
        viewModel.sendMessage("Question")
        advanceUntilIdle()

        assertEquals(Participant.ERROR, uiMessages().last().participant)
        assertFalse(viewModel.quotaExceeded.value)
        assertFalse(viewModel.emailVerificationRequired.value)
        assertFalse(viewModel.isLoading.value)
        assertTrue(uiMessages().none { it.isPending })
    }

    @Test
    fun sendMessage_networkError_showsErrorMessage() = runTest(testDispatcher) {
        ai.failure = IOException("offline")
        viewModel.sendMessage("Question")
        advanceUntilIdle()

        assertEquals(Participant.ERROR, uiMessages().last().participant)
    }

    @Test
    fun sendMessage_failedExchange_isNotAddedToHistory() = runTest(testDispatcher) {
        ai.failure = BackendException.QuotaExceeded()
        viewModel.sendMessage("Rejected question")
        advanceUntilIdle()

        ai.failure = null
        viewModel.sendMessage("Next question")
        advanceUntilIdle()

        assertEquals(listOf("user" to "Next question"), texts(ai.calls[1].second))
    }

    @Test
    fun dismissingDialogs_resetsFlags() = runTest(testDispatcher) {
        ai.failure = BackendException.QuotaExceeded()
        viewModel.sendMessage("Question")
        advanceUntilIdle()
        viewModel.onQuotaExceededDialogDismissed()
        assertFalse(viewModel.quotaExceeded.value)

        ai.failure = BackendException.EmailVerificationRequired()
        viewModel.sendMessage("Question")
        advanceUntilIdle()
        viewModel.onEmailVerificationRequiredDialogDismissed()
        assertFalse(viewModel.emailVerificationRequired.value)
    }

    // --- reset ---

    @Test
    fun reset_restoresDefaultPurposeAndClearsHistory() = runTest(testDispatcher) {
        viewModel.initWithRecipeContext(sampleRecipe)
        viewModel.sendMessage("About the recipe")
        advanceUntilIdle()

        viewModel.reset()
        viewModel.sendMessage("General question")
        advanceUntilIdle()

        val (purpose, messages) = ai.calls.last()
        assertEquals(AiPurpose.OVERLAY_DEFAULT, purpose)
        assertEquals(listOf("user" to "General question"), texts(messages))
    }

    // --- buildRecipeContextText ---

    private val sampleRecipe = Recipe(
        title = "Pasta Carbonara",
        summary = "Classic Italian pasta dish.",
        ingredients = listOf(
            Ingredient(name = "spaghetti", quantity = "400", unit = "g"),
            Ingredient(name = "guanciale", quantity = "150", unit = "g")
        ),
        instructions = listOf(
            "Boil pasta in salted water.",
            "Fry guanciale until crispy.",
            "Mix eggs and cheese, combine with pasta off heat."
        ),
        tipsAndTricks = "Use pasta water to loosen the sauce."
    )

    @Test
    fun buildRecipeContextText_includesTitle() {
        val result = buildRecipeContextText(sampleRecipe)
        assertTrue(result.contains("Pasta Carbonara"))
    }

    @Test
    fun buildRecipeContextText_includesSummary() {
        val result = buildRecipeContextText(sampleRecipe)
        assertTrue(result.contains("Classic Italian pasta dish."))
    }

    @Test
    fun buildRecipeContextText_includesIngredients() {
        val result = buildRecipeContextText(sampleRecipe)
        assertTrue(result.contains("spaghetti"))
        assertTrue(result.contains("guanciale"))
    }

    @Test
    fun buildRecipeContextText_includesInstructions() {
        val result = buildRecipeContextText(sampleRecipe)
        assertTrue(result.contains("Boil pasta"))
        assertTrue(result.contains("Fry guanciale"))
    }

    @Test
    fun buildRecipeContextText_includesTips() {
        val result = buildRecipeContextText(sampleRecipe)
        assertTrue(result.contains("pasta water"))
    }

    @Test
    fun buildRecipeContextText_withNoIngredients_omitsIngredientSection() {
        val recipe = sampleRecipe.copyOf(ingredients = emptyList())
        val result = buildRecipeContextText(recipe)
        assertFalse(result.contains("Ingredients:"))
    }

    @Test
    fun buildRecipeContextText_withNoInstructions_omitsInstructionSection() {
        val recipe = sampleRecipe.copyOf(instructions = emptyList())
        val result = buildRecipeContextText(recipe)
        assertFalse(result.contains("Instructions:"))
    }

    @Test
    fun buildRecipeContextText_withBlankTips_omitsTipsSection() {
        val recipe = sampleRecipe.copyOf(tipsAndTricks = "")
        val result = buildRecipeContextText(recipe)
        assertFalse(result.contains("Tips:"))
    }

    @Test
    fun buildRecipeContextText_withNullTips_omitsTipsSection() {
        val recipe = sampleRecipe.copyOf(tipsAndTricks = null)
        val result = buildRecipeContextText(recipe)
        assertFalse(result.contains("Tips:"))
    }

    @Test
    fun buildRecipeContextText_instructionsAreNumbered() {
        val result = buildRecipeContextText(sampleRecipe)
        assertTrue(result.contains("1."))
        assertTrue(result.contains("2."))
        assertTrue(result.contains("3."))
    }
}
