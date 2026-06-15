package com.orbit.app.data

import com.orbit.app.audit.AuditLogWriter
import com.orbit.app.data.dao.ActiveIntentDao
import com.orbit.app.data.dao.AuditLogDao
import com.orbit.app.data.ipc.ActiveIntentParcel
import com.orbit.app.data.ipc.IActiveIntentObserver
import com.orbit.app.data.model.AuditAction
import com.orbit.app.resolution.ResolutionActor
import com.orbit.app.resolution.ResolutionKind
import com.orbit.app.resolution.ResolutionReceipt
import com.orbit.app.resolution.ResolutionSurface
import com.orbit.app.resolution.ResolutionSurfacingVerdict
import com.orbit.app.resolution.ResolutionTargetType
import com.orbit.app.understanding.ActiveIntentResolver
import com.orbit.app.understanding.domain.ResolutionReason
import com.orbit.app.understanding.domain.UnderstandingMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.util.concurrent.ConcurrentHashMap

class ActiveIntentRepository(
    private val activeIntentDao: ActiveIntentDao,
    private val auditLogDao: AuditLogDao? = null,
    private val scope: CoroutineScope,
    private val clock: () -> Long = { System.currentTimeMillis() },
    private val auditWriter: AuditLogWriter = AuditLogWriter(clock = clock),
    private val resolver: ActiveIntentResolver = ActiveIntentResolver(),
    private val resolutionReceiptSink: ResolutionReceiptSink? = null,
    private val resolutionVerdictProvider: ResolutionVerdictProvider? = null
) {
    private val observerJobs = ConcurrentHashMap<IBinderKey, Job>()

    fun observeActiveIntents(observer: IActiveIntentObserver) {
        val key = IBinderKey(observer.asBinder())
        observerJobs.remove(key)?.cancel()
        val job = scope.launch(Dispatchers.IO) {
            activeIntentDao.observeCleanupQueue().collectLatest { rows ->
                try {
                    val visibleRows = rows.filter { row ->
                        resolutionVerdictProvider?.verdictFor(
                            targetType = ResolutionTargetType.ACTIVE_INTENT,
                            targetId = row.intentId,
                            nowMillis = clock(),
                            surface = ResolutionSurface.CLEANUP_QUEUE,
                        )?.verdict?.let { verdict ->
                            verdict == ResolutionSurfacingVerdict.ACTIVE
                        } ?: true
                    }
                    observer.onActiveIntentsChanged(visibleRows.map(ActiveIntentParcel::fromEntity))
                } catch (_: android.os.RemoteException) {
                    observerJobs.remove(key)?.cancel()
                }
            }
        }
        observerJobs[key] = job
    }

    fun stopObservingActiveIntents(observer: IActiveIntentObserver) {
        val key = IBinderKey(observer.asBinder())
        observerJobs.remove(key)?.cancel()
    }

    suspend fun resolveActiveIntent(
        intentId: String,
        resolutionReason: String,
        userConfirmed: Boolean
    ): Boolean {
        val entity = activeIntentDao.getById(intentId) ?: return false
        val reason = runCatching { ResolutionReason.valueOf(resolutionReason) }.getOrNull() ?: return false
        val resolved = resolver.resolve(
            entity = entity,
            reason = reason,
            resolvedAt = clock(),
            userConfirmed = userConfirmed
        )
        return activeIntentDao.markResolved(
            intentId = intentId,
            status = resolved.status.name,
            resolutionReason = resolved.resolutionReason?.name,
            resolvedAt = resolved.resolvedAt,
            userConfirmed = resolved.userConfirmed,
            updatedAt = resolved.updatedAt
        ).also { changed ->
            if (changed > 0) {
                recordResolutionReceipt(entity, reason, resolved.resolvedAt ?: clock(), userConfirmed)
            }
        } > 0
    }

    suspend fun markNotNow(intentId: String): Boolean {
        val entity = activeIntentDao.getById(intentId) ?: return false
        return resolutionReceiptSink?.record(
            ResolutionReceipt(
                id = "active-intent-not-now:$intentId:${clock()}",
                targetType = ResolutionTargetType.ACTIVE_INTENT,
                targetId = intentId,
                envelopeId = entity.captureId,
                relatedType = ResolutionTargetType.ENVELOPE,
                relatedId = entity.captureId,
                kind = ResolutionKind.NOT_NOW,
                actor = ResolutionActor.USER,
                reason = "user_not_now",
                occurredAtMillis = clock(),
                metadataJson = JSONObject()
                    .put("intentId", intentId)
                    .put("captureId", entity.captureId)
                    .toString(),
            )
        ) == true
    }

    suspend fun snooze(intentId: String, untilMillis: Long): Boolean {
        val now = clock()
        if (untilMillis <= now) return false
        val entity = activeIntentDao.getById(intentId) ?: return false
        return resolutionReceiptSink?.record(
            ResolutionReceipt(
                id = "active-intent-snooze:$intentId:$untilMillis",
                targetType = ResolutionTargetType.ACTIVE_INTENT,
                targetId = intentId,
                envelopeId = entity.captureId,
                relatedType = ResolutionTargetType.ENVELOPE,
                relatedId = entity.captureId,
                kind = ResolutionKind.SNOOZED,
                actor = ResolutionActor.USER,
                reason = "user_snoozed",
                occurredAtMillis = now,
                effectiveUntilMillis = untilMillis,
                metadataJson = JSONObject()
                    .put("intentId", intentId)
                    .put("captureId", entity.captureId)
                    .put("effectiveUntilMillis", untilMillis)
                    .toString(),
            )
        ) == true
    }

    suspend fun requestEscalation(intentId: String, mode: String): Boolean {
        val auditDao = auditLogDao ?: return false
        val entity = activeIntentDao.getById(intentId) ?: return false
        val requestedMode = runCatching { UnderstandingMode.valueOf(mode) }.getOrNull() ?: return false
        if (requestedMode == UnderstandingMode.BASIC) return false

        auditDao.insert(
            auditWriter.build(
                action = AuditAction.ACTIVE_INTENT_ESCALATION_REQUESTED,
                description = "Orbit decision review requested",
                envelopeId = entity.captureId,
                extraJson = JSONObject().apply {
                    put("intentId", entity.intentId)
                    put("captureId", entity.captureId)
                    put("mode", requestedMode.name)
                    put("intentType", entity.intentType.name)
                    put("completionKeyStatus", entity.completionKeyStatus.name)
                    put("reviewContext", ActiveIntentReviewContext.build(entity, requestedMode))
                }.toString()
            )
        )
        val reviewedAt = clock()
        activeIntentDao.upsert(
            entity.copy(
                primaryEvidenceJson = decisionReviewEvidence(entity, requestedMode, reviewedAt),
                primaryAction = "ask_orbit_review",
                updatedAt = reviewedAt
            )
        )
        return true
    }

    private suspend fun recordResolutionReceipt(
        entity: com.orbit.app.data.entity.ActiveIntentEntity,
        reason: ResolutionReason,
        occurredAtMillis: Long,
        userConfirmed: Boolean,
    ) {
        val kind = when (reason) {
            ResolutionReason.NOT_INTERESTED,
            ResolutionReason.USER_ARCHIVED -> ResolutionKind.DISMISSED
            ResolutionReason.AUTO_EXPIRED -> ResolutionKind.STALE
            ResolutionReason.INVALIDATED,
            ResolutionReason.SOURCE_DELETED -> ResolutionKind.INVALIDATED
            ResolutionReason.BOUGHT,
            ResolutionReason.COOKED,
            ResolutionReason.READ_OR_WATCHED,
            ResolutionReason.VISITED,
            ResolutionReason.REPLIED_OR_DONE -> ResolutionKind.RESOLVED
        }
        resolutionReceiptSink?.record(
            ResolutionReceipt(
                id = "active-intent:${entity.intentId}:$occurredAtMillis",
                targetType = ResolutionTargetType.ACTIVE_INTENT,
                targetId = entity.intentId,
                envelopeId = entity.captureId,
                relatedType = ResolutionTargetType.ENVELOPE,
                relatedId = entity.captureId,
                kind = kind,
                actor = if (userConfirmed) ResolutionActor.USER else ResolutionActor.SYSTEM,
                reason = reason.name,
                occurredAtMillis = occurredAtMillis,
                metadataJson = JSONObject()
                    .put("intentId", entity.intentId)
                    .put("captureId", entity.captureId)
                    .put("resolutionReason", reason.name)
                    .put("userConfirmed", userConfirmed)
                    .toString(),
            )
        )
    }

    private fun decisionReviewEvidence(
        entity: com.orbit.app.data.entity.ActiveIntentEntity,
        mode: UnderstandingMode,
        reviewedAt: Long
    ): String {
        val prior = runCatching { JSONObject(entity.primaryEvidenceJson) }.getOrNull()
        val priorExcerpt = prior?.optString("excerpt")?.takeIf { it.isNotBlank() }
        val category = entity.intentType
        val reason = when (category) {
            com.orbit.app.understanding.domain.IntentCategory.CHAT_ACTION ->
                "Orbit thinks this is a message follow-up. Decide whether you replied, still need to reply, or can clear it."
            com.orbit.app.understanding.domain.IntentCategory.EVENT_TICKET_RESERVATION ->
                "Orbit thinks this may be an event, ticket, reservation, or booking. Decide whether to save it, attend it, or clear it."
            com.orbit.app.understanding.domain.IntentCategory.BUY_LATER_PRODUCT,
            com.orbit.app.understanding.domain.IntentCategory.GIFT_IDEA ->
                "Orbit thinks this is a buy-or-save decision. Decide whether to keep it, buy it, or clear it."
            com.orbit.app.understanding.domain.IntentCategory.READ_OR_WATCH_LATER ->
                "Orbit thinks this is something saved for later. Decide whether to read/watch it, keep it, or clear it."
            com.orbit.app.understanding.domain.IntentCategory.MAYBE_OLD_OR_INACTIVE ->
                "Orbit thinks this may no longer need follow-up. Decide whether it is still useful or safe to clear."
            com.orbit.app.understanding.domain.IntentCategory.UNKNOWN ->
                "Orbit needs one human hint to know the next action. Add context if it matters, or clear it if it does not."
            else ->
                "Orbit reviewed the local clues. Decide whether this still needs action or can be cleared."
        }
        return JSONObject().apply {
            put("kind", "ORBIT_REVIEW")
            put("label", category.name)
            put("source", "orbit_review")
            put("reason", reason)
            put("createdAt", reviewedAt)
            priorExcerpt?.let { put("excerpt", it.take(240)) }
            put("confidence", if (mode == UnderstandingMode.SMART) 0.72 else 0.62)
        }.toString().take(com.orbit.app.understanding.domain.CompactEvidencePayload.MAX_JSON_CHARS)
    }
}
