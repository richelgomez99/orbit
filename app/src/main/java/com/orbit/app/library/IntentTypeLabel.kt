package com.orbit.app.library

/**
 * Spec 020 (S3) — maps a [com.orbit.app.memory.MemorySearchResult.intent] value
 * to a short, human-facing type label for the Library row badge.
 *
 * `intent` carries either an [com.orbit.app.understanding.domain.IntentCategory]
 * name (RECIPE, EVENT_TICKET_RESERVATION, …) when the classifier assigned an
 * informative category, or a product `Intent` name (WANT_IT, REFERENCE, …)
 * otherwise. This is the single place that renders both taxonomies as one
 * concept — "what kind of thing is this".
 *
 * Returns `null` for uninformative values (AMBIGUOUS / UNKNOWN / inactive) so
 * the UI shows no badge rather than a meaningless one.
 */
object IntentTypeLabel {

    private val LABELS: Map<String, String> = mapOf(
        // Rich IntentCategory taxonomy (preferred when present).
        "BUY_LATER_PRODUCT" to "Product",
        "RECIPE" to "Recipe",
        "QR_OR_BARCODE" to "Code",
        "RECEIPT_OR_ORDER" to "Receipt",
        "EVENT_TICKET_RESERVATION" to "Event",
        "COUPON_OR_PROMO" to "Coupon",
        "READ_OR_WATCH_LATER" to "Read later",
        "PLACE_OR_TRAVEL_IDEA" to "Place",
        "GIFT_IDEA" to "Gift",
        "CHAT_ACTION" to "Action",
        // Product Intent taxonomy (fallback when no informative category).
        "WANT_IT" to "Want it",
        "REFERENCE" to "Reference",
        "READ_LATER" to "Read later",
        "FOR_SOMEONE" to "For someone",
        "INTERESTING" to "Interesting",
    )

    /** Human label for [intent], or null when the type is uninformative. */
    fun forIntent(intent: String?): String? {
        val key = intent?.trim()?.uppercase() ?: return null
        return LABELS[key]
    }
}
