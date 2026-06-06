package com.orbit.app.data

import androidx.room.withTransaction
import com.orbit.app.audit.AuditLogWriter
import com.orbit.app.data.dao.MemoryCandidateProjection
import com.orbit.app.data.dao.PromotedMemoryProjection
import com.orbit.app.data.entity.MemoryCandidateEntity
import com.orbit.app.data.entity.MemoryCandidateSupportEntity
import com.orbit.app.data.entity.PromotedMemoryEntity
import com.orbit.app.data.entity.PromotedMemorySupportEntity
import com.orbit.app.data.ipc.IMemoryCandidateObserver
import com.orbit.app.data.ipc.IPromotedMemoryObserver
import com.orbit.app.data.ipc.MemoryCandidateParcel
import com.orbit.app.data.ipc.MemoryDecisionResultParcel
import com.orbit.app.data.ipc.PromotedMemoryParcel
import com.orbit.app.data.model.AuditAction
import com.orbit.app.data.model.MemoryCandidateKind
import com.orbit.app.data.model.MemoryCandidateSource
import com.orbit.app.data.model.MemoryCandidateState
import com.orbit.app.data.model.MemorySensitivity
import com.orbit.app.data.model.MemorySupportType
import com.orbit.app.data.model.PromotedMemoryConfidence
import com.orbit.app.data.model.PromotedMemoryKind
import com.orbit.app.data.model.PromotedMemorySource
import com.orbit.app.data.model.PromotedMemoryState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class MemoryRepositoryDelegate(
    private val database: OrbitDatabase,
    private val auditWriter: AuditLogWriter,
    private val scope: CoroutineScope,
    private val clock: () -> Long = { System.currentTimeMillis() }
) {
    private val candidateDao = database.memoryCandidateDao()
    private val candidateSupportDao = database.memoryCandidateSupportDao()
    private val promotedDao = database.promotedMemoryDao()
    private val promotedSupportDao = database.promotedMemorySupportDao()
    private val envelopeDao = database.intentEnvelopeDao()
    private val auditDao = database.auditLogDao()

    private val observerJobs = ConcurrentHashMap<IBinderKey, Job>()

    fun observePendingCandidates(limit: Int, observer: IMemoryCandidateObserver) {
        val key = IBinderKey(observer.asBinder())
        observerJobs.remove(key)?.cancel()
        val cappedLimit = limit.coerceIn(1, 50)
        val job = scope.launch(Dispatchers.IO) {
            candidateDao.observePendingProjections(cappedLimit).collectLatest { rows ->
                try {
                    observer.onMemoryCandidatesChanged(rows.map { it.toParcel() })
                } catch (_: android.os.RemoteException) {
                    observerJobs.remove(key)?.cancel()
                }
            }
        }
        observerJobs[key] = job
    }

    fun stopObservingCandidates(observer: IMemoryCandidateObserver) {
        val key = IBinderKey(observer.asBinder())
        observerJobs.remove(key)?.cancel()
    }

    fun observePromotedMemories(limit: Int, observer: IPromotedMemoryObserver) {
        val key = IBinderKey(observer.asBinder())
        observerJobs.remove(key)?.cancel()
        val cappedLimit = limit.coerceIn(1, 50)
        val job = scope.launch(Dispatchers.IO) {
            promotedDao.observeActiveProjections(cappedLimit).collectLatest { rows ->
                try {
                    observer.onPromotedMemoriesChanged(rows.map { it.toParcel() })
                } catch (_: android.os.RemoteException) {
                    observerJobs.remove(key)?.cancel()
                }
            }
        }
        observerJobs[key] = job
    }

    fun stopObservingPromotedMemories(observer: IPromotedMemoryObserver) {
        val key = IBinderKey(observer.asBinder())
        observerJobs.remove(key)?.cancel()
    }

    suspend fun acceptCandidate(
        candidateId: String,
        editedLabel: String?,
        editedFactText: String?
    ): MemoryDecisionResultParcel {
        val now = clock()
        var result = MemoryDecisionResultParcel(
            ok = false,
            candidateId = candidateId,
            memoryId = null,
            status = "not_found",
            message = "I could not find that memory suggestion."
        )
        database.withTransaction {
            val candidate = candidateDao.getById(candidateId)
            if (candidate == null) return@withTransaction

            promotedDao.findByCandidateId(candidateId)?.let { existing ->
                result = MemoryDecisionResultParcel(
                    ok = true,
                    candidateId = candidateId,
                    memoryId = existing.id,
                    status = "already_promoted",
                    message = "Already remembered."
                )
                return@withTransaction
            }

            if (candidate.state !in setOf(MemoryCandidateState.PENDING, MemoryCandidateState.ASKED)) {
                result = MemoryDecisionResultParcel(
                    ok = false,
                    candidateId = candidateId,
                    memoryId = null,
                    status = "not_pending",
                    message = "That memory suggestion is no longer pending."
                )
                return@withTransaction
            }

            val support = candidateSupportDao.listForCandidate(candidateId)
            if (support.isEmpty()) {
                candidateDao.markTerminal(candidateId, MemoryCandidateState.INVALIDATED, "source_missing", now)
                result = MemoryDecisionResultParcel(
                    ok = false,
                    candidateId = candidateId,
                    memoryId = null,
                    status = "source_missing",
                    message = "I could not find saved evidence for that memory."
                )
                return@withTransaction
            }

            val label = editedLabel?.trim()?.takeIf { it.isNotEmpty() } ?: candidate.displayLabel
            val factObject = editedFactText?.trim()?.takeIf { it.isNotEmpty() } ?: candidate.objectValue
            if (label.length > 200 || factObject.length > 300) {
                result = MemoryDecisionResultParcel(
                    ok = false,
                    candidateId = candidateId,
                    memoryId = null,
                    status = "validation_failed",
                    message = "That memory is too long. Shorten it before saving."
                )
                return@withTransaction
            }

            val memoryId = UUID.randomUUID().toString()
            promotedDao.insert(
                PromotedMemoryEntity(
                    id = memoryId,
                    candidateId = candidateId,
                    memoryKind = candidate.candidateKind.toPromotedKind(),
                    state = PromotedMemoryState.ACTIVE,
                    displayLabel = label,
                    subject = candidate.subject,
                    predicate = candidate.predicate,
                    objectValue = factObject,
                    confidenceLabel = PromotedMemoryConfidence.CONFIRMED,
                    sensitivity = candidate.sensitivity,
                    source = PromotedMemorySource.USER_CONFIRMED,
                    supportingEnvelopeIdsJson = support.envelopeIdsJson(),
                    supportingEvidenceIdsJson = candidate.supportingEvidenceIdsJson,
                    supportingFeedbackIdsJson = candidate.supportingFeedbackIdsJson,
                    createdAt = now,
                    updatedAt = now,
                    validFrom = now,
                    validTo = null,
                    invalidatedAt = null,
                    lastUsedAt = null,
                    useCount = 0
                )
            )
            promotedSupportDao.insertAll(
                support.map {
                    PromotedMemorySupportEntity(
                        memoryId = memoryId,
                        envelopeId = it.envelopeId,
                        supportType = it.supportType,
                        evidenceId = it.evidenceId,
                        createdAt = now
                    )
                }
            )
            candidateDao.markTerminal(candidateId, MemoryCandidateState.PROMOTED, "user_accepted", now)
            auditDao.insert(
                auditWriter.build(
                    action = AuditAction.MEMORY_CANDIDATE_ACCEPTED,
                    description = "Accepted memory candidate",
                    envelopeId = support.firstOrNull()?.envelopeId,
                    extraJson = JSONObject()
                        .put("candidateId", candidateId)
                        .put("memoryId", memoryId)
                        .put("sourceCount", support.size)
                        .put("edited", label != candidate.displayLabel || factObject != candidate.objectValue)
                        .toString()
                )
            )
            result = MemoryDecisionResultParcel(
                ok = true,
                candidateId = candidateId,
                memoryId = memoryId,
                status = "promoted",
                message = "Saved to Orbit memory."
            )
        }
        return result
    }

    suspend fun rejectCandidate(candidateId: String, reason: String?): MemoryDecisionResultParcel {
        val now = clock()
        var result = MemoryDecisionResultParcel(
            ok = false,
            candidateId = candidateId,
            memoryId = null,
            status = "not_found",
            message = "I could not find that memory suggestion."
        )
        database.withTransaction {
            val candidate = candidateDao.getById(candidateId) ?: return@withTransaction
            if (candidate.state == MemoryCandidateState.REJECTED) {
                result = MemoryDecisionResultParcel(
                    ok = true,
                    candidateId = candidateId,
                    memoryId = null,
                    status = "already_rejected",
                    message = "Already dismissed."
                )
                return@withTransaction
            }
            if (candidate.state !in setOf(MemoryCandidateState.PENDING, MemoryCandidateState.ASKED)) {
                result = MemoryDecisionResultParcel(
                    ok = false,
                    candidateId = candidateId,
                    memoryId = null,
                    status = "not_pending",
                    message = "That memory suggestion is no longer pending."
                )
                return@withTransaction
            }
            val support = candidateSupportDao.listForCandidate(candidateId)
            candidateDao.markTerminal(candidateId, MemoryCandidateState.REJECTED, reason ?: "user_rejected", now)
            auditDao.insert(
                auditWriter.build(
                    action = AuditAction.MEMORY_CANDIDATE_REJECTED,
                    description = "Rejected memory candidate",
                    envelopeId = support.firstOrNull()?.envelopeId,
                    extraJson = JSONObject()
                        .put("candidateId", candidateId)
                        .put("sourceCount", support.size)
                        .put("reason", reason ?: "user_rejected")
                        .toString()
                )
            )
            result = MemoryDecisionResultParcel(
                ok = true,
                candidateId = candidateId,
                memoryId = null,
                status = "rejected",
                message = "Dismissed. Orbit will not remember that."
            )
        }
        return result
    }

    suspend fun debugSeedDemoMemoryCandidates(): Int {
        val source = envelopeDao.searchActive("startup event", 5).firstOrNull()
            ?: envelopeDao.searchActive("recipe", 5).firstOrNull()
            ?: envelopeDao.searchActive("Shopping list for recipe night", 5).firstOrNull()
            ?: return 0
        val id = "debug-memory-interest-${source.id}"
        val now = clock()
        var inserted = 0
        database.withTransaction {
            val candidate = MemoryCandidateEntity(
                id = id,
                candidateKind = MemoryCandidateKind.INTEREST,
                state = MemoryCandidateState.PENDING,
                displayLabel = "Interested in startup events",
                subject = "user",
                predicate = "interested_in",
                objectValue = "startup events",
                confidence = 0.78f,
                sensitivity = MemorySensitivity.NORMAL,
                supportingEnvelopeIdsJson = JSONArray().put(source.id).toString(),
                supportingEvidenceIdsJson = null,
                supportingFeedbackIdsJson = null,
                askUserCopy = "Remember that startup events matter to you?",
                createdAt = now,
                updatedAt = now,
                expiresAt = null,
                decidedAt = null,
                decisionReason = null,
                modelLabel = "debug_seed",
                promptVersion = null,
                source = MemoryCandidateSource.DEBUG_SEED
            )
            if (candidateDao.insert(candidate) != -1L) {
                inserted += 1
                candidateSupportDao.insertAll(
                    listOf(
                        MemoryCandidateSupportEntity(
                            candidateId = id,
                            envelopeId = source.id,
                            supportType = MemorySupportType.CAPTURE,
                            evidenceId = null,
                            createdAt = now
                        )
                    )
                )
                auditDao.insert(
                    auditWriter.build(
                        action = AuditAction.MEMORY_CANDIDATE_PROPOSED,
                        description = "Debug memory candidate proposed",
                        envelopeId = source.id,
                        extraJson = JSONObject()
                            .put("candidateId", id)
                            .put("source", "debug_seed")
                            .toString()
                    )
                )
            }
        }
        return inserted
    }

    private fun MemoryCandidateProjection.toParcel(): MemoryCandidateParcel =
        MemoryCandidateParcel(
            candidateId = candidateId,
            candidateKind = candidateKind,
            state = state,
            displayLabel = displayLabel,
            factText = factText(subject, predicate, objectValue),
            confidenceLabel = confidence.toConfidenceLabel(),
            sensitivity = sensitivity,
            sourceCount = sourceCount,
            primarySourceEnvelopeId = primarySourceEnvelopeId,
            primarySourceTitle = primarySourceTitle,
            primarySourceDayLocal = primarySourceDayLocal,
            askUserCopy = askUserCopy,
            createdAtMillis = createdAtMillis,
            updatedAtMillis = updatedAtMillis
        )

    private fun PromotedMemoryProjection.toParcel(): PromotedMemoryParcel =
        PromotedMemoryParcel(
            memoryId = memoryId,
            memoryKind = memoryKind,
            state = state,
            displayLabel = displayLabel,
            factText = factText(subject, predicate, objectValue),
            confidenceLabel = confidenceLabel,
            sensitivity = sensitivity,
            sourceCount = sourceCount,
            lastUsedAtMillis = lastUsedAtMillis,
            useCount = useCount,
            updatedAtMillis = updatedAtMillis
        )

    private fun MemoryCandidateKind.toPromotedKind(): PromotedMemoryKind = when (this) {
        MemoryCandidateKind.PROFILE_FACT -> PromotedMemoryKind.DECLARED_PROFILE_FACT
        MemoryCandidateKind.PREFERENCE -> PromotedMemoryKind.CONFIRMED_PREFERENCE
        MemoryCandidateKind.INTEREST -> PromotedMemoryKind.RECURRING_INTEREST
        MemoryCandidateKind.PATTERN -> PromotedMemoryKind.BEHAVIOR_PATTERN
        MemoryCandidateKind.RELATIONSHIP -> PromotedMemoryKind.RELATIONSHIP
        MemoryCandidateKind.CORRECTION_RULE -> PromotedMemoryKind.CORRECTION_RULE
    }

    private fun List<MemoryCandidateSupportEntity>.envelopeIdsJson(): String =
        JSONArray().also { arr -> forEach { arr.put(it.envelopeId) } }.toString()

    private fun factText(subject: String, predicate: String, objectValue: String): String =
        listOf(subject, predicate.replace('_', ' '), objectValue)
            .filter { it.isNotBlank() }
            .joinToString(" ")

    private fun Float.toConfidenceLabel(): String = when {
        this >= 0.85f -> "high"
        this >= 0.65f -> "medium"
        else -> "low"
    }
}
