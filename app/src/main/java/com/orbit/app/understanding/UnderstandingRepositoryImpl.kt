package com.orbit.app.understanding

import com.orbit.app.data.dao.CaptureUnderstandingDao
import com.orbit.app.data.dao.EvidenceBundleDao
import com.orbit.app.data.entity.CaptureUnderstandingEntity
import com.orbit.app.data.entity.EvidenceBundleEntity
import com.orbit.app.understanding.domain.EscalationRequest
import com.orbit.app.understanding.domain.GroundingConstraints
import com.orbit.app.understanding.domain.UnderstandingResult
import kotlinx.coroutines.flow.Flow

class UnderstandingRepositoryImpl(
    private val captureUnderstandingDao: CaptureUnderstandingDao,
    private val evidenceBundleDao: EvidenceBundleDao,
    private val activeIntentRepository: ActiveIntentRepository? = null,
    private val activeIntentResolver: (suspend (UnderstandingResult) -> com.orbit.app.data.entity.ActiveIntentEntity?)? = null,
    private val escalationHandler: (suspend (EscalationRequest) -> Unit)? = null,
    private val clock: () -> Long = { System.currentTimeMillis() }
) : UnderstandingRepository {

    override fun getUnderstanding(captureId: String): Flow<CaptureUnderstandingEntity?> =
        captureUnderstandingDao.getByCapture(captureId)

    override suspend fun saveUnderstanding(result: UnderstandingResult) {
        val now = clock()
        captureUnderstandingDao.upsert(
            CaptureUnderstandingEntity(
                captureId = result.captureId,
                mode = result.mode.name,
                status = result.status.name,
                title = result.title,
                summaryText = result.summaryText,
                groundingConstraintsJson = result.groundingConstraints.toStorageJson(),
                contentHashHex = result.contentHashHex,
                canonicalUrl = result.canonicalUrl,
                sourceIdentityJson = result.sourceIdentityJson,
                createdAt = now,
                updatedAt = now,
                invalidatedAt = null
            )
        )
        activeIntentResolver?.invoke(result)?.let { activeIntent ->
            activeIntentRepository?.upsert(activeIntent)
        }
    }

    override suspend fun requestEscalation(request: EscalationRequest) {
        val handler = escalationHandler
            ?: error("No user-triggered understanding escalation handler is configured.")
        handler(request)
    }

    override suspend fun getEvidenceBundles(captureId: String): List<EvidenceBundleEntity> =
        evidenceBundleDao.getByCaptureId(captureId)

    private fun GroundingConstraints.toStorageJson(): String {
        val escapedConstraints = constraints.joinToString(",") { "\"${it.escapeJson()}\"" }
        return "{\"constraints\":[$escapedConstraints],\"evidenceLevel\":\"${evidenceLevel.name}\"}"
    }

    private fun String.escapeJson(): String = buildString {
        for (ch in this@escapeJson) {
            when (ch) {
                '\\' -> append("\\\\")
                '"' -> append("\\\"")
                '\n' -> append("\\n")
                '\r' -> append("\\r")
                '\t' -> append("\\t")
                else -> append(ch)
            }
        }
    }
}
