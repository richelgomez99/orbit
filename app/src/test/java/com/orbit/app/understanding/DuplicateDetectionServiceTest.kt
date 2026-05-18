package com.orbit.app.understanding

import com.orbit.app.data.dao.CaptureUnderstandingDao
import com.orbit.app.data.entity.CaptureUnderstandingEntity
import com.orbit.app.understanding.domain.MatchType
import com.orbit.app.understanding.engine.DuplicateDetectionService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DuplicateDetectionServiceTest {

    @Test
    fun detectDuplicate_matchesContentHash() = runTest {
        val service = DuplicateDetectionService(
            FakeCaptureUnderstandingDao(
                rows = listOf(row("existing", contentHashHex = "hash-a"))
            )
        )

        val match = service.detectDuplicate("new", "hash-a", null)

        assertEquals("existing", match?.existingCaptureId)
        assertEquals(MatchType.EXACT_HASH, match?.matchType)
    }

    @Test
    fun detectDuplicate_matchesCanonicalUrl() = runTest {
        val service = DuplicateDetectionService(
            FakeCaptureUnderstandingDao(
                rows = listOf(row("existing", canonicalUrl = "https://example.com/story"))
            )
        )

        val match = service.detectDuplicate("new", null, "https://example.com/story")

        assertEquals("existing", match?.existingCaptureId)
        assertEquals(MatchType.CANONICAL_URL, match?.matchType)
    }

    @Test
    fun detectDuplicate_returnsBothWhenHashAndUrlMatch() = runTest {
        val service = DuplicateDetectionService(
            FakeCaptureUnderstandingDao(
                rows = listOf(row("existing", contentHashHex = "hash-a", canonicalUrl = "https://example.com/story"))
            )
        )

        val match = service.detectDuplicate("new", "hash-a", "https://example.com/story")

        assertEquals("existing", match?.existingCaptureId)
        assertEquals(MatchType.BOTH, match?.matchType)
    }

    @Test
    fun detectDuplicate_excludesSelfAndInvalidatedRows() = runTest {
        val service = DuplicateDetectionService(
            FakeCaptureUnderstandingDao(
                rows = listOf(
                    row("new", contentHashHex = "hash-a"),
                    row("invalid", contentHashHex = "hash-a", invalidatedAt = 10L)
                )
            )
        )

        assertNull(service.detectDuplicate("new", "hash-a", null))
    }

    private fun row(
        captureId: String,
        contentHashHex: String? = null,
        canonicalUrl: String? = null,
        invalidatedAt: Long? = null
    ) = CaptureUnderstandingEntity(
        captureId = captureId,
        mode = "BASIC",
        status = "READY",
        title = null,
        summaryText = null,
        groundingConstraintsJson = "{}",
        contentHashHex = contentHashHex,
        canonicalUrl = canonicalUrl,
        sourceIdentityJson = null,
        createdAt = 1L,
        updatedAt = 1L,
        invalidatedAt = invalidatedAt
    )

    private class FakeCaptureUnderstandingDao(
        private val rows: List<CaptureUnderstandingEntity>
    ) : CaptureUnderstandingDao {
        override suspend fun upsert(entity: CaptureUnderstandingEntity) = Unit
        override fun getByCapture(captureId: String): Flow<CaptureUnderstandingEntity?> =
            flowOf(rows.firstOrNull { it.captureId == captureId })
        override suspend fun getByContentHash(hex: String): List<CaptureUnderstandingEntity> =
            rows.filter { it.contentHashHex == hex && it.invalidatedAt == null }
        override suspend fun getByCanonicalUrl(canonicalUrl: String): List<CaptureUnderstandingEntity> =
            rows.filter { it.canonicalUrl == canonicalUrl && it.invalidatedAt == null }
        override suspend fun markInvalidated(captureId: String, invalidatedAt: Long) = Unit
        override suspend fun deleteByCapture(captureId: String) = Unit
    }
}
