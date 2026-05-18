package com.orbit.app.understanding

import com.orbit.app.data.entity.CaptureUnderstandingEntity
import com.orbit.app.data.entity.EvidenceBundleEntity
import com.orbit.app.understanding.domain.EscalationRequest
import com.orbit.app.understanding.domain.UnderstandingResult
import kotlinx.coroutines.flow.Flow

/**
 * Repository boundary for compact capture understanding data.
 *
 * Return types intentionally exclude raw HTML, full screenshots, full text,
 * embeddings, prompts, model responses, and full evidence payloads.
 */
interface UnderstandingRepository {
    fun getUnderstanding(captureId: String): Flow<CaptureUnderstandingEntity?>
    suspend fun saveUnderstanding(result: UnderstandingResult)
    suspend fun requestEscalation(request: EscalationRequest)
    suspend fun getEvidenceBundles(captureId: String): List<EvidenceBundleEntity>
}
