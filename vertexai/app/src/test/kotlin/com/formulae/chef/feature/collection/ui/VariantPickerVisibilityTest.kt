package com.formulae.chef.feature.collection.ui

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VariantPickerVisibilityTest {

    @Test
    fun `non-owner sees picker read-only when recipe has variants`() {
        assertTrue(shouldShowVariantPicker(isOwner = false, hasVariants = true, canCreateVariant = false))
        assertTrue(shouldShowVariantPicker(isOwner = false, hasVariants = true, canCreateVariant = true))
    }

    @Test
    fun `non-owner sees nothing when recipe has no variants`() {
        assertFalse(shouldShowVariantPicker(isOwner = false, hasVariants = false, canCreateVariant = true))
        assertFalse(shouldShowVariantPicker(isOwner = false, hasVariants = false, canCreateVariant = false))
    }

    @Test
    fun `owner without variants sees picker only where a create handler exists`() {
        assertTrue(shouldShowVariantPicker(isOwner = true, hasVariants = false, canCreateVariant = true))
        assertFalse(shouldShowVariantPicker(isOwner = true, hasVariants = false, canCreateVariant = false))
    }

    @Test
    fun `owner with variants always sees picker`() {
        assertTrue(shouldShowVariantPicker(isOwner = true, hasVariants = true, canCreateVariant = false))
        assertTrue(shouldShowVariantPicker(isOwner = true, hasVariants = true, canCreateVariant = true))
    }
}
