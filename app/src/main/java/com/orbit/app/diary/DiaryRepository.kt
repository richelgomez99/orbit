package com.orbit.app.diary

import com.orbit.app.action.ipc.ActionExecuteRequestParcel
import com.orbit.app.action.ipc.ActionExecuteResultParcel
import com.orbit.app.data.ClusterCardModel
import com.orbit.app.data.ipc.ActionProposalParcel
import com.orbit.app.data.ipc.ActiveIntentParcel
import com.orbit.app.data.ipc.ActionDraftParcel
import com.orbit.app.data.ipc.DayPageParcel
import com.orbit.app.data.ipc.EnvelopeViewParcel
import com.orbit.app.data.ipc.MemoryCandidateParcel
import com.orbit.app.data.ipc.MemoryDecisionResultParcel
import com.orbit.app.data.ipc.PromotedMemoryParcel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

/**
 * T049 seam — the diary VM's view of the envelope repository. The AIDL
 * `IEnvelopeRepository.Stub` uses a callback-based observer; this interface
 * flattens that to a cold [Flow] so the VM stays unit-testable without any
 * Android IPC plumbing. Production binding is owned by [DiaryActivity]
 * (T052) via an adapter that bridges `IEnvelopeObserver` → `Flow`.
 */
interface DiaryRepository {

    /**
     * Emits a [DayPageParcel] for [isoDate] whenever the underlying data
     * changes. The flow completes only when collection is cancelled.
     */
    fun observeDay(isoDate: String): Flow<DayPageParcel>

    /** Reassign an envelope's intent (US2 tap-to-reassign per T051). */
    suspend fun reassignIntent(envelopeId: String, newIntentName: String, reason: String?)

    /** Archive an envelope (out of the Diary, still on disk). */
    suspend fun archive(envelopeId: String)

    /** Soft-delete an envelope. */
    suspend fun delete(envelopeId: String)

    /** T069 — re-enqueue non-succeeded URL hydrations for an envelope. */
    suspend fun retryHydration(envelopeId: String)

    /** T055b — single-envelope fetch for the detail screen. */
    suspend fun getEnvelope(envelopeId: String): EnvelopeViewParcel

    /** Spec 017 — latest user note attached to an envelope, if any. */
    suspend fun getLatestNote(envelopeId: String): String? = null

    /** Spec 017 — create or edit the latest note attached to an envelope. */
    suspend fun createOrUpdateLatestNote(envelopeId: String, text: String): Boolean = false

    /**
     * T056 — paginated list of ISO local dates (newest first) that have
     * at least one non-archived, non-deleted envelope. Backs the Diary's
     * `HorizontalPager` → `DiaryPagingSource` so backscroll skips empty
     * days.
     */
    suspend fun distinctDayLocalsWithContent(limit: Int, offset: Int): List<String>

    // ---- Spec 003 v1.1 — Orbit Actions (T053) -----------------------------

    /**
     * Live feed of action proposals attached to [envelopeId]. Emits the
     * full current set on every change so observers don't reconstruct
     * deltas (mirrors [observeDay]).
     */
    fun observeProposals(envelopeId: String): Flow<List<ActionProposalParcel>>

    /** Flips proposal `state` PROPOSED→CONFIRMED + audits ACTION_CONFIRMED. */
    suspend fun markProposalConfirmed(proposalId: String): Boolean

    /** Flips proposal `state` PROPOSED→DISMISSED + audits ACTION_DISMISSED. */
    suspend fun markProposalDismissed(proposalId: String): Boolean

    /** Dispatches the side effect via `:capture` IActionExecutor binder. */
    suspend fun executeAction(request: ActionExecuteRequestParcel): ActionExecuteResultParcel

    /** Best-effort cancel within the 5s undo window. Returns false past the window. */
    suspend fun cancelWithinUndoWindow(executionId: String): Boolean

    /** T064 (003 US2) — toggle one item on a derived to-do envelope. */
    suspend fun setTodoItemDone(envelopeId: String, itemIndex: Int, done: Boolean)

    // ---- Spec 002 Phase 11 Block 9 — Cluster surface (T148) ---------------

    /**
     * Live feed of currently-surfaceable clusters. Mirrors
     * `IEnvelopeRepository.observeClusters` (T134) — one cold flow that
     * re-emits the full set whenever the underlying cluster table or
     * its membership changes. Read-side gates (`clusterEmitEnabled`,
     * `clusterModelLabelLock`, surviving members ≥ 3, terminal-state
     * filter) live in the data layer; the VM simply consumes.
     *
     * Default `emptyFlow()` so existing JVM test fakes (which predate
     * Block 9 wiring) don't have to be rewritten — they observe a day
     * with no clusters and the cluster slot stays hidden.
     */
    fun observeClusters(): Flow<List<ClusterCardModel>> = flowOf(emptyList())

    /**
     * Phase 11 Block 10 (T148 review FU#2) — user-driven dismiss for
     * a cluster card. Transitions the cluster row to DISMISSED and
     * audits `CLUSTER_DISMISSED`. Returns `true` iff a row was actually
     * transitioned (idempotent on already-terminal rows).
     *
     * Default no-op so existing JVM test fakes don't have to implement
     * it; production wiring is in `BinderDiaryRepository`.
     */
    suspend fun dismissCluster(clusterId: String): Boolean = false

    // ---- Spec 004 — Active Intent cleanup queue --------------------------

    /** Live compact queue of captures that still need user action. */
    fun observeActiveIntents(): Flow<List<ActiveIntentParcel>> = flowOf(emptyList())

    /** Resolve/archive an Active Intent row through the `:ml` boundary. */
    suspend fun resolveActiveIntent(
        intentId: String,
        resolutionReason: String,
        userConfirmed: Boolean
    ): Boolean = false

    /** Audit-only Smart/Deep escalation request. Does not dispatch work. */
    suspend fun requestActiveIntentEscalation(intentId: String, mode: String): Boolean = false

    // ---- Spec 006 — Orbit action draft workspace -------------------------

    /** Compact pending action drafts for the Orbit tab. */
    fun observeActionDrafts(limit: Int = 20): Flow<List<ActionDraftParcel>> = flowOf(emptyList())

    // ---- Spec 007 — Orbit memory review --------------------------------

    /** Compact pending memory candidates for the Orbit tab. */
    fun observeMemoryCandidates(limit: Int = 20): Flow<List<MemoryCandidateParcel>> = flowOf(emptyList())

    /** Compact accepted memories for the Orbit tab and future Settings memory surface. */
    fun observePromotedMemories(limit: Int = 20): Flow<List<PromotedMemoryParcel>> = flowOf(emptyList())

    suspend fun acceptMemoryCandidate(
        candidateId: String,
        editedLabel: String? = null,
        editedFactText: String? = null
    ): MemoryDecisionResultParcel = MemoryDecisionResultParcel(
        ok = false,
        candidateId = candidateId,
        memoryId = null,
        status = "unavailable",
        message = "Orbit memory review is not available yet."
    )

    suspend fun rejectMemoryCandidate(
        candidateId: String,
        reason: String? = null
    ): MemoryDecisionResultParcel = MemoryDecisionResultParcel(
        ok = false,
        candidateId = candidateId,
        memoryId = null,
        status = "unavailable",
        message = "Orbit memory review is not available yet."
    )
}
