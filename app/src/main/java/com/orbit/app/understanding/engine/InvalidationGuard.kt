package com.orbit.app.understanding.engine

import com.orbit.app.data.dao.InvalidationRecordDao

class InvalidatedCaptureException(captureId: String) : IllegalStateException(
    "Capture $captureId has been invalidated; derived understanding must not be served."
)

class InvalidationGuard(
    private val invalidationRecordDao: InvalidationRecordDao
) {

    /**
     * @ForFutureFeature: All future retrieval, action, memory, cloud, KG, and
     * agent systems (specs 005 through 010) MUST call this before serving
     * cached or derived data for a captureId.
     */
    suspend fun assertNotInvalidated(captureId: String) {
        if (invalidationRecordDao.existsForCapture(captureId)) {
            throw InvalidatedCaptureException(captureId)
        }
    }
}
