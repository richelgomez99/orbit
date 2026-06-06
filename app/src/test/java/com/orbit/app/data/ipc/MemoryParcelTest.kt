package com.orbit.app.data.ipc

import org.junit.Assert.assertEquals
import org.junit.Test

class MemoryParcelTest {

    @Test
    fun candidateParcel_keepsCompactReviewFields() {
        val candidate = MemoryCandidateParcel(
            candidateId = "candidate-1",
            candidateKind = "INTEREST",
            state = "PENDING",
            displayLabel = "Interested in startup events",
            factText = "user interested in startup events",
            confidenceLabel = "medium",
            sensitivity = "NORMAL",
            sourceCount = 2,
            primarySourceEnvelopeId = "env-1",
            primarySourceTitle = "Startup event ticket",
            primarySourceDayLocal = "2026-06-05",
            askUserCopy = "Remember this?",
            createdAtMillis = 10L,
            updatedAtMillis = 20L
        )

        assertEquals("candidate-1", candidate.candidateId)
        assertEquals("INTEREST", candidate.candidateKind)
        assertEquals("env-1", candidate.primarySourceEnvelopeId)
        assertEquals(2, candidate.sourceCount)
    }

    @Test
    fun decisionResult_carriesUserFacingStatus() {
        val result = MemoryDecisionResultParcel(
            ok = true,
            candidateId = "candidate-1",
            memoryId = "memory-1",
            status = "promoted",
            message = "Saved to Orbit memory."
        )

        assertEquals(true, result.ok)
        assertEquals("memory-1", result.memoryId)
        assertEquals("promoted", result.status)
    }
}
