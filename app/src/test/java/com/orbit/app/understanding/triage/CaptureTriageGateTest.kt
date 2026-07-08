package com.orbit.app.understanding.triage

import com.orbit.app.data.model.IntentSource
import com.orbit.app.understanding.domain.CompletionKey
import com.orbit.app.understanding.domain.CompletionKeyKind
import com.orbit.app.understanding.domain.IntentCategory
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CaptureTriageGateTest {

    @Test
    fun userChipAloneIsAStrongKeep() {
        val v = CaptureTriageGate.evaluate(
            input(category = IntentCategory.UNKNOWN, confidence = 0.2f, text = "short", intentSource = IntentSource.USER_CHIP)
        )
        assertTrue(v.worthEnhancing)
        assertTrue(TriageSignal.USER_CHIP in v.fired)
    }

    @Test
    fun completionKeyAloneIsAStrongKeep() {
        val v = CaptureTriageGate.evaluate(
            input(category = IntentCategory.UNKNOWN, confidence = 0.2f, text = "short", key = key(CompletionKeyKind.DATE))
        )
        assertTrue(v.worthEnhancing)
    }

    @Test
    fun longThrowawayNoteAloneIsDropped() {
        // Only substance text, nothing else — a long stray thought must not pass.
        val v = CaptureTriageGate.evaluate(
            input(
                category = IntentCategory.UNKNOWN,
                confidence = 0.2f,
                text = "just thinking out loud about nothing in particular for a while ".repeat(2),
            )
        )
        assertFalse(v.worthEnhancing)
        assertTrue(TriageSignal.HAS_TEXT_OF_SUBSTANCE in v.fired)
    }

    @Test
    fun twoWeakSignalsPass() {
        val v = CaptureTriageGate.evaluate(
            input(
                category = IntentCategory.RECIPE,
                confidence = 0.8f,
                text = "Miso-glazed salmon with ginger and rice, roast for twenty minutes at four hundred.",
                canonicalUrl = "https://example.com/salmon",
            )
        )
        assertTrue(v.worthEnhancing)
    }

    @Test
    fun bareUrlDoesNotCountAsSubstance() {
        // A single CATEGORY_KNOWN signal, text is a bare URL → not worth.
        val v = CaptureTriageGate.evaluate(
            input(category = IntentCategory.RECIPE, confidence = 0.8f, text = "https://example.com/x", canonicalUrl = null)
        )
        assertFalse(TriageSignal.HAS_TEXT_OF_SUBSTANCE in v.fired)
        assertFalse(v.worthEnhancing)
    }

    private fun input(
        category: IntentCategory = IntentCategory.RECIPE,
        confidence: Float = 0.9f,
        key: CompletionKey? = null,
        intentSource: IntentSource = IntentSource.AUTO_AMBIGUOUS,
        text: String? = "Some capture text",
        canonicalUrl: String? = null,
        redaction: Boolean = false,
    ) = TriageInput(
        captureId = "cap-1",
        text = text,
        category = category,
        categoryConfidence = confidence,
        completionKey = key,
        canonicalUrl = canonicalUrl,
        contentHashHex = "hash",
        intentSource = intentSource,
        redactionMarkersPresent = redaction,
    )

    private fun key(kind: CompletionKeyKind, label: String = "x") =
        CompletionKey(kind, label, "test", 0.9f, null)
}
