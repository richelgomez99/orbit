package com.orbit.app.understanding.domain

import com.orbit.app.data.model.AppCategory

/** Domain-level source identity used for provenance, distinct from UI glyph helpers. */
data class SourceIdentity(
    val provider: String?,
    val appLabel: String?,
    val category: AppCategory,
    val confidence: Float,
    val trustLevel: SourceTrustLevel = SourceTrustLevel.UNKNOWN,
    val evidenceBasis: SourceEvidenceBasis = SourceEvidenceBasis.UNKNOWN
) {
    fun toStorageJson(): String = buildString {
        append('{')
        append("\"provider\":")
        append(provider?.let { "\"${it.escapeJson()}\"" } ?: "null")
        append(',')
        append("\"appLabel\":")
        append(appLabel?.let { "\"${it.escapeJson()}\"" } ?: "null")
        append(',')
        append("\"category\":\"")
        append(category.name)
        append("\",")
        append("\"confidence\":")
        append(confidence)
        append(',')
        append("\"trustLevel\":\"")
        append(trustLevel.name)
        append("\",")
        append("\"evidenceBasis\":\"")
        append(evidenceBasis.name)
        append("\"")
        append('}')
    }
}

enum class SourceTrustLevel {
    HIGH,
    MEDIUM,
    LOW_MEDIUM,
    LOW,
    UNKNOWN
}

enum class SourceEvidenceBasis {
    URL,
    DEEPLINK,
    FOREGROUND_APP,
    OCR_LOGO,
    OCR_TEXT,
    MODEL_INFERENCE,
    CATEGORY_ONLY,
    UNKNOWN
}

data class DomainBrand(
    val provider: String,
    val category: AppCategory
)

object DomainBrandMap {
    private val exactHosts: Map<String, DomainBrand> = mapOf(
        "youtube.com" to DomainBrand("YouTube", AppCategory.VIDEO),
        "youtube-nocookie.com" to DomainBrand("YouTube", AppCategory.VIDEO),
        "youtu.be" to DomainBrand("YouTube", AppCategory.VIDEO),
        "twitter.com" to DomainBrand("Twitter/X", AppCategory.SOCIAL),
        "x.com" to DomainBrand("Twitter/X", AppCategory.SOCIAL),
        "instagram.com" to DomainBrand("Instagram", AppCategory.SOCIAL),
        "reddit.com" to DomainBrand("Reddit", AppCategory.SOCIAL),
        "github.com" to DomainBrand("GitHub", AppCategory.READING),
        "linkedin.com" to DomainBrand("LinkedIn", AppCategory.SOCIAL),
        "tiktok.com" to DomainBrand("TikTok", AppCategory.SOCIAL),
        "spotify.com" to DomainBrand("Spotify", AppCategory.VIDEO),
        "substack.com" to DomainBrand("Substack", AppCategory.READING),
        "medium.com" to DomainBrand("Medium", AppCategory.READING),
        "bbc.com" to DomainBrand("BBC", AppCategory.READING),
        "bbc.co.uk" to DomainBrand("BBC", AppCategory.READING),
        "nytimes.com" to DomainBrand("NYT", AppCategory.READING)
    )

    fun lookup(host: String?): DomainBrand? {
        val normalized = host
            ?.trim()
            ?.lowercase()
            ?.removePrefix("www.")
            ?.trimEnd('.')
            ?: return null
        return exactHosts[normalized]
            ?: exactHosts.entries.firstOrNull { (domain, _) -> normalized.endsWith(".$domain") }?.value
    }
}

private fun String.escapeJson(): String = buildString {
    for (ch in this@escapeJson) {
        when (ch) {
            '\\' -> append("\\\\")
            '"' -> append("\\\"")
            '\n' -> append("\\n")
            '\r' -> append("\\r")
            '\t' -> append("\\t")
            else -> append(ch)
        }
    }
}
