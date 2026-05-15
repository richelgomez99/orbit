package com.orbit.app.understanding.engine

import com.orbit.app.data.model.AppCategory
import com.orbit.app.understanding.domain.IntentCategory

data class IntentEvidenceInput(
    val text: String? = null,
    val canonicalUrl: String? = null,
    val foregroundPackageName: String? = null,
    val foregroundAppLabel: String? = null,
    val fallbackCategory: AppCategory? = null,
    val capturedAtMillis: Long? = null
)

data class IntentCategoryResult(
    val category: IntentCategory,
    val confidence: Float,
    val evidenceSummary: String
)

class IntentCategoryClassifier {
    fun classify(input: IntentEvidenceInput): IntentCategoryResult {
        val text = input.text.normalizedForMatching()
        val url = input.canonicalUrl.orEmpty().lowercase()
        val app = listOfNotNull(input.foregroundPackageName, input.foregroundAppLabel)
            .joinToString(" ")
            .lowercase()
        val combined = listOf(text, url, app).joinToString(" ")

        return when {
            combined.hasAny("qr", "barcode", "scan code", "boarding pass") ->
                result(IntentCategory.QR_OR_BARCODE, 0.88f, "code-like text")
            combined.hasAny("order", "receipt", "total", "tracking", "return by", "confirmation") && combined.hasPrice() ->
                result(IntentCategory.RECEIPT_OR_ORDER, 0.86f, "order or receipt evidence")
            combined.hasAny("coupon", "promo", "discount", "expires", "use code", "code:") ->
                result(IntentCategory.COUPON_OR_PROMO, 0.84f, "coupon or expiry evidence")
            combined.hasAny("reply", "call", "send", "remind", "can you", "please", "deadline", "todo", "to do") ->
                result(IntentCategory.CHAT_ACTION, 0.76f, "requested action text")
            combined.hasAny("ticket", "reservation", "appointment", "calendar", "check-in", "check in", "gate") || combined.hasDateLikeText() ->
                result(IntentCategory.EVENT_TICKET_RESERVATION, 0.78f, "date or event evidence")
            combined.hasAny("ingredients", "recipe", "preheat", "tablespoon", "tsp", "tbsp", "cups") ->
                result(IntentCategory.RECIPE, 0.86f, "recipe terms")
            combined.hasAny("gift", "birthday", "anniversary", "for mom", "for dad", "for her", "for him") ->
                result(IntentCategory.GIFT_IDEA, 0.78f, "gift/occasion terms")
            combined.hasAny("restaurant", "hotel", "maps", "directions", "address", "near me") || app.contains("maps") ->
                result(IntentCategory.PLACE_OR_TRAVEL_IDEA, 0.8f, "place or map evidence")
            combined.hasAny("watch later", "read later", "article", "thread", "video", "youtube", "substack", "medium") ||
                input.fallbackCategory == AppCategory.READING || input.fallbackCategory == AppCategory.VIDEO ->
                result(IntentCategory.READ_OR_WATCH_LATER, 0.74f, "read/watch source")
            combined.hasAny("buy", "cart", "wishlist", "size", "add to bag", "shop", "sale") || combined.hasPrice() ->
                result(IntentCategory.BUY_LATER_PRODUCT, 0.74f, "shopping evidence")
            text.isBlank() && input.canonicalUrl.isNullOrBlank() ->
                result(IntentCategory.MAYBE_OLD_OR_INACTIVE, 0.45f, "no readable evidence")
            else -> result(IntentCategory.UNKNOWN, 0.3f, "no category rule matched")
        }
    }

    private fun result(category: IntentCategory, confidence: Float, summary: String): IntentCategoryResult =
        IntentCategoryResult(category = category, confidence = confidence, evidenceSummary = summary)
}

internal fun String?.normalizedForMatching(): String =
    this.orEmpty().lowercase().replace(Regex("\\s+"), " ").trim()

internal fun String.hasAny(vararg needles: String): Boolean = needles.any { contains(it) }

internal fun String.hasPrice(): Boolean = Regex("[$€£]\\s?\\d+(?:[.,]\\d{2})?").containsMatchIn(this)

internal fun String.hasDateLikeText(): Boolean = Regex(
    "\\b(?:jan|feb|mar|apr|may|jun|jul|aug|sep|oct|nov|dec|today|tomorrow|mon|tue|wed|thu|fri|sat|sun)\\b|\\b\\d{1,2}[/-]\\d{1,2}(?:[/-]\\d{2,4})?\\b"
).containsMatchIn(this)
