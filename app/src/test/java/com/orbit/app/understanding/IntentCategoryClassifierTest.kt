package com.orbit.app.understanding

import com.orbit.app.data.model.AppCategory
import com.orbit.app.understanding.domain.IntentCategory
import com.orbit.app.understanding.engine.IntentCategoryClassifier
import com.orbit.app.understanding.engine.IntentEvidenceInput
import org.junit.Assert.assertEquals
import org.junit.Test

class IntentCategoryClassifierTest {
    private val classifier = IntentCategoryClassifier()

    @Test
    fun classify_coversActiveIntentTaxonomy() {
        val fixtures = listOf(
            "Nike Pegasus $119 add to bag" to IntentCategory.BUY_LATER_PRODUCT,
            "Recipe\n2 cups flour\npreheat oven" to IntentCategory.RECIPE,
            "QR code: HTTPS://EXAMPLE.COM/PASS" to IntentCategory.QR_OR_BARCODE,
            "Order #ABC123 total $42.10 tracking" to IntentCategory.RECEIPT_OR_ORDER,
            "Ticket reservation Mar 12 at 7:30 venue" to IntentCategory.EVENT_TICKET_RESERVATION,
            "Use code SAVE20 expires tomorrow" to IntentCategory.COUPON_OR_PROMO,
            "Watch later YouTube video essay" to IntentCategory.READ_OR_WATCH_LATER,
            "Restaurant address 123 Main Street maps" to IntentCategory.PLACE_OR_TRAVEL_IDEA,
            "Gift idea for mom birthday $35" to IntentCategory.GIFT_IDEA,
            "Please reply to Sam by tomorrow" to IntentCategory.CHAT_ACTION
        )

        fixtures.forEach { (text, expected) ->
            assertEquals(expected, classifier.classify(IntentEvidenceInput(text = text)).category)
        }
    }

    @Test
    fun classify_blankVisualOnlyIsMaybeOldOrInactive() {
        assertEquals(
            IntentCategory.MAYBE_OLD_OR_INACTIVE,
            classifier.classify(IntentEvidenceInput()).category
        )
    }

    @Test
    fun classify_readingFallbackIsReadOrWatchLater() {
        assertEquals(
            IntentCategory.READ_OR_WATCH_LATER,
            classifier.classify(IntentEvidenceInput(fallbackCategory = AppCategory.READING)).category
        )
    }

    @Test
    fun classify_unmatchedEvidenceIsUnknown() {
        assertEquals(
            IntentCategory.UNKNOWN,
            classifier.classify(IntentEvidenceInput(text = "blue abstract wallpaper" )).category
        )
    }
}
