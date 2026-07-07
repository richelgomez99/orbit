package com.orbit.app.ai.model

/** Tracks which LLM produced a result — Principle IX (LLM Sovereignty). */
sealed class LlmProvenance {
    /** On-device Gemini Nano via AICore. */
    data object LocalNano : LlmProvenance()

    /**
     * On-device BYOM model (spec 022) — e.g. Gemma via MediaPipe LLM
     * Inference. Local, no network. [model] is the catalog id / label.
     */
    data class LocalByom(val model: String) : LlmProvenance()

    /** Orbit-managed cloud model (spec 005, v1.1). */
    data class OrbitManaged(val model: String) : LlmProvenance()

    /** Bring-your-own-key cloud model (spec 005, v1.1). */
    data class Byok(val provider: String, val model: String) : LlmProvenance()
}
