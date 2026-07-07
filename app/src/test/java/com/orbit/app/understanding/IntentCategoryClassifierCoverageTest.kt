package com.orbit.app.understanding

import com.orbit.app.understanding.domain.IntentCategory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

/**
 * Recall-focused coverage for [IntentCategoryClassifier]. These cases were
 * previously misclassified as UNKNOWN (or the wrong category) because the
 * keyword lists were thin — the driver was a saved recipe surfacing as a
 * generic note. Negative guards protect the precision the ordered
 * first-match-wins design encodes.
 */
class IntentCategoryClassifierCoverageTest {

    private val classifier = IntentCategoryClassifier()

    private fun classify(
        text: String?,
        appCategory: String = "BROWSER",
        source: String = "Chrome",
        url: String? = null,
    ) = classifier.classify(
        ClassificationInput(
            text = text,
            appCategory = appCategory,
            sourceAppLabel = source,
            canonicalUrl = url,
            capturedAtMillis = NOW,
            nowMillis = NOW,
        )
    ).category

    // ---- Recipe recall ---------------------------------------------------

    @Test
    fun recipeByCookingMethodKeyword() {
        assertEquals(
            IntentCategory.RECIPE,
            classify("Miso-glazed salmon — whisk the miso with mirin, then roast."),
        )
    }

    @Test
    fun recipeByStructuralTempAndTime_noRecipeWord() {
        // No "recipe"/method keyword — pure temperature + cook-time fingerprint.
        assertEquals(
            IntentCategory.RECIPE,
            classify("Chicken thighs, lemon, thyme. 25 min at 375F, then rest."),
        )
    }

    @Test
    fun recipeStructureDoesNotFireOnAPlainMeeting() {
        // A cook-time alone (no temp, no method) must NOT read as a recipe.
        assertNotEquals(
            IntentCategory.RECIPE,
            classify("Team sync in 30 minutes to review the Q3 plan."),
        )
    }

    // ---- Event recall + pruned-token guards ------------------------------

    @Test
    fun eventByDoorsOpenAndWillCall() {
        assertEquals(
            IntentCategory.EVENT_TICKET_RESERVATION,
            classify("Indie showcase Saturday — doors open 8pm, will-call under my name."),
        )
    }

    @Test
    fun eventByRsvp() {
        assertEquals(
            IntentCategory.EVENT_TICKET_RESERVATION,
            classify("Founder office hours Thursday 4pm, RSVP link in bio."),
        )
    }

    @Test
    fun tomorrowDoesNotFalseMatchEvent() {
        // Guards the removal of the ambiguous "row "/"gate "/"doors " tokens
        // ("tomorrow" contains "row ").
        assertNotEquals(
            IntentCategory.EVENT_TICKET_RESERVATION,
            classify("Let's grab coffee tomorrow and catch up."),
        )
    }

    // ---- Gift recall -----------------------------------------------------

    @Test
    fun giftByBirthdayAndWantingPhrasing() {
        assertEquals(
            IntentCategory.GIFT_IDEA,
            classify("Mom's birthday — she mentioned wanting the ceramic pour-over set."),
        )
    }

    // ---- Read/watch recall ----------------------------------------------

    @Test
    fun readLaterByArticlePhrasing() {
        assertEquals(
            IntentCategory.READ_OR_WATCH_LATER,
            classify("Long read on the attention economy — worth a read this weekend."),
        )
    }

    // ---- Shopping + coupon recall ---------------------------------------

    @Test
    fun shoppingByStockAndShippingSignals() {
        assertEquals(
            IntentCategory.BUY_LATER_PRODUCT,
            classify("Espresso grinder — in stock, free shipping."),
        )
    }

    @Test
    fun couponByPercentOffAndCode() {
        assertEquals(
            IntentCategory.COUPON_OR_PROMO,
            classify("Flash sale: 20% off, use code SAVE20."),
        )
    }

    private companion object {
        const val NOW = 1_779_216_000_000L
    }
}
