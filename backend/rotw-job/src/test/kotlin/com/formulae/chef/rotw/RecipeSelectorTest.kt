package com.formulae.chef.rotw

import com.formulae.chef.rotw.job.selectRecipe
import com.formulae.chef.rotw.model.RecipeData
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class RecipeSelectorTest {

    private val stew = RecipeData(id = "r1", title = "Peanut Stew", isFavourite = true)
    private val pasta = RecipeData(id = "r2", title = "Pasta Carbonara", isFavourite = true)
    private val risotto = RecipeData(id = "r3", title = "Mushroom Risotto", isFavourite = true)

    @Test
    fun `returns null when no favourites available`() {
        val result = selectRecipe(emptyList(), emptySet())

        assertNull(result)
    }

    @Test
    fun `returns null when all favourites already selected`() {
        val favourites = listOf(stew, pasta)
        val alreadySelected = setOf("r1", "r2")

        val result = selectRecipe(favourites, alreadySelected)

        assertNull(result)
    }

    @Test
    fun `returns the only eligible recipe`() {
        val favourites = listOf(stew, pasta)
        val alreadySelected = setOf("r1")

        val result = selectRecipe(favourites, alreadySelected)

        assertNotNull(result)
        assertEquals("r2", result.id)
        assertEquals("Pasta Carbonara", result.title)
    }

    @Test
    fun `selects only from unselected recipes`() {
        val favourites = listOf(stew, pasta, risotto)
        val alreadySelected = setOf("r1", "r3")

        val result = selectRecipe(favourites, alreadySelected)

        assertNotNull(result)
        assertEquals("r2", result.id)
    }

    @Test
    fun `returns a recipe when none have been selected yet`() {
        val favourites = listOf(stew, pasta, risotto)

        val result = selectRecipe(favourites, emptySet())

        assertNotNull(result)
        assertTrue(result.id in setOf("r1", "r2", "r3"))
    }

    @Test
    fun `does not select recipe whose id is in alreadySelected`() {
        val favourites = listOf(stew)
        val alreadySelected = setOf("r1")

        val result = selectRecipe(favourites, alreadySelected)

        assertNull(result)
    }

    @Test
    fun `selection is random across multiple eligible recipes`() {
        val favourites = (1..20).map { RecipeData(id = "r$it", title = "Recipe $it", isFavourite = true) }
        val alreadySelected = emptySet<String>()

        val selectedIds = (1..100).mapNotNull { selectRecipe(favourites, alreadySelected)?.id }.toSet()

        assertTrue(selectedIds.size > 1, "Expected random selection to produce multiple different IDs over 100 runs")
    }
}
