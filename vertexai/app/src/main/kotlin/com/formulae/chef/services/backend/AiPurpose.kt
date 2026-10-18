package com.formulae.chef.services.backend

/**
 * What an AI completion is for. The backend picks system prompt, model and sampling parameters
 * from the purpose, and decides whether the call counts against the beta quota.
 */
enum class AiPurpose(val wireName: String) {
    CHAT("chat"),
    DERIVE_RECIPE_JSON("derive_recipe_json"),
    EXTRACT_PREFERENCES("extract_preferences"),
    COMPACT_HISTORY("compact_history"),
    RECIPE_ADJUST("recipe_adjust"),
    OVERLAY_DEFAULT("overlay_default"),
    OVERLAY_RECIPE_CONTEXT("overlay_recipe_context")
}
