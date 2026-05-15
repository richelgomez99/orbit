package com.orbit.app.understanding.engine

import com.orbit.app.understanding.domain.CompletionKey
import com.orbit.app.understanding.domain.CompletionKeyResult
import com.orbit.app.understanding.domain.CompletionKeyStatus
import com.orbit.app.understanding.domain.IntentCategory

class CompletionKeyExtractor {
    fun extract(category: IntentCategory, input: IntentEvidenceInput): CompletionKeyResult {
        val rawText = input.text.orEmpty().trim()
        val text = rawText.normalizedForMatching()
        val url = input.canonicalUrl
        return when (category) {
            IntentCategory.BUY_LATER_PRODUCT -> product(rawText, text, url, input)
            IntentCategory.RECIPE -> recipe(rawText, text, input)
            IntentCategory.QR_OR_BARCODE -> code(rawText, text)
            IntentCategory.RECEIPT_OR_ORDER -> receipt(rawText, text, input)
            IntentCategory.EVENT_TICKET_RESERVATION -> event(rawText, text)
            IntentCategory.COUPON_OR_PROMO -> coupon(rawText, text, input)
            IntentCategory.READ_OR_WATCH_LATER -> readWatch(rawText, input)
            IntentCategory.PLACE_OR_TRAVEL_IDEA -> place(rawText, text, input)
            IntentCategory.GIFT_IDEA -> gift(rawText, text, input)
            IntentCategory.CHAT_ACTION -> chatAction(rawText, text, input)
            IntentCategory.MAYBE_OLD_OR_INACTIVE -> notActionable(category, "No readable active intent evidence")
            IntentCategory.UNKNOWN -> missing(category, listOf("category"), "No completion key rule matched")
        }
    }

    private fun product(rawText: String, text: String, url: String?, input: IntentEvidenceInput): CompletionKeyResult {
        val price = price(rawText)
        val merchant = input.foregroundAppLabel ?: url?.hostLike()
        val product = firstUsefulLine(rawText) ?: merchant
        val missing = buildList {
            if (product.isNullOrBlank()) add("productName")
            if (merchant.isNullOrBlank()) add("merchantOrSource")
        }
        return if (missing.isEmpty()) found(
            IntentCategory.BUY_LATER_PRODUCT,
            CompletionKey.Product(productName = product!!, merchantOrSource = merchant, visiblePrice = price, urlOrDeepLink = url),
            "product=${product}; merchant=${merchant.orEmpty()}; price=${price.orEmpty()}",
            if (price != null) 0.82f else 0.68f
        ) else missing(IntentCategory.BUY_LATER_PRODUCT, missing, "Looks like a product, but merchant or product text is missing")
    }

    private fun recipe(rawText: String, text: String, input: IntentEvidenceInput): CompletionKeyResult {
        val ingredients = ingredientLines(rawText)
        val title = firstUsefulLine(rawText) ?: input.foregroundAppLabel
        val missing = buildList {
            if (title.isNullOrBlank()) add("recipeTitle")
            if (ingredients.isEmpty()) add("ingredients")
        }
        return if (missing.isEmpty()) found(
            IntentCategory.RECIPE,
            CompletionKey.Recipe(recipeTitle = title!!, source = input.foregroundAppLabel, ingredients = ingredients),
            "${ingredients.size} ingredients found",
            0.84f
        ) else missing(IntentCategory.RECIPE, missing, "Looks like a recipe, but ingredients were not found")
    }

    private fun code(rawText: String, text: String): CompletionKeyResult {
        val payload = Regex("(?:qr|barcode|code)[:#]?\\s*([A-Z0-9:/?&._=-]{6,})", RegexOption.IGNORE_CASE)
            .find(rawText)?.groupValues?.getOrNull(1)
            ?: Regex("\\b[A-Z0-9]{8,}\\b").find(rawText)?.value
        return if (payload != null) found(
            IntentCategory.QR_OR_BARCODE,
            CompletionKey.Code(decodedPayload = payload, expiry = expiry(rawText), context = firstUsefulLine(rawText)),
            "decoded payload candidate found",
            0.78f
        ) else missing(IntentCategory.QR_OR_BARCODE, listOf("decodedPayload"), "Looks like a code screenshot, but no payload text was decoded")
    }

