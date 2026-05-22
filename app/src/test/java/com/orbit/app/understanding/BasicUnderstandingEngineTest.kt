package com.orbit.app.understanding

import com.orbit.app.understanding.domain.CompactEvidencePayload
import com.orbit.app.understanding.domain.CompletionKeyKind
import com.orbit.app.understanding.domain.CompletionKeyStatus
import com.orbit.app.understanding.domain.IntentCategory
import com.orbit.app.understanding.domain.UnderstandingMode
import com.orbit.app.understanding.domain.UnderstandingStatus
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BasicUnderstandingEngineTest {

    @Test
    fun classifierUsesLocalTextAndAppSignals() {
        val classifier = IntentCategoryClassifier()

        val shopping = classifier.classify(
            ClassificationInput(
                text = "Add to cart before sale ends. Price $42.00",
                appCategory = "BROWSER",
                sourceAppLabel = "Chrome",
                canonicalUrl = null,
                capturedAtMillis = NOW,
                nowMillis = NOW
            )
        )
        val watchLater = classifier.classify(
            ClassificationInput(
                text = null,
                appCategory = "VIDEO",
                sourceAppLabel = "YouTube",
                canonicalUrl = "https://youtu.be/example",
                capturedAtMillis = NOW,
                nowMillis = NOW
            )
        )

        assertEquals(IntentCategory.COUPON_OR_PROMO, shopping.category)
        assertEquals(IntentCategory.READ_OR_WATCH_LATER, watchLater.category)
    }

    @Test
    fun classifierKeepsMessagingAppointmentAsChatUnlessTicketEvidenceIsStronger() {
        val classifier = IntentCategoryClassifier()

        val message = classifier.classify(
            ClassificationInput(
                text = "Okay. I can't go with him to his appointment. Chelsea has a scheduled appointment at 2pm",
                appCategory = "MESSAGING",
                sourceAppLabel = "Messages",
                canonicalUrl = null,
                capturedAtMillis = NOW,
                nowMillis = NOW
            )
        )
        val ticket = classifier.classify(
            ClassificationInput(
                text = "Ticket confirmed. Boarding pass check-in opens at 2pm",
                appCategory = "MESSAGING",
                sourceAppLabel = "Messages",
                canonicalUrl = null,
                capturedAtMillis = NOW,
                nowMillis = NOW
            )
        )

        assertEquals(IntentCategory.CHAT_ACTION, message.category)
        assertEquals(IntentCategory.EVENT_TICKET_RESERVATION, ticket.category)
    }

    @Test
    fun completionKeyExtractorFindsFirstPassKeys() {
        val extractor = CompletionKeyExtractor()
        val keys = extractor.extract(
            text = """
                Order #ABCD-1234 confirmed.
                Promo code SAVE20 expires May 20, 2026.
                Ship to 123 Market Street.
                Ingredients: 2 cups flour.
                https://example.com/item
            """.trimIndent(),
            canonicalUrl = null
        )

        assertTrue(keys.any { it.kind == CompletionKeyKind.ORDER_ID && it.label == "ABCD-1234" })
        assertTrue(keys.any { it.kind == CompletionKeyKind.COUPON_CODE && it.label == "SAVE20" })
        assertTrue(keys.any { it.kind == CompletionKeyKind.DATE })
        assertTrue(keys.any { it.kind == CompletionKeyKind.ADDRESS })
        assertTrue(keys.any { it.kind == CompletionKeyKind.INGREDIENT })
        assertTrue(keys.any { it.kind == CompletionKeyKind.URL })
    }

    @Test
    fun completionKeyExtractorTreatsSimpleTimesAsDateKeys() {
        val keys = CompletionKeyExtractor().extract(
            text = "Chelsea has a scheduled appointment at 2pm",
            canonicalUrl = null
        )

        assertTrue(keys.any { it.kind == CompletionKeyKind.DATE && it.label.equals("2pm", ignoreCase = true) })
    }

    @Test
    fun contentHasherNormalizesTextButDoesNotCanonicalizeUrls() {
        val first = ContentHasher.hashNormalizedText("  Buy   Milk\n")
        val second = ContentHasher.hashNormalizedText("buy milk")
        val urlA = ContentHasher.hashNormalizedText("https://example.com/a?b=1&utm_source=x")
        val urlB = ContentHasher.hashNormalizedText("https://example.com/a?b=1")

        assertEquals(first, second)
        assertNotEquals(urlA, urlB)
        assertEquals(64, ContentHasher.hashArtifact(byteArrayOf(1, 2, 3)).length)
    }

    @Test
    fun basicEngineBuildsBoundedLocalUnderstandingWithoutFetchingUrl() {
        val engine = BasicUnderstandingEngine()

        val result = engine.understand(
            BasicUnderstandingInput(
                captureId = "capture-1",
                textContent = "Running shoes price $42.00 with promo code SAVE20",
                sourceAppLabel = "Chrome",
                appCategory = "BROWSER",
                canonicalUrl = "http://localhost:9/unreachable",
                capturedAtMillis = NOW,
                nowMillis = NOW
            )
        )

        assertEquals(UnderstandingMode.BASIC, result.mode)
        assertEquals(UnderstandingStatus.READY, result.status)
        assertEquals(IntentCategory.COUPON_OR_PROMO, result.category)
        assertEquals(CompletionKeyStatus.FOUND, result.completionKeyStatus)
        assertNotNull(result.completionKey)
        assertEquals("http://localhost:9/unreachable", result.canonicalUrl)
        assertTrue(result.groundingConstraints.forbiddenSignals.contains("public_url_fetch"))
        assertTrue(result.groundingConstraints.forbiddenSignals.contains("cloud_llm"))
        assertTrue(result.evidencePayloadJson.isNotEmpty())
        result.evidencePayloadJson.forEach { payload ->
            assertTrue(payload.length <= CompactEvidencePayload.MAX_JSON_CHARS)
            assertOnlyAllowedEvidenceKeys(payload)
        }
    }

    @Test
    fun basicEngineReportsMissingContextForUnknownCapture() {
        val result = BasicUnderstandingEngine().understand(
            BasicUnderstandingInput(
                captureId = "empty",
                textContent = null,
                sourceAppLabel = null,
                appCategory = "UNKNOWN_SOURCE",
                canonicalUrl = null,
                capturedAtMillis = NOW,
                nowMillis = NOW
            )
        )

        assertEquals(UnderstandingStatus.LIMITED, result.status)
        assertEquals(IntentCategory.UNKNOWN, result.category)
        assertEquals(CompletionKeyStatus.NEEDS_ESCALATION, result.completionKeyStatus)
        assertEquals(null, result.contentHashHex)
        assertTrue(result.groundingConstraints.missingSignals.contains("local_text_or_ocr"))
    }

    private fun assertOnlyAllowedEvidenceKeys(payload: String) {
        val json = JSONObject(payload)
        val keys = json.keys().asSequence().toList()
        assertTrue(keys.isNotEmpty())
        keys.forEach { key ->
            assertTrue("unexpected key $key", CompactEvidencePayload.isAllowedKey(key))
        }
    }

    private companion object {
        const val NOW = 1_779_216_000_000L
    }
}