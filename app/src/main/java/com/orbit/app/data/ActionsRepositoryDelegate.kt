package com.orbit.app.data

import androidx.room.withTransaction
import com.orbit.app.audit.AuditLogWriter
import com.orbit.app.data.entity.ActionExecutionEntity
import com.orbit.app.data.entity.ActionProposalEntity
import com.orbit.app.data.entity.IntentEnvelopeEntity
import com.orbit.app.data.entity.StateSnapshot
import com.orbit.app.data.ipc.ActionDraftParcel
import com.orbit.app.data.ipc.ActionProposalParcel
import com.orbit.app.data.ipc.AppFunctionSummaryParcel
import com.orbit.app.data.ipc.IActionDraftObserver
import com.orbit.app.data.ipc.IActionProposalObserver
import com.orbit.app.data.model.ActionExecutionOutcome
import com.orbit.app.data.model.ActionProposalState
import com.orbit.app.data.model.AuditAction
import com.orbit.app.data.model.ContentType
import com.orbit.app.data.model.EnvelopeKind
import com.orbit.app.data.model.Intent
import com.orbit.app.data.model.IntentSource
import com.orbit.app.data.model.LlmProvenance
import com.orbit.app.data.model.SensitivityScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.time.Instant
import java.time.ZoneId
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * Spec 003 v1.1 — actions-side delegate the [EnvelopeRepositoryImpl] forwards
 * its new AIDL methods to. Lives separately so the 678-line Impl stays
 * focused on envelope CRUD + URL hydration.
 *
 * Same audit-log atomicity contract as `EnvelopeStorageBackend.*Transaction`:
 * every state mutation here writes its audit row inside the same
 * `database.withTransaction { }` block (audit-log-contract.md §6).
 */
