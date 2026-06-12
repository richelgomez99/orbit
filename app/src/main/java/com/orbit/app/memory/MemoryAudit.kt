package com.orbit.app.memory

import com.orbit.app.audit.AuditLogWriter
import com.orbit.app.cloud.BudgetDecisionReason
import com.orbit.app.cloud.CloudCapability
import com.orbit.app.cloud.CloudUsageOutcome
import com.orbit.app.data.entity.AuditLogEntryEntity
import com.orbit.app.data.model.AuditAction
import org.json.JSONObject
import java.security.MessageDigest

/**
 * Builds local-only audit rows for Spec 005 memory operations.
 *
 * Callers pass raw query/payload material only long enough to compute a
 * SHA-256 digest; the resulting audit JSON stores counts, outcomes, and
 * digests, never memory text or raw search questions.
 */
class MemoryAudit(
    private val writer: AuditLogWriter = AuditLogWriter(),
) {
    fun upserted(
        requestId: String,
        envelopeId: String,
        payloadForDigest: String,
        latencyMs: Long,
        outcome: String = "success",
        capability: CloudCapability = CloudCapability.COMPACT_INDEX_UPSERT,
    ): AuditLogEntryEntity = writer.build(
        action = AuditAction.MEMORY_INDEX_UPSERTED,
        description = "Memory index upserted",
        envelopeId = envelopeId,
        extraJson = extras(
            endpoint = "upsert",
            requestId = requestId,
            envelopeId = envelopeId,
            payloadDigest = sha256(payloadForDigest),
            latencyMs = latencyMs,
            outcome = outcome,
            cloudCapability = capability,
            cloudOutcome = CloudUsageOutcome.SUCCESS,
        ),
    )

    fun tombstoned(
        requestId: String,
        envelopeId: String,
        reason: String,
        latencyMs: Long,
        outcome: String = "success",
        capability: CloudCapability = CloudCapability.COMPACT_INDEX_TOMBSTONE,
    ): AuditLogEntryEntity = writer.build(
        action = AuditAction.MEMORY_INDEX_TOMBSTONED,
        description = "Memory index tombstoned",
        envelopeId = envelopeId,
        extraJson = extras(
            endpoint = "tombstone",
            requestId = requestId,
            envelopeId = envelopeId,
            latencyMs = latencyMs,
            outcome = outcome,
            cloudCapability = capability,
            cloudOutcome = CloudUsageOutcome.SUCCESS,
            errorKind = null,
            extra = mapOf("reason" to reason),
        ),
    )

    fun searchRequested(
        requestId: String,
        query: String,
        resultCount: Int,
        latencyMs: Long,
        outcome: String,
    ): AuditLogEntryEntity = writer.build(
        action = AuditAction.MEMORY_SEARCH_REQUESTED,
        description = "Memory search requested",
        extraJson = extras(
            endpoint = "search",
            requestId = requestId,
            queryDigest = sha256(query),
            resultCount = resultCount,
            latencyMs = latencyMs,
            outcome = outcome,
        ),
    )

    fun semanticSearchRequested(
        requestId: String,
        query: String,
        resultCount: Int,
        latencyMs: Long,
        outcome: String,
        retrievalMode: String = "hybrid",
    ): AuditLogEntryEntity = writer.build(
        action = AuditAction.MEMORY_SEARCH_REQUESTED,
        description = "Semantic memory search requested",
        extraJson = extras(
            endpoint = "semantic_search",
            requestId = requestId,
            queryDigest = sha256(query),
            resultCount = resultCount,
            latencyMs = latencyMs,
            outcome = outcome,
            extra = mapOf("retrievalMode" to retrievalMode),
        ),
    )

    fun askRequested(
        requestId: String,
        question: String,
        resultCount: Int,
        latencyMs: Long,
        outcome: String,
    ): AuditLogEntryEntity = writer.build(
        action = AuditAction.MEMORY_ASK_REQUESTED,
        description = "Ask Orbit memory request",
        extraJson = extras(
            endpoint = "ask",
            requestId = requestId,
            queryDigest = sha256(question),
            resultCount = resultCount,
            latencyMs = latencyMs,
            outcome = outcome,
        ),
    )

    fun groundedAskRequested(
        requestId: String,
        question: String,
        citationCount: Int,
        candidateCount: Int,
        latencyMs: Long,
        outcome: String,
        answerStatus: String,
    ): AuditLogEntryEntity = writer.build(
        action = AuditAction.MEMORY_ASK_REQUESTED,
        description = "Grounded Ask Orbit memory request",
        extraJson = extras(
            endpoint = "grounded_ask",
            requestId = requestId,
            queryDigest = sha256(question),
            resultCount = citationCount,
            latencyMs = latencyMs,
            outcome = outcome,
            extra = mapOf(
                "answerStatus" to answerStatus,
                "candidateCount" to candidateCount,
            ),
        ),
    )

    fun embeddingUpdated(
        requestId: String,
        envelopeId: String,
        compactInputForDigest: String,
        embeddingModel: String,
        dimensions: Int,
        latencyMs: Long,
        outcome: String = "success",
    ): AuditLogEntryEntity = writer.build(
        action = AuditAction.MEMORY_INDEX_UPSERTED,
        description = "Memory embedding updated",
        envelopeId = envelopeId,
        extraJson = extras(
            endpoint = "embed",
            requestId = requestId,
            envelopeId = envelopeId,
            payloadDigest = sha256(compactInputForDigest),
            latencyMs = latencyMs,
            outcome = outcome,
            extra = mapOf(
                "embeddingModel" to embeddingModel,
                "dimensions" to dimensions,
            ),
        ),
    )

    fun fallbackUsed(
        requestId: String,
        endpoint: String,
        reason: String,
        resultCount: Int,
        latencyMs: Long,
    ): AuditLogEntryEntity = writer.build(
        action = AuditAction.MEMORY_SEARCH_REQUESTED,
        description = "Memory local fallback used",
        extraJson = extras(
            endpoint = endpoint,
            requestId = requestId,
            resultCount = resultCount,
            latencyMs = latencyMs,
            outcome = "local_fallback",
            extra = mapOf("reason" to reason),
        ),
    )

    fun gatewayFailed(
        requestId: String,
        endpoint: String,
        errorKind: String,
        latencyMs: Long? = null,
        envelopeId: String? = null,
        capability: CloudCapability? = null,
    ): AuditLogEntryEntity = writer.build(
        action = AuditAction.MEMORY_GATEWAY_FAILED,
        description = "Memory gateway failed",
        envelopeId = envelopeId,
        extraJson = extras(
            endpoint = endpoint,
            requestId = requestId,
            envelopeId = envelopeId,
            latencyMs = latencyMs,
            outcome = "failed",
            errorKind = errorKind,
            cloudCapability = capability,
            cloudOutcome = CloudUsageOutcome.FAILED,
        ),
    )

    fun syncSkipped(
        requestId: String,
        reason: String,
        envelopeId: String? = null,
        capability: CloudCapability? = null,
    ): AuditLogEntryEntity = writer.build(
        action = AuditAction.MEMORY_SYNC_SKIPPED,
        description = "Memory sync skipped",
        envelopeId = envelopeId,
        extraJson = extras(
            endpoint = "sync",
            requestId = requestId,
            envelopeId = envelopeId,
            outcome = "skipped",
            cloudCapability = capability,
            cloudOutcome = CloudUsageOutcome.SKIPPED,
            cloudReason = BudgetDecisionReason.DISABLED_BY_USER,
            extra = mapOf("reason" to reason),
        ),
    )

    private fun extras(
        endpoint: String,
        requestId: String,
        envelopeId: String? = null,
        payloadDigest: String? = null,
        queryDigest: String? = null,
        resultCount: Int? = null,
        latencyMs: Long? = null,
        outcome: String,
        errorKind: String? = null,
        cloudCapability: CloudCapability? = null,
        cloudOutcome: CloudUsageOutcome? = null,
        cloudReason: BudgetDecisionReason? = null,
        extra: Map<String, Any> = emptyMap(),
    ): String {
        val json = JSONObject()
            .put("provider", PROVIDER)
            .put("endpoint", endpoint)
            .put("requestId", requestId)
            .put("outcome", outcome)
        envelopeId?.let { json.put("envelopeId", it) }
        payloadDigest?.let { json.put("payloadDigest", it) }
        queryDigest?.let { json.put("queryDigest", it) }
        resultCount?.let { json.put("resultCount", it) }
        latencyMs?.let { json.put("latencyMs", it) }
        errorKind?.let { json.put("errorKind", it) }
        cloudCapability?.let { json.put("capability", it.name) }
        cloudOutcome?.let { json.put("cloudOutcome", it.name) }
        cloudReason?.let { json.put("cloudReason", it.name) }
        extra.forEach { (key, value) -> json.put(key, value) }
        return json.toString()
    }

    companion object {
        const val PROVIDER: String = "mongodb_atlas"

        fun sha256(value: String): String {
            val bytes = MessageDigest.getInstance("SHA-256").digest(value.toByteArray(Charsets.UTF_8))
            return bytes.joinToString(separator = "") { "%02x".format(it) }
        }
    }
}
