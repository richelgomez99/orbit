package com.orbit.app.understanding

import com.orbit.app.understanding.domain.CompletionKeyStatus
import com.orbit.app.understanding.domain.IntentCategory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import java.util.concurrent.TimeUnit

class BasicUnderstandingBiopsyTest {

    @Test
    fun atLeastTwentyCaptureBiopsyCoversInitialCategoriesWithExpectedBasicBehavior() {
        val engine = BasicUnderstandingEngine()
        val results = SAMPLES.map { sample -> sample to engine.understand(sample.input()) }

        results.forEach { (sample, result) ->
            assertEquals(sample.id, sample.expectedCategory, result.category)
            assertEquals(sample.id, sample.expectedCompletionStatus, result.completionKeyStatus)
            if (sample.expectedCompletionStatus == CompletionKeyStatus.FOUND) {
                assertNotNull(sample.id, result.completionKey)
            }
        }

        assertEquals(21, results.size)
        assertEquals(IntentCategory.entries.toSet(), results.map { it.second.category }.toSet())
        assertEquals(18, results.count { it.second.completionKeyStatus == CompletionKeyStatus.FOUND })
        assertEquals(1, results.count { it.second.completionKeyStatus == CompletionKeyStatus.MISSING })
        assertEquals(1, results.count { it.second.completionKeyStatus == CompletionKeyStatus.NOT_ACTIONABLE })
        assertEquals(1, results.count { it.second.completionKeyStatus == CompletionKeyStatus.NEEDS_ESCALATION })
    }

    private data class BiopsySample(
        val id: String,
        val text: String?,
        val sourceAppLabel: String?,
        val appCategory: String?,
        val canonicalUrl: String?,
        val capturedAtMillis: Long = NOW,
        val expectedCategory: IntentCategory,
        val expectedCompletionStatus: CompletionKeyStatus,
    ) {
        fun input(): BasicUnderstandingInput = BasicUnderstandingInput(
            captureId = id,
            textContent = text,
            sourceAppLabel = sourceAppLabel,
            appCategory = appCategory,
            canonicalUrl = canonicalUrl,
            capturedAtMillis = capturedAtMillis,
            nowMillis = NOW,
        )
    }