    private fun receipt(rawText: String, text: String, input: IntentEvidenceInput): CompletionKeyResult {
        val merchant = input.foregroundAppLabel ?: firstUsefulLine(rawText)
        val orderId = Regex("(?:order|confirmation|receipt)\\s*(?:#|id|number|no\\.)?\\s*[:#]?\\s*([A-Z0-9-]{5,})", RegexOption.IGNORE_CASE)
            .find(rawText)?.groupValues?.getOrNull(1)
        val total = Regex("(?:total|paid|amount)\\s*[:#]?\\s*([$€£]\\s?\\d+(?:[.,]\\d{2})?)", RegexOption.IGNORE_CASE)
            .find(rawText)?.groupValues?.getOrNull(1) ?: price(rawText)
        val missing = buildList {
            if (merchant.isNullOrBlank()) add("merchant")
            if (orderId.isNullOrBlank() && total.isNullOrBlank()) add("orderIdOrTotal")
        }
        return if (missing.isEmpty()) found(
            IntentCategory.RECEIPT_OR_ORDER,
            CompletionKey.ReceiptOrder(merchant = merchant!!, orderId = orderId, total = total, date = date(rawText), trackingOrReturnClue = tracking(rawText)),
            "merchant=${merchant}; order=${orderId.orEmpty()}; total=${total.orEmpty()}",
            0.82f
        ) else missing(IntentCategory.RECEIPT_OR_ORDER, missing, "Looks like a receipt/order, but merchant and order evidence are incomplete")
    }

    private fun event(rawText: String, text: String): CompletionKeyResult {
        val dateTime = date(rawText)
        val location = Regex("(?:at|location|venue)[: ]+([A-Za-z0-9 .,'-]{4,})", RegexOption.IGNORE_CASE)
            .find(rawText)?.groupValues?.getOrNull(1)?.trim()
        return if (dateTime != null) found(
            IntentCategory.EVENT_TICKET_RESERVATION,
            CompletionKey.EventTicket(dateTime = dateTime, location = location, confirmationId = confirmation(rawText), ticketIdentity = firstUsefulLine(rawText)),
            "date/time found",
            0.78f
        ) else missing(IntentCategory.EVENT_TICKET_RESERVATION, listOf("dateTime"), "Looks like an event or ticket, but no date/time was found")
    }

    private fun coupon(rawText: String, text: String, input: IntentEvidenceInput): CompletionKeyResult {
        val code = Regex("(?:code|promo)[: ]+([A-Z0-9-]{4,})", RegexOption.IGNORE_CASE)
            .find(rawText)?.groupValues?.getOrNull(1)
            ?: Regex("\\b[A-Z0-9]{5,12}\\b").find(rawText)?.value
        return if (code != null) found(
            IntentCategory.COUPON_OR_PROMO,
            CompletionKey.Coupon(code = code, merchant = input.foregroundAppLabel, expiresAt = expiry(rawText), terms = firstLineContaining(rawText, "minimum", "terms", "expires")),
            "coupon code found",
            0.8f
        ) else missing(IntentCategory.COUPON_OR_PROMO, listOf("code"), "Looks like a coupon, but no code was found")
    }

    private fun readWatch(rawText: String, input: IntentEvidenceInput): CompletionKeyResult {
        val title = firstUsefulLine(rawText) ?: input.canonicalUrl?.hostLike() ?: input.foregroundAppLabel
        return if (title != null) found(
            IntentCategory.READ_OR_WATCH_LATER,
            CompletionKey.ReadWatch(title = title, source = input.foregroundAppLabel ?: input.canonicalUrl?.hostLike(), canonicalUrlOrAppIdentity = input.canonicalUrl ?: input.foregroundPackageName),
            "title/source found",
            0.72f
        ) else missing(IntentCategory.READ_OR_WATCH_LATER, listOf("title"), "Looks like read/watch later, but no title or source was found")
    }

    private fun place(rawText: String, text: String, input: IntentEvidenceInput): CompletionKeyResult {
        val place = firstUsefulLine(rawText) ?: input.foregroundAppLabel
        val address = firstLineContaining(rawText, "street", "st.", "avenue", "ave", "road", "rd", "maps")
        return if (place != null) found(
            IntentCategory.PLACE_OR_TRAVEL_IDEA,
            CompletionKey.Place(placeName = place, addressOrMapIdentity = address, source = input.foregroundAppLabel),
            "place candidate found",
            0.7f
        ) else missing(IntentCategory.PLACE_OR_TRAVEL_IDEA, listOf("placeName"), "Looks like a place, but no place name was found")
    }

