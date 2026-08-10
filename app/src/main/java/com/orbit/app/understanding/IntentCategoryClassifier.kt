package com.orbit.app.understanding

import com.orbit.app.understanding.domain.IntentCategory
import java.util.Locale
import java.util.concurrent.TimeUnit

data class ClassificationInput(
    val text: String?,
    val appCategory: String?,
    val sourceAppLabel: String?,
    val canonicalUrl: String?,
    val capturedAtMillis: Long,
    val nowMillis: Long
)

data class ClassificationResult(
    val category: IntentCategory,
    val confidence: Float,
    val evidence: List<String>
)

class IntentCategoryClassifier {

    fun classify(input: ClassificationInput): ClassificationResult {
        val text = input.text.orEmpty().lowercase(Locale.ROOT)
        val source = input.sourceAppLabel.orEmpty().lowercase(Locale.ROOT)
        val appCategory = input.appCategory.orEmpty().uppercase(Locale.ROOT)
        val url = input.canonicalUrl.orEmpty().lowercase(Locale.ROOT)
        val combined = listOf(text, source, url).joinToString(" ")
        val messagingSource = isMessagingSource(appCategory, source)

        rule(IntentCategory.QR_OR_BARCODE, 0.92f, "qr_or_barcode_text", combined, QR_TERMS)?.let { return it }
        rule(IntentCategory.RECEIPT_OR_ORDER, 0.88f, "order_or_receipt_text", combined, ORDER_TERMS)?.let { return it }
        rule(IntentCategory.COUPON_OR_PROMO, 0.86f, "coupon_or_promo_text", combined, COUPON_TERMS)?.let { return it }
        rule(IntentCategory.EVENT_TICKET_RESERVATION, 0.84f, "event_ticket_text", combined, STRONG_EVENT_TERMS)?.let { return it }
        if (messagingSource) {
            return ClassificationResult(IntentCategory.CHAT_ACTION, 0.7f, listOf("messaging_source"))
        }
        rule(IntentCategory.EVENT_TICKET_RESERVATION, 0.76f, "event_detail_text", combined, WEAK_EVENT_TERMS)?.let { return it }
        rule(IntentCategory.RECIPE, 0.82f, "recipe_text", combined, RECIPE_TERMS)?.let { return it }
        rule(IntentCategory.PLACE_OR_TRAVEL_IDEA, 0.8f, "place_or_travel_text", combined, PLACE_TERMS)?.let { return it }
        rule(IntentCategory.GIFT_IDEA, 0.78f, "gift_text", combined, GIFT_TERMS)?.let { return it }
        rule(IntentCategory.CHAT_ACTION, 0.78f, "chat_action_text", combined, CHAT_TERMS)?.let { return it }
        rule(IntentCategory.BUY_LATER_PRODUCT, 0.76f, "shopping_text", combined, SHOPPING_TERMS)?.let { return it }

        if (appCategory in setOf("VIDEO", "READING")) {
            return ClassificationResult(IntentCategory.READ_OR_WATCH_LATER, 0.74f, listOf("app_category:$appCategory"))
        }
        if (source in READ_WATCH_SOURCES || url.contains("youtube.com") || url.contains("youtu.be")) {
            return ClassificationResult(IntentCategory.READ_OR_WATCH_LATER, 0.72f, listOf("source_or_url_read_watch"))
        }
        if (messagingSource) {
            return ClassificationResult(IntentCategory.CHAT_ACTION, 0.62f, listOf("app_category:$appCategory"))
        }
        if (isLikelyInactive(input.capturedAtMillis, input.nowMillis) && combined.isBlank()) {
            return ClassificationResult(IntentCategory.MAYBE_OLD_OR_INACTIVE, 0.56f, listOf("old_capture_no_local_signals"))
        }
        return ClassificationResult(IntentCategory.UNKNOWN, 0.2f, emptyList())
    }

    private fun rule(
        category: IntentCategory,
        confidence: Float,
        evidence: String,
        text: String,
        terms: List<String>
    ): ClassificationResult? {
        if (terms.any { text.contains(it) }) {
            return ClassificationResult(category, confidence, listOf(evidence))
        }
        return null
    }

    private fun isLikelyInactive(capturedAtMillis: Long, nowMillis: Long): Boolean {
        if (capturedAtMillis <= 0L || nowMillis <= capturedAtMillis) return false
        return nowMillis - capturedAtMillis >= TimeUnit.DAYS.toMillis(90)
    }

    private fun isMessagingSource(appCategory: String, source: String): Boolean =
        appCategory == "MESSAGING" ||
            appCategory == "WORK_EMAIL" ||
            MESSAGING_SOURCES.any { source.contains(it) }

    private companion object {
        val QR_TERMS = listOf("qr code", "barcode", "scan code", "scan this")
        val ORDER_TERMS = listOf("order #", "order number", "confirmation", "tracking", "receipt", "delivered")
        val COUPON_TERMS = listOf("coupon", "promo code", "discount code", "expires", "sale ends")
        val STRONG_EVENT_TERMS = listOf("ticket", "reservation", "booking", "check-in", "boarding", "boarding pass")
        val WEAK_EVENT_TERMS = listOf("appointment", "scheduled", "calendar invite")
        val RECIPE_TERMS = listOf("recipe", "ingredients", "preheat", "tablespoon", "teaspoon")
        val PLACE_TERMS = listOf("hotel", "flight", "restaurant", "address", "directions", "map", "itinerary")
        val GIFT_TERMS = listOf("gift idea", "wishlist", "for mom", "for dad", "birthday gift")
        val CHAT_TERMS = listOf("reply", "respond", "message", "text me", "email me", "call me")
        val SHOPPING_TERMS = listOf("add to cart", "buy now", "price", "subtotal", "product", "shipping")
        val READ_WATCH_SOURCES = setOf("youtube", "netflix", "kindle", "pocket", "reader", "spotify")
        val MESSAGING_SOURCES = setOf("messages", "messenger", "whatsapp", "signal", "telegram", "gmail", "outlook", "mail")
    }
}