package com.orbit.app.understanding.domain

import org.json.JSONArray
import org.json.JSONObject

data class GroundingConstraints(
    val mode: UnderstandingMode,
    val allowedSignals: List<String>,
    val missingSignals: List<String>,
    val forbiddenSignals: List<String>
) {
    fun toCompactJson(): String = JSONObject().apply {
        put("mode", mode.name)
        put("allowedSignals", JSONArray(allowedSignals))
        put("missingSignals", JSONArray(missingSignals))
        put("forbiddenSignals", JSONArray(forbiddenSignals))
    }.toString().take(CompactEvidencePayload.MAX_JSON_CHARS)

    companion object {
        private val basicForbiddenSignals = listOf(
            "cloud_llm",
            "public_url_fetch",
            "oembed",
            "readability",
            "browser_automation",
            "vlm"
        )

        fun basic(
            hasText: Boolean,
            hasSourceIdentity: Boolean,
            hasCanonicalUrl: Boolean
        ): GroundingConstraints {
            val missing = buildList {
                if (!hasText) add("local_text_or_ocr")
                if (!hasSourceIdentity) add("source_identity")
                if (!hasCanonicalUrl) add("canonical_url")
            }
            return GroundingConstraints(
                mode = UnderstandingMode.BASIC,
                allowedSignals = listOf(
                    "foreground_app_label",
                    "app_category",
                    "capture_timestamp",
                    "existing_local_text",
                    "existing_canonical_url",
                    "local_regex"
                ),
                missingSignals = missing,
                forbiddenSignals = basicForbiddenSignals
            )
        }
    }
}