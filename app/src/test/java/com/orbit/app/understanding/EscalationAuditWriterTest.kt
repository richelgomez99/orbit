package com.orbit.app.understanding

import com.orbit.app.data.dao.AuditLogDao
import com.orbit.app.data.entity.AuditLogEntryEntity
import com.orbit.app.data.model.AuditAction
import com.orbit.app.understanding.domain.UnderstandingMode
import com.orbit.app.understanding.engine.EscalationAuditWriter
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class EscalationAuditWriterTest {

    @Test
    fun writeEscalationRequested_recordsSmartMode() = runTest {
        val auditDao = RecordingAuditLogDao()
        val writer = EscalationAuditWriter(auditDao, clock = { 123L }, idProvider = { "audit-1" })

        val token = writer.writeEscalationRequested("capture-1", UnderstandingMode.SMART)

        assertEquals("capture-1", token.captureId)
        assertEquals(UnderstandingMode.SMART, token.mode)
        assertEquals(AuditAction.UNDERSTANDING_ESCALATION_REQUESTED, auditDao.entries.single().action)
        assertEquals("capture-1", auditDao.entries.single().envelopeId)
        assertEquals("{\"mode\":\"SMART\"}", auditDao.entries.single().extraJson)
    }

    @Test
    fun writeEscalationRequested_recordsDeepMode() = runTest {
        val auditDao = RecordingAuditLogDao()
        val writer = EscalationAuditWriter(auditDao, clock = { 456L }, idProvider = { "audit-2" })

        writer.writeEscalationRequested("capture-2", UnderstandingMode.DEEP)

        assertEquals("{\"mode\":\"DEEP\"}", auditDao.entries.single().extraJson)
    }

    @Test(expected = IllegalArgumentException::class)
    fun writeEscalationRequested_rejectsBasicMode() = runTest {
        EscalationAuditWriter(RecordingAuditLogDao()).writeEscalationRequested(
            "capture-3",
            UnderstandingMode.BASIC
        )
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
