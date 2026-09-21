package com.formulae.chef.services.authentication

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AdminUsersTest {

    @Test
    fun `known admin email is admin`() {
        assertTrue(AdminUsers.isAdmin("humlekottekonsult@gmail.com"))
    }

    @Test
    fun `unknown email is not admin`() {
        assertFalse(AdminUsers.isAdmin("someone-else@example.com"))
    }

    @Test
    fun `null email is not admin`() {
        assertFalse(AdminUsers.isAdmin(null))
    }
}
