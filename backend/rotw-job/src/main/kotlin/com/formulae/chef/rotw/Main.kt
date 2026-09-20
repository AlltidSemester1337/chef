package com.formulae.chef.rotw

import com.formulae.chef.rotw.job.RecipeOfTheMonthJob
import kotlinx.coroutines.runBlocking

/**
 * Set ROTW_RECOVER_OPERATION (plus ROTW_RECOVER_RECIPE_ID, ROTW_RECOVER_RECIPE_TITLE,
 * ROTW_RECOVER_MONTH_OF) to complete a run whose Veo operation already succeeded but
 * whose upload/RTDB write step crashed, instead of running the normal monthly job.
 */
fun main() = runBlocking {
    val operationName = System.getenv("ROTW_RECOVER_OPERATION")
    if (operationName != null) {
        RecipeOfTheMonthJob().recoverOperation(
            operationName = operationName,
            recipeId = System.getenv("ROTW_RECOVER_RECIPE_ID") ?: error("ROTW_RECOVER_RECIPE_ID env var not set"),
            recipeTitle = System.getenv("ROTW_RECOVER_RECIPE_TITLE")
                ?: error("ROTW_RECOVER_RECIPE_TITLE env var not set"),
            monthOf = System.getenv("ROTW_RECOVER_MONTH_OF") ?: error("ROTW_RECOVER_MONTH_OF env var not set")
        )
    } else {
        RecipeOfTheMonthJob().execute()
    }
}
