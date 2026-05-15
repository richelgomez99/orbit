package com.orbit.app.understanding.engine

import com.orbit.app.data.dao.AuditLogDao
import com.orbit.app.data.entity.AuditLogEntryEntity
import com.orbit.app.data.model.AuditAction
import com.orbit.app.understanding.domain.UnderstandingMode
import java.util.UUID

data class EscalationAuditToken(
    val captureId: String,
    val mode: UnderstandingMode,
    val writtenAt: Long
)

class EscalationAuditWriter(
    private val auditLogDao: AuditLogDao,
    private val clock: () -> Long = { System.currentTimeMillis() },
    private val idProvider: () -> String = { UUID.randomUUID().toString() }
) {
    suspend fun writeEscalationRequested(
        captureId: String,
        mode: UnderstandingMode
    ): EscalationAuditToken {
        require(mode != UnderstandingMode.BASIC) { "BASIC does not require escalation audit." }
        val now = clock()
        auditLogDao.insert(
            AuditLogEntryEntity(
                id = idProvider(),
                at = now,
                action = AuditAction.UNDERSTANDING_ESCALATION_REQUESTED,
                description = "Understanding escalation requested: ${mode.name}",
                envelopeId = captureId,
                extraJson = "{\"mode\":\"${mode.name}\"}"
            )
        )
        return EscalationAuditToken(captureId = captureId, mode = mode, writtenAt = now)
    }
}