class ActionsRepositoryDelegate(
    private val database: OrbitDatabase,
    private val registry: AppFunctionRegistry,
    private val auditWriter: AuditLogWriter,
    private val scope: CoroutineScope,
    private val clock: () -> Long = { System.currentTimeMillis() }
) {

    private val proposalDao = database.actionProposalDao()
    private val executionDao = database.actionExecutionDao()
    private val skillDao = database.appFunctionSkillDao()
    private val auditDao = database.auditLogDao()
    private val envelopeDao = database.intentEnvelopeDao()

    private val observerJobs = ConcurrentHashMap<IBinderKey, Job>()

    suspend fun lookupAppFunction(functionId: String): AppFunctionSummaryParcel? =
        skillDao.lookupLatest(functionId)?.toParcel()

    suspend fun listAppFunctions(appPackage: String): List<AppFunctionSummaryParcel> =
        skillDao.listForApp(appPackage).map { it.toParcel() }

    /**
     * Persists a freshly extracted batch of proposals. Caller (the LLM
     * orchestrator) guarantees `confidence ≥ 0.55` and that each
     * `(envelopeId, functionId)` is fresh — DAO insert uses IGNORE so
     * duplicates from a re-run are silent no-ops.
     */
    suspend fun writeProposals(
        envelopeId: String,
        proposals: List<ActionProposalEntity>
    ) {
        if (proposals.isEmpty()) return
        database.withTransaction {
            for (p in proposals) {
                val inserted = proposalDao.insert(p)
                if (inserted == -1L) continue
                auditDao.insert(
                    auditWriter.build(
                        action = AuditAction.ACTION_PROPOSED,
                        description = "Proposed ${p.functionId} (${"%.2f".format(p.confidence)})",
                        envelopeId = envelopeId,
                        extraJson = JSONObject().apply {
                            put("proposalId", p.id)
                            put("functionId", p.functionId)
                            put("confidence", p.confidence)
                            put("provenance", p.provenance.name)
                            put("sensitivity", p.sensitivityScope.name)
                        }.toString()
                    )
                )
            }
        }
    }

    /** User confirmed a chip. Returns true when state actually changed. */
    suspend fun markProposalConfirmed(proposalId: String): Boolean {
        val now = clock()
        var changed = false
        database.withTransaction {
            val rows = proposalDao.markConfirmed(proposalId, now)
            if (rows > 0) {
                changed = true
                val proposal = proposalDao.getById(proposalId)
                auditDao.insert(
                    auditWriter.build(
                        action = AuditAction.ACTION_CONFIRMED,
                        description = "Confirmed proposal $proposalId",
                        envelopeId = proposal?.envelopeId,
                        extraJson = """{"proposalId":"$proposalId","functionId":"${proposal?.functionId}"}"""
                    )
                )
            }
        }
        return changed
    }

    /** User dismissed a chip. Returns true when state actually changed. */
    suspend fun markProposalDismissed(proposalId: String): Boolean {
        val now = clock()
        var changed = false
        database.withTransaction {
            val rows = proposalDao.markDismissed(proposalId, now)
            if (rows > 0) {
                changed = true
                val proposal = proposalDao.getById(proposalId)
                auditDao.insert(
                    auditWriter.build(
                        action = AuditAction.ACTION_DISMISSED,
                        description = "Dismissed proposal $proposalId",
                        envelopeId = proposal?.envelopeId,
                        extraJson = """{"proposalId":"$proposalId","functionId":"${proposal?.functionId}"}"""
                    )
                )
            }
        }
        return changed
    }

    /**
     * Records a finished action invocation: insert or update the
     * `action_execution` row, mark the proposal CONFIRMED if not already,
     * insert the `skill_usage` aggregation row, and emit an audit row —
     * all in a single Room transaction.
     */
    suspend fun recordActionInvocation(
        executionId: String,
        proposalId: String,
        functionId: String,
        outcome: ActionExecutionOutcome,
        outcomeReason: String?,
        dispatchedAtMillis: Long,
        completedAtMillis: Long,
        latencyMs: Long,
        episodeId: String?
    ) {
        database.withTransaction {
            val existing = executionDao.getById(executionId)
            if (existing == null) {
                executionDao.insert(
                    ActionExecutionEntity(
                        id = executionId,
                        proposalId = proposalId,
                        functionId = functionId,
                        outcome = outcome,
                        outcomeReason = outcomeReason,
                        dispatchedAt = dispatchedAtMillis,
                        completedAt = completedAtMillis.takeIf { it > 0 },
                        latencyMs = latencyMs.takeIf { it >= 0 },
                        episodeId = episodeId
                    )
                )
            } else {
                executionDao.markOutcome(
                    id = executionId,
                    outcome = outcome,
                    reason = outcomeReason,
                    completedAt = completedAtMillis,
                    latencyMs = latencyMs
                )
            }

            registry.recordInvocation(
                skillId = functionId,
                executionId = executionId,
                proposalId = proposalId,
                episodeId = episodeId,
                outcome = outcome,
                latencyMs = latencyMs.coerceAtLeast(0),
                invokedAt = dispatchedAtMillis
            )

            val proposal = proposalDao.getById(proposalId)
            val envelopeId = proposal?.envelopeId
            // Per action-execution-contract.md §5: USER_CANCELLED maps to
            // ACTION_FAILED with reason=user_cancelled (not ACTION_DISMISSED —
            // that's reserved for proposal-level dismissals before execute).
            val auditAction = when (outcome) {
                ActionExecutionOutcome.PENDING -> AuditAction.ACTION_EXECUTED
                ActionExecutionOutcome.DISPATCHED -> AuditAction.ACTION_EXECUTED
                ActionExecutionOutcome.SUCCESS -> AuditAction.ACTION_EXECUTED
                ActionExecutionOutcome.FAILED -> AuditAction.ACTION_FAILED
                ActionExecutionOutcome.USER_CANCELLED -> AuditAction.ACTION_FAILED
            }
            auditDao.insert(
                auditWriter.build(
                    action = auditAction,
                    description = "Action $functionId outcome=${outcome.name}",
                    envelopeId = envelopeId,
                    extraJson = JSONObject().apply {
                        put("executionId", executionId)
                        put("proposalId", proposalId)
                        put("functionId", functionId)
                        put("outcome", outcome.name)
                        put("latencyMs", latencyMs)
                        if (outcomeReason != null) put("reason", outcomeReason)
                    }.toString()
                )
            )

            // T091 — schema-failure proposals are terminal: flip to
            // INVALIDATED so the UI hides them and a retry-tap won't
            // re-fire the (still-broken) Intent. Mirrors
            // action-execution-contract.md §4 step 1.
            if (outcome == ActionExecutionOutcome.FAILED &&
                (outcomeReason == "schema_invalidated" || outcomeReason == "schema_mismatch")
            ) {
                proposalDao.markInvalidated(proposalId, completedAtMillis.coerceAtLeast(dispatchedAtMillis))
            }
        }
    }

    /**
     * Forward-only stream of non-terminal proposals for an envelope to a UI
     * observer. Mirrors `EnvelopeRepositoryImpl.observeDay` lifecycle —
     * cancel previous job for the same binder; auto-stop on RemoteException.
     */
    fun observeProposalsForEnvelope(envelopeId: String, observer: IActionProposalObserver) {
        val key = IBinderKey(observer.asBinder())
        observerJobs.remove(key)?.cancel()
        val job = scope.launch(Dispatchers.IO) {
            proposalDao.observeProposedForEnvelope(envelopeId).collectLatest { rows ->
                try {
                    observer.onProposals(rows.map { it.toParcel() })
                } catch (_: android.os.RemoteException) {
                    observerJobs.remove(key)?.cancel()
                }
            }
        }
        observerJobs[key] = job
    }

    fun stopObservingProposals(observer: IActionProposalObserver) {
        val key = IBinderKey(observer.asBinder())
        observerJobs.remove(key)?.cancel()
    }

    fun observePendingActionDrafts(limit: Int, observer: IActionDraftObserver) {
        val key = IBinderKey(observer.asBinder())
        observerJobs.remove(key)?.cancel()
        val cappedLimit = limit.coerceIn(1, 50)
        val job = scope.launch(Dispatchers.IO) {
            proposalDao.observePendingDrafts(cappedLimit).collectLatest { rows ->
                try {
                    observer.onActionDraftsChanged(rows.map { it.toParcel() })
                } catch (_: android.os.RemoteException) {
                    observerJobs.remove(key)?.cancel()
                }
            }
        }
        observerJobs[key] = job
    }

    fun stopObservingActionDrafts(observer: IActionDraftObserver) {
        val key = IBinderKey(observer.asBinder())
        observerJobs.remove(key)?.cancel()
    }

    suspend fun debugSeedDemoActionProposals(): Int {
        var count = 0
        envelopeDao.searchActive("Concert ticket saved", 3).firstOrNull()?.let { source ->
            val proposal = ActionProposalEntity(
                id = "debug-calendar-${source.id}",
                envelopeId = source.id,
                functionId = "calendar.createEvent",
                schemaVersion = 1,
                argsJson = JSONObject().apply {
                    val start = clock() + 3L * 24L * 60L * 60L * 1000L
                    put("title", "Concert")
                    put("startEpochMillis", start)
                    put("endEpochMillis", start + 2L * 60L * 60L * 1000L)
                    put("notes", "Created from Orbit demo capture.")
                    put("tzId", ZoneId.systemDefault().id)
                }.toString(),
                previewTitle = "Add concert to calendar",
                previewSubtitle = "Review before opening Calendar",
                confidence = 0.94f,
                provenance = LlmProvenance.LOCAL_NANO,
                state = ActionProposalState.PROPOSED,
                sensitivityScope = SensitivityScope.PERSONAL,
                createdAt = clock(),
                stateChangedAt = clock()
            )
            writeProposals(source.id, listOf(proposal))
            count += 1
        }
        envelopeDao.searchActive("Shopping list for recipe night", 3).firstOrNull()?.let { source ->
            val proposal = ActionProposalEntity(
                id = "debug-todo-${source.id}",
                envelopeId = source.id,
                functionId = "tasks.createTodo",
                schemaVersion = 1,
                argsJson = JSONObject().apply {
                    put("target", "local")
                    put("items", JSONArray().apply {
                        put("salmon")
                        put("miso")
                        put("ginger")
                        put("rice")
                        put("lemons")
                    })
                }.toString(),
                previewTitle = "Create recipe shopping list",
                previewSubtitle = "Local follow-up items",
                confidence = 0.92f,
                provenance = LlmProvenance.LOCAL_NANO,
                state = ActionProposalState.PROPOSED,
                sensitivityScope = SensitivityScope.PERSONAL,
                createdAt = clock(),
                stateChangedAt = clock()
            )
            writeProposals(source.id, listOf(proposal))
            count += 1
        }
        return count
    }

    // ---- T060/T061 — derived to-do envelopes ------------------------

    /**
     * T061 — local-target dispatch path for [TodoActionHandler]. Inserts
     * one derived list envelope in a single Room transaction:
     *   kind=DERIVED, intent=WANT_IT, intentSource=AUTO_AMBIGUOUS,
     *   todoMetaJson populated, derivedFromEnvelopeIdsJson=[parentId].
     * Source envelope is NOT mutated (Principle III).
     *
     * `itemsJson` is a JSON array; each element is either a string
     * (text-only item) or an object `{"text":"…","dueEpochMillis":<long>}`.
     * Malformed items are skipped silently. Empty array returns `[]`.
     *
     * The insert fires an `ENVELOPE_CREATED` audit row carrying
     * `derived_from_proposal_id` per quickstart §4 step 9.
     *
     * Returns the newly created list envelope id.
     */
    suspend fun createDerivedTodoEnvelope(
        parentEnvelopeId: String,
        itemsJson: String,
        proposalId: String
    ): List<String> {
        val parent = envelopeDao.getById(parentEnvelopeId) ?: return emptyList()
        val parsed = parseItemsJson(itemsJson)
        if (parsed.isEmpty()) return emptyList()

        val now = clock()
        val parentIdsJson = JSONArray().apply { put(parentEnvelopeId) }.toString()
        val proposal = proposalDao.getById(proposalId)
        val newId = UUID.randomUUID().toString()
        val todoMeta = buildTodoMetaJson(parsed, proposalId)
        val listTitle = proposal?.previewTitle?.takeIf { it.isNotBlank() }
            ?: if (parsed.size == 1) parsed.first().text else "To-do list (${parsed.size} items)"

        database.withTransaction {
            val derived = IntentEnvelopeEntity(
                id = newId,
                contentType = ContentType.TEXT,
                textContent = listTitle,
                imageUri = null,
                textContentSha256 = null,
                intent = Intent.WANT_IT,
                intentConfidence = null,
                intentSource = IntentSource.AUTO_AMBIGUOUS,
                intentHistoryJson = JSONArray().put(
                    JSONObject()
                        .put("at", now)
                        .put("intent", Intent.WANT_IT.name)
                        .put("source", IntentSource.AUTO_AMBIGUOUS.name)
                ).toString(),
                state = parent.state.copy(),
                createdAt = now,
                dayLocal = computeDayLocal(now, parent.state.tzId),
                kind = EnvelopeKind.DERIVED,
                derivedFromEnvelopeIdsJson = parentIdsJson,
                todoMetaJson = todoMeta
            )
            envelopeDao.insert(derived)
            auditDao.insert(
                auditWriter.build(
                    action = AuditAction.ENVELOPE_CREATED,
                    description = "Derived to-do list from proposal $proposalId",
                    envelopeId = newId,
                    extraJson = JSONObject().apply {
                        put("derived_from_proposal_id", proposalId)
                        put("parent_envelope_id", parentEnvelopeId)
                        put("source", "todo_add")
                        put("item_count", parsed.size)
                    }.toString()
                )
            )
        }
        return listOf(newId)
    }

    /**
     * T064 — toggle one item's `done` flag. Re-serialises the envelope's
     * `todoMetaJson`. No-op (returns silently) when the envelope has no
     * `todoMetaJson`, the index is out of range, or the JSON is
     * malformed. Intentionally does NOT write an audit row — checkbox
     * toggles are high-frequency UI noise per audit-log-contract §2.
     */
    suspend fun setTodoItemDone(envelopeId: String, itemIndex: Int, done: Boolean) {
        val envelope = envelopeDao.getById(envelopeId) ?: return
        val current = envelope.todoMetaJson ?: return
        val updated = runCatching {
            val obj = JSONObject(current)
            val items = obj.optJSONArray("items") ?: return@runCatching null
            if (itemIndex !in 0 until items.length()) return@runCatching null
            val item = items.optJSONObject(itemIndex) ?: return@runCatching null
            item.put("done", done)
            obj.toString()
        }.getOrNull() ?: return
        envelopeDao.updateTodoMetaJson(envelopeId, updated)
    }

    // ---- helpers ----

    private data class ParsedItem(val text: String, val dueEpochMillis: Long?)

    private fun parseItemsJson(raw: String): List<ParsedItem> {
        val arr = runCatching { JSONArray(raw) }.getOrNull() ?: return emptyList()
        val out = mutableListOf<ParsedItem>()
        for (i in 0 until arr.length()) {
            when (val element = arr.opt(i)) {
                is String -> {
                    val t = element.trim()
                    if (t.isNotEmpty()) out.add(ParsedItem(t, null))
                }
                is JSONObject -> {
                    val t = element.optString("text").trim()
                    if (t.isNotEmpty()) {
                        val due = if (element.has("dueEpochMillis"))
                            element.optLong("dueEpochMillis", -1L).takeIf { it >= 0 }
                        else null
                        out.add(ParsedItem(t, due))
                    }
                }
            }
        }
        return out
    }

    private fun buildTodoMetaJson(items: List<ParsedItem>, proposalId: String): String {
        val itemArray = JSONArray()
        items.forEach { item ->
            itemArray.put(
                JSONObject().apply {
                    put("text", item.text)
                    put("done", false)
                    if (item.dueEpochMillis != null) put("dueEpochMillis", item.dueEpochMillis)
                    else put("dueEpochMillis", JSONObject.NULL)
                }
            )
        }
        return JSONObject().apply {
            put("items", itemArray)
            put("derivedFromProposalId", proposalId)
        }.toString()
    }

    private fun computeDayLocal(nowMillis: Long, tzId: String): String =
        Instant.ofEpochMilli(nowMillis).atZone(ZoneId.of(tzId)).toLocalDate().toString()
}

