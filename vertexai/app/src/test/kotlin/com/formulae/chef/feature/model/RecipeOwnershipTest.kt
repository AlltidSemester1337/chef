package com.formulae.chef.feature.model

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RecipeOwnershipTest {

    @Test
    fun `recipe is owned by user with matching uid`() {
        assertTrue(Recipe(uid = "user-1").isOwnedBy("user-1"))
    }

    @Test
    fun `recipe is not owned by a different user`() {
        assertFalse(Recipe(uid = "user-1").isOwnedBy("user-2"))
    }

    @Test
    fun `recipe is not owned when current uid is null`() {
        assertFalse(Recipe(uid = "user-1").isOwnedBy(null))
    }

    @Test
    fun `recipe with unset uid is never owned`() {
        assertFalse(Recipe(uid = "").isOwnedBy(""))
        assertFalse(Recipe(uid = "").isOwnedBy(null))
    }
}