    private companion object {
        const val NOW = 1_779_216_000_000L
        val OLD = NOW - TimeUnit.DAYS.toMillis(120)

        val SAMPLES = listOf(
            BiopsySample(
                id = "buy-later-price",
                text = "Trail running jacket price $129.00 free shipping",
                sourceAppLabel = "Chrome",
                appCategory = "BROWSER",
                canonicalUrl = "https://shop.example/jacket",
                expectedCategory = IntentCategory.BUY_LATER_PRODUCT,
                expectedCompletionStatus = CompletionKeyStatus.FOUND,
            ),
            BiopsySample(
                id = "buy-later-url-only",
                text = "Product page for headphones",
                sourceAppLabel = "Chrome",
                appCategory = "BROWSER",
                canonicalUrl = "https://shop.example/headphones",
                expectedCategory = IntentCategory.BUY_LATER_PRODUCT,
                expectedCompletionStatus = CompletionKeyStatus.FOUND,
            ),
            BiopsySample(
                id = "recipe-ingredients",
                text = "Recipe: pancakes. Ingredients: 2 cups flour and 1 cup milk.",
                sourceAppLabel = "Chrome",
                appCategory = "BROWSER",
                canonicalUrl = null,
                expectedCategory = IntentCategory.RECIPE,
                expectedCompletionStatus = CompletionKeyStatus.FOUND,
            ),
            BiopsySample(
                id = "recipe-teaspoon",
                text = "Preheat oven. Add 1 teaspoon vanilla and bake.",
                sourceAppLabel = "Notes",
                appCategory = "READING",
                canonicalUrl = null,
                expectedCategory = IntentCategory.RECIPE,
                expectedCompletionStatus = CompletionKeyStatus.FOUND,
            ),
            BiopsySample(
                id = "qr-payload",
                text = "QR code payload: WIFI:T:WPA;S:OrbitCafe;P:pass1234;;",
                sourceAppLabel = "Camera",
                appCategory = "UTILITY",
                canonicalUrl = null,
                expectedCategory = IntentCategory.QR_OR_BARCODE,
                expectedCompletionStatus = CompletionKeyStatus.FOUND,
            ),
            BiopsySample(
                id = "barcode-url",
                text = "Scan this barcode for https://example.com/check-in",
                sourceAppLabel = "Gallery",
                appCategory = "UTILITY",
                canonicalUrl = null,
                expectedCategory = IntentCategory.QR_OR_BARCODE,
                expectedCompletionStatus = CompletionKeyStatus.FOUND,
            ),
            BiopsySample(
                id = "order-id",
                text = "Order #ABCD-1234 confirmed. Tracking starts tomorrow.",
                sourceAppLabel = "Gmail",
                appCategory = "WORK_EMAIL",
                canonicalUrl = null,
                expectedCategory = IntentCategory.RECEIPT_OR_ORDER,
                expectedCompletionStatus = CompletionKeyStatus.FOUND,
            ),
            BiopsySample(
                id = "receipt-total",
                text = "Receipt from Orbit Market. Subtotal $18.42 delivered.",
                sourceAppLabel = "Gmail",
                appCategory = "WORK_EMAIL",
                canonicalUrl = null,
                expectedCategory = IntentCategory.RECEIPT_OR_ORDER,
                expectedCompletionStatus = CompletionKeyStatus.FOUND,
            ),
            BiopsySample(
                id = "event-ticket",
                text = "Concert ticket confirmed. Seat B12. May 20, 2026.",
                sourceAppLabel = "Ticketmaster",
                appCategory = "BROWSER",
                canonicalUrl = null,
                expectedCategory = IntentCategory.EVENT_TICKET_RESERVATION,
                expectedCompletionStatus = CompletionKeyStatus.FOUND,
            ),
            BiopsySample(
                id = "event-booking",
                text = "Booking BK99881 for dinner reservation on 05/22/2026.",
                sourceAppLabel = "OpenTable",
                appCategory = "BROWSER",
                canonicalUrl = null,
                expectedCategory = IntentCategory.EVENT_TICKET_RESERVATION,
                expectedCompletionStatus = CompletionKeyStatus.FOUND,
            ),
            BiopsySample(
                id = "coupon-code",
                text = "Promo code SAVE20 expires May 20, 2026.",
                sourceAppLabel = "Chrome",
                appCategory = "BROWSER",
                canonicalUrl = null,
                expectedCategory = IntentCategory.COUPON_OR_PROMO,
                expectedCompletionStatus = CompletionKeyStatus.FOUND,
            ),
            BiopsySample(
                id = "coupon-expiry",
                text = "Discount code ORBIT15. Sale ends 05/25/2026.",
                sourceAppLabel = "Chrome",
                appCategory = "BROWSER",
                canonicalUrl = null,
                expectedCategory = IntentCategory.COUPON_OR_PROMO,
                expectedCompletionStatus = CompletionKeyStatus.FOUND,
            ),
            BiopsySample(
                id = "watch-later-youtube",
                text = null,
                sourceAppLabel = "YouTube",
                appCategory = "VIDEO",
                canonicalUrl = "https://youtu.be/abc123",
                expectedCategory = IntentCategory.READ_OR_WATCH_LATER,
                expectedCompletionStatus = CompletionKeyStatus.FOUND,
            ),
            BiopsySample(
                id = "read-later-url",
                text = "Long article to read later",
                sourceAppLabel = "Pocket",
                appCategory = "READING",
                canonicalUrl = "https://example.com/article",
                expectedCategory = IntentCategory.READ_OR_WATCH_LATER,
                expectedCompletionStatus = CompletionKeyStatus.FOUND,
            ),
            BiopsySample(
                id = "place-address",
                text = "Restaurant to try: 123 Market Street near the map pin.",
                sourceAppLabel = "Maps",
                appCategory = "BROWSER",
                canonicalUrl = null,
                expectedCategory = IntentCategory.PLACE_OR_TRAVEL_IDEA,
                expectedCompletionStatus = CompletionKeyStatus.FOUND,
            ),
            BiopsySample(
                id = "travel-flight",
                text = "Flight idea to San Juan. Itinerary saved for June 10, 2026.",
                sourceAppLabel = "Chrome",
                appCategory = "BROWSER",
                canonicalUrl = null,
                expectedCategory = IntentCategory.PLACE_OR_TRAVEL_IDEA,
                expectedCompletionStatus = CompletionKeyStatus.FOUND,
            ),
            BiopsySample(
                id = "gift-price",
                text = "Gift idea for mom: silk scarf price $48.00",
                sourceAppLabel = "Chrome",
                appCategory = "BROWSER",
                canonicalUrl = null,
                expectedCategory = IntentCategory.GIFT_IDEA,
                expectedCompletionStatus = CompletionKeyStatus.FOUND,
            ),
            BiopsySample(
                id = "chat-appointment",
                text = "Can you reply? Chelsea has a scheduled appointment at 2pm.",
                sourceAppLabel = "Messages",
                appCategory = "MESSAGING",
                canonicalUrl = null,
                expectedCategory = IntentCategory.CHAT_ACTION,
                expectedCompletionStatus = CompletionKeyStatus.FOUND,
            ),
            BiopsySample(
                id = "chat-missing-action-detail",
                text = "Please reply when you can about the pickup plan.",
                sourceAppLabel = "Messages",
                appCategory = "MESSAGING",
                canonicalUrl = null,
                expectedCategory = IntentCategory.CHAT_ACTION,
                expectedCompletionStatus = CompletionKeyStatus.MISSING,
            ),
            BiopsySample(
                id = "maybe-old-empty",
                text = null,
                sourceAppLabel = null,
                appCategory = null,
                canonicalUrl = null,
                capturedAtMillis = OLD,
                expectedCategory = IntentCategory.MAYBE_OLD_OR_INACTIVE,
                expectedCompletionStatus = CompletionKeyStatus.NOT_ACTIONABLE,
            ),
            BiopsySample(
                id = "unknown-current-empty",
                text = null,
                sourceAppLabel = null,
                appCategory = null,
                canonicalUrl = null,
                expectedCategory = IntentCategory.UNKNOWN,
                expectedCompletionStatus = CompletionKeyStatus.NEEDS_ESCALATION,
            ),
        )
    }
}