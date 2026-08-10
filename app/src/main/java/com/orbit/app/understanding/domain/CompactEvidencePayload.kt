package com.orbit.app.understanding.domain

object CompactEvidencePayload {
    const val MAX_TITLE_CHARS = 120
    const val MAX_SUMMARY_CHARS = 280
    const val MAX_ACTION_LABEL_CHARS = 80
    const val MAX_JSON_CHARS = 2_048
    const val MAX_EXCERPT_CHARS = 240

    val allowedKeys: Set<String> = setOf(
        "kind",
        "label",
        "source",
        "confidence",
        "excerpt",
        "hash",
        "reason",
        "createdAt",
        "reviewedAt"
    )

    fun isAllowedKey(key: String): Boolean = key in allowedKeys

    fun cappedExcerpt(excerpt: String): String =
        excerpt.trim().take(MAX_EXCERPT_CHARS)
}