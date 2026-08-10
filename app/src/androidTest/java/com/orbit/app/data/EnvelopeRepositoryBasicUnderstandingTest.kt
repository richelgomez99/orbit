package com.orbit.app.data

import com.orbit.app.audit.AuditLogWriter
import com.orbit.app.data.dao.ActiveIntentDao
import com.orbit.app.data.dao.CaptureUnderstandingDao
import com.orbit.app.data.dao.EvidenceBundleDao
import com.orbit.app.data.entity.ActiveIntentEntity
import com.orbit.app.data.entity.AuditLogEntryEntity
import com.orbit.app.data.entity.CaptureUnderstandingEntity
import com.orbit.app.data.entity.ContinuationEntity
import com.orbit.app.data.entity.ContinuationResultEntity
import com.orbit.app.data.entity.EnvelopeNoteEntity
import com.orbit.app.data.entity.EvidenceBundleEntity
import com.orbit.app.data.entity.IntentEnvelopeEntity
import com.orbit.app.data.entity.IntentEnvelopeWithResults
import com.orbit.app.data.ipc.IntentEnvelopeDraftParcel
import com.orbit.app.data.ipc.StateSnapshotParcel
import com.orbit.app.data.model.ActivityState
import com.orbit.app.data.model.AppCategory
import com.orbit.app.data.model.ContentType
import com.orbit.app.data.model.ContinuationStatus
import com.orbit.app.data.model.Intent
import com.orbit.app.data.model.IntentSource
import com.orbit.app.understanding.BasicUnderstandingWriter
import com.orbit.app.understanding.domain.ActiveIntentStatus
import com.orbit.app.understanding.domain.CompletionKeyStatus
import com.orbit.app.understanding.domain.IntentCategory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class EnvelopeRepositoryBasicUnderstandingTest {

    @Test
    fun sealTextCapturePersistsBasicUnderstandingAndActiveIntent() {
        val backend = FakeBackend()
        val captureDao = FakeCaptureUnderstandingDao()
        val activeDao = FakeActiveIntentDao()
        val repository = repository(backend, captureDao, FakeEvidenceBundleDao(), activeDao)

        val result = repository.sealWithResult(
            draft = IntentEnvelopeDraftParcel(
                contentType = ContentType.TEXT.name,
                textContent = "Promo code SAVE20 for running shoes at $42.00 https://example.com/item",
                imageUri = null,
                intent = Intent.WANT_IT.name,
                intentConfidence = 0.92f,
                intentSource = IntentSource.USER_CHIP.name
            ),
            state = state()
        )

        val captureId = result.envelopeId
        assertEquals(captureId, backend.sealedEnvelope?.id)
        assertEquals(captureId, captureDao.upserted?.captureId)
        assertEquals(IntentCategory.COUPON_OR_PROMO, captureDao.upserted?.category)
        assertEquals(CompletionKeyStatus.FOUND, captureDao.upserted?.completionKeyStatus)
        assertEquals(captureId, activeDao.upserted?.captureId)
        assertEquals(ActiveIntentStatus.ACTIVE, activeDao.upserted?.status)
    }

    @Test
    fun screenshotOcrHydrationRefreshesBasicUnderstandingAndActiveIntent() {
        val backend = FakeBackend()
        val captureDao = FakeCaptureUnderstandingDao()
        val activeDao = FakeActiveIntentDao()
        val repository = repository(backend, captureDao, FakeEvidenceBundleDao(), activeDao)
        val captureId = repository.sealWithResult(
            draft = IntentEnvelopeDraftParcel(
                contentType = ContentType.IMAGE.name,
                textContent = null,
                imageUri = "content://media/external/images/media/42",
                intent = Intent.REFERENCE.name,
                intentConfidence = 0.70f,
                intentSource = IntentSource.PREDICTED_SILENT.name
            ),
            state = state(sourceAppLabel = "Samsung Gallery")
        ).envelopeId

        repository.seedScreenshotHydrations(
            envelopeId = captureId,
            ocrText = "Order #ABCD-1234 confirmed at https://shop.example/orders/ABCD-1234",
            urls = arrayOf("https://shop.example/orders/ABCD-1234")
        )

        assertEquals(1, backend.seededContinuations.size)
        assertEquals(captureId, captureDao.upserted?.captureId)
        assertEquals(IntentCategory.RECEIPT_OR_ORDER, captureDao.upserted?.category)
        assertEquals(CompletionKeyStatus.FOUND, captureDao.upserted?.completionKeyStatus)
        assertNotNull(captureDao.upserted?.completionKeyJson)
        assertEquals(captureId, activeDao.upserted?.captureId)
    }

    private fun repository(
        backend: FakeBackend,
        captureDao: FakeCaptureUnderstandingDao,
        evidenceDao: FakeEvidenceBundleDao,
        activeDao: FakeActiveIntentDao
    ) = EnvelopeRepositoryImpl(
        backend = backend,
        auditWriter = AuditLogWriter(clock = { NOW }, idGen = { "audit-${backend.auditCounter++}" }),
        scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined),
        clock = { NOW },
        basicUnderstandingWriter = BasicUnderstandingWriter(
            captureUnderstandingDao = captureDao,
            evidenceBundleDao = evidenceDao,
            activeIntentDao = activeDao
        )
    )

    private fun state(sourceAppLabel: String = "Chrome") = StateSnapshotParcel(
        appCategory = AppCategory.BROWSER.name,
        activityState = ActivityState.STILL.name,
        tzId = "UTC",
        hourLocal = 12,
        dayOfWeekLocal = 2,
        sourceAppLabel = sourceAppLabel
    )

    private class FakeBackend : EnvelopeStorageBackend {
        var sealedEnvelope: IntentEnvelopeEntity? = null
        var auditCounter = 0
        val seededContinuations = mutableListOf<ContinuationEntity>()

        override suspend fun sealTransaction(
            envelope: IntentEnvelopeEntity,
            continuations: List<ContinuationEntity>,
            auditEntries: List<AuditLogEntryEntity>
        ) {
            sealedEnvelope = envelope
        }

        override suspend fun getEnvelope(id: String): IntentEnvelopeEntity? =
            sealedEnvelope?.takeIf { it.id == id }

        override suspend fun seedScreenshotHydrationsTransaction(
            continuations: List<ContinuationEntity>,
            auditEntries: List<AuditLogEntryEntity>
        ) {
            seededContinuations.addAll(continuations)
        }

        override suspend fun findActiveEnvelopeByPrimaryCanonicalUrlHash(hash: String): IntentEnvelopeEntity? = null
        override suspend fun findActiveEnvelopeByContinuationCanonicalUrlHash(hash: String): IntentEnvelopeEntity? = null
        override suspend fun findActiveEnvelopeByTextContentSha256(hash: String): IntentEnvelopeEntity? = null
        override suspend fun findActiveEnvelopeByExactTextContent(text: String): IntentEnvelopeEntity? = null
        override suspend fun findContinuationResultByCanonicalUrlHash(hash: String): ContinuationResultEntity? = null
        override suspend fun recordDuplicateCaptureAttempt(auditEntry: AuditLogEntryEntity) = Unit

        override suspend fun reassignIntentTransaction(id: String, newIntent: String, intentSource: String, confidence: Float?, historyJson: String, auditEntry: AuditLogEntryEntity) = error("unused")
        override suspend fun archiveTransaction(id: String, auditEntry: AuditLogEntryEntity) = error("unused")
        override suspend fun softDeleteTransaction(id: String, deletedAt: Long, auditEntry: AuditLogEntryEntity) = error("unused")
        override suspend fun restoreFromTrashTransaction(id: String, auditEntry: AuditLogEntryEntity) = error("unused")
        override suspend fun hardDeleteTransaction(id: String, auditEntry: AuditLogEntryEntity) = error("unused")
        override suspend fun insertDigestTransaction(envelope: IntentEnvelopeEntity, auditEntry: AuditLogEntryEntity): Boolean = error("unused")
        override suspend fun insertClusterSummaryTransaction(envelope: IntentEnvelopeEntity, auditEntry: AuditLogEntryEntity) = error("unused")
        override suspend fun listRegularEnvelopesInWindow(windowStartDayLocal: String, windowEndDayLocalInclusive: String, limit: Int): List<IntentEnvelopeEntity> = emptyList()
        override suspend fun cascadeDigestInvalidation(deletedEnvelopeId: String, now: Long, auditFor: (String) -> AuditLogEntryEntity): List<String> = emptyList()
        override suspend fun listIdsSoftDeletedBefore(cutoffMillis: Long): List<String> = emptyList()
        override suspend fun undoSealTransaction(envelopeId: String) = error("unused")
        override fun observeDay(dayLocal: String): Flow<List<IntentEnvelopeEntity>> = emptyFlow()
        override fun observeDayWithResults(dayLocal: String): Flow<List<IntentEnvelopeWithResults>> = emptyFlow()
        override suspend fun getLatestNoteForEnvelope(envelopeId: String): EnvelopeNoteEntity? = null
        override suspend fun createOrUpdateLatestNote(note: EnvelopeNoteEntity) = error("unused")
        override suspend fun listSoftDeletedWithinDays(days: Int, nowMillis: Long): List<IntentEnvelopeEntity> = emptyList()
        override suspend fun countSoftDeletedWithinDays(days: Int, nowMillis: Long): Int = 0
        override suspend fun distinctDayLocalsWithContent(limit: Int, offset: Int): List<String> = emptyList()
        override suspend fun existsNonArchivedNonDeletedInLast30Days(appCategory: String, intent: String, nowMillis: Long): Boolean = false
        override suspend fun getLatestResultForEnvelope(envelopeId: String, sharedResultId: String?): ContinuationResultEntity? = null
        override suspend fun listRetryableContinuations(envelopeId: String): List<ContinuationEntity> = emptyList()
        override suspend fun completeUrlHydrationTransaction(
            continuationId: String,
            result: ContinuationResultEntity?,
            newStatus: ContinuationStatus,
            completedAt: Long,
            failureReason: String?,
            auditEntry: AuditLogEntryEntity,
            envelopeIdForDedupe: String?,
            dedupeExistingResultId: String?
        ) = error("unused")
        override suspend fun countAll(): Int = 0
        override suspend fun countArchived(): Int = 0
        override suspend fun countDeleted(): Int = 0
    }

    private class FakeCaptureUnderstandingDao : CaptureUnderstandingDao {
        var upserted: CaptureUnderstandingEntity? = null
        override suspend fun upsert(entity: CaptureUnderstandingEntity) {
            upserted = entity
        }
        override fun observeByCaptureId(captureId: String): Flow<CaptureUnderstandingEntity?> = flowOf(upserted)
        override suspend fun getByCaptureId(captureId: String): CaptureUnderstandingEntity? = upserted
        override suspend fun getByContentHash(hex: String): List<CaptureUnderstandingEntity> = emptyList()
        override suspend fun getByCanonicalUrl(canonicalUrl: String): List<CaptureUnderstandingEntity> = emptyList()
        override suspend fun markInvalidated(captureId: String, invalidatedAt: Long): Int = 0
        override suspend fun deleteByCaptureId(captureId: String): Int = 0
    }

    private class FakeEvidenceBundleDao : EvidenceBundleDao {
        override suspend fun insert(entity: EvidenceBundleEntity) = Unit
        override suspend fun insertAll(entities: List<EvidenceBundleEntity>) = Unit
        override suspend fun getByCaptureId(captureId: String): List<EvidenceBundleEntity> = emptyList()
        override suspend fun deleteByCaptureId(captureId: String): Int = 0
    }

    private class FakeActiveIntentDao : ActiveIntentDao {
        var upserted: ActiveIntentEntity? = null
        override suspend fun upsert(entity: ActiveIntentEntity) {
            upserted = entity
        }
        override suspend fun getById(intentId: String): ActiveIntentEntity? = upserted?.takeIf { it.intentId == intentId }
        override suspend fun getByCaptureId(captureId: String): List<ActiveIntentEntity> = listOfNotNull(upserted).filter { it.captureId == captureId }
        override fun observeActive(): Flow<List<ActiveIntentEntity>> = emptyFlow()
        override fun observeCleanupQueue(): Flow<List<ActiveIntentEntity>> = emptyFlow()
        override suspend fun getActiveBasic(limit: Int): List<ActiveIntentEntity> = emptyList()
        override suspend fun markResolved(intentId: String, status: String, resolutionReason: String?, resolvedAt: Long?, userConfirmed: Boolean, updatedAt: Long): Int = 0
        override suspend fun markInvalidatedForCapture(captureId: String, invalidatedAt: Long, resolutionReason: String): Int = 0
        override suspend fun deleteByCaptureId(captureId: String): Int = 0
    }

    private companion object {
        const val NOW = 1_779_216_300_000L
    }
}