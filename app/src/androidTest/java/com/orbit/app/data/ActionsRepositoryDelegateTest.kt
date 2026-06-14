package com.orbit.app.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.orbit.app.action.BuiltInAppFunctionSchemas
import com.orbit.app.audit.AuditLogWriter
import com.orbit.app.data.entity.ActionProposalEntity
import com.orbit.app.data.model.ActionExecutionOutcome
import com.orbit.app.data.model.ActionProposalState
import com.orbit.app.data.model.AuditAction
import com.orbit.app.data.model.EnvelopeKind
import com.orbit.app.data.model.Intent
import com.orbit.app.data.model.LlmProvenance
import com.orbit.app.data.model.SensitivityScope
import com.orbit.app.resolution.ResolutionKind
import com.orbit.app.resolution.ResolutionTargetType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.json.JSONArray
import org.json.JSONObject
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID

/**
 * T031 — audit-atomicity tests for [ActionsRepositoryDelegate].
 *
 * Constitution audit rule (audit-log-contract.md §6): every state mutation
 * MUST write its audit row inside the same Room transaction as the mutation
 * itself. The point of this test is to assert two invariants:
 *   1. **Atomicity** — a successful mutation produces exactly one audit row
 *      with the matching action and `extraJson` payload.
 *   2. **Anti-phantom** — a no-op mutation (e.g., dismissing an already-
 *      dismissed proposal) MUST NOT write an audit row, otherwise the
 *      audit log would balloon under retry storms.
 *
 * Plus a third invariant specific to `recordActionInvocation`: it writes
 * three rows (action_execution, skill_usage, audit) in one txn.
 */
@RunWith(AndroidJUnit4::class)
class ActionsRepositoryDelegateTest {

    private lateinit var db: OrbitDatabase
    private lateinit var registry: AppFunctionRegistry
    private lateinit var delegate: ActionsRepositoryDelegate
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private var clock: Long = 1_700_000_000_000L
    private val now: () -> Long = { clock }