    private fun gift(rawText: String, text: String, input: IntentEvidenceInput): CompletionKeyResult {
        val item = firstUsefulLine(rawText) ?: input.foregroundAppLabel
        return if (item != null) found(
            IntentCategory.GIFT_IDEA,
            CompletionKey.Gift(item = item, recipientOrOccasion = firstLineContaining(rawText, "birthday", "anniversary", "mom", "dad"), source = input.foregroundAppLabel, price = price(rawText)),
            "gift item candidate found",
            0.68f
        ) else missing(IntentCategory.GIFT_IDEA, listOf("item"), "Looks like a gift idea, but no item was found")
    }

    private fun chatAction(rawText: String, text: String, input: IntentEvidenceInput): CompletionKeyResult {
        val actionLine = firstLineContaining(rawText, "please", "can you", "reply", "call", "send", "remind", "todo", "to do")
        return if (actionLine != null) found(
            IntentCategory.CHAT_ACTION,
            CompletionKey.ChatAction(personOrSource = input.foregroundAppLabel, requestedAction = actionLine, deadline = date(rawText)),
            "requested action found",
            0.74f
        ) else missing(IntentCategory.CHAT_ACTION, listOf("requestedAction"), "Looks like a chat action, but no requested action was found")
    }

    private fun found(category: IntentCategory, key: CompletionKey, evidenceSummary: String, confidence: Float): CompletionKeyResult =
        CompletionKeyResult(category, CompletionKeyStatus.FOUND, key, evidenceSummary = evidenceSummary, confidence = confidence)

    private fun missing(category: IntentCategory, fields: List<String>, summary: String): CompletionKeyResult =
        CompletionKeyResult(category, CompletionKeyStatus.MISSING, null, missingFields = fields, evidenceSummary = summary, confidence = 0.4f)

    private fun notActionable(category: IntentCategory, summary: String): CompletionKeyResult =
        CompletionKeyResult(category, CompletionKeyStatus.NOT_ACTIONABLE, null, evidenceSummary = summary, confidence = 0.2f)

    private fun firstUsefulLine(text: String): String? = text.lineSequence()
        .map { it.trim() }
        .firstOrNull { it.length >= 4 && !it.contains("http", ignoreCase = true) }

    private fun firstLineContaining(text: String, vararg needles: String): String? = text.lineSequence()
        .map { it.trim() }
        .firstOrNull { line -> needles.any { line.contains(it, ignoreCase = true) } }

    private fun ingredientLines(text: String): List<String> = text.lineSequence()
        .map { it.trim() }
        .filter { line -> line.contains(Regex("\\b(?:cup|cups|tbsp|tsp|tablespoon|teaspoon|gram|grams|oz|ingredient|salt|pepper|oil|flour|sugar)\\b", RegexOption.IGNORE_CASE)) }
        .take(20)
        .toList()

    private fun price(text: String): String? = Regex("[$€£]\\s?\\d+(?:[.,]\\d{2})?").find(text)?.value

    private fun date(text: String): String? = Regex(
        "\\b(?:Jan|Feb|Mar|Apr|May|Jun|Jul|Aug|Sep|Oct|Nov|Dec)[a-z]*\\s+\\d{1,2}(?:,\\s*\\d{4})?(?:\\s+at\\s+\\d{1,2}:\\d{2})?|\\b\\d{1,2}[/-]\\d{1,2}(?:[/-]\\d{2,4})?\\b|\\b(?:today|tomorrow)\\b",
        RegexOption.IGNORE_CASE
    ).find(text)?.value

    private fun expiry(text: String): String? = Regex("(?:exp|expires|valid until)[: ]+([^\\n]+)", RegexOption.IGNORE_CASE)
        .find(text)?.groupValues?.getOrNull(1)?.trim() ?: date(text)

    private fun tracking(text: String): String? = firstLineContaining(text, "tracking", "return", "warranty")

    private fun confirmation(text: String): String? = Regex("(?:confirmation|ticket|reservation)\\s*(?:#|id)?[: ]+([A-Z0-9-]{5,})", RegexOption.IGNORE_CASE)
        .find(text)?.groupValues?.getOrNull(1)

    private fun String.hostLike(): String = removePrefix("https://").removePrefix("http://").substringBefore('/').removePrefix("www.")
}
