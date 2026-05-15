package com.orbit.app.understanding.engine

import androidx.room.withTransaction
import com.orbit.app.data.OrbitDatabase
import com.orbit.app.data.dao.AuditLogDao
import com.orbit.app.data.dao.ActiveIntentDao
import com.orbit.app.data.dao.CaptureUnderstandingDao
import com.orbit.app.data.dao.EvidenceBundleDao
import com.orbit.app.data.dao.InvalidationRecordDao
import com.orbit.app.data.entity.AuditLogEntryEntity
import com.orbit.app.data.entity.InvalidationRecordEntity
import com.orbit.app.data.model.AuditAction
import java.util.UUID

enum class InvalidationReason {
    CAPTURE_DELETED,
    USER_REQUESTED,
    CORRECTION_APPLIED
}

data class InvalidationResult(
    val captureId: String,
    val invalidatedAt: Long
)

class InvalidationService(
    private val invalidationRecordDao: InvalidationRecordDao,
    private val captureUnderstandingDao: CaptureUnderstandingDao,
    private val evidenceBundleDao: EvidenceBundleDao,
    private val activeIntentDao: ActiveIntentDao?,
    private val auditLogDao: AuditLogDao,
    private val transactionRunner: suspend (suspend () -> Unit) -> Unit,
    private val clock: () -> Long = { System.currentTimeMillis() }
) {
    constructor(
        database: OrbitDatabase,
        clock: () -> Long = { System.currentTimeMillis() }
    ) : this(
        invalidationRecordDao = database.invalidationRecordDao(),
        captureUnderstandingDao = database.captureUnderstandingDao(),
        evidenceBundleDao = database.evidenceBundleDao(),
        activeIntentDao = database.activeIntentDao(),
        auditLogDao = database.auditLogDao(),
        transactionRunner = { block -> database.withTransaction { block() } },
        clock = clock
    )

    suspend fun invalidateForCapture(
        captureId: String,
        reason: InvalidationReason
    ): InvalidationResult {
        val invalidatedAt = clock()
        transactionRunner {
            if (!invalidationRecordDao.existsForCapture(captureId)) {
                invalidationRecordDao.insert(
                    InvalidationRecordEntity(
                        captureId = captureId,
                        invalidatedAt = invalidatedAt,
                        reason = reason.name
                    )
                )
            }
            captureUnderstandingDao.markInvalidated(captureId, invalidatedAt)
            evidenceBundleDao.deleteByCapture(captureId)
            activeIntentDao?.invalidateByCapture(captureId, "SOURCE_DELETED", invalidatedAt)
            auditLogDao.insert(
                AuditLogEntryEntity(
                    id = UUID.randomUUID().toString(),
                    at = invalidatedAt,
                    action = AuditAction.UNDERSTANDING_INVALIDATED,
                    description = "Capture understanding invalidated: ${reason.name}",
                    envelopeId = captureId,
                    extraJson = "{\"reason\":\"${reason.name}\"}"
                )
            )
        }
        return InvalidationResult(captureId = captureId, invalidatedAt = invalidatedAt)
    }
}
