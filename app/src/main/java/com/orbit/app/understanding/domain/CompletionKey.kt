package com.orbit.app.understanding.domain

import org.json.JSONArray
import org.json.JSONObject

enum class CompletionKeyKind {
    PRICE,
    ORDER_ID,
    DATE,
    COUPON_CODE,
    ADDRESS,
    INGREDIENT,
    URL,
    QR_PAYLOAD
}

data class CompletionKey(
    val kind: CompletionKeyKind,
    val label: String,
    val source: String,
    val confidence: Float,
    val excerpt: String?
) {
    fun toCompactJson(): String = JSONObject().apply {
        put("kind", kind.name)
        put("label", label.trim().take(MAX_LABEL_CHARS))
        put("source", source.trim().take(MAX_SOURCE_CHARS))
        put("confidence", confidence.coerceIn(0f, 1f).toDouble())
        excerpt?.trim()?.takeIf { it.isNotBlank() }?.let {
            put("excerpt", CompactEvidencePayload.cappedExcerpt(it))
        }
    }.toString().take(CompactEvidencePayload.MAX_JSON_CHARS)

    companion object {
        private const val MAX_LABEL_CHARS = 120
        private const val MAX_SOURCE_CHARS = 80

        fun listToCompactJson(keys: List<CompletionKey>): String = JSONArray(
            keys.take(MAX_KEYS).map { JSONObject(it.toCompactJson()) }
        ).toString().take(CompactEvidencePayload.MAX_JSON_CHARS)

        private const val MAX_KEYS = 8
    }
}