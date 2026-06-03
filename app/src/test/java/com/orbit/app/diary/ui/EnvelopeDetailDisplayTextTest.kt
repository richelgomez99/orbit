package com.orbit.app.diary.ui

import com.orbit.app.data.ipc.EnvelopeViewParcel
import org.junit.Assert.assertEquals
import org.junit.Test

class EnvelopeDetailDisplayTextTest {
    @Test
    fun imageWithoutSourceLabel_usesPhoneScreenshotFallback() {
        val env = envelope(contentType = "IMAGE", appCategory = "UNKNOWN_SOURCE", sourceAppLabel = null)

        assertEquals("Screenshot saved from phone", EnvelopeDetailDisplayText.title(env))
    }

    @Test
    fun imageWithSourceLabel_usesSourceSpecificScreenshotTitle() {
        val env = envelope(contentType = "IMAGE", appCategory = "UNKNOWN_SOURCE", sourceAppLabel = "Chrome")

        assertEquals("Screenshot from Chrome", EnvelopeDetailDisplayText.title(env))
    }

    @Test
    fun imageWithIntentResolverSource_usesPhoneScreenshotFallback() {
        val env = envelope(contentType = "IMAGE", appCategory = "UNKNOWN_SOURCE", sourceAppLabel = "IntentResolver")

        assertEquals("Screenshot saved from phone", EnvelopeDetailDisplayText.title(env))
    }

    @Test
    fun nonImageWithIntentResolverSource_usesHumanFallback() {
        val env = envelope(contentType = "TEXT", appCategory = "UNKNOWN_SOURCE", sourceAppLabel = "IntentResolver")

        assertEquals("Capture from an app", EnvelopeDetailDisplayText.title(env))
    }

    @Test
    fun unknownNonImageSource_usesHumanFallbackInsteadOfUnknown() {
        val env = envelope(contentType = "TEXT", appCategory = "UNKNOWN_SOURCE", sourceAppLabel = null)

        assertEquals("Capture from an app", EnvelopeDetailDisplayText.title(env))
    }

    private fun envelope(
        contentType: String,
        appCategory: String,
        sourceAppLabel: String?,
    ): EnvelopeViewParcel = EnvelopeViewParcel(
        id = "env-test",
        contentType = contentType,
        textContent = null,
        imageUri = null,
        intent = "REFERENCE",
        intentSource = "USER",
        createdAtMillis = 0L,
        dayLocal = "2026-06-03",
        isArchived = false,
        title = null,
        domain = null,
        excerpt = null,
        summary = null,
        appCategory = appCategory,
        activityState = "UNKNOWN",
        hourLocal = 12,
        dayOfWeekLocal = 3,
        sourceAppLabel = sourceAppLabel
    )
}
