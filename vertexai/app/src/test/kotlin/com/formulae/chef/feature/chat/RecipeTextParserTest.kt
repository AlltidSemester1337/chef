package com.formulae.chef.feature.chat

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RecipeTextParserTest {

    // Shape requested by the chat system prompt's "Recipe format" section.
    private val promptFormatRecipe = """
        Here's a warming curry for tonight!

        ## Beef Rendang
        A rich, slow-cooked Indonesian curry.

        **Difficulty:** Medium
        **Prep time:** 20 minutes
        **Cook time:** 2 hours
        **Servings:** 4

        **Ingredients:**
        - 800 g beef chuck, cubed
        - 400 ml coconut milk
        - **2 tbsp** rendang paste

        **Instructions:**
        1. Brown the beef in batches.
        2. Add the paste and fry for 2 minutes.
        **For the finish:**
        3. Pour in the coconut milk and simmer for 2 hours.

        **Approximate nutrition per serving:** 550 kcal, 40 g protein

        **Tips:**
        - Keeps 3 days in the fridge.
        1. Freeze leftovers in portions.
    """.trimIndent()

    @Test
    fun `detects a recipe in the system-prompt format`() {
        val preview = parseRecipeText(promptFormatRecipe)

        assertNotNull(preview)
        preview!!
        assertEquals("Beef Rendang", preview.title)
        assertEquals("Here's a warming curry for tonight!", preview.intro)
        assertEquals("A rich, slow-cooked Indonesian curry.", preview.description)
        assertEquals(
            listOf("Difficulty: Medium", "Prep time: 20 minutes", "Cook time: 2 hours", "Servings: 4"),
            preview.details
        )
        assertEquals(3, preview.ingredientCount)
        // Sub-heading inside the steps doesn't end them; numbered tips after "Tips:" aren't steps.
        assertEquals(3, preview.stepCount)
    }

    @Test
    fun `body starts after the title and keeps the rest of the recipe`() {
        val preview = parseRecipeText(promptFormatRecipe)!!

        assertTrue(preview.body.startsWith("A rich, slow-cooked Indonesian curry."))
        assertTrue(preview.body.contains("**Ingredients:**"))
        assertTrue(preview.body.endsWith("1. Freeze leftovers in portions."))
    }

    @Test
    fun `bold title line and method heading are recognised`() {
        val text = """
            **Quick Tomato Pasta**
            Serves: 2

            Ingredients
            * 200 g spaghetti
            * 1 can tomatoes

            Method
            1) Boil the pasta.
            2) Warm the tomatoes and toss.
        """.trimIndent()

        val preview = parseRecipeText(text)!!

        assertEquals("Quick Tomato Pasta", preview.title)
        assertEquals("", preview.intro)
        assertEquals(listOf("Serves: 2"), preview.details)
        assertEquals(2, preview.ingredientCount)
        assertEquals(2, preview.stepCount)
    }

    @Test
    fun `recipe without a title starts at the details`() {
        val text = """
            Sure, try this:

            **Prep time:** 5 minutes

            **Ingredients:**
            - 2 eggs

            **Instructions:**
            1. Scramble the eggs.
        """.trimIndent()

        val preview = parseRecipeText(text)!!

        assertNull(preview.title)
        assertEquals("Sure, try this:", preview.intro)
        assertTrue(preview.body.startsWith("**Prep time:** 5 minutes"))
    }

    @Test
    fun `conversational message is not a recipe`() {
        assertNull(parseRecipeText("What ingredients do you have at home? I can suggest something."))
        assertNull(parseRecipeText(""))
    }

    @Test
    fun `ingredients without instructions is not a recipe`() {
        val text = """
            **Ingredients:**
            - flour
            - sugar
        """.trimIndent()

        assertNull(parseRecipeText(text))
    }

    @Test
    fun `headings without list items are not a recipe`() {
        val text = """
            Ingredients:
            Whatever you have in the fridge.
            Instructions:
            Improvise!
        """.trimIndent()

        assertNull(parseRecipeText(text))
    }
}
