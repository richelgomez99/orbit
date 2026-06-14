package com.orbit.app.resolution

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ResolutionVerdictResolverTest {

    private val resolver = ResolutionVerdictResolver()

    @Test
    fun noReceiptsIsActive() {
        val verdict = resolver.resolve(emptyList(), nowMillis = NOW)

        assertEquals(ResolutionSurfacingVerdict.ACTIVE, verdict.verdict)
        assertNull(verdict.kind)
    }

    @Test
    fun duplicateRecaptureDoesNotHideCanonicalTarget() {
        val verdict = resolver.resolve(
            listOf(receipt("r1", ResolutionKind.DUPLICATE_RECAPTURE)),
            nowMillis = NOW,
        )

        assertEquals(ResolutionSurfacingVerdict.ACTIVE, verdict.verdict)
    }

    @Test
    fun dismissedHidesFromCleanup() {
        val verdict = resolver.resolve(
            listOf(receipt("r1", ResolutionKind.DISMISSED)),
            nowMillis = NOW,
        )

        assertEquals(ResolutionSurfacingVerdict.DISMISSED, verdict.verdict)
        assertEquals("r1", verdict.receiptId)
    }

    @Test
    fun notNowHidesFromCleanupButNotMemoryBrowsing() {
        val receipts = listOf(receipt("r1", ResolutionKind.NOT_NOW))

        assertEquals(
            ResolutionSurfacingVerdict.DISMISSED,
            resolver.resolve(receipts, nowMillis = NOW, surface = ResolutionSurface.CLEANUP_QUEUE).verdict,
        )
        assertEquals(
            ResolutionSurfacingVerdict.ACTIVE,
            resolver.resolve(receipts, nowMillis = NOW, surface = ResolutionSurface.MEMORY_BROWSING).verdict,
        )
    }

    @Test
    fun unexpiredSnoozeHidesUntilTimestamp() {
        val verdict = resolver.resolve(
            listOf(receipt("r1", ResolutionKind.SNOOZED, effectiveUntilMillis = NOW + 1_000)),
            nowMillis = NOW,
        )

        assertEquals(ResolutionSurfacingVerdict.HIDDEN_UNTIL, verdict.verdict)
        assertEquals(NOW + 1_000, verdict.hiddenUntilMillis)
    }

    @Test
    fun expiredSnoozeReturnsActive() {
        val verdict = resolver.resolve(
            listOf(receipt("r1", ResolutionKind.SNOOZED, effectiveUntilMillis = NOW - 1)),
            nowMillis = NOW,
        )

        assertEquals(ResolutionSurfacingVerdict.ACTIVE, verdict.verdict)
    }

    @Test
    fun doneResolvesUntilReopened() {
        val done = receipt("done", ResolutionKind.DONE, at = NOW - 10)

        assertEquals(
            ResolutionSurfacingVerdict.RESOLVED,
            resolver.resolve(listOf(done), nowMillis = NOW).verdict,
        )
        assertEquals(
            ResolutionSurfacingVerdict.ACTIVE,
            resolver.resolve(
                listOf(done, receipt("reopened", ResolutionKind.REOPENED, at = NOW - 1)),
                nowMillis = NOW,
            ).verdict,
        )
    }

    @Test
    fun invalidatedAndSourceDeletedWin() {
        val receipts = listOf(
            receipt("done", ResolutionKind.DONE, at = NOW - 20),
            receipt("reopened", ResolutionKind.REOPENED, at = NOW - 10),
            receipt("invalid", ResolutionKind.INVALIDATED, at = NOW - 1),
        )

        val verdict = resolver.resolve(receipts, nowMillis = NOW)

        assertEquals(ResolutionSurfacingVerdict.INVALIDATED, verdict.verdict)
        assertEquals(ResolutionKind.INVALIDATED, verdict.kind)
    }

    @Test
    fun staleAndConflictReturnStale() {
        assertEquals(
            ResolutionSurfacingVerdict.STALE,
            resolver.resolve(listOf(receipt("stale", ResolutionKind.STALE)), nowMillis = NOW).verdict,
        )
        assertEquals(
            ResolutionSurfacingVerdict.STALE,
            resolver.resolve(listOf(receipt("conflict", ResolutionKind.CONFLICT)), nowMillis = NOW).verdict,
        )
    }

    private fun receipt(
        id: String,
        kind: ResolutionKind,
        at: Long = NOW,
        effectiveUntilMillis: Long? = null,
    ) = ResolutionReceipt(
        id = id,
        targetType = ResolutionTargetType.ACTIVE_INTENT,
        targetId = "target-1",
        kind = kind,
        actor = if (kind == ResolutionKind.DUPLICATE_RECAPTURE) {
            ResolutionActor.DUPLICATE_DETECTOR
        } else {
            ResolutionActor.USER
        },
        occurredAtMillis = at,
        effectiveUntilMillis = effectiveUntilMillis,
    )

    private companion object {
        const val NOW = 1_781_000_000_000L
    }
}
