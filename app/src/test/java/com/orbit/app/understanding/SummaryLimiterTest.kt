package com.orbit.app.understanding

import com.orbit.app.understanding.domain.DuplicateMatch
import com.orbit.app.understanding.domain.EvidenceLevel
import com.orbit.app.understanding.domain.GroundingConstraints
import com.orbit.app.understanding.domain.UnderstandingMode
import com.orbit.app.understanding.domain.UnderstandingResult
import com.orbit.app.understanding.domain.UnderstandingStatus
import com.orbit.app.understanding.engine.SummaryLimiter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SummaryLimiterTest {

    @Test
    fun enforce_metadataOnlyStripsFullContentClaims() {
        val result = SummaryLimiter.enforce(
            baseResult(
                summaryText = "The article says Orbit is useful. Saved metadata from example.com.",
                evidenceLevel = EvidenceLevel.METADATA_ONLY
            )
        )

        assertFalse(result.summaryText.orEmpty().contains("The article says"))
        assertTrue(result.summaryText.orEmpty().contains("Saved metadata"))
        assertTrue(result.groundingConstraints.constraints.contains("Orbit read only metadata, not the full page"))
    }

    @Test
    fun enforce_fullEvidenceLeavesSummaryAlone() {
        val result = SummaryLimiter.enforce(
            baseResult(
                summaryText = "The article says Orbit is useful.",
                evidenceLevel = EvidenceLevel.FULL
            )
        )
        assertEquals("The article says Orbit is useful.", result.summaryText)
    }

    private fun baseResult(
        summaryText: String,
        evidenceLevel: EvidenceLevel
    ): UnderstandingResult = UnderstandingResult(
        captureId = "capture-1",
        status = UnderstandingStatus.READY,
        mode = UnderstandingMode.BASIC,
        title = "Title",
        summaryText = summaryText,
        groundingConstraints = GroundingConstraints(emptyList(), evidenceLevel),
        sourceIdentityJson = null,
        contentHashHex = null,
        canonicalUrl = null,
        evidenceBundleIds = emptyList(),
        duplicateMatch = null as DuplicateMatch?
    )
}
