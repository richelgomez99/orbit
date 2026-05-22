package com.orbit.app.understanding

import com.orbit.app.understanding.domain.CompletionKey
import com.orbit.app.understanding.domain.CompletionKeyKind

class CompletionKeyExtractor {

    fun extract(text: String?, canonicalUrl: String?): List<CompletionKey> {
        val sourceText = text.orEmpty()
        val keys = buildList {
            find(QR_PAYLOAD, sourceText)?.let { add(key(CompletionKeyKind.QR_PAYLOAD, it, "local_regex", 0.9f, sourceText)) }
            find(COUPON_CODE, sourceText)?.let { add(key(CompletionKeyKind.COUPON_CODE, it, "local_regex", 0.86f, sourceText)) }
            find(ORDER_ID, sourceText)?.let { add(key(CompletionKeyKind.ORDER_ID, it, "local_regex", 0.84f, sourceText)) }
            find(PRICE, sourceText)?.let { add(key(CompletionKeyKind.PRICE, it, "local_regex", 0.78f, sourceText)) }
            find(DATE, sourceText)?.let { add(key(CompletionKeyKind.DATE, it, "local_regex", 0.72f, sourceText)) }
            find(TIME, sourceText)?.let { add(key(CompletionKeyKind.DATE, it, "local_regex", 0.68f, sourceText)) }
            find(ADDRESS, sourceText)?.let { add(key(CompletionKeyKind.ADDRESS, it, "local_regex", 0.7f, sourceText)) }
            find(INGREDIENT, sourceText)?.let { add(key(CompletionKeyKind.INGREDIENT, it, "local_regex", 0.66f, sourceText)) }
            val url = find(URL, sourceText) ?: canonicalUrl?.trim()?.takeIf { it.isNotBlank() }
            url?.let { add(key(CompletionKeyKind.URL, it, if (it == canonicalUrl) "canonical_url" else "local_regex", 0.82f, sourceText)) }
        }
        return keys.distinctBy { it.kind to it.label }.take(MAX_KEYS)
    }

    fun primaryKey(text: String?, canonicalUrl: String?): CompletionKey? = extract(text, canonicalUrl).firstOrNull()

    private fun find(regex: Regex, text: String): String? {
        val match = regex.find(text) ?: return null
        val captured = match.groupValues.drop(1).firstOrNull { it.isNotBlank() }
        return (captured ?: match.value).trim().take(MAX_LABEL_CHARS)
    }

    private fun key(
        kind: CompletionKeyKind,
        label: String,
        source: String,
        confidence: Float,
        text: String
    ): CompletionKey = CompletionKey(
        kind = kind,
        label = label,
        source = source,
        confidence = confidence,
        excerpt = localExcerpt(text, label)
    )

    private fun localExcerpt(text: String, label: String): String? {
        if (text.isBlank()) return null
        val index = text.indexOf(label, ignoreCase = true).takeIf { it >= 0 } ?: return text.take(MAX_EXCERPT_CHARS)
        val start = (index - EXCERPT_CONTEXT_CHARS).coerceAtLeast(0)
        val end = (index + label.length + EXCERPT_CONTEXT_CHARS).coerceAtMost(text.length)
        return text.substring(start, end).trim()
    }

    private companion object {
        const val MAX_KEYS = 8
        const val MAX_LABEL_CHARS = 120
        const val MAX_EXCERPT_CHARS = 240
        const val EXCERPT_CONTEXT_CHARS = 60

        val PRICE = Regex("""(?<![A-Za-z0-9])(?:\$\s?\d{1,4}(?:,\d{3})*(?:\.\d{2})?|\d+(?:\.\d{2})?\s?(?:USD|usd))(?![A-Za-z0-9])""")
        val ORDER_ID = Regex("""(?i)\b(?:order|confirmation|booking|reservation|tracking)\s*(?:#|no\.?|number|id)?\s*[:#-]?\s*([A-Z0-9][A-Z0-9-]{4,})\b""")
        val DATE = Regex("""(?i)\b(?:jan|feb|mar|apr|may|jun|jul|aug|sep|sept|oct|nov|dec)[a-z]*\s+\d{1,2}(?:,\s*\d{4})?|\b\d{1,2}/\d{1,2}/\d{2,4}\b|\b\d{4}-\d{2}-\d{2}\b""")
        val TIME = Regex("""(?i)\b\d{1,2}(?::\d{2})?\s?(?:am|pm)\b""")
        val COUPON_CODE = Regex("""(?i)\b(?:coupon|promo|discount)?\s*code\s*[:#-]?\s*([A-Z0-9][A-Z0-9-]{3,})\b""")
        val ADDRESS = Regex("""(?i)\b\d{1,6}\s+[A-Za-z0-9 .'\-]+\s(?:street|st|avenue|ave|road|rd|boulevard|blvd|drive|dr|lane|ln|way|court|ct)\b""")
        val INGREDIENT = Regex("""(?i)\b(?:\d+(?:/\d+)?\s*)?(?:cups?|tbsp|tablespoons?|tsp|teaspoons?|grams?|g|oz)\s+[A-Za-z][A-Za-z ]{2,40}\b""")
        val URL = Regex("""https?://[^\s)\]}>"]+""")
        val QR_PAYLOAD = Regex("""(?i)\b(?:qr|barcode|payload|scan)\s*(?:code|payload)?\s*[:=-]\s*(\S.{2,80})""")
    }
}