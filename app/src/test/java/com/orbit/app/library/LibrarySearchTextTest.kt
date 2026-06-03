package com.orbit.app.library

import com.orbit.app.memory.MemorySearchResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LibrarySearchTextTest {

    @Test
    fun queryVariantsStemRescheduleToMatchRescheduling() {
        val variants = LibrarySearchText.queryVariants("reschedule")

        assertTrue(variants.contains("reschedule"))
        assertTrue(variants.contains("reschedul"))
    }

    @Test
    fun queryVariantsStemReschedulingToSameRoot() {
        val variants = LibrarySearchText.queryVariants("rescheduling")

        assertTrue(variants.contains("rescheduling"))
        assertTrue(variants.contains("reschedul"))
    }

    @Test
    fun queryVariantsPreserveQrAsMeaningfulShortToken() {
        val variants = LibrarySearchText.queryVariants("qr code")

        assertTrue(variants.contains("qr"))
        assertTrue(variants.contains("code"))
    }

    @Test
    fun resultMatchesMultiTokenQueryRequiresAllMeaningfulTokens() {
        val qrResult = result(
            "env-qr",
            title = "QR code for event check-in",
            summary = "Context: qr code",
        )
        val codeOnlyResult = result(
            "env-code",
            title = "codex --sandbox workspace-write",
            summary = "codex command saved from terminal",
        )

        assertTrue(LibrarySearchText.resultMatchesQuery(qrResult, "qr code"))
        assertEquals(false, LibrarySearchText.resultMatchesQuery(codeOnlyResult, "qr code"))
    }

    @Test
    fun dedupeResultsCollapseRepeatedSeedCaptures() {
        val results = listOf(
            result("env-1"),
            result("env-2"),
            result("env-3", title = "Different capture"),
        )

        assertEquals(
            listOf("env-1", "env-3"),
            LibrarySearchText.dedupeResults(results).map { it.envelopeId },
        )
    }

    private fun result(
        envelopeId: String,
        title: String = "Dentist appointment reminder conflicts with Monday travel.",
        summary: String = "Dentist appointment reminder conflicts with Monday travel.",
    ) = MemorySearchResult(
        envelopeId = envelopeId,
        rank = 1,
        score = 0.9f,
        title = title,
        summary = summary,
        dayLocal = "2026-05-30",
        createdAtMillis = 1_780_000_000_000L,
        intent = "FOR_SOMEONE",
        sourceAppLabel = "Calendar",
    )
}
