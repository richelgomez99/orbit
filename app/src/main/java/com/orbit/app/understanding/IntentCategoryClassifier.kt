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
        if (hasRecipeStructure(text)) {
            return ClassificationResult(IntentCategory.RECIPE, 0.78f, listOf("recipe_structure"))
        }
        rule(IntentCategory.PLACE_OR_TRAVEL_IDEA, 0.8f, "place_or_travel_text", combined, PLACE_TERMS)?.let { return it }
        rule(IntentCategory.GIFT_IDEA, 0.78f, "gift_text", combined, GIFT_TERMS)?.let { return it }
        rule(IntentCategory.READ_OR_WATCH_LATER, 0.75f, "read_watch_text", combined, READ_WATCH_TERMS)?.let { return it }
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

    /**
     * True when [text] carries the structural fingerprint of a recipe without
     * naming one: a cooking temperature and/or method combined with a cook time
     * (e.g. "roast at 400F for 12 minutes", "20 min at 400F"). Requires two
     * independent signals so it doesn't fire on a bare "30 minutes" meeting.
     */
    private fun hasRecipeStructure(text: String): Boolean {
        val temp = OVEN_TEMP.containsMatchIn(text)
        val time = COOK_TIME.containsMatchIn(text)
        val method = COOK_METHOD.containsMatchIn(text)
        return (temp && time) || (method && time) || (temp && method)
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
        val QR_TERMS = listOf("qr code", "barcode", "scan code", "scan this", "scan to")
        val ORDER_TERMS = listOf(
            "order #", "order number", "order confirmed", "your order", "confirmation",
            "tracking", "tracking number", "receipt", "invoice", "delivered", "shipped",
            "out for delivery", "estimated delivery", "payment received", "return label"
        )
        val COUPON_TERMS = listOf(
            "coupon", "promo code", "promo:", "discount code", "voucher", "use code",
            "expires", "sale ends", "deal ends", "limited time", "% off", "percent off",
            "save $", "clearance", "flash sale"
        )
        val STRONG_EVENT_TERMS = listOf(
            "ticket", "reservation", "reserved", "booking", "booked", "check-in", "boarding",
            "boarding pass", "rsvp", "doors open", "will-call", "will call",
            "venue", "showtime", "kickoff", "box office"
        )
        val WEAK_EVENT_TERMS = listOf(
            "appointment", "scheduled", "calendar invite", "meeting at", "rescheduled",
            "starts at", "happening on"
        )
        val RECIPE_TERMS = listOf(
            "recipe", "ingredients", "preheat", "tablespoon", "teaspoon", "tbsp", "tsp",
            "bake", "roast", "broil", "saute", "sauté", "simmer", "marinade", "marinate",
            "glaze", "glazed", "whisk", "knead", "dough", "batter", "season with", "garnish",
            "serve over", "serves ", "cook for", "cups flour", "diced", "minced", "chopped",
            "stir in", "fold in"
        )
        val PLACE_TERMS = listOf(
            "hotel", "flight", "restaurant", "address", "directions", "map", "itinerary",
            "airbnb", "check into", "things to do", "neighborhood", "terminal", "departure",
            "arrival", "layover", "sightseeing", "landmark"
        )
        val GIFT_TERMS = listOf(
            "gift idea", "gift for", "present for", "wishlist", "wish list", "for mom", "for dad",
            "birthday gift", "'s birthday", "her birthday", "his birthday", "christmas gift",
            "anniversary gift", "mentioned wanting", "wants that", "wants the", "she wants", "he wants"
        )
        val CHAT_TERMS = listOf(
            "reply", "respond", "message", "text me", "text back", "email me", "call me",
            "get back to", "follow up with", "dm ", "reach out"
        )
        val SHOPPING_TERMS = listOf(
            "add to cart", "add to bag", "buy now", "buy it", "checkout", "in stock",
            "out of stock", "sold out", "free shipping", "price", "subtotal", "product",
            "shipping", "on sale", "wishlist item"
        )
        // Text-level read/watch signals (source/url/app-category still handled below).
        val READ_WATCH_TERMS = listOf(
            "read later", "must read", "great read", "worth a read", "article", "essay",
            "watch this", "watch later", "documentary", "podcast", "episode", "thread worth",
            "long read", "blog post", "newsletter"
        )
        val READ_WATCH_SOURCES = setOf("youtube", "netflix", "kindle", "pocket", "reader", "spotify")
        val MESSAGING_SOURCES = setOf("messages", "messenger", "whatsapp", "signal", "telegram", "gmail", "outlook", "mail")

        // Structural recipe fingerprint: an oven/stove temperature (e.g. "400F",
        // "350 °F") paired with a cook-time or cooking method. Catches ingredient-
        // list recipes that never say the word "recipe".
        val OVEN_TEMP = Regex("""\b\d{2,3}\s?°?\s?f\b""")
        val COOK_TIME = Regex("""\b\d{1,3}\s?(?:min|mins|minutes|hours?|hrs?)\b""")
        val COOK_METHOD = Regex("""\b(?:bake|roast|broil|grill|simmer|boil|fry|saute|sauté|whisk|marinate|preheat)\w*""")
    }
}