package com.orbit.app.understanding.engine

import com.orbit.app.understanding.domain.SourceEvidenceBasis
import com.orbit.app.understanding.domain.SourceTrustLevel

data class SourceEvidenceRank(
    val trustLevel: SourceTrustLevel,
    val evidenceBasis: SourceEvidenceBasis,
    val confidence: Float,
    val label: String
)

class SourceEvidenceRanker {
    fun rank(
        canonicalUrl: String?,
        foregroundPackageName: String?,
        foregroundAppLabel: String?,
        visibleText: String?,
        modelInferredLabel: String? = null
    ): SourceEvidenceRank = when {
        !canonicalUrl.isNullOrBlank() -> SourceEvidenceRank(
            trustLevel = SourceTrustLevel.HIGH,
            evidenceBasis = SourceEvidenceBasis.URL,
            confidence = 0.95f,
            label = canonicalUrl.hostLike()
        )
        !foregroundPackageName.isNullOrBlank() || !foregroundAppLabel.isNullOrBlank() -> SourceEvidenceRank(
            trustLevel = SourceTrustLevel.HIGH,
            evidenceBasis = SourceEvidenceBasis.FOREGROUND_APP,
            confidence = 0.9f,
            label = foregroundAppLabel ?: foregroundPackageName.orEmpty()
        )
        visibleText.hasLikelyLogo() -> SourceEvidenceRank(
            trustLevel = SourceTrustLevel.MEDIUM,
            evidenceBasis = SourceEvidenceBasis.OCR_LOGO,
            confidence = 0.7f,
            label = visibleText!!.lineSequence().first().trim()
        )
        !visibleText.isNullOrBlank() -> SourceEvidenceRank(
            trustLevel = SourceTrustLevel.LOW_MEDIUM,
            evidenceBasis = SourceEvidenceBasis.OCR_TEXT,
            confidence = 0.55f,
            label = visibleText.lineSequence().firstOrNull()?.trim().orEmpty()
        )
        !modelInferredLabel.isNullOrBlank() -> SourceEvidenceRank(
            trustLevel = SourceTrustLevel.LOW,
            evidenceBasis = SourceEvidenceBasis.MODEL_INFERENCE,
            confidence = 0.35f,
            label = modelInferredLabel
        )
        else -> SourceEvidenceRank(
            trustLevel = SourceTrustLevel.UNKNOWN,
            evidenceBasis = SourceEvidenceBasis.UNKNOWN,
            confidence = 0f,
            label = "Unknown"
        )
    }

    private fun String?.hasLikelyLogo(): Boolean = !this.isNullOrBlank() &&
        lineSequence().firstOrNull()?.trim().orEmpty().length in 2..24

    private fun String.hostLike(): String = removePrefix("https://").removePrefix("http://").substringBefore('/').removePrefix("www.")
}
