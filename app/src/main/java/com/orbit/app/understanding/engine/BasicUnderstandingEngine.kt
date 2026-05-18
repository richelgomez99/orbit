package com.orbit.app.understanding.engine

import com.orbit.app.capture.YoutubeUrlCanonicalizer
import com.orbit.app.data.model.AppCategory
import com.orbit.app.understanding.domain.EvidenceLevel
import com.orbit.app.understanding.domain.GroundingConstraints
import com.orbit.app.understanding.domain.UnderstandingMode
import com.orbit.app.understanding.domain.UnderstandingResult
import com.orbit.app.understanding.domain.UnderstandingStatus
import java.net.URI

data class BasicCaptureInput(
    val captureId: String,
    val rawUrl: String? = null,
    val textContent: String? = null,
    val imageUri: String? = null,
    val foregroundPackageName: String? = null,
    val foregroundAppLabel: String? = null,
    val fallbackCategory: AppCategory? = null
)

/** Basic local understanding only: no network, no model, no public fetch. */
class BasicUnderstandingEngine(
    private val duplicateDetectionService: DuplicateDetectionService? = null
) {

    suspend fun understand(input: BasicCaptureInput): UnderstandingResult {
        val canonicalUrl = input.rawUrl?.let { YoutubeUrlCanonicalizer.canonicalize(it) }
        val contentHash = when {
            !input.textContent.isNullOrBlank() -> ContentHasher.normalizedTextHash(input.textContent)
            !input.rawUrl.isNullOrBlank() -> ContentHasher.hash(input.rawUrl.toByteArray(Charsets.UTF_8))
            !input.imageUri.isNullOrBlank() -> ContentHasher.hash(input.imageUri.toByteArray(Charsets.UTF_8))
            else -> null
        }
        val sourceIdentity = SourceIdentityDomainResolver.resolve(
            canonicalUrl = canonicalUrl,
            foregroundPackageName = input.foregroundPackageName,
            foregroundAppLabel = input.foregroundAppLabel,
            fallbackCategory = input.fallbackCategory
        )
        val evidenceLevel = if (canonicalUrl != null || !input.textContent.isNullOrBlank()) {
            EvidenceLevel.METADATA_ONLY
        } else {
            EvidenceLevel.VISUAL_ONLY
        }
        val status = if (canonicalUrl != null || contentHash != null) UnderstandingStatus.READY else UnderstandingStatus.LIMITED
        val title = canonicalUrl?.hostTitle() ?: sourceIdentity.provider ?: sourceIdentity.appLabel
        val summary = when {
            canonicalUrl != null -> "Saved URL from ${title ?: "unknown source"}."
            !input.textContent.isNullOrBlank() -> "Saved text capture."
            !input.imageUri.isNullOrBlank() -> "Saved visual capture."
            else -> null
        }
        val duplicateMatch = duplicateDetectionService?.detectDuplicate(
            captureId = input.captureId,
            contentHashHex = contentHash,
            canonicalUrl = canonicalUrl
        )

        return UnderstandingResult(
            captureId = input.captureId,
            status = status,
            mode = UnderstandingMode.BASIC,
            title = title,
            summaryText = summary,
            groundingConstraints = GroundingConstraints(
                constraints = basicConstraints(evidenceLevel),
                evidenceLevel = evidenceLevel
            ),
            sourceIdentityJson = sourceIdentity.toStorageJson(),
            contentHashHex = contentHash,
            canonicalUrl = canonicalUrl,
            evidenceBundleIds = emptyList(),
            duplicateMatch = duplicateMatch
        ).let(SummaryLimiter::enforce)
    }

    private fun basicConstraints(evidenceLevel: EvidenceLevel): List<String> = when (evidenceLevel) {
        EvidenceLevel.METADATA_ONLY -> listOf("basic-local-only", "Orbit did not fetch or read the full page")
        EvidenceLevel.VISUAL_ONLY -> listOf("basic-local-only", "Orbit read only visual content")
        else -> listOf("basic-local-only")
    }

    private fun String.hostTitle(): String? = runCatching {
        URI(this).host?.removePrefix("www.")
    }.getOrNull()
}
