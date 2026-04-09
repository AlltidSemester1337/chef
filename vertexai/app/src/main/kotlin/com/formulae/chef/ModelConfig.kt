package com.formulae.chef

/**
 * Centralized Vertex AI Gemini model configuration.
 *
 * Migrated from Gemini 2.5 to Gemini 3.0 per Google Cloud deprecation notice.
 * Deprecation timeline: no earlier than October 16, 2026.
 *
 * IMPORTANT — verify model IDs in the Vertex AI console before deploying:
 * https://cloud.google.com/vertex-ai/generative-ai/docs/learn/model-versioning
 *
 * IMPORTANT — Gemini 3 requires thought signature circulation to maintain reasoning
 * capabilities across multi-turn sessions. Capture the thought signatures from each
 * response and include them verbatim in the follow-up request.
 * See: https://cloud.google.com/vertex-ai/generative-ai/docs/thinking/thought-signatures
 */
object ModelConfig {
    /** Primary conversational model — recipe chat, recipe adjustment */
    const val CHAT_MODEL = "gemini-3.0-flash"

    /** Lightweight model — structured JSON extraction, preference detection, history compaction */
    const val LITE_MODEL = "gemini-3.0-flash-lite"

    /** Multimodal model — image generation from recipe descriptions */
    const val IMAGE_MODEL = "gemini-3.0-flash-image"
}
