package com.orbit.app.understanding

import com.orbit.app.understanding.domain.EvidenceLevel
import com.orbit.app.understanding.domain.GroundingConstraints
import com.orbit.app.understanding.domain.IntentCategory
import com.orbit.app.understanding.domain.UnderstandingMode
import com.orbit.app.understanding.domain.UnderstandingResult
import com.orbit.app.understanding.domain.UnderstandingStatus
import com.orbit.app.understanding.engine.ActiveIntentResolver
import com.orbit.app.understanding.engine.BasicCaptureInput
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ActiveIntentResolverTest {
    @Test
    fun resolve_createsActiveIntentForRecoverableBasicEvidence() {
        val resolver = ActiveIntentResolver(clockMillis = { 123L })
        val result = resolver.resolve(
            input = BasicCaptureInput(
                captureId = "capture-1",
                textContent = "Use code SAVE20 expires tomorrow",
                foregroundAppLabel = "Shop"
            ),
            understandingResult = understandingResult("capture-1")
        )

        assertNotNull(result)
        assertEquals(IntentCategory.COUPON_OR_PROMO.name, result!!.intentType)
        assertEquals("ACTIVE", result.status)
        assertTrue(result.primaryEvidenceJson.contains("SAVE20"))
        assertEquals(123L, result.createdAt)
    }

    @Test
    fun resolve_skipsUnknownEvidence() {
        val resolver = ActiveIntentResolver(clockMillis = { 123L })
        val result = resolver.resolve(
            input = BasicCaptureInput(captureId = "capture-2", textContent = "random note"),
            understandingResult = understandingResult("capture-2")
        )

        assertNull(result)
    }

    private fun understandingResult(captureId: String): UnderstandingResult = UnderstandingResult(
        captureId = captureId,
        status = UnderstandingStatus.READY,
        mode = UnderstandingMode.BASIC,
        title = null,
        summaryText = null,
        groundingConstraints = GroundingConstraints(
            constraints = listOf("basic-local-only"),
            evidenceLevel = EvidenceLevel.METADATA_ONLY
        ),
        sourceIdentityJson = null,
        contentHashHex = null,
        canonicalUrl = null,
        evidenceBundleIds = emptyList(),
        duplicateMatch = null
    )
}
