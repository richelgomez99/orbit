package com.orbit.app.understanding

import com.orbit.app.data.dao.AuditLogDao
import com.orbit.app.data.dao.CaptureUnderstandingDao
import com.orbit.app.data.dao.EvidenceBundleDao
import com.orbit.app.data.dao.InvalidationRecordDao
import com.orbit.app.data.entity.AuditLogEntryEntity
import com.orbit.app.data.entity.CaptureUnderstandingEntity
import com.orbit.app.data.entity.EvidenceBundleEntity
import com.orbit.app.data.entity.InvalidationRecordEntity
import com.orbit.app.data.model.AuditAction
import com.orbit.app.understanding.engine.InvalidationReason
import com.orbit.app.understanding.engine.InvalidationService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class InvalidationServiceTest {

    @Test
    fun invalidateForCapture_marksUnderstandingDeletesEvidenceAndAudits() = runTest {
        val captureDao = RecordingCaptureUnderstandingDao()
        val evidenceDao = RecordingEvidenceBundleDao(
            mutableListOf(
                EvidenceBundleEntity("evidence-1", "capture-1", "URL_METADATA", "{}", 1L)
            )
        )
        val invalidationDao = RecordingInvalidationRecordDao()
        val auditDao = RecordingAuditLogDao()
        val service = InvalidationService(
            invalidationRecordDao = invalidationDao,
            captureUnderstandingDao = captureDao,
            evidenceBundleDao = evidenceDao,
            activeIntentDao = null,
            auditLogDao = auditDao,
            transactionRunner = { block -> block() },
            clock = { 999L }
        )

        service.invalidateForCapture("capture-1", InvalidationReason.CAPTURE_DELETED)
        service.invalidateForCapture("capture-1", InvalidationReason.CAPTURE_DELETED)

        assertTrue(invalidationDao.existsForCapture("capture-1"))
        assertEquals(999L, captureDao.invalidatedAtByCapture["capture-1"])
        assertTrue(evidenceDao.getByCaptureId("capture-1").isEmpty())
        assertEquals(AuditAction.UNDERSTANDING_INVALIDATED, auditDao.entries.first().action)
        assertEquals("capture-1", auditDao.entries.first().envelopeId)
        assertNotNull(auditDao.entries.first().id)
        assertEquals(1, invalidationDao.records.size)
    }

    private class RecordingCaptureUnderstandingDao : CaptureUnderstandingDao {
        val invalidatedAtByCapture = mutableMapOf<String, Long>()

        override suspend fun upsert(entity: CaptureUnderstandingEntity) = Unit
        override fun getByCapture(captureId: String): Flow<CaptureUnderstandingEntity?> = flowOf(null)
        override suspend fun getByContentHash(hex: String): List<CaptureUnderstandingEntity> = emptyList()
        override suspend fun getByCanonicalUrl(canonicalUrl: String): List<CaptureUnderstandingEntity> = emptyList()
        override suspend fun markInvalidated(captureId: String, invalidatedAt: Long) {
            invalidatedAtByCapture[captureId] = invalidatedAt
        }
        override suspend fun deleteByCapture(captureId: String) = Unit
    }

    private class RecordingEvidenceBundleDao(
        private val rows: MutableList<EvidenceBundleEntity>
    ) : EvidenceBundleDao {
        override suspend fun insert(entity: EvidenceBundleEntity) {
            rows += entity
        }
        override suspend fun getByCaptureId(captureId: String): List<EvidenceBundleEntity> =
            rows.filter { it.captureId == captureId }
        override suspend fun deleteByCapture(captureId: String) {
            rows.removeAll { it.captureId == captureId }
        }
    }

    private class RecordingInvalidationRecordDao : InvalidationRecordDao {
        val records = mutableMapOf<String, InvalidationRecordEntity>()

        override suspend fun insert(entity: InvalidationRecordEntity) {
            records[entity.captureId] = entity
        }
        override suspend fun getById(captureId: String): InvalidationRecordEntity? = records[captureId]
        override suspend fun existsForCapture(captureId: String): Boolean = records.containsKey(captureId)
    }

    private class RecordingAuditLogDao : AuditLogDao {
        val entries = mutableListOf<AuditLogEntryEntity>()

        override suspend fun insert(entry: AuditLogEntryEntity) {
            entries += entry
        }
        override suspend fun entriesForDay(startMillis: Long, endMillis: Long): List<AuditLogEntryEntity> = entries
        override suspend fun entriesForEnvelope(envelopeId: String): List<AuditLogEntryEntity> =
            entries.filter { it.envelopeId == envelopeId }
        override suspend fun countForDay(startMillis: Long, endMillis: Long, action: String): Int =
            entries.count { it.action.name == action }
        override suspend fun deleteOlderThan(cutoffMillis: Long): Int = 0
        override suspend fun deleteByEnvelopeId(envelopeId: String) = Unit
        override suspend fun listAll(): List<AuditLogEntryEntity> = entries
    }
}
