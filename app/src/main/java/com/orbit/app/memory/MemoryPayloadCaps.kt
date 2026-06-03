package com.orbit.app.memory

object MemoryPayloadCaps {
    const val TITLE_MAX = 140
    const val SUMMARY_MAX = 500
    const val EVIDENCE_EXCERPT_MAX = 240
    const val EVIDENCE_MAX = 5
    const val TAG_MAX = 64
    const val TAGS_MAX = 20
    const val COMPACT_TEXT_MAX = 1_600

    val bannedKeys: Set<String> = setOf(
        "rawScreenshot",
        "imageBytes",
        "rawOcr",
        "ocrText",
        "rawHtml",
        "clipboardText",
        "prompt",
        "modelResponse",
        "auditLog",
        "accessToken",
        "refreshToken",
        "mongodbUri"
    )

    fun cap(value: String?, max: Int): String? =
        value
            ?.trim()
            ?.replace(Regex("\\s+"), " ")
            ?.take(max)
            ?.takeIf { it.isNotBlank() }

    fun isBannedKey(key: String): Boolean =
        key in bannedKeys ||
            key.endsWith("Secret", ignoreCase = true) ||
            key.endsWith("Token", ignoreCase = true) ||
            key.endsWith("Key", ignoreCase = true)
}
