package com.orbit.app.library

import com.orbit.app.data.ipc.EnvelopeViewParcel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LocalEnvelopeMemoryResultMapperTest {

    @Test
    fun noteOnlyMatchUsesContextCitation() {
        val result = LocalEnvelopeMemoryResultMapper.toMemorySearchResult(
            envelope = envelope(
                title = "Hotel confirmation",
                summary = "Saved booking page",
                textContent = "Standard hotel booking details",
            ),
            query = "passport pickup",
            note = "Need passport pickup before checking in.",
        )

        assertEquals("Context: Need passport pickup before checking in.", result.summary)
        assertEquals("NOTE", result.matchedEvidence.single().kind)
        assertEquals("Context", result.matchedEvidence.single().label)
        assertEquals("note", result.matchedEvidence.single().source)
        assertTrue(result.score >= 8f)
    }

    @Test
    fun nonNoteMatchUsesLocalCaptureCitation() {
        val result = LocalEnvelopeMemoryResultMapper.toMemorySearchResult(
            envelope = envelope(textContent = "Dentist appointment was rescheduled."),
            query = "rescheduled",
            note = "Unrelated travel note",
        )

        assertEquals("LOCAL_TEXT", result.matchedEvidence.single().kind)
        assertEquals("Local capture", result.matchedEvidence.single().label)
        assertEquals("envelope", result.matchedEvidence.single().source)
    }

    private fun envelope(
        title: String? = null,
        summary: String? = null,
        textContent: String? = null,
    ) = EnvelopeViewParcel(
        id = "env-1",
        contentType = "TEXT",
        textContent = textContent,
        imageUri = null,
        intent = "REFERENCE",
        intentSource = "USER_CHIP",
        createdAtMillis = 1_780_000_000_000L,
        dayLocal = "2026-06-13",
        isArchived = false,
        title = title,
        domain = null,
        excerpt = null,
        summary = summary,
        appCategory = "OTHER",
        activityState = "STILL",
        hourLocal = 10,
        dayOfWeekLocal = 6,
        canonicalUrl = null,
        deletedAtMillis = null,
        todoMetaJson = null,
        sourceAppLabel = "Orbit",
    )
}
