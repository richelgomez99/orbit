package com.orbit.app.understanding

import com.orbit.app.understanding.domain.CompletionKeyStatus
import com.orbit.app.understanding.domain.IntentCategory
import com.orbit.app.understanding.engine.CompletionKeyExtractor
import com.orbit.app.understanding.engine.IntentEvidenceInput
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CompletionKeyExtractorTest {
    private val extractor = CompletionKeyExtractor()

    @Test
    fun extract_productKeyWhenProductAndMerchantPresent() {
        val result = extractor.extract(
            IntentCategory.BUY_LATER_PRODUCT,
            IntentEvidenceInput(text = "Trail shoes\n$89.00", foregroundAppLabel = "REI")
        )

        assertEquals(CompletionKeyStatus.FOUND, result.status)
        assertTrue(result.toStorageJson().contains("Trail shoes"))
    }

    @Test
    fun extract_couponMissingCodeIsLimited() {
        val result = extractor.extract(
            IntentCategory.COUPON_OR_PROMO,
            IntentEvidenceInput(text = "Holiday discount expires tomorrow")
        )

        assertEquals(CompletionKeyStatus.MISSING, result.status)
        assertEquals(listOf("code"), result.missingFields)
    }

    @Test
    fun extract_chatActionFindsRequestedAction() {
        val result = extractor.extract(
            IntentCategory.CHAT_ACTION,
            IntentEvidenceInput(text = "Sam\nPlease send the deck tomorrow", foregroundAppLabel = "Messages")
        )

        assertEquals(CompletionKeyStatus.FOUND, result.status)
        assertTrue(result.toStorageJson().contains("Please send the deck"))
    }

    @Test
    fun extract_findsKeysForRemainingCategories() {
        val fixtures = listOf(
            IntentCategory.RECIPE to IntentEvidenceInput(text = "Pancakes\n2 cups flour\n1 tbsp sugar"),
            IntentCategory.QR_OR_BARCODE to IntentEvidenceInput(text = "QR code: HTTPS://EXAMPLE.COM/PASS"),
            IntentCategory.RECEIPT_OR_ORDER to IntentEvidenceInput(text = "Store\nOrder #ABC123\nTotal $42.10", foregroundAppLabel = "Store"),
            IntentCategory.EVENT_TICKET_RESERVATION to IntentEvidenceInput(text = "Concert\nMar 12 at 7:30\nVenue: Hall"),
            IntentCategory.READ_OR_WATCH_LATER to IntentEvidenceInput(text = "Long article title", canonicalUrl = "https://example.com/story"),
            IntentCategory.PLACE_OR_TRAVEL_IDEA to IntentEvidenceInput(text = "Cafe Roma\n123 Main Street"),
            IntentCategory.GIFT_IDEA to IntentEvidenceInput(text = "Desk lamp\nGift idea for mom\n$35")
        )

        fixtures.forEach { (category, input) ->
            assertEquals(category.name, CompletionKeyStatus.FOUND, extractor.extract(category, input).status)
        }
    }
}
