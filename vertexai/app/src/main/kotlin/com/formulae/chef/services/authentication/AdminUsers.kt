package com.formulae.chef.services.authentication

object AdminUsers {
    private val ADMIN_EMAILS = setOf("humlekottekonsult@gmail.com")

    fun isAdmin(email: String?): Boolean = email != null && ADMIN_EMAILS.contains(email)
}
