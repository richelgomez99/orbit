package com.orbit.app.understanding

import com.orbit.app.data.dao.ActiveIntentDao
import com.orbit.app.data.entity.ActiveIntentEntity
import com.orbit.app.understanding.domain.ActiveIntentResolutionReason
import com.orbit.app.understanding.domain.ActiveIntentStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

data class ActiveIntentUiModel(
    val intentId: String,
    val captureId: String,
    val intentType: String,
    val status: String,
    val primaryEvidenceJson: String,
    val primaryAction: String?,
    val dueAt: Long?,
    val expiresAt: Long?,
    val updatedAt: Long
)

data class ActiveIntentGroup(
    val intentType: String,
    val items: List<ActiveIntentUiModel>
)

interface ActiveIntentRepository {
    fun observeActiveGroups(): Flow<List<ActiveIntentGroup>>
    suspend fun getByCapture(captureId: String): List<ActiveIntentUiModel>
    suspend fun upsert(entity: ActiveIntentEntity)
    suspend fun resolve(intentId: String, reason: ActiveIntentResolutionReason, atMillis: Long = System.currentTimeMillis())
    suspend fun archive(intentId: String, atMillis: Long = System.currentTimeMillis())
    suspend fun expire(intentId: String, atMillis: Long = System.currentTimeMillis())
    suspend fun markNotInterested(intentId: String, atMillis: Long = System.currentTimeMillis())
    suspend fun invalidateCapture(captureId: String, atMillis: Long = System.currentTimeMillis())
}

class RoomActiveIntentRepository(
    private val activeIntentDao: ActiveIntentDao
) : ActiveIntentRepository {
    override fun observeActiveGroups(): Flow<List<ActiveIntentGroup>> =
        activeIntentDao.observeActive().map { rows ->
            rows.map { it.toUiModel() }
                .groupBy { it.intentType }
                .map { (intentType, items) -> ActiveIntentGroup(intentType, items.sortedByDescending { it.updatedAt }) }
                .sortedBy { it.intentType }
        }

    override suspend fun getByCapture(captureId: String): List<ActiveIntentUiModel> =
        activeIntentDao.getByCapture(captureId).map { it.toUiModel() }

    override suspend fun upsert(entity: ActiveIntentEntity) {
        activeIntentDao.upsert(entity)
    }

    override suspend fun resolve(intentId: String, reason: ActiveIntentResolutionReason, atMillis: Long) {
        activeIntentDao.transition(
            intentId = intentId,
            status = ActiveIntentStatus.RESOLVED.name,
            resolutionReason = reason.name,
            resolvedAt = atMillis,
            userConfirmed = true,
            updatedAt = atMillis
        )
    }

    override suspend fun archive(intentId: String, atMillis: Long) {
        activeIntentDao.transition(
            intentId = intentId,
            status = ActiveIntentStatus.ARCHIVED.name,
            resolutionReason = ActiveIntentResolutionReason.USER_ARCHIVED.name,
            resolvedAt = atMillis,
            userConfirmed = true,
            updatedAt = atMillis
        )
    }

    override suspend fun expire(intentId: String, atMillis: Long) {
        activeIntentDao.transition(
            intentId = intentId,
            status = ActiveIntentStatus.EXPIRED.name,
            resolutionReason = ActiveIntentResolutionReason.AUTO_EXPIRED.name,
            resolvedAt = atMillis,
            userConfirmed = false,
            updatedAt = atMillis
        )
    }

    override suspend fun markNotInterested(intentId: String, atMillis: Long) {
        activeIntentDao.transition(
            intentId = intentId,
            status = ActiveIntentStatus.ARCHIVED.name,
            resolutionReason = ActiveIntentResolutionReason.NOT_INTERESTED.name,
            resolvedAt = atMillis,
            userConfirmed = true,
            updatedAt = atMillis
        )
    }

    override suspend fun invalidateCapture(captureId: String, atMillis: Long) {
        activeIntentDao.invalidateByCapture(
            captureId = captureId,
            reason = ActiveIntentResolutionReason.SOURCE_DELETED.name,
            invalidatedAt = atMillis
        )
    }

    private fun ActiveIntentEntity.toUiModel(): ActiveIntentUiModel = ActiveIntentUiModel(
        intentId = intentId,
        captureId = captureId,
        intentType = intentType,
        status = status,
        primaryEvidenceJson = primaryEvidenceJson,
        primaryAction = primaryAction,
        dueAt = dueAt,
        expiresAt = expiresAt,
        updatedAt = updatedAt
    )
}
