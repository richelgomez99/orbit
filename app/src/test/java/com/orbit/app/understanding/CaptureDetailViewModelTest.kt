package com.orbit.app.understanding

import com.orbit.app.data.dao.InvalidationRecordDao
import com.orbit.app.data.entity.CaptureUnderstandingEntity
import com.orbit.app.data.entity.EvidenceBundleEntity
import com.orbit.app.data.entity.InvalidationRecordEntity
import com.orbit.app.ui.understanding.CaptureDetailUiState
import com.orbit.app.ui.understanding.CaptureDetailViewModel
import com.orbit.app.understanding.domain.EscalationRequest
import com.orbit.app.understanding.domain.UnderstandingMode
import com.orbit.app.understanding.engine.InvalidationGuard
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CaptureDetailViewModelTest {

    @Test
    fun loadsReadyStateWithoutRawEvidencePayloads() = runTest {
        val scope = TestScope(UnconfinedTestDispatcher(testScheduler))
        val viewModel = CaptureDetailViewModel(
            captureId = "capture-1",
            repository = FakeUnderstandingRepository(),
            invalidationGuard = InvalidationGuard(FakeInvalidationRecordDao(invalidated = false)),
            scopeOverride = scope
        )
        scope.advanceUntilIdle()

        val state = viewModel.state.value
        assertTrue("expected Ready, got $state", state is CaptureDetailUiState.Ready)
        state as CaptureDetailUiState.Ready
        assertEquals("Title", state.title)
        assertEquals(listOf("URL_METADATA"), state.evidenceTypes)
        assertEquals("YouTube", state.sourceIdentity.provider)
    }

    @Test
    fun invalidatedCaptureSurfacesNoEvidenceError() = runTest {
        val scope = TestScope(UnconfinedTestDispatcher(testScheduler))
        val viewModel = CaptureDetailViewModel(
            captureId = "capture-1",
            repository = FakeUnderstandingRepository(),
            invalidationGuard = InvalidationGuard(FakeInvalidationRecordDao(invalidated = true)),
            scopeOverride = scope
        )
        scope.advanceUntilIdle()

        val state = viewModel.state.value
        assertTrue("expected Error, got $state", state is CaptureDetailUiState.Error)
        assertEquals("No evidence available", (state as CaptureDetailUiState.Error).message)
    }

    private class FakeUnderstandingRepository : UnderstandingRepository {
        override fun getUnderstanding(captureId: String): Flow<CaptureUnderstandingEntity?> = flowOf(
            CaptureUnderstandingEntity(
                captureId = captureId,
                mode = "BASIC",
                status = "READY",
                title = "Title",
                summaryText = "Saved URL from youtube.com.",
                groundingConstraintsJson = "{\"constraints\":[\"basic-local-only\"],\"evidenceLevel\":\"METADATA_ONLY\"}",
                contentHashHex = "hash",
                canonicalUrl = "https://youtube.com/watch?v=abc",
                sourceIdentityJson = "{\"provider\":\"YouTube\",\"appLabel\":null,\"category\":\"VIDEO\",\"confidence\":1.0}",
                createdAt = 1L,
                updatedAt = 1L,
                invalidatedAt = null
            )
        )

        override suspend fun saveUnderstanding(result: com.orbit.app.understanding.domain.UnderstandingResult) = Unit
        override suspend fun requestEscalation(request: EscalationRequest) = Unit
        override suspend fun getEvidenceBundles(captureId: String): List<EvidenceBundleEntity> = listOf(
            EvidenceBundleEntity(
                id = "evidence-1",
                captureId = captureId,
                bundleType = "URL_METADATA",
                payloadJson = "{\"title\":\"Title\"}",
                createdAt = 1L
            )
        )
    }

    private class FakeInvalidationRecordDao(
        private val invalidated: Boolean
    ) : InvalidationRecordDao {
        override suspend fun insert(entity: InvalidationRecordEntity) = Unit
        override suspend fun getById(captureId: String): InvalidationRecordEntity? = null
        override suspend fun existsForCapture(captureId: String): Boolean = invalidated
    }
}
