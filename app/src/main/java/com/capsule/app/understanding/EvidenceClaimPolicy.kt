package com.capsule.app.understanding

object EvidenceClaimPolicy {
    enum class ClaimKind { FULL_CONTENT_SUMMARY, METADATA_SUMMARY, VISUAL_SUMMARY, LIMITATION_ONLY }

    fun canSupport(kind: EvidenceKind, status: EvidenceStatus, claim: ClaimKind): Boolean {
        if (status !in setOf(EvidenceStatus.READY, EvidenceStatus.PARTIAL, EvidenceStatus.LIMITED)) {
            return claim == ClaimKind.LIMITATION_ONLY
        }
        return when (claim) {
            ClaimKind.FULL_CONTENT_SUMMARY -> kind in setOf(
                EvidenceKind.SAVED_TEXT,
                EvidenceKind.READABLE_PUBLIC_TEXT,
                EvidenceKind.OCR_TEXT,
                EvidenceKind.TRANSCRIPT
            ) && status == EvidenceStatus.READY
            ClaimKind.METADATA_SUMMARY -> kind in setOf(
                EvidenceKind.SAVED_URL,
                EvidenceKind.URL_METADATA,
                EvidenceKind.MEDIA_METADATA,
                EvidenceKind.DOCUMENT_METADATA
            )
            ClaimKind.VISUAL_SUMMARY -> kind in setOf(
                EvidenceKind.OCR_TEXT,
                EvidenceKind.SCREENSHOT_REFERENCE
            )
            ClaimKind.LIMITATION_ONLY -> true
        }
    }

    fun limitationsFor(kind: EvidenceKind, status: EvidenceStatus): List<LimitationCode> = buildList {
        when (status) {
            EvidenceStatus.FAILED -> add(LimitationCode.FAILED_FETCH)
            EvidenceStatus.SUPPRESSED -> add(LimitationCode.DOMAIN_SUPPRESSED)
            EvidenceStatus.INVALIDATED -> add(LimitationCode.LOW_CONFIDENCE)
            EvidenceStatus.PARTIAL,
            EvidenceStatus.LIMITED -> add(LimitationCode.LOW_CONFIDENCE)
            EvidenceStatus.READY -> Unit
        }
        when (kind) {
            EvidenceKind.SAVED_URL,
            EvidenceKind.URL_METADATA,
            EvidenceKind.MEDIA_METADATA,
            EvidenceKind.DOCUMENT_METADATA -> add(LimitationCode.METADATA_ONLY)
            EvidenceKind.SCREENSHOT_REFERENCE -> add(LimitationCode.VISUAL_ONLY)
            EvidenceKind.FETCH_LIMITATION -> add(LimitationCode.FAILED_FETCH)
            EvidenceKind.MODEL_LIMITATION -> add(LimitationCode.CLOUD_DISABLED)
            else -> Unit
        }
    }.distinct()
}