private fun ActionProposalEntity.toParcel() = ActionProposalParcel(
    id = id,
    envelopeId = envelopeId,
    functionId = functionId,
    schemaVersion = schemaVersion,
    argsJson = argsJson,
    previewTitle = previewTitle,
    previewSubtitle = previewSubtitle,
    confidence = confidence,
    provenance = provenance.name,
    state = state.name,
    sensitivityScope = sensitivityScope.name,
    createdAtMillis = createdAt,
    stateChangedAtMillis = stateChangedAt
)

private fun com.orbit.app.data.dao.ActionDraftProjection.toParcel() = ActionDraftParcel(
    proposalId = proposalId,
    sourceEnvelopeId = sourceEnvelopeId,
    functionId = functionId,
    schemaVersion = schemaVersion,
    argsJson = argsJson,
    previewTitle = previewTitle,
    previewSubtitle = previewSubtitle,
    confidence = confidence,
    provenance = provenance,
    state = state,
    sensitivityScope = sensitivityScope,
    createdAtMillis = createdAtMillis,
    stateChangedAtMillis = stateChangedAtMillis,
    displayName = displayName,
    sideEffects = sideEffects,
    reversibility = reversibility,
    sourceTitle = sourceTitle,
    sourceAppLabel = sourceAppLabel,
    sourceDayLocal = sourceDayLocal
)

private fun com.orbit.app.data.entity.AppFunctionSkillEntity.toParcel() = AppFunctionSummaryParcel(
    functionId = functionId,
    appPackage = appPackage,
    displayName = displayName,
    description = description,
    schemaVersion = schemaVersion,
    argsSchemaJson = argsSchemaJson,
    sideEffects = sideEffects.name,
    reversibility = reversibility.name,
    sensitivityScope = sensitivityScope.name,
    registeredAtMillis = registeredAt,
    updatedAtMillis = updatedAt
)
