package com.orbit.app.understanding

import com.orbit.app.data.dao.ActiveIntentDao
import com.orbit.app.data.dao.CaptureUnderstandingDao
import com.orbit.app.data.dao.InvalidationRecordDao
import com.orbit.app.data.entity.ActiveIntentEntity
import com.orbit.app.data.entity.CaptureUnderstandingEntity
import com.orbit.app.data.entity.InvalidationRecordEntity
import com.orbit.app.understanding.domain.ActiveIntentStatus
import com.orbit.app.understanding.domain.CompletionKeyStatus
import com.orbit.app.understanding.domain.IntentCategory
import com.orbit.app.understanding.domain.InvalidationReason
import com.orbit.app.understanding.domain.ResolutionReason
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ActiveIntentProjectionTest {

    @Test
    fun projectorCreatesActiveIntentForActionableUnderstanding() {
        val result = basicResult(
            category = IntentCategory.BUY_LATER_PRODUCT,
            completionKeyStatus = CompletionKeyStatus.FOUND
        )
        val projection = ActiveIntentProjector { "intent-${it.captureId}" }.project(result, NOW)

        assertEquals(ProjectionOutcome.ACTIVE, projection.outcome)
        val entity = requireNotNull(projection.entity)
        assertEquals("intent-capture-1", entity.intentId)
        assertEquals(IntentCategory.BUY_LATER_PRODUCT, entity.intentType)
        assertEquals(ActiveIntentStatus.ACTIVE, entity.status)
        assertEquals("buy_or_skip", entity.primaryAction)
        assertEquals(false, entity.userConfirmed)
        assertEquals(NOW, entity.createdAt)
        assertNotNull(entity.completionKeyJson)
        assertEquals("COMPLETION_KEY", org.json.JSONObject(entity.primaryEvidenceJson).getString("kind"))
        assertEquals(true, org.json.JSONObject(entity.primaryEvidenceJson).has("excerpt"))
    }

    @Test
    fun projectorKeepsMaybeOldAsQueueableTriageCategory() {
        val projection = ActiveIntentProjector { "intent-old" }.project(
            basicResult(
                category = IntentCategory.MAYBE_OLD_OR_INACTIVE,
                completionKeyStatus = CompletionKeyStatus.NOT_ACTIONABLE,
                completionKeyJson = null
            ),
            NOW
        )

        assertEquals(ProjectionOutcome.MAYBE_OLD_OR_INACTIVE, projection.outcome)
        val entity = requireNotNull(projection.entity)
        assertEquals(IntentCategory.MAYBE_OLD_OR_INACTIVE, entity.intentType)
        assertEquals("archive_or_keep", entity.primaryAction)
        assertEquals(CompletionKeyStatus.NOT_ACTIONABLE, entity.completionKeyStatus)
    }

    @Test
    fun projectorCanDropNonActionableUnknownRows() {
        val projection = ActiveIntentProjector { "unused" }.project(
            basicResult(
                category = IntentCategory.UNKNOWN,
                completionKeyStatus = CompletionKeyStatus.NOT_ACTIONABLE,
                completionKeyJson = null
            ),
            NOW
        )

        assertEquals(ProjectionOutcome.NON_ACTIONABLE, projection.outcome)
        assertNull(projection.entity)
    }

    @Test
    fun resolverMapsResolutionReasonsToLifecycleStates() {
        val resolver = ActiveIntentResolver()
        val entity = activeIntent()

        assertEquals(ActiveIntentStatus.RESOLVED, resolver.resolve(entity, ResolutionReason.BOUGHT, NOW).status)
        assertEquals(ActiveIntentStatus.ARCHIVED, resolver.resolve(entity, ResolutionReason.NOT_INTERESTED, NOW).status)
        assertEquals(ActiveIntentStatus.INVALIDATED, resolver.resolve(entity, ResolutionReason.SOURCE_DELETED, NOW, userConfirmed = false).status)
        assertEquals(false, resolver.resolve(entity, ResolutionReason.SOURCE_DELETED, NOW, userConfirmed = false).userConfirmed)
    }

    @Test
    fun resolverExpiresActiveRowsOnlyAfterExpiryTime() {
        val resolver = ActiveIntentResolver()
        val active = activeIntent().copy(expiresAt = NOW - 1)
        val future = activeIntent().copy(expiresAt = NOW + 1)
        val archived = active.copy(status = ActiveIntentStatus.ARCHIVED)

        assertEquals(ActiveIntentStatus.EXPIRED, resolver.expireIfNeeded(active, NOW).status)
        assertEquals(ActiveIntentStatus.ACTIVE, resolver.expireIfNeeded(future, NOW).status)
        assertEquals(ActiveIntentStatus.ARCHIVED, resolver.expireIfNeeded(archived, NOW).status)
    }

    @Test
    fun invalidationServiceMarksUnderstandingIntentAndTombstone() = runTest {
        val captureDao = FakeCaptureUnderstandingDao(markedRows = 1)
        val activeDao = FakeActiveIntentDao(markedRows = 2)
        val invalidationDao = FakeInvalidationRecordDao()
        val service = InvalidationService(captureDao, activeDao, invalidationDao)

        val result = service.invalidateCapture(
            captureId = "capture-1",
            reason = InvalidationReason.CAPTURE_DELETED,
            invalidatedAt = NOW
        )

        assertEquals(1, result.understandingRowsInvalidated)
        assertEquals(2, result.activeIntentRowsInvalidated)
        assertEquals("capture-1", captureDao.lastCaptureId)
        assertEquals("capture-1", activeDao.lastCaptureId)
        assertEquals("SOURCE_DELETED", activeDao.lastResolutionReason)
        assertEquals(InvalidationReason.CAPTURE_DELETED.name, invalidationDao.lastInserted?.reason)
    }

    private fun basicResult(
        category: IntentCategory,
        completionKeyStatus: CompletionKeyStatus,
        completionKeyJson: String? = "{}"
    ): BasicUnderstandingResult {
        val engineResult = BasicUnderstandingEngine().understand(
            BasicUnderstandingInput(
                captureId = "capture-1",
                textContent = "Running shoes price $42.00",
                sourceAppLabel = "Chrome",
                appCategory = "BROWSER",
                canonicalUrl = "https://example.com/shoes",
                capturedAtMillis = NOW,
                nowMillis = NOW
            )
        )
        val completionKey = if (completionKeyJson == null) null else engineResult.completionKey
        return engineResult.copy(
            category = category,
            completionKey = completionKey,
            completionKeyStatus = completionKeyStatus
        )
    }

    private fun activeIntent(): ActiveIntentEntity = ActiveIntentEntity(
        intentId = "intent-1",
        captureId = "capture-1",
        intentType = IntentCategory.BUY_LATER_PRODUCT,
        status = ActiveIntentStatus.ACTIVE,
        completionKeyJson = "{}",
        completionKeyStatus = CompletionKeyStatus.FOUND,
        primaryEvidenceJson = "{}",
        primaryAction = "buy_or_skip",
        dueAt = null,
        expiresAt = null,
        resolutionReason = null,
        resolvedAt = null,
        userConfirmed = false,
        createdAt = NOW,
        updatedAt = NOW
    )

    private class FakeCaptureUnderstandingDao(
        private val markedRows: Int
    ) : CaptureUnderstandingDao {
        var lastCaptureId: String? = null

        override suspend fun upsert(entity: CaptureUnderstandingEntity) = Unit
        override fun observeByCaptureId(captureId: String): Flow<CaptureUnderstandingEntity?> = flowOf(null)
        override suspend fun getByCaptureId(captureId: String): CaptureUnderstandingEntity? = null
        override suspend fun getByContentHash(hex: String): List<CaptureUnderstandingEntity> = emptyList()
        override suspend fun getByCanonicalUrl(canonicalUrl: String): List<CaptureUnderstandingEntity> = emptyList()
        override suspend fun markInvalidated(captureId: String, invalidatedAt: Long): Int {
            lastCaptureId = captureId
            return markedRows
        }
        override suspend fun deleteByCaptureId(captureId: String): Int = 0
    }

    private class FakeActiveIntentDao(
        private val markedRows: Int
    ) : ActiveIntentDao {
        var lastCaptureId: String? = null
        var lastResolutionReason: String? = null

        override suspend fun upsert(entity: ActiveIntentEntity) = Unit
        override suspend fun getById(intentId: String): ActiveIntentEntity? = null
        override suspend fun getByCaptureId(captureId: String): List<ActiveIntentEntity> = emptyList()
        override fun observeActive(): Flow<List<ActiveIntentEntity>> = flowOf(emptyList())
        override fun observeCleanupQueue(): Flow<List<ActiveIntentEntity>> = flowOf(emptyList())
        override suspend fun getActiveBasic(limit: Int): List<ActiveIntentEntity> = emptyList()
        override suspend fun markResolved(
            intentId: String,
            status: String,
            resolutionReason: String?,
            resolvedAt: Long?,
            userConfirmed: Boolean,
            updatedAt: Long
        ): Int = 0
        override suspend fun markInvalidatedForCapture(
            captureId: String,
            invalidatedAt: Long,
            resolutionReason: String
        ): Int {
            lastCaptureId = captureId
            lastResolutionReason = resolutionReason
            return markedRows
        }
        override suspend fun deleteByCaptureId(captureId: String): Int = 0
    }

    private class FakeInvalidationRecordDao : InvalidationRecordDao {
        var lastInserted: InvalidationRecordEntity? = null

        override suspend fun insert(entity: InvalidationRecordEntity) {
            lastInserted = entity
        }
        override suspend fun getByCaptureId(captureId: String): InvalidationRecordEntity? = lastInserted
        override suspend fun existsForCapture(captureId: String): Boolean = lastInserted?.captureId == captureId
    }

    private companion object {
        const val NOW = 1_779_216_100_000L
    }
}