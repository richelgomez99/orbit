package com.orbit.app.cloud

import com.orbit.app.audit.AuditLogWriter
import com.orbit.app.data.entity.AuditLogEntryEntity
import com.orbit.app.data.model.AuditAction
import org.json.JSONObject
import java.security.MessageDigest

class CloudUsageReceiptWriter(
    private val writer: AuditLogWriter = AuditLogWriter(),
) {
    fun receipt(
        requestId: String,
        capability: CloudCapability,
        outcome: CloudUsageOutcome,
        reason: BudgetDecisionReason? = null,
        endpoint: String? = null,
        envelopeId: String? = null,
        latencyMs: Long? = null,
        resultCount: Int? = null,
        provider: String? = null,
        model: String? = null,
        inputForDigest: String? = null,
        payloadForDigest: String? = null,
        estimatedInputTokens: Int? = null,
        estimatedOutputTokens: Int? = null,
        estimatedCostCents: Long? = null,
    ): AuditLogEntryEntity {
        val extra = JSONObject()
            .put("requestId", requestId)
            .put("capability", capability.name)
            .put("outcome", outcome.name)
        reason?.let { extra.put("reason", it.name) }
        endpoint?.let { extra.put("endpoint", it) }
        envelopeId?.let { extra.put("envelopeId", it) }
        latencyMs?.let { extra.put("latencyMs", it) }
        resultCount?.let { extra.put("resultCount", it) }
        provider?.let { extra.put("provider", it) }
        model?.let { extra.put("model", it) }
        inputForDigest?.let { extra.put("inputDigest", sha256(it)) }
        payloadForDigest?.let { extra.put("payloadDigest", sha256(it)) }
        estimatedInputTokens?.let { extra.put("estimatedInputTokens", it) }
        estimatedOutputTokens?.let { extra.put("estimatedOutputTokens", it) }
        estimatedCostCents?.let { extra.put("estimatedCostCents", it) }

        return writer.build(
            action = AuditAction.CLOUD_USAGE_RECORDED,
            description = "Cloud usage decision recorded",
            envelopeId = envelopeId,
            extraJson = extra.toString(),
        )
    }

    fun skippedDecision(
        decision: BudgetDecision,
        endpoint: String? = null,
        envelopeId: String? = null,
        inputForDigest: String? = null,
    ): AuditLogEntryEntity = receipt(
        requestId = decision.requestId,
        capability = decision.capability,
        outcome = if (decision.reason == BudgetDecisionReason.BUDGET_EXHAUSTED) {
            CloudUsageOutcome.BUDGET_DENIED
        } else {
            CloudUsageOutcome.SKIPPED
        },
        reason = decision.reason,
        endpoint = endpoint,
        envelopeId = envelopeId,
        inputForDigest = inputForDigest,
        estimatedCostCents = decision.estimatedCostCents,
    )

    companion object {
        fun sha256(value: String): String {
            val bytes = MessageDigest.getInstance("SHA-256").digest(value.toByteArray(Charsets.UTF_8))
            return bytes.joinToString(separator = "") { "%02x".format(it) }
        }
    }
}
