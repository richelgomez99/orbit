package com.orbit.app.memory

import org.junit.Assert.assertEquals
import org.junit.Test

class MemoryDisplayTextTest {

    @Test
    fun titlePrefersCancellationStatusOverBrandTitle() {
        val text = "Tech Week event, An Engineering Mindset for AI Adoption Beyond Engineering, scheduled for June 2. " +
            "Due to an unforeseen scheduling conflict, we will be rescheduling it for a later date."

        assertEquals(
            "Due to an unforeseen scheduling conflict, we will be rescheduling it for a later date.",
            MemoryDisplayText.title(
                existingTitle = "Tech Week event, An Engineering Mindset",
                text = text,
            ),
        )
    }

    @Test
    fun titleUsesExistingTitleWhenNoStatusSentenceExists() {
        assertEquals(
            "Ramen restaurant near hotel",
            MemoryDisplayText.title(
                existingTitle = "Ramen restaurant near hotel",
                text = "Ramen restaurant near the hotel, open late after startup event.",
            ),
        )
    }

    @Test
    fun excerptFallsBackToStatusSentenceWhenQueryDoesNotMatch() {
        val text = "Brand header. Registration is canceled and refunds are available next week."

        assertEquals(
            "Registration is canceled and refunds are available next week.",
            MemoryDisplayText.excerptForQuery(text, query = "missing"),
        )
    }
}
