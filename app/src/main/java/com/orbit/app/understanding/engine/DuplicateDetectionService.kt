package com.orbit.app.understanding.engine

import com.orbit.app.data.dao.CaptureUnderstandingDao
import com.orbit.app.understanding.domain.DuplicateMatch
import com.orbit.app.understanding.domain.MatchType

class DuplicateDetectionService(
    private val captureUnderstandingDao: CaptureUnderstandingDao
) {

    suspend fun detectDuplicate(
        captureId: String,
        contentHashHex: String?,
        canonicalUrl: String?
    ): DuplicateMatch? {
        val hashMatch = contentHashHex
            ?.takeIf { it.isNotBlank() }
            ?.let { hash ->
                captureUnderstandingDao.getByContentHash(hash)
                    .firstOrNull { it.captureId != captureId && it.invalidatedAt == null }
            }

        val urlMatch = canonicalUrl
            ?.takeIf { it.isNotBlank() }
            ?.let { url ->
                captureUnderstandingDao.getByCanonicalUrl(url)
                    .firstOrNull { it.captureId != captureId && it.invalidatedAt == null }
            }

        return when {
            hashMatch != null && urlMatch != null -> DuplicateMatch(
                existingCaptureId = hashMatch.captureId,
                matchType = MatchType.BOTH
            )
            hashMatch != null -> DuplicateMatch(hashMatch.captureId, MatchType.EXACT_HASH)
            urlMatch != null -> DuplicateMatch(urlMatch.captureId, MatchType.CANONICAL_URL)
            else -> null
        }
    }
}
