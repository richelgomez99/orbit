package com.orbit.app.understanding

import com.orbit.app.data.dao.ActiveIntentDao
import com.orbit.app.data.dao.CaptureUnderstandingDao
import com.orbit.app.data.dao.InvalidationRecordDao
import com.orbit.app.data.entity.InvalidationRecordEntity
import com.orbit.app.understanding.domain.InvalidationReason
import com.orbit.app.understanding.domain.ResolutionReason

data class InvalidationResult(
    val captureId: String,
    val reason: InvalidationReason,
    val understandingRowsInvalidated: Int,
    val activeIntentRowsInvalidated: Int
)

class InvalidationService(
    private val captureUnderstandingDao: CaptureUnderstandingDao,
    private val activeIntentDao: ActiveIntentDao,
    private val invalidationRecordDao: InvalidationRecordDao
) {

    suspend fun invalidateCapture(
        captureId: String,
        reason: InvalidationReason,
        invalidatedAt: Long
    ): InvalidationResult {
        invalidationRecordDao.insert(
            InvalidationRecordEntity(
                captureId = captureId,
                invalidatedAt = invalidatedAt,
                reason = reason.name
            )
        )
        val understandingRows = captureUnderstandingDao.markInvalidated(captureId, invalidatedAt)
        val activeRows = activeIntentDao.markInvalidatedForCapture(
            captureId = captureId,
            invalidatedAt = invalidatedAt,
            resolutionReason = resolutionReasonFor(reason).name
        )
        return InvalidationResult(
            captureId = captureId,
            reason = reason,
            understandingRowsInvalidated = understandingRows,
            activeIntentRowsInvalidated = activeRows
        )
    }

    private fun resolutionReasonFor(reason: InvalidationReason): ResolutionReason = when (reason) {
        InvalidationReason.CAPTURE_DELETED -> ResolutionReason.SOURCE_DELETED
        InvalidationReason.USER_REQUESTED,
        InvalidationReason.CORRECTION_APPLIED,
        InvalidationReason.SOURCE_SUPERSEDED -> ResolutionReason.INVALIDATED
    }
}