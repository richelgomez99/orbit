package com.capsule.app.understanding

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EvidenceClaimRulesTest {
    @Test
    fun readableTextCanSupportFullContentClaim() {
        assertTrue(
            EvidenceClaimPolicy.canSupport(
                EvidenceKind.READABLE_PUBLIC_TEXT,
                EvidenceStatus.READY,
                EvidenceClaimPolicy.ClaimKind.FULL_CONTENT_SUMMARY
            )
        )
    }

    @Test
    fun metadataOnlyEvidenceCannotSupportFullContentClaim() {
        assertFalse(
            EvidenceClaimPolicy.canSupport(
                EvidenceKind.URL_METADATA,
                EvidenceStatus.READY,
                EvidenceClaimPolicy.ClaimKind.FULL_CONTENT_SUMMARY
            )
        )
    }

    @Test
    fun failedOrSuppressedEvidenceOnlySupportsLimitations() {
        assertTrue(
            EvidenceClaimPolicy.canSupport(
                EvidenceKind.FETCH_LIMITATION,
                EvidenceStatus.FAILED,
                EvidenceClaimPolicy.ClaimKind.LIMITATION_ONLY
            )
        )
        assertFalse(
            EvidenceClaimPolicy.canSupport(
                EvidenceKind.FETCH_LIMITATION,
                EvidenceStatus.FAILED,
                EvidenceClaimPolicy.ClaimKind.METADATA_SUMMARY
            )
        )
    }
}