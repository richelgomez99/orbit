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

    @Test
    fun categoryNamePreferredOverProductIntent() {
        // Spec 020 (S3) — a classified recipe must carry RECIPE, not the
        // always-AMBIGUOUS product intent, so the Library can badge it.
        val result = LocalEnvelopeMemoryResultMapper.toMemorySearchResult(
            envelope = envelope(
                textContent = "Preheat oven, 2 tablespoons butter",
                intent = "AMBIGUOUS",
                categoryName = "RECIPE",
            ),
            query = "butter",
            note = null,
        )

        assertEquals("RECIPE", result.intent)
    }

    @Test
    fun productIntentUsedWhenNoCategory() {
        val result = LocalEnvelopeMemoryResultMapper.toMemorySearchResult(
            envelope = envelope(
                textContent = "Dentist appointment was rescheduled.",
                intent = "REFERENCE",
                categoryName = null,
            ),
            query = "rescheduled",
            note = null,
        )

        assertEquals("REFERENCE", result.intent)
    }

    private fun envelope(
        title: String? = null,
        summary: String? = null,
        textContent: String? = null,
        intent: String = "REFERENCE",
        categoryName: String? = null,
    ) = EnvelopeViewParcel(
        id = "env-1",
        contentType = "TEXT",
        textContent = textContent,
        imageUri = null,
        intent = intent,
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
        categoryName = categoryName,
    )
}
