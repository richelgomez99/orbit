package com.orbit.app.understanding.domain

import org.json.JSONArray
import org.json.JSONObject

data class SourceIdentity(
    val sourceAppLabel: String?,
    val appCategory: String?,
    val canonicalUrl: String?,
    val confidence: Float,
    val evidenceBasis: List<String>
) {
    fun toCompactJson(): String = JSONObject().apply {
        sourceAppLabel?.trim()?.takeIf { it.isNotBlank() }?.let { put("label", it.take(MAX_LABEL_CHARS)) }
        appCategory?.trim()?.takeIf { it.isNotBlank() }?.let { put("category", it.take(MAX_LABEL_CHARS)) }
        canonicalUrl?.trim()?.takeIf { it.isNotBlank() }?.let { put("canonicalUrl", it.take(MAX_URL_CHARS)) }
        put("confidence", confidence.coerceIn(0f, 1f).toDouble())
        put(
            "evidenceBasis",
            JSONArray(evidenceBasis.map { it.trim().take(MAX_BASIS_CHARS) }.filter { it.isNotBlank() })
        )
    }.toString().take(CompactEvidencePayload.MAX_JSON_CHARS)

    companion object {
        private const val MAX_LABEL_CHARS = 120
        private const val MAX_URL_CHARS = 512
        private const val MAX_BASIS_CHARS = 80

        fun fromLocalSignals(
            sourceAppLabel: String?,
            appCategory: String?,
            canonicalUrl: String?
        ): SourceIdentity {
            val normalizedAppCategory = appCategory?.trim()?.takeIf { it.isNotBlank() && it != "UNKNOWN_SOURCE" }
            val basis = buildList {
                if (!sourceAppLabel.isNullOrBlank()) add("source_app_label")
                if (normalizedAppCategory != null) add("app_category")
                if (!canonicalUrl.isNullOrBlank()) add("canonical_url")
            }
            val confidence = when {
                !canonicalUrl.isNullOrBlank() && !sourceAppLabel.isNullOrBlank() -> 0.9f
                !canonicalUrl.isNullOrBlank() -> 0.78f
                !sourceAppLabel.isNullOrBlank() -> 0.72f
                normalizedAppCategory != null -> 0.58f
                else -> 0.2f
            }
            return SourceIdentity(
                sourceAppLabel = sourceAppLabel?.trim()?.takeIf { it.isNotBlank() },
                appCategory = normalizedAppCategory,
                canonicalUrl = canonicalUrl?.trim()?.takeIf { it.isNotBlank() },
                confidence = confidence,
                evidenceBasis = basis
            )
        }
    }
}