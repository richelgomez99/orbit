package com.orbit.app.data.model

/**
 * The source process / model that produced a proposal. Persisted on
 * [com.orbit.app.data.entity.ActionProposalEntity.provenance] so the
 * audit log can attribute every proposed action to its originating LLM.
 *
 * - LOCAL_NANO: on-device Gemini Nano via AICore (the only v1.1 source).
 * - ORBIT_MANAGED: Orbit-managed cloud proxy (spec 005, future).
 * - BYOK: user-provided cloud key (spec 005, future).
 */
enum class LlmProvenance {
    LOCAL_NANO,
    ORBIT_MANAGED,
    BYOK
}

/**
 * Map the runtime sealed-class provenance from `:ml` ([com.orbit.app.ai.model.LlmProvenance])
 * down to the persistable enum used on entities.
 */
fun com.orbit.app.ai.model.LlmProvenance.toEntityEnum(): LlmProvenance = when (this) {
    is com.orbit.app.ai.model.LlmProvenance.LocalNano -> LlmProvenance.LOCAL_NANO
    // Persistence granularity: both on-device sources map to LOCAL_NANO at the
    // entity layer (no schema migration). The richer BYOM model label survives
    // in-memory via LlmProvenance.LocalByom.model for audit/model-label paths.
    is com.orbit.app.ai.model.LlmProvenance.LocalByom -> LlmProvenance.LOCAL_NANO
    is com.orbit.app.ai.model.LlmProvenance.OrbitManaged -> LlmProvenance.ORBIT_MANAGED
    is com.orbit.app.ai.model.LlmProvenance.Byok -> LlmProvenance.BYOK
}
