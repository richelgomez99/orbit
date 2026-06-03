package com.orbit.app.memory

import com.orbit.app.data.entity.AuditLogEntryEntity

interface MemoryIndexSource {
    suspend fun load(envelopeId: String): MemoryIndexSnapshot?
    suspend fun insertAudit(entry: AuditLogEntryEntity)
}
