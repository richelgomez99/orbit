package com.orbit.app.understanding

import com.orbit.app.data.dao.InvalidationRecordDao
import com.orbit.app.data.entity.InvalidationRecordEntity
import com.orbit.app.understanding.engine.InvalidatedCaptureException
import com.orbit.app.understanding.engine.InvalidationGuard
import kotlinx.coroutines.test.runTest
import org.junit.Test

class InvalidationGuardTest {

    @Test
    fun assertNotInvalidated_allowsMissingRecord() = runTest {
        InvalidationGuard(FakeInvalidationRecordDao(invalidated = false)).assertNotInvalidated("capture-1")
    }

    @Test(expected = InvalidatedCaptureException::class)
    fun assertNotInvalidated_blocksInvalidatedCapture() = runTest {
        InvalidationGuard(FakeInvalidationRecordDao(invalidated = true)).assertNotInvalidated("capture-1")
    }

    private class FakeInvalidationRecordDao(
        private val invalidated: Boolean
    ) : InvalidationRecordDao {
        override suspend fun insert(entity: InvalidationRecordEntity) = Unit
        override suspend fun getById(captureId: String): InvalidationRecordEntity? = null
        override suspend fun existsForCapture(captureId: String): Boolean = invalidated
    }
}
