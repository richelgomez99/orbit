package com.orbit.app.memory

import com.orbit.app.data.OrbitDatabase
import com.orbit.app.data.entity.AuditLogEntryEntity

class RoomMemoryIndexSource(
    private val database: OrbitDatabase,
) : MemoryIndexSource {
    override suspend fun load(envelopeId: String): MemoryIndexSnapshot? {
        val envelope = database.intentEnvelopeDao().getById(envelopeId) ?: return null
        val latestResult = envelope.sharedContinuationResultId
            ?.let { database.continuationResultDao().findById(it) }
            ?: database.continuationResultDao().getByEnvelopeId(envelopeId).firstOrNull()
        return MemoryIndexSnapshot(
            envelope = envelope,
            latestResult = latestResult,
            note = database.envelopeNoteDao().latestForEnvelope(envelopeId),
            understanding = database.captureUnderstandingDao().getByCaptureId(envelopeId),
            evidenceBundles = database.evidenceBundleDao().getByCaptureId(envelopeId),
        )
    }

    override suspend fun insertAudit(entry: AuditLogEntryEntity) {
        database.auditLogDao().insert(entry)
    }
}
