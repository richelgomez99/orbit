package com.orbit.app.understanding.domain

/**
 * Spec 004 — declared limitations on what Orbit was able to verify.
 * Surfaced in [com.orbit.app.ui.understanding.CaptureDetailScreen] as
 * a grounding constraint banner when non-empty or when status=LIMITED.
 *
 * @ForFutureFeature: specs 005 (retrieval + Ask citations), 006
 * (approval action runtime), 007 (memory candidates inspector),
 * 008 (cloud controls / storage budgeting), 009 (KG backend POC),
 * and 010 (agent coordinator) MUST honour these constraints and MUST
 * NOT assert full-content claims unless evidenceLevel == FULL.
 *
 * @param constraints Human-readable constraint strings, e.g.
 *   "cloud-unavailable", "Orbit read only metadata, not the full page".
 * @param evidenceLevel The strongest evidence type available for this result.
 */
data class GroundingConstraints(
    val constraints: List<String>,
    val evidenceLevel: EvidenceLevel
) {
    companion object {
        val NONE = GroundingConstraints(constraints = emptyList(), evidenceLevel = EvidenceLevel.FULL)
    }
}
