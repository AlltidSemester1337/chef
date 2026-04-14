package com.formulae.chef.rotw

import com.formulae.chef.rotw.job.RecipeOfTheMonthJob
import kotlinx.coroutines.runBlocking

fun main() = runBlocking {
    RecipeOfTheMonthJob().execute()
}
