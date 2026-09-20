package com.formulae.chef.rotw

import com.formulae.chef.rotw.job.isFirstSundayOfMonth
import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class FirstSundayOfMonthTest {

    @Test
    fun `first Sunday of the month returns true`() {
        // 2026-04-05 is a Sunday, within the first 7 days of April
        assertTrue(isFirstSundayOfMonth(LocalDate.of(2026, 4, 5)))
    }

    @Test
    fun `second Sunday of the month returns false`() {
        // 2026-04-12 is a Sunday, but not within the first 7 days
        assertFalse(isFirstSundayOfMonth(LocalDate.of(2026, 4, 12)))
    }

    @Test
    fun `non-Sunday within the first 7 days returns false`() {
        // 2026-04-01 is a Wednesday
        assertFalse(isFirstSundayOfMonth(LocalDate.of(2026, 4, 1)))
    }

    @Test
    fun `7th of the month on a Sunday returns true`() {
        // 2026-06-07 is a Sunday, the latest possible date for a first Sunday
        assertTrue(isFirstSundayOfMonth(LocalDate.of(2026, 6, 7)))
    }

    @Test
    fun `8th of the month on a Sunday returns false`() {
        // 2025-06-08 is a Sunday, but outside the first-7-days window
        assertFalse(isFirstSundayOfMonth(LocalDate.of(2025, 6, 8)))
    }
}
