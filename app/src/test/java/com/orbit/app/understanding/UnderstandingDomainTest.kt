package com.orbit.app.understanding

import com.orbit.app.understanding.domain.ActiveIntentStatus
import com.orbit.app.understanding.domain.CompactEvidencePayload
import com.orbit.app.understanding.domain.CompletionKey
import com.orbit.app.understanding.domain.CompletionKeyKind
import com.orbit.app.understanding.domain.GroundingConstraints
import com.orbit.app.understanding.domain.IntentCategory
import com.orbit.app.understanding.domain.SourceIdentity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UnderstandingDomainTest {

    @Test
    fun maybeOldIsCategoryOnly() {
        assertTrue(IntentCategory.entries.contains(IntentCategory.MAYBE_OLD_OR_INACTIVE))
        assertFalse(ActiveIntentStatus.entries.any { it.name == "MAYBE_OLD_OR_INACTIVE" })
    }

    @Test
    fun compactEvidencePayloadAllowsOnlyKnownKeys() {
        assertTrue(CompactEvidencePayload.isAllowedKey("kind"))
        assertTrue(CompactEvidencePayload.isAllowedKey("createdAt"))
        assertFalse(CompactEvidencePayload.isAllowedKey("rawHtml"))
        assertFalse(CompactEvidencePayload.isAllowedKey("fullOcrText"))
        assertFalse(CompactEvidencePayload.isAllowedKey("embedding"))
    }

    @Test
    fun cappedExcerptTrimsAndLimitsText() {
        val longExcerpt = "  " + "a".repeat(CompactEvidencePayload.MAX_EXCERPT_CHARS + 10)

        val capped = CompactEvidencePayload.cappedExcerpt(longExcerpt)

        assertEquals(CompactEvidencePayload.MAX_EXCERPT_CHARS, capped.length)
        assertTrue(capped.all { it == 'a' })
    }

    @Test
    fun compactDomainJsonStaysBounded() {
        val completionJson = CompletionKey(
            kind = CompletionKeyKind.COUPON_CODE,
            label = "SAVE20",
            source = "local_regex",
            confidence = 0.9f,
            excerpt = "Use code SAVE20 today"
        ).toCompactJson()
        val sourceJson = SourceIdentity.fromLocalSignals(
            sourceAppLabel = "Chrome",
            appCategory = "BROWSER",
            canonicalUrl = "https://example.com/product"
        ).toCompactJson()
        val groundingJson = GroundingConstraints.basic(
            hasText = true,
            hasSourceIdentity = true,
            hasCanonicalUrl = true
        ).toCompactJson()

        assertTrue(completionJson.length <= CompactEvidencePayload.MAX_JSON_CHARS)
        assertTrue(sourceJson.length <= CompactEvidencePayload.MAX_JSON_CHARS)
        assertTrue(groundingJson.length <= CompactEvidencePayload.MAX_JSON_CHARS)
        assertTrue(groundingJson.contains("public_url_fetch"))
    }
}