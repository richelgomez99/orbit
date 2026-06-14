package com.orbit.app.understanding

import com.orbit.app.audit.AuditLogWriter
import com.orbit.app.data.dao.ActiveIntentDao
import com.orbit.app.data.dao.AuditLogDao
import com.orbit.app.data.dao.CaptureUnderstandingDao
import com.orbit.app.data.dao.EvidenceBundleDao
import com.orbit.app.data.entity.CaptureUnderstandingEntity
import com.orbit.app.data.entity.EvidenceBundleEntity
import com.orbit.app.data.model.AuditAction
import com.orbit.app.data.ResolutionReceiptSink
import com.orbit.app.resolution.ResolutionActor
import com.orbit.app.resolution.ResolutionKind
import com.orbit.app.resolution.ResolutionReceipt
import com.orbit.app.resolution.ResolutionTargetType
import com.orbit.app.understanding.domain.ActiveIntentStatus
import org.json.JSONObject

class BasicUnderstandingWriter(
    private val captureUnderstandingDao: CaptureUnderstandingDao,
    private val evidenceBundleDao: EvidenceBundleDao,
    private val activeIntentDao: ActiveIntentDao,
    private val auditLogDao: AuditLogDao? = null,
    private val auditWriter: AuditLogWriter = AuditLogWriter(),
    private val resolutionReceiptSink: ResolutionReceiptSink? = null,
    private val engine: BasicUnderstandingEngine = BasicUnderstandingEngine(),
    private val projector: ActiveIntentProjector = ActiveIntentProjector { result -> "basic:${result.captureId}" },
    private val evidenceIdFactory: (captureId: String, index: Int) -> String = { captureId, index ->
        "$captureId:basic:$index"
    }
) {

    suspend fun persist(input: BasicUnderstandingInput): BasicUnderstandingResult {
        val result = engine.understand(input)
        val existingUnderstanding = captureUnderstandingDao.getByCaptureId(result.captureId)
        val duplicateUnderstanding = result.contentHashHex
            ?.let { hash ->
                captureUnderstandingDao.getByContentHash(hash).firstOrNull { candidate ->
                    candidate.captureId != result.captureId && candidate.invalidatedAt == null
                }
            }
        captureUnderstandingDao.upsert(result.toEntity(existingUnderstanding?.createdAt ?: input.nowMillis, input.nowMillis))

        evidenceBundleDao.deleteByCaptureId(result.captureId)
        evidenceBundleDao.insertAll(
            result.evidencePayloadJson.mapIndexed { index, payload ->
                EvidenceBundleEntity(
                    id = evidenceIdFactory(result.captureId, index),
                    captureId = result.captureId,
                    bundleType = "BASIC_LOCAL_EVIDENCE",
                    payloadJson = payload,
                    createdAt = input.nowMillis
                )
            }
        )

        if (duplicateUnderstanding != null) {
            recordDuplicateSuppression(result, duplicateUnderstanding.captureId)
            recordDuplicateSuppressionReceipt(result, duplicateUnderstanding.captureId, input.nowMillis)
            return result
        }
        val projected = projector.project(result, input.nowMillis).entity ?: return result
        val existingIntent = activeIntentDao.getById(projected.intentId)
        if (existingIntent == null || existingIntent.status == ActiveIntentStatus.ACTIVE) {
            val preserveOrbitReview = existingIntent?.primaryEvidenceJson?.isOrbitReviewEvidence() == true
            activeIntentDao.upsert(
                projected.copy(
                    createdAt = existingIntent?.createdAt ?: projected.createdAt,
                    primaryEvidenceJson = if (preserveOrbitReview) existingIntent.primaryEvidenceJson else projected.primaryEvidenceJson,
                    primaryAction = if (preserveOrbitReview) existingIntent.primaryAction else projected.primaryAction,
                    updatedAt = input.nowMillis
                )
            )
        }
        return result
    }

    suspend fun refreshActiveFromSidecars(
        limit: Int = DEFAULT_REFRESH_LIMIT,
        nowMillis: Long = System.currentTimeMillis()
    ): Int {
        var refreshed = 0
        activeIntentDao.getActiveBasic(limit.coerceAtLeast(0)).forEach { activeIntent ->
            val understanding = captureUnderstandingDao.getByCaptureId(activeIntent.captureId)
                ?.takeIf { it.invalidatedAt == null }
                ?: return@forEach
            val source = understanding.sourceIdentityJson.parseSourceIdentity()
            persist(
                BasicUnderstandingInput(
                    captureId = understanding.captureId,
                    textContent = understanding.summaryText,
                    sourceAppLabel = source.sourceAppLabel,
                    appCategory = source.appCategory,
                    canonicalUrl = understanding.canonicalUrl ?: source.canonicalUrl,
                    capturedAtMillis = understanding.createdAt,
                    nowMillis = nowMillis
                )
            )
            refreshed += 1
        }
        return refreshed
    }

    private fun BasicUnderstandingResult.toEntity(createdAt: Long, updatedAt: Long): CaptureUnderstandingEntity =
        CaptureUnderstandingEntity(
            captureId = captureId,
            mode = mode,
            status = status,
            category = category,
            categoryConfidence = categoryConfidence,
            title = title,
            summaryText = summaryText,
            completionKeyJson = completionKey?.toCompactJson(),
            completionKeyStatus = completionKeyStatus,
            sourceIdentityJson = sourceIdentity.toCompactJson(),
            contentHashHex = contentHashHex,
            canonicalUrl = canonicalUrl,
            groundingConstraintsJson = groundingConstraints.toCompactJson(),
            createdAt = createdAt,
            updatedAt = updatedAt,
            invalidatedAt = null
        )

    private data class StoredSourceIdentity(
        val sourceAppLabel: String?,
        val appCategory: String?,
        val canonicalUrl: String?
    )

    private fun String?.parseSourceIdentity(): StoredSourceIdentity {
        val json = runCatching { JSONObject(this ?: "{}") }.getOrNull()
        return StoredSourceIdentity(
            sourceAppLabel = json?.optString("label")?.takeIf { it.isNotBlank() },
            appCategory = json?.optString("category")?.takeIf { it.isNotBlank() },
            canonicalUrl = json?.optString("canonicalUrl")?.takeIf { it.isNotBlank() }
        )
    }

    private fun String.isOrbitReviewEvidence(): Boolean =
        runCatching { JSONObject(this).optString("kind") }.getOrNull() == "ORBIT_REVIEW"

    private suspend fun recordDuplicateSuppression(result: BasicUnderstandingResult, existingCaptureId: String) {
        val hash = result.contentHashHex ?: return
        auditLogDao?.insert(
            auditWriter.build(
                action = AuditAction.ACTIVE_INTENT_DUPLICATE_SUPPRESSED,
                description = "Suppressed duplicate Active Intent projection",
                envelopeId = result.captureId,
                extraJson = JSONObject()
                    .put("captureId", result.captureId)
                    .put("existingCaptureId", existingCaptureId)
                    .put("contentHashHex", hash)
                    .put("outcome", "active_intent_projection_skipped")
                    .toString()
            )
        )
    }

    private suspend fun recordDuplicateSuppressionReceipt(
        result: BasicUnderstandingResult,
        existingCaptureId: String,
        nowMillis: Long,
    ) {
        val hash = result.contentHashHex ?: return
        resolutionReceiptSink?.record(
            ResolutionReceipt(
                id = "understanding-duplicate:$existingCaptureId:${result.captureId}",
                targetType = ResolutionTargetType.ENVELOPE,
                targetId = existingCaptureId,
                envelopeId = existingCaptureId,
                relatedType = ResolutionTargetType.ENVELOPE,
                relatedId = result.captureId,
                kind = ResolutionKind.DUPLICATE_RECAPTURE,
                actor = ResolutionActor.DUPLICATE_DETECTOR,
                reason = "CONTENT_HASH",
                occurredAtMillis = nowMillis,
                metadataJson = JSONObject()
                    .put("contentHashHex", hash)
                    .put("duplicateCaptureId", result.captureId)
                    .toString(),
            )
        )
    }

    private companion object {
        const val DEFAULT_REFRESH_LIMIT = 50
    }
}
