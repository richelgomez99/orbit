package com.orbit.app.understanding.engine

import com.orbit.app.capture.YoutubeUrlCanonicalizer
import com.orbit.app.net.ProviderMetadataResolver
import com.orbit.app.net.UnderstandingMetadataFetcher
import com.orbit.app.understanding.domain.EvidenceLevel
import com.orbit.app.understanding.domain.GroundingConstraints
import com.orbit.app.understanding.domain.UnderstandingMode
import com.orbit.app.understanding.domain.UnderstandingResult
import com.orbit.app.understanding.domain.UnderstandingStatus

class SmartUnderstandingEngine(
    private val metadataFetcher: UnderstandingMetadataFetcher,
    private val providerMetadataResolver: ProviderMetadataResolver? = null,
    private val fallback: (suspend () -> UnderstandingResult)? = null
) {
    suspend fun understand(
        captureId: String,
        rawUrl: String,
        auditToken: EscalationAuditToken,
        mode: UnderstandingMode = UnderstandingMode.SMART
    ): UnderstandingResult {
        require(mode == UnderstandingMode.SMART || mode == UnderstandingMode.DEEP) {
            "SmartUnderstandingEngine requires SMART or DEEP mode."
        }
        require(auditToken.captureId == captureId && auditToken.mode == mode) {
            "Escalation must be audit-logged before engine dispatch."
        }

        return runCatching {
            val canonicalUrl = YoutubeUrlCanonicalizer.canonicalize(rawUrl) ?: rawUrl
            val providerResult = providerMetadataResolver?.resolve(canonicalUrl)
            val metadata = metadataFetcher.fetch(canonicalUrl, mode)
            val sourceIdentity = SourceIdentityDomainResolver.resolve(
                canonicalUrl = metadata.canonicalUrl ?: canonicalUrl,
                foregroundPackageName = null
            )
            val title = providerResult?.title ?: metadata.title
            val summary = metadata.description
                ?: providerResult?.readableHtml?.lineSequence()?.firstOrNull { it.isNotBlank() }

            SummaryLimiter.enforce(
                UnderstandingResult(
                    captureId = captureId,
                    status = UnderstandingStatus.READY,
                    mode = mode,
                    title = title,
                    summaryText = summary,
                    groundingConstraints = GroundingConstraints(
                        constraints = listOf("user-triggered-${mode.name.lowercase()}"),
                        evidenceLevel = EvidenceLevel.PARTIAL
                    ),
                    sourceIdentityJson = sourceIdentity.toStorageJson(),
                    contentHashHex = metadata.normalizedContentHashHex,
                    canonicalUrl = metadata.canonicalUrl ?: canonicalUrl,
                    evidenceBundleIds = emptyList(),
                    duplicateMatch = null
                )
            )
        }.getOrElse {
            fallback?.invoke() ?: UnderstandingResult(
                captureId = captureId,
                status = UnderstandingStatus.LIMITED,
                mode = mode,
                title = null,
                summaryText = null,
                groundingConstraints = GroundingConstraints(
                    constraints = listOf("cloud-unavailable"),
                    evidenceLevel = EvidenceLevel.METADATA_ONLY
                ),
                sourceIdentityJson = null,
                contentHashHex = null,
                canonicalUrl = YoutubeUrlCanonicalizer.canonicalize(rawUrl),
                evidenceBundleIds = emptyList(),
                duplicateMatch = null
            )
        }
    }
}
