package com.orbit.app.diary

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.orbit.app.action.ipc.ActionExecuteRequestParcel
import com.orbit.app.data.ClusterCardModel
import com.orbit.app.data.ipc.ActionProposalParcel
import com.orbit.app.data.ipc.MemoryDecisionResultParcel
import com.orbit.app.understanding.domain.ResolutionReason
import com.orbit.app.understanding.domain.UnderstandingMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId

/**
 * T049 — diary screen VM. Wires the repository observer to the pure-logic
 * [ThreadGrouper] (T047) and [DayHeaderGenerator] (T048), exposing the
 * result as [DayUiState].
 *
 * Deliberately **does not** own the AIDL binding — [DiaryRepository] is
 * an injected seam so JVM unit tests can substitute a fake. The AIDL
 * adapter lives in the `:ui` process host (T052 `DiaryActivity`).
 *
 * [scope] is injected (rather than always using `viewModelScope`) so the
 * test harness can pass its own `TestScope` and tick the dispatcher deterministically.
 */
class DiaryViewModel(
    private val repository: DiaryRepository,
    private val threadGrouper: ThreadGrouper,
    private val dayHeaderGenerator: DayHeaderGenerator,
    scopeOverride: CoroutineScope? = null,
    private val zoneId: ZoneId = ZoneId.systemDefault()
) : ViewModel() {

    private val scope: CoroutineScope = scopeOverride ?: viewModelScope

    private val _state = MutableStateFlow<DayUiState>(DayUiState.Loading(isoDate = ""))
    val state: StateFlow<DayUiState> = _state.asStateFlow()

    private val _activeIntentState = MutableStateFlow<ActiveIntentUiState>(ActiveIntentUiState.Loading)
    val activeIntentState: StateFlow<ActiveIntentUiState> = _activeIntentState.asStateFlow()

    private var currentJob: Job? = null
    private var currentIsoDate: String? = null

    init {
        scope.launch {
            repository.observeActiveIntents()
                .catch { e ->
                    _activeIntentState.value = ActiveIntentUiState.Error(
                        e.message ?: e::class.java.simpleName
                    )
                }
                .collect { intents ->
                    _activeIntentState.value = ActiveIntentUiState.from(intents)
                }
        }
    }

    /**
     * Subscribe to [isoDate]. Cancels any in-flight subscription and emits
     * [DayUiState.Loading] before the first upstream page arrives.
     *
     * Idempotent: calling with the same [isoDate] twice is a no-op.
     *
     * Phase 11 Block 9 / T148 — combines the per-day envelope flow with
     * the global cluster flow ([DiaryRepository.observeClusters]) so the
     * Diary slot can render a cluster card on cluster days. Clusters
     * that don't belong to [isoDate] (in [zoneId]) are filtered out
     * before the state is emitted; non-cluster days observe an empty
     * cluster list and render no slot.
     */
    fun observe(isoDate: String) {
        if (currentIsoDate == isoDate && currentJob?.isActive == true) return

        currentJob?.cancel()
        currentIsoDate = isoDate
        _state.value = DayUiState.Loading(isoDate)

        currentJob = scope.launch {
            val pages = repository.observeDay(isoDate)
            // T148 — observeClusters is global. We always combine, but
            // start with `emptyList()` so the page flow alone can drive
            // the first emission (clusters are usually a no-op).
            val clusters = repository.observeClusters().onStart { emit(emptyList()) }

            combine(pages, clusters) { page, allClusters ->
                page to allClusters.filter { it.belongsToDay(isoDate, zoneId) }
            }
                .catch { e ->
                    _state.value = DayUiState.Error(
                        isoDate = isoDate,
                        message = e.message ?: e::class.java.simpleName
                    )
                }
                .collect { (page, dayClusters) ->
                    _state.value = reduce(isoDate, page.envelopes, dayClusters)
                }
        }
    }

    /** Tap-to-reassign (T051). Errors are swallowed — UI stays optimistic. */
    fun onReassignIntent(envelopeId: String, newIntentName: String, reason: String? = null) {
        scope.launch {
            runCatching { repository.reassignIntent(envelopeId, newIntentName, reason) }
        }
    }

    fun onArchive(envelopeId: String) {
        scope.launch { runCatching { repository.archive(envelopeId) } }
    }

    fun onDelete(envelopeId: String) {
        scope.launch { runCatching { repository.delete(envelopeId) } }
    }

    /** T069 — user tapped the “Couldn’t enrich this link” retry affordance. */
    fun onRetryHydration(envelopeId: String) {
        scope.launch { runCatching { repository.retryHydration(envelopeId) } }
    }

    // ---- Spec 003 v1.1 — Orbit Actions (T053) -----------------------------

    /**
     * Live proposals for [envelopeId]. Hot subscriptions are owned by the
     * caller composable's lifecycle (via `LaunchedEffect` + `collect`),
     * so the VM stays a thin passthrough — no per-envelope cache to leak.
     */
    fun observeProposals(envelopeId: String): Flow<List<ActionProposalParcel>> =
        repository.observeProposals(envelopeId)

    fun observeActionDrafts(limit: Int = 20) = repository.observeActionDrafts(limit)

    fun observeMemoryCandidates(limit: Int = 20) = repository.observeMemoryCandidates(limit)

    fun observePromotedMemories(limit: Int = 20) = repository.observePromotedMemories(limit)

    private val _undoState = MutableStateFlow<UndoToastState?>(null)
    /**
     * Active 5s undo toast, or `null` when the window has expired or the
     * action was cancelled. UI clears its own toast when this drops to
     * `null`.
     */
    val undoState: StateFlow<UndoToastState?> = _undoState.asStateFlow()

    private var undoExpiryJob: Job? = null
    private val confirmingProposalIds = mutableSetOf<String>()

    private val _actionNotice = MutableStateFlow<ActionNoticeState?>(null)
    val actionNotice: StateFlow<ActionNoticeState?> = _actionNotice.asStateFlow()

    /**
     * Confirm a proposal — flips state to CONFIRMED, dispatches the side
     * effect via the `:capture` executor, opens the 5s undo window. The
     * VM swallows IPC errors and surfaces the result via [undoState].
     *
     * [editedArgsJson] lets the preview card pass user-edited fields
     * (title, start, end, location, notes, tzId) while keeping the
     * proposal row's original args intact for forensics. When `null`,
     * the original [ActionProposalParcel.argsJson] is used.
     */
    fun onConfirmProposal(
        proposal: ActionProposalParcel,
        editedArgsJson: String? = null
    ) {
        if (!confirmingProposalIds.add(proposal.id)) return
        scope.launch {
            try {
                runCatching {
                    val changed = repository.markProposalConfirmed(proposal.id)
                    if (!changed) return@runCatching
                    val undoEligible = proposal.functionId != "calendar.createEvent"
                    val request = ActionExecuteRequestParcel(
                        proposalId = proposal.id,
                        envelopeId = proposal.envelopeId,
                        functionId = proposal.functionId,
                        schemaVersion = proposal.schemaVersion,
                        argsJson = ActionApprovalArgs.forExecution(proposal, editedArgsJson),
                        sensitivityScope = proposal.sensitivityScope,
                        confirmedAtMillis = System.currentTimeMillis(),
                        withUndo = undoEligible
                    )
                    val result = repository.executeAction(request)
                    when (result.outcome) {
                        "DISPATCHED", "SUCCESS" -> {
                            if (undoEligible) {
                                openUndoToast(
                                    UndoToastState(
                                        executionId = result.executionId,
                                        previewTitle = proposal.previewTitle,
                                        outcome = result.outcome,
                                        outcomeReason = result.outcomeReason
                                    )
                                )
                            } else {
                                openActionNotice(
                                    ActionNoticeState(
                                        id = result.executionId,
                                        message = "Opening ${proposal.previewTitle}"
                                    )
                                )
                            }
                        }
                        "FAILED" -> {
                            openActionNotice(
                                ActionNoticeState(
                                    id = result.executionId,
                                    message = actionFailureMessage(result.outcomeReason)
                                )
                            )
                        }
                        "USER_CANCELLED" -> {
                            openActionNotice(
                                ActionNoticeState(
                                    id = result.executionId,
                                    message = "Action cancelled."
                                )
                            )
                        }
                        else -> {
                            openActionNotice(
                                ActionNoticeState(
                                    id = result.executionId,
                                    message = "Action finished with status ${result.outcome.lowercase()}."
                                )
                            )
                        }
                    }
                }
            } finally {
                confirmingProposalIds.remove(proposal.id)
            }
        }
    }

    /** Dismiss before execute → ACTION_DISMISSED audit; no executor call. */
    fun onDismissProposal(proposalId: String) {
        scope.launch { runCatching { repository.markProposalDismissed(proposalId) } }
    }

    /** User tapped the toast Undo affordance within the 5s window. */
    fun onUndoExecution(executionId: String) {
        scope.launch {
            runCatching { repository.cancelWithinUndoWindow(executionId) }
            // Dismiss the toast immediately regardless of cancel outcome —
            // either the cancel succeeded (toast no longer relevant) or
            // the window already expired (no point keeping the toast).
            if (_undoState.value?.executionId == executionId) {
                undoExpiryJob?.cancel()
                _undoState.value = null
            }
        }
    }

    /** Manually clear the toast (e.g. user swiped it away). */
    fun onUndoToastDismissed() {
        undoExpiryJob?.cancel()
        _undoState.value = null
    }

    fun onActionNoticeDismissed() {
        _actionNotice.value = null
    }

    fun onAcceptMemoryCandidate(
        candidateId: String,
        editedLabel: String? = null,
        editedFactText: String? = null
    ) {
        scope.launch {
            val result = runCatching {
                repository.acceptMemoryCandidate(candidateId, editedLabel, editedFactText)
            }.getOrElse {
                MemoryDecisionResultParcel(
                    ok = false,
                    candidateId = candidateId,
                    memoryId = null,
                    status = "error",
                    message = "Orbit could not save that memory. Try again."
                )
            }
            openActionNotice(
                ActionNoticeState(
                    id = "memory-$candidateId-${System.currentTimeMillis()}",
                    message = result.message
                )
            )
        }
    }

    fun onRejectMemoryCandidate(candidateId: String, reason: String? = null) {
        scope.launch {
            val result = runCatching {
                repository.rejectMemoryCandidate(candidateId, reason)
            }.getOrElse {
                MemoryDecisionResultParcel(
                    ok = false,
                    candidateId = candidateId,
                    memoryId = null,
                    status = "error",
                    message = "Orbit could not dismiss that memory. Try again."
                )
            }
            openActionNotice(
                ActionNoticeState(
                    id = "memory-$candidateId-${System.currentTimeMillis()}",
                    message = result.message
                )
            )
        }
    }

    /**
     * T064 (003 US2) — toggle a `done` item on a derived to-do envelope.
     * Fire-and-forget; the underlying `setTodoItemDone` is idempotent
     * and writes through the `:ml` binder which re-emits the envelope
     * view downstream.
     */
    fun onToggleTodoItem(envelopeId: String, itemIndex: Int, done: Boolean) {
        scope.launch {
            runCatching { repository.setTodoItemDone(envelopeId, itemIndex, done) }
        }
    }

    fun onResolveActiveIntent(
        intentId: String,
        reason: ResolutionReason,
        userConfirmed: Boolean = true
    ) {
        scope.launch {
            runCatching {
                repository.resolveActiveIntent(
                    intentId = intentId,
                    resolutionReason = reason.name,
                    userConfirmed = userConfirmed
                )
            }
        }
    }

    fun onRequestActiveIntentEscalation(
        intentId: String,
        mode: UnderstandingMode = UnderstandingMode.SMART
    ) {
        scope.launch {
            runCatching { repository.requestActiveIntentEscalation(intentId, mode.name) }
        }
    }

    private fun openUndoToast(state: UndoToastState) {
        undoExpiryJob?.cancel()
        _undoState.value = state
        undoExpiryJob = scope.launch {
            delay(UNDO_WINDOW_MILLIS)
            // Only clear if the same toast is still visible — avoids
            // racing a fresh toast that arrived after this one.
            if (_undoState.value?.executionId == state.executionId) {
                _undoState.value = null
            }
        }
    }

    private fun openActionNotice(state: ActionNoticeState) {
        _actionNotice.value = state
    }

    /** UI-facing snapshot of the active undo window. */
    data class UndoToastState(
        val executionId: String,
        val previewTitle: String,
        val outcome: String,
        val outcomeReason: String?
    )

    data class ActionNoticeState(
        val id: String,
        val message: String
    )

    private companion object {
        /** Per action-execution-contract.md §5: 5 s undo window. */
        const val UNDO_WINDOW_MILLIS = 5_000L
    }

    private suspend fun reduce(
        isoDate: String,
        envelopes: List<com.orbit.app.data.ipc.EnvelopeViewParcel>,
        dayClusters: List<ClusterCardModel>
    ): DayUiState {
        if (envelopes.isEmpty() && dayClusters.isEmpty()) return DayUiState.Empty(isoDate)

        val threads = if (envelopes.isEmpty()) emptyList() else threadGrouper.group(envelopes)
        val header = dayHeaderGenerator.generate(isoDate, envelopes)
        return DayUiState.Ready(
            isoDate = isoDate,
            header = header.text,
            generationLocale = header.generationLocale,
            threads = threads,
            clusters = dayClusters
        )
    }

    /**
     * T148 — does this cluster's `timeBucketStart` resolve to [isoDate]
     * in [zone]? Cluster placement is anchored to the bucket's start so
     * a cluster spanning a midnight boundary surfaces on the day it
     * began. Aligns with how envelope `dayLocal` is computed upstream.
     */
    private fun ClusterCardModel.belongsToDay(isoDate: String, zone: ZoneId): Boolean {
        val day = Instant.ofEpochMilli(timeBucketStart)
            .atZone(zone)
            .toLocalDate()
            .toString()
        return day == isoDate
    }

    // ---- Phase 11 Block 10 (T148 review FU#2) — user-driven dismiss ----

    /**
     * User tapped the cluster-card "Dismiss" affordance. Fire-and-forget;
     * the cluster row transitions to DISMISSED on disk and the data-layer
     * flow re-emits with the row removed, so the card vanishes from the
     * Diary slot on the next collection. IPC errors are swallowed (the
     * card is best-effort UI; the worst case is a stale visual that
     * disappears on the next page change).
     */
    fun onDismissCluster(clusterId: String) {
        scope.launch {
            runCatching { repository.dismissCluster(clusterId) }
        }
    }

    override fun onCleared() {
        currentJob?.cancel()
        undoExpiryJob?.cancel()
        super.onCleared()
    }
}

internal fun actionFailureMessage(reason: String?): String = when (reason) {
    "ml_binder_unavailable" -> "Orbit could not reach local storage. Try again in a moment."
    "unknown_skill",
    "skill_not_registered" -> "Orbit does not know how to run this action yet."
    "schema_invalidated",
    "schema_mismatch" -> "This draft is out of date. Capture it again or wait for Orbit to extract a fresh draft."
    "intent_resolve_failed",
    "no_handler" -> "No app on this phone can open that action."
    "security_exception" -> "Android blocked this action for safety."
    null,
    "" -> "Orbit could not complete that action. Try again."
    else -> "Orbit could not complete that action. Try again."
}
