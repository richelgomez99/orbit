package com.orbit.app.library

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class IntentTypeLabelTest {

    @Test
    fun mapsRichCategoryNames() {
        assertEquals("Recipe", IntentTypeLabel.forIntent("RECIPE"))
        assertEquals("Event", IntentTypeLabel.forIntent("EVENT_TICKET_RESERVATION"))
        assertEquals("Coupon", IntentTypeLabel.forIntent("COUPON_OR_PROMO"))
        assertEquals("Product", IntentTypeLabel.forIntent("BUY_LATER_PRODUCT"))
    }

    @Test
    fun mapsProductIntentNames() {
        assertEquals("Want it", IntentTypeLabel.forIntent("WANT_IT"))
        assertEquals("Reference", IntentTypeLabel.forIntent("REFERENCE"))
    }

    @Test
    fun uninformativeValuesReturnNull() {
        assertNull(IntentTypeLabel.forIntent("AMBIGUOUS"))
        assertNull(IntentTypeLabel.forIntent("UNKNOWN"))
        assertNull(IntentTypeLabel.forIntent("MAYBE_OLD_OR_INACTIVE"))
        assertNull(IntentTypeLabel.forIntent(null))
        assertNull(IntentTypeLabel.forIntent(""))
        assertNull(IntentTypeLabel.forIntent("not_a_real_value"))
    }

    @Test
    fun isCaseAndWhitespaceInsensitive() {
        assertEquals("Recipe", IntentTypeLabel.forIntent("  recipe "))
    }
}