    @Before
    fun setUp() = kotlinx.coroutines.runBlocking {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        db = Room.inMemoryDatabaseBuilder(context, OrbitDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        val auditWriter = AuditLogWriter(clock = now, idGen = { UUID.randomUUID().toString() })
        registry = AppFunctionRegistry(
            database = db,
            skillDao = db.appFunctionSkillDao(),
            usageDao = db.skillUsageDao(),
            auditLogDao = db.auditLogDao(),
            auditWriter = auditWriter,
            now = now
        )
        registry.registerAll(BuiltInAppFunctionSchemas.ALL)
        delegate = ActionsRepositoryDelegate(
            database = db,
            registry = registry,
            auditWriter = auditWriter,
            scope = scope,
            clock = now,
            resolutionReceiptSink = ResolutionRepository(db.resolutionReceiptDao())
        )
        seedEnvelope("env-1")
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun writeProposals_writesProposalAndAudit_inOneTransaction() = runTest {
        val before = countAudit(AuditAction.ACTION_PROPOSED)
        delegate.writeProposals("env-1", listOf(makeProposal("p1", "calendar.createEvent")))

        // Both rows present.
        assertNotNull(db.actionProposalDao().getById("p1"))
        assertEquals(before + 1, countAudit(AuditAction.ACTION_PROPOSED))

        // Re-running with the same proposalId is a DAO IGNORE no-op and
        // must not write a phantom audit row.
        delegate.writeProposals("env-1", listOf(makeProposal("p1", "calendar.createEvent")))
        assertEquals(before + 1, countAudit(AuditAction.ACTION_PROPOSED))
    }

    @Test
    fun markProposalConfirmed_writesAudit_only_whenStateChanges() = runTest {
        delegate.writeProposals("env-1", listOf(makeProposal("p1", "calendar.createEvent")))
        val baseline = countAudit(AuditAction.ACTION_CONFIRMED)

        clock += 1_000L
        val first = delegate.markProposalConfirmed("p1")
        assertTrue("first confirm must report state change", first)
        assertEquals(baseline + 1, countAudit(AuditAction.ACTION_CONFIRMED))
        val row = db.actionProposalDao().getById("p1")!!
        assertEquals(ActionProposalState.CONFIRMED, row.state)

        // Second confirm is a no-op: WHERE state='PROPOSED' filters it out.
        val second = delegate.markProposalConfirmed("p1")
        assertFalse("second confirm must report no-op", second)
        assertEquals(
            "no-op confirm MUST NOT write a phantom audit row",
            baseline + 1, countAudit(AuditAction.ACTION_CONFIRMED)
        )
    }

    @Test
    fun markProposalDismissed_writesAudit_only_whenStateChanges() = runTest {
        delegate.writeProposals("env-1", listOf(makeProposal("p1", "calendar.createEvent")))
        val baseline = countAudit(AuditAction.ACTION_DISMISSED)

        val first = delegate.markProposalDismissed("p1")
        assertTrue(first)
        assertEquals(baseline + 1, countAudit(AuditAction.ACTION_DISMISSED))
        val receipts = db.resolutionReceiptDao().getForTarget(
            ResolutionTargetType.ACTION_PROPOSAL,
            "p1",
        )
        assertEquals(1, receipts.size)
        assertEquals(ResolutionKind.DISMISSED, receipts.single().kind)
        assertEquals("env-1", receipts.single().envelopeId)

        val second = delegate.markProposalDismissed("p1")
        assertFalse(second)
        assertEquals(baseline + 1, countAudit(AuditAction.ACTION_DISMISSED))
        assertEquals(
            "no-op dismiss MUST NOT write a phantom resolution receipt",
            1,
            db.resolutionReceiptDao().getForTarget(ResolutionTargetType.ACTION_PROPOSAL, "p1").size,
        )
    }

    @Test
    fun recordActionInvocation_writesExecutionAndUsageAndAudit_atomically() = runTest {
        delegate.writeProposals("env-1", listOf(makeProposal("p1", "calendar.createEvent")))

        val executionId = "exec-1"
        delegate.recordActionInvocation(
            executionId = executionId,
            proposalId = "p1",
            functionId = "calendar.createEvent",
            outcome = ActionExecutionOutcome.DISPATCHED,
            outcomeReason = null,
            dispatchedAtMillis = clock,
            completedAtMillis = clock,
            latencyMs = 17L,
            episodeId = null
        )

        // 1. action_execution row exists with the expected outcome.
        val exec = db.actionExecutionDao().getById(executionId)!!
        assertEquals(ActionExecutionOutcome.DISPATCHED, exec.outcome)
        assertEquals(17L, exec.latencyMs)

        // 2. skill_usage row written by registry.recordInvocation.
        assertEquals(1, db.skillUsageDao().countForSkill("calendar.createEvent"))

        // 3. Exactly one ACTION_EXECUTED audit row tied to the envelope.
        val executedRows = db.auditLogDao().listAll()
            .filter { it.action == AuditAction.ACTION_EXECUTED }
        assertEquals(1, executedRows.size)
        val payload = executedRows.first().extraJson!!
        assertTrue(payload.contains("\"executionId\":\"$executionId\""))
        assertTrue(payload.contains("\"outcome\":\"DISPATCHED\""))
        assertTrue(payload.contains("\"latencyMs\":17"))
    }

    @Test
    fun recordActionInvocation_failedOutcome_writesActionFailedAudit() = runTest {
        delegate.writeProposals("env-1", listOf(makeProposal("p1", "calendar.createEvent")))

        delegate.recordActionInvocation(
            executionId = "exec-bad",
            proposalId = "p1",
            functionId = "calendar.createEvent",
            outcome = ActionExecutionOutcome.FAILED,
            outcomeReason = "intent_resolve_failed",
            dispatchedAtMillis = clock,
            completedAtMillis = clock,
            latencyMs = 5L,
            episodeId = null
        )

        val failedRows = db.auditLogDao().listAll()
            .filter { it.action == AuditAction.ACTION_FAILED }
        assertEquals(1, failedRows.size)
        assertTrue(failedRows.first().extraJson!!.contains("intent_resolve_failed"))
        // No ACTION_EXECUTED row for a failure.
        assertEquals(0, db.auditLogDao().listAll().count { it.action == AuditAction.ACTION_EXECUTED })
    }

    @Test
    fun recordActionInvocation_schemaInvalidation_writesInvalidatedReceipt() = runTest {
        delegate.writeProposals("env-1", listOf(makeProposal("p1", "calendar.createEvent")))

        delegate.recordActionInvocation(
            executionId = "exec-schema",
            proposalId = "p1",
            functionId = "calendar.createEvent",
            outcome = ActionExecutionOutcome.FAILED,
            outcomeReason = "schema_invalidated",
            dispatchedAtMillis = clock,
            completedAtMillis = clock,
            latencyMs = 5L,
            episodeId = null
        )

        val row = db.actionProposalDao().getById("p1")!!
        assertEquals(ActionProposalState.INVALIDATED, row.state)
        val receipts = db.resolutionReceiptDao().getForTarget(
            ResolutionTargetType.ACTION_PROPOSAL,
            "p1",
        )
        assertEquals(1, receipts.size)
        assertEquals(ResolutionKind.INVALIDATED, receipts.single().kind)
        assertEquals("schema_invalidated", receipts.single().reason)
    }

    @Test
    fun createDerivedTodoEnvelope_writesOneDerivedListEnvelopeAndAuditProvenance() = runTest {
        delegate.writeProposals("env-1", listOf(makeProposal("p-todo", "tasks.createTodo")))
        val itemsJson = JSONArray()
            .put("Buy salmon")
            .put(
                JSONObject()
                    .put("text", "Buy ginger")
                    .put("dueEpochMillis", 1_745_164_800_000L)
            )
            .toString()

        val ids = delegate.createDerivedTodoEnvelope(
            parentEnvelopeId = "env-1",
            itemsJson = itemsJson,
            proposalId = "p-todo"
        )

        assertEquals(1, ids.size)
        val row = db.intentEnvelopeDao().getById(ids.single())!!
        assertEquals("Lunch", row.textContent)
        assertEquals(EnvelopeKind.DERIVED, row.kind)
        assertEquals(Intent.WANT_IT, row.intent)
        assertTrue(row.derivedFromEnvelopeIdsJson!!.contains("\"env-1\""))
        val meta = JSONObject(row.todoMetaJson!!)
        assertEquals("p-todo", meta.getString("derivedFromProposalId"))
        val todoItems = meta.getJSONArray("items")
        assertEquals(2, todoItems.length())
        assertEquals("Buy salmon", todoItems.getJSONObject(0).getString("text"))
        assertFalse(todoItems.getJSONObject(0).getBoolean("done"))
        assertEquals("Buy ginger", todoItems.getJSONObject(1).getString("text"))
        assertEquals(
            1_745_164_800_000L,
            todoItems.getJSONObject(1).getLong("dueEpochMillis")
        )

        val created = db.auditLogDao().listAll()
            .filter { it.action == AuditAction.ENVELOPE_CREATED }
        assertEquals(1, created.size)
        val extra = JSONObject(created.single().extraJson!!)
        assertEquals("p-todo", extra.getString("derived_from_proposal_id"))
        assertEquals("env-1", extra.getString("parent_envelope_id"))
        assertEquals("todo_add", extra.getString("source"))
        assertEquals(2, extra.getInt("item_count"))

        val ingredientMatches = db.intentEnvelopeDao().searchActive("ginger", limit = 10)
        assertEquals(
            "ingredient search should surface the grouped list envelope, not require one envelope per item",
            listOf(row.id),
            ingredientMatches.map { it.id }
        )

        delegate.setTodoItemDone(row.id, itemIndex = 0, done = true)
        assertEquals(
            "first done item should not create aggregate completion yet",
            0,
            db.resolutionReceiptDao().getForTarget(ResolutionTargetType.TODO_LIST, row.id).size,
        )

        delegate.setTodoItemDone(row.id, itemIndex = 1, done = true)
        var receipts = db.resolutionReceiptDao().getForTarget(ResolutionTargetType.TODO_LIST, row.id)
        assertEquals(1, receipts.size)
        assertEquals(ResolutionKind.DONE, receipts.single().kind)
        assertEquals("p-todo", receipts.single().relatedId)

        delegate.setTodoItemDone(row.id, itemIndex = 0, done = false)
        receipts = db.resolutionReceiptDao().getForTarget(ResolutionTargetType.TODO_LIST, row.id)
        assertEquals(2, receipts.size)
        assertEquals(ResolutionKind.REOPENED, receipts.last().kind)
    }

    @Test
    fun recordActionInvocation_writesSkillUsageForSuccessFailureAndCancel() = runTest {
        delegate.writeProposals(
            "env-1",
            listOf(
                makeProposal("p-todo", "tasks.createTodo"),
                makeProposal("p-share", "share.delegate"),
                makeProposal("p-calendar", "calendar.createEvent")
            )
        )

        delegate.recordActionInvocation(
            executionId = "exec-todo",
            proposalId = "p-todo",
            functionId = "tasks.createTodo",
            outcome = ActionExecutionOutcome.SUCCESS,
            outcomeReason = null,
            dispatchedAtMillis = clock,
            completedAtMillis = clock,
            latencyMs = 10L,
            episodeId = null
        )
        delegate.recordActionInvocation(
            executionId = "exec-share",
            proposalId = "p-share",
            functionId = "share.delegate",
            outcome = ActionExecutionOutcome.FAILED,
            outcomeReason = "unknown_skill",
            dispatchedAtMillis = clock,
            completedAtMillis = clock,
            latencyMs = 0L,
            episodeId = null
        )
        delegate.recordActionInvocation(
            executionId = "exec-calendar",
            proposalId = "p-calendar",
            functionId = "calendar.createEvent",
            outcome = ActionExecutionOutcome.DISPATCHED,
            outcomeReason = null,
            dispatchedAtMillis = clock,
            completedAtMillis = clock,
            latencyMs = 20L,
            episodeId = null
        )
        delegate.recordActionInvocation(
            executionId = "exec-calendar",
            proposalId = "p-calendar",
            functionId = "calendar.createEvent",
            outcome = ActionExecutionOutcome.USER_CANCELLED,
            outcomeReason = "user_cancelled",
            dispatchedAtMillis = clock,
            completedAtMillis = clock + 1_000L,
            latencyMs = 1_000L,
            episodeId = null
        )

        assertEquals(1, db.skillUsageDao().countForSkill("tasks.createTodo"))
        assertEquals(1, db.skillUsageDao().countForSkill("share.delegate"))
        assertEquals(
            "calendar dispatch and user-cancel are both auditable usage outcomes",
            2,
            db.skillUsageDao().countForSkill("calendar.createEvent")
        )
    }

    // ---- Helpers ----

    private fun makeProposal(id: String, functionId: String) = ActionProposalEntity(
        id = id,
        envelopeId = "env-1",
        functionId = functionId,
        schemaVersion = 1,
        argsJson = "{\"title\":\"Lunch\"}",
        previewTitle = "Lunch",
        previewSubtitle = null,
        confidence = 0.82f,
        provenance = LlmProvenance.LOCAL_NANO,
        state = ActionProposalState.PROPOSED,
        sensitivityScope = SensitivityScope.PUBLIC,
        createdAt = clock,
        stateChangedAt = clock
    )

    private suspend fun countAudit(action: AuditAction): Int =
        db.auditLogDao().listAll().count { it.action == action }

    private fun seedEnvelope(id: String) {
        db.openHelper.writableDatabase.execSQL(
            """
            INSERT INTO intent_envelope(
                id, contentType, textContent, imageUri, textContentSha256,
                intent, intentConfidence, intentSource, intentHistoryJson,
                createdAt, day_local, isArchived, isDeleted, deletedAt,
                sharedContinuationResultId, appCategory, activityState,
                tzId, hourLocal, dayOfWeekLocal,
                kind, derivedFromEnvelopeIdsJson, todoMetaJson
            ) VALUES('$id', 'TEXT', 't', NULL, NULL,
                'WANT_IT', NULL, 'USER_CHIP', '[]',
                $clock, '2026-04-26', 0, 0, NULL, NULL,
                'OTHER', 'STILL', 'UTC', 12, 5,
                'REGULAR', NULL, NULL)
            """.trimIndent()
        )
    }
}
