package com.orbit.app.understanding.engine

import com.orbit.app.understanding.domain.EvidenceLevel
import com.orbit.app.understanding.domain.GroundingConstraints
import com.orbit.app.understanding.domain.UnderstandingResult

object SummaryLimiter {

    private val fullContentClaimPatterns = listOf(
        Regex("\\bthe article says\\b", RegexOption.IGNORE_CASE),
        Regex("\\baccording to the text\\b", RegexOption.IGNORE_CASE),
        Regex("\\bthe page explains\\b", RegexOption.IGNORE_CASE),
        Regex("\\bthe author argues\\b", RegexOption.IGNORE_CASE)
    )

    fun enforce(result: UnderstandingResult): UnderstandingResult {
        val evidenceLevel = result.groundingConstraints.evidenceLevel
        val addedConstraint = when (evidenceLevel) {
            EvidenceLevel.METADATA_ONLY -> "Orbit read only metadata, not the full page"
            EvidenceLevel.VISUAL_ONLY -> "Orbit read only visual content"
            else -> null
        }
        if (addedConstraint == null) return result

        val filteredSummary = result.summaryText
            ?.split(Regex("(?<=[.!?])\\s+"))
            ?.filterNot { sentence -> fullContentClaimPatterns.any { it.containsMatchIn(sentence) } }
            ?.joinToString(" ")
            ?.ifBlank { null }

        return result.copy(
            summaryText = filteredSummary,
            groundingConstraints = GroundingConstraints(
                constraints = (result.groundingConstraints.constraints + addedConstraint).distinct(),
                evidenceLevel = evidenceLevel
            )
        )
    }
}
