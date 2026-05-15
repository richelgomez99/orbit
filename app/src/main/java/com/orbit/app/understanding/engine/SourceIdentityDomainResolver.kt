package com.orbit.app.understanding.engine

import com.orbit.app.capture.AppCategoryDictionary
import com.orbit.app.data.model.AppCategory
import com.orbit.app.understanding.domain.DomainBrandMap
import com.orbit.app.understanding.domain.SourceEvidenceBasis
import com.orbit.app.understanding.domain.SourceIdentity
import com.orbit.app.understanding.domain.SourceTrustLevel
import java.net.URI
import java.util.Locale

/** Resolves durable domain-level capture provenance without implying a provider from category alone. */
object SourceIdentityDomainResolver {

    fun resolve(
        canonicalUrl: String?,
        foregroundPackageName: String?,
        foregroundAppLabel: String? = AppCategoryDictionary.displayName(foregroundPackageName),
        fallbackCategory: AppCategory? = null
    ): SourceIdentity {
        val host = canonicalUrl.hostOrNull()
        val brand = DomainBrandMap.lookup(host)
        if (brand != null) {
            return SourceIdentity(
                provider = brand.provider,
                appLabel = foregroundAppLabel,
                category = brand.category,
                confidence = 0.95f,
                trustLevel = SourceTrustLevel.HIGH,
                evidenceBasis = SourceEvidenceBasis.URL
            )
        }

        val appCategory = AppCategoryDictionary.categorize(foregroundPackageName)
        if (!foregroundAppLabel.isNullOrBlank()) {
            return SourceIdentity(
                provider = null,
                appLabel = foregroundAppLabel,
                category = appCategory,
                confidence = 0.8f,
                trustLevel = SourceTrustLevel.HIGH,
                evidenceBasis = SourceEvidenceBasis.FOREGROUND_APP
            )
        }

        val category = fallbackCategory
            ?: if (host != null) AppCategory.OTHER else appCategory
        return SourceIdentity(
            provider = null,
            appLabel = null,
            category = category,
            confidence = if (category == AppCategory.UNKNOWN_SOURCE) 0.0f else 0.45f,
            trustLevel = if (category == AppCategory.UNKNOWN_SOURCE) SourceTrustLevel.UNKNOWN else SourceTrustLevel.LOW,
            evidenceBasis = if (category == AppCategory.UNKNOWN_SOURCE) SourceEvidenceBasis.UNKNOWN else SourceEvidenceBasis.CATEGORY_ONLY
        )
    }

    private fun String?.hostOrNull(): String? {
        val value = this?.trim().orEmpty()
        if (value.isBlank()) return null
        val withScheme = if (value.startsWith("http://") || value.startsWith("https://")) value else "https://$value"
        return runCatching { URI(withScheme).host }
            .getOrNull()
            ?.lowercase(Locale.ROOT)
            ?.removePrefix("www.")
            ?.trimEnd('.')
    }
}
