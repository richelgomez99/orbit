package com.orbit.app.understanding

import com.orbit.app.data.entity.ActiveIntentEntity
import com.orbit.app.understanding.domain.ActiveIntentStatus
import com.orbit.app.understanding.domain.CompactEvidencePayload
import com.orbit.app.understanding.domain.CompletionKeyStatus
import com.orbit.app.understanding.domain.IntentCategory
import org.json.JSONObject
import java.util.UUID

enum class ProjectionOutcome {
    ACTIVE,
    MAYBE_OLD_OR_INACTIVE,
    NON_ACTIONABLE
}

data class ActiveIntentProjection(
    val outcome: ProjectionOutcome,
    val entity: ActiveIntentEntity?
)

class ActiveIntentProjector(
    private val intentIdFactory: (BasicUnderstandingResult) -> String = { UUID.randomUUID().toString() }
) {

    fun project(result: BasicUnderstandingResult, nowMillis: Long): ActiveIntentProjection {
        if (result.completionKeyStatus == CompletionKeyStatus.NOT_ACTIONABLE &&
            result.category != IntentCategory.MAYBE_OLD_OR_INACTIVE
        ) {
            return ActiveIntentProjection(ProjectionOutcome.NON_ACTIONABLE, null)
        }

        val outcome = when (result.category) {
            IntentCategory.MAYBE_OLD_OR_INACTIVE -> ProjectionOutcome.MAYBE_OLD_OR_INACTIVE
            else -> ProjectionOutcome.ACTIVE
        }

        return ActiveIntentProjection(
            outcome = outcome,
            entity = ActiveIntentEntity(
                intentId = intentIdFactory(result),
                captureId = result.captureId,
                intentType = result.category,
                status = ActiveIntentStatus.ACTIVE,
                completionKeyJson = result.completionKey?.toCompactJson(),
                completionKeyStatus = result.completionKeyStatus,
                primaryEvidenceJson = primaryEvidence(result),
                primaryAction = primaryAction(result.category, result.completionKeyStatus),
                dueAt = null,
                expiresAt = null,
                resolutionReason = null,
                resolvedAt = null,
                userConfirmed = false,
                createdAt = nowMillis,
                updatedAt = nowMillis
            )
        )
    }

    private fun primaryEvidence(result: BasicUnderstandingResult): String {
        result.evidencePayloadJson
            .mapNotNull { payload -> runCatching { JSONObject(payload) }.getOrNull()?.let { it to payload } }
            .sortedWith(
                compareByDescending<Pair<JSONObject, String>> { (json, _) -> if (json.has("excerpt")) 1 else 0 }
                    .thenByDescending { (json, _) -> if (json.optString("kind") == "COMPLETION_KEY") 1 else 0 }
                    .thenByDescending { (json, _) -> if (json.optString("kind") == "CATEGORY") 1 else 0 }
            )
            .firstOrNull()
            ?.second
            ?.let { return it.take(CompactEvidencePayload.MAX_JSON_CHARS) }
        return JSONObject().apply {
            put("kind", "LIMITED")
            put("label", result.category.name)
            put("source", "basic")
            put("confidence", result.categoryConfidence.coerceIn(0f, 1f).toDouble())
        }.toString().take(CompactEvidencePayload.MAX_JSON_CHARS)
    }

    private fun primaryAction(category: IntentCategory, completionKeyStatus: CompletionKeyStatus): String {
        if (completionKeyStatus == CompletionKeyStatus.NEEDS_ESCALATION) return "add_context"
        return when (category) {
            IntentCategory.BUY_LATER_PRODUCT -> "buy_or_skip"
            IntentCategory.RECIPE -> "cook_or_archive"
            IntentCategory.QR_OR_BARCODE -> "scan_or_open"
            IntentCategory.RECEIPT_OR_ORDER -> "track_or_archive"
            IntentCategory.EVENT_TICKET_RESERVATION -> "attend_or_archive"
            IntentCategory.COUPON_OR_PROMO -> "redeem_or_archive"
            IntentCategory.READ_OR_WATCH_LATER -> "read_watch_or_archive"
            IntentCategory.PLACE_OR_TRAVEL_IDEA -> "visit_or_archive"
            IntentCategory.GIFT_IDEA -> "save_gift_or_dismiss"
            IntentCategory.CHAT_ACTION -> "reply_or_done"
            IntentCategory.MAYBE_OLD_OR_INACTIVE -> "archive_or_keep"
            IntentCategory.UNKNOWN -> "add_context"
        }
    }
}