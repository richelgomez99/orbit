package com.capsule.app.understanding

import com.capsule.app.net.ProviderMetadataResolver
import java.net.URI
import java.util.Locale

object SourceIdentityResolver {
    const val RESOLVER_VERSION = 1

    data class Input(
        val urls: List<String>,
        val foregroundAppLabel: String?,
        val genericCategory: String?,
        val evidenceIds: List<String>
    )

    data class Resolved(
        val providerKey: String?,
        val providerLabel: String?,
        val originAppLabel: String?,
        val genericCategory: String?,
        val displayLabel: String,
        val secondaryLabel: String?,
        val glyphKind: UnderstandingSourceGlyphKind,
        val confidence: Float,
        val evidenceIds: List<String>,
        val limitations: List<LimitationCode>
    )

    fun resolve(input: Input): Resolved {
        val originApp = input.foregroundAppLabel?.trim()?.takeIf { it.isNotBlank() }
        val youtubeUrl = input.urls.firstOrNull { ProviderMetadataResolver.isYouTubeUrl(it.withDefaultScheme()) }
        if (youtubeUrl != null) {
            return Resolved(
                providerKey = "youtube",
                providerLabel = "YouTube",
                originAppLabel = originApp,
                genericCategory = input.genericCategory,
                displayLabel = "YouTube",
                secondaryLabel = originApp?.takeUnless { it.equals("YouTube", ignoreCase = true) }?.let { "via $it" },
                glyphKind = UnderstandingSourceGlyphKind.PROVIDER,
                confidence = 0.95f,
                evidenceIds = input.evidenceIds,
                limitations = emptyList()
            )
        }

        val host = input.urls.firstOrNull()?.hostLabel()
        if (!host.isNullOrBlank()) {
            return Resolved(
                providerKey = null,
                providerLabel = host,
                originAppLabel = originApp,
                genericCategory = input.genericCategory,
                displayLabel = host,
                secondaryLabel = originApp?.let { "via $it" },
                glyphKind = UnderstandingSourceGlyphKind.PROVIDER,
                confidence = 0.75f,
                evidenceIds = input.evidenceIds,
                limitations = listOf(LimitationCode.METADATA_ONLY)
            )
        }

        if (!originApp.isNullOrBlank()) {
            return Resolved(
                providerKey = null,
                providerLabel = originApp,
                originAppLabel = originApp,
                genericCategory = input.genericCategory,
                displayLabel = originApp,
                secondaryLabel = input.genericCategory?.categoryLabel(),
                glyphKind = UnderstandingSourceGlyphKind.APP,
                confidence = 0.7f,
                evidenceIds = input.evidenceIds,
                limitations = listOf(LimitationCode.LOW_CONFIDENCE)
            )
        }

        val categoryLabel = input.genericCategory?.categoryLabel()
        if (!categoryLabel.isNullOrBlank()) {
            return Resolved(
                providerKey = null,
                providerLabel = null,
                originAppLabel = null,
                genericCategory = input.genericCategory,
                displayLabel = categoryLabel,
                secondaryLabel = null,
                glyphKind = UnderstandingSourceGlyphKind.GENERIC_CATEGORY,
                confidence = 0.45f,
                evidenceIds = input.evidenceIds,
                limitations = listOf(LimitationCode.LOW_CONFIDENCE)
            )
        }

        return Resolved(
            providerKey = null,
            providerLabel = null,
            originAppLabel = null,
            genericCategory = null,
            displayLabel = "Unknown source",
            secondaryLabel = null,
            glyphKind = UnderstandingSourceGlyphKind.UNKNOWN,
            confidence = 0.2f,
            evidenceIds = input.evidenceIds,
            limitations = listOf(LimitationCode.LOW_CONFIDENCE)
        )
    }

    private fun String.withDefaultScheme(): String =
        if (contains("://")) this else "https://$this"

    private fun String.hostLabel(): String? = runCatching {
        URI(withDefaultScheme()).host
            ?.lowercase(Locale.ROOT)
            ?.trimEnd('.')
            ?.removePrefix("www.")
    }.getOrNull()

    private fun String.categoryLabel(): String =
        lowercase(Locale.ROOT)
            .replace('_', ' ')
            .replaceFirstChar { it.titlecase(Locale.ROOT) }
}