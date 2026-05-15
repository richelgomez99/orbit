package com.capsule.app.understanding

import com.capsule.app.data.entity.DeletionInvalidationEntity
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

object DeletionInvalidationCascade {
    fun captureDeleted(
        captureId: String,
        auditTraceId: String?,
        atMillis: Long
    ): DeletionInvalidationEntity = DeletionInvalidationEntity(
        id = UUID.randomUUID().toString(),
        captureId = captureId,
        reason = InvalidationReason.CAPTURE_DELETED,
        affectedRecordRefsJson = affectedRefs(captureId),
        downstreamEligibility = DownstreamEligibility.INELIGIBLE,
        auditTraceId = auditTraceId,
        createdAt = atMillis,
        cloudReceiptStatus = CloudReceiptStatus.NOT_APPLICABLE
    )

    private fun affectedRefs(captureId: String): String {
        val rows = JSONArray()
        listOf(
            "source_identity",
            "canonical_url",
            "evidence_bundle",
            "understanding_job",
            "capture_understanding",
            "correction_feedback",
            "future_downstream_inputs"
        ).forEach { type ->
            rows.put(
                JSONObject()
                    .put("type", type)
                    .put("captureId", captureId)
            )
        }
        return rows.toString()
    }
}