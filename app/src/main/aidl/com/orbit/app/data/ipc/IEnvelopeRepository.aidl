// IEnvelopeRepository.aidl
package com.orbit.app.data.ipc;

import com.orbit.app.data.ipc.IntentEnvelopeDraftParcel;
import com.orbit.app.data.ipc.StateSnapshotParcel;
import com.orbit.app.data.ipc.SealResultParcel;
import com.orbit.app.data.ipc.EnvelopeViewParcel;
import com.orbit.app.data.ipc.DayPageParcel;
import com.orbit.app.data.ipc.IEnvelopeObserver;
import com.orbit.app.data.ipc.ActionProposalParcel;
import com.orbit.app.data.ipc.ActionDraftParcel;
import com.orbit.app.data.ipc.AppFunctionSummaryParcel;
import com.orbit.app.data.ipc.IActionProposalObserver;
import com.orbit.app.data.ipc.IActionDraftObserver;
import com.orbit.app.data.ipc.MemoryDecisionResultParcel;
import com.orbit.app.data.ipc.IMemoryCandidateObserver;
import com.orbit.app.data.ipc.IPromotedMemoryObserver;
import com.orbit.app.data.ipc.ClusterCardParcel;
import com.orbit.app.data.ipc.IClusterObserver;
import com.orbit.app.data.ipc.ActiveIntentParcel;
import com.orbit.app.data.ipc.IActiveIntentObserver;
import com.orbit.app.data.ipc.GraphWhyThisParcel;
import com.orbit.app.data.ipc.AgentPlanParcel;

interface IEnvelopeRepository {

    // ---- Seal path (called by :capture) ----
    String seal(
        in IntentEnvelopeDraftParcel draft,
        in StateSnapshotParcel state
    );

    SealResultParcel sealWithResult(
        in IntentEnvelopeDraftParcel draft,
        in StateSnapshotParcel state
    );

    // ---- Read path (called by :ui) ----
    void observeDay(String isoDate, IEnvelopeObserver observer);
    void stopObserving(IEnvelopeObserver observer);
    EnvelopeViewParcel getEnvelope(String envelopeId);
    List<EnvelopeViewParcel> searchLocalEnvelopes(String query, int limit);

    // ---- Mutate path (called by :ui) ----
    void reassignIntent(String envelopeId, String newIntentName, String reasonOpt);
    String getLatestNote(String envelopeId);
    boolean createOrUpdateLatestNote(String envelopeId, String text);
    void archive(String envelopeId);
    void delete(String envelopeId);
    boolean undo(String envelopeId);

    // ---- Spec 005 compact memory index sync ----
    // Called by WorkManager from the default process. The actual corpus read
    // and compact-payload construction stay inside this :ml repository
    // service; only the compact request crosses from :ml to :net.
    String syncMemoryIndex(String envelopeId, String mode, String reason);

    // ---- Soft-delete / trash ----
    void restoreFromTrash(String envelopeId);
    List<EnvelopeViewParcel> listSoftDeletedWithinDays(int days);
    int countSoftDeletedWithinDays(int days);
    // T091a — user-initiated hard purge from Trash (bypasses soft-delete,
    // audits ENVELOPE_HARD_PURGED with reason="user_purge").
    void hardDelete(String envelopeId);

    // ---- Diagnostics ----
    int countAll();
    int countArchived();
    int countDeleted();

    // ---- Multi-day pager (T056 / T050 follow-up) ----
    // Returns ISO-8601 local dates (newest first) that have at least one
    // non-archived, non-deleted envelope. Paginated so the DiaryPagingSource
    // can fetch older non-empty days in batches as the user swipes.
    List<String> distinctDayLocalsWithContent(int limit, int offset);

    // ---- Pre-seal classification support ----
    // Exposes the silent-wrap prior-match check so the UI-side
    // SilentWrapPredicate can decide chip-row vs silent-wrap before seal().
    // Mirrors EnvelopeStorageBackend.existsNonArchivedNonDeletedInLast30Days.
    boolean existsPriorIntent(String appCategory, String intent);

    // ---- URL hydration write-back (T066 merge-zone completion) ----
    // Called by UrlHydrateWorker before summarization to fetch only compact
    // prompt hints from :ml. The returned JSON is capped and must not contain
    // raw screenshot data, raw OCR, full HTML, or full envelope text.
    String getUrlHydrationContext(String envelopeId);

    // Called by UrlHydrateWorker from the default WorkManager process once
    // the network fetch + Readability + Nano summariser pass is done.
    // Writes ContinuationResultEntity (on success), updates
    // ContinuationEntity.status, and emits a CONTINUATION_COMPLETED audit
    // row — all in a single Room transaction (audit-log-contract.md §6).
    //
    // Nullable string params carry null via AIDL's standard string-null
    // handling. `ok == true` means the fetch + summariser produced a
    // persistable result; `ok == false` means terminal failure and the
    // ContinuationEntity transitions to FAILED_PERMANENT regardless of
    // the original errorKind (transient errors are retried by WorkManager
    // without ever hitting this method).
    void completeUrlHydration(
        String continuationId,
        String envelopeId,
        String canonicalUrl,
        String canonicalUrlHash,
        boolean ok,
        String title,
        String domain,
        String summary,
        String summaryModel,
        String failureReason
    );

    // ---- Manual hydration retry (T069 follow-up) ----
    // Called from Diary when the user taps the "Couldn't enrich this
    // link. Try again" affordance. Looks up non-succeeded continuations
    // for the envelope and re-enqueues each via ContinuationEngine.
    void retryHydration(String envelopeId);

    // ---- Screenshot OCR hydration (T076 / Phase 6 US4) ----
    // Called by ScreenshotUrlExtractWorker from the default WorkManager
    // process after running on-device OCR against an IMAGE envelope.
    // Writes one PENDING URL_HYDRATE ContinuationEntity per unique URL,
    // dedupes against the canonical-hash cache (emitting URL_DEDUPE_HIT
    // rows for cache hits), enqueues URL_HYDRATE WorkManager jobs, and
    // emits one INFERENCE_RUN audit row summarising the OCR pass (kind,
    // ocrLen, urlCount). All in a single Room transaction.
    //
    // `ocrText` is only used for the audit metadata (length + URL count).
    // Raw OCR text is NOT persisted anywhere (principle VIII — Collect
    // Only What You Use).
    void seedScreenshotHydrations(
        String envelopeId,
        String ocrText,
        in String[] urls
    );

    // ---- Spec 003 v1.1 — Orbit Actions ----

    // Looks up the registered AppFunction by id at the *latest* schema
    // version. Returns null when not registered.
    AppFunctionSummaryParcel lookupAppFunction(String functionId);

    // All registered AppFunctions for an app. v1.1 only ever returns the
    // built-in Orbit set; spec 008 expands to third-party packages.
    List<AppFunctionSummaryParcel> listAppFunctions(String appPackage);

    // Records a finished action invocation: writes the action_execution row,
    // updates the proposal state, and emits the matching audit row inside
    // a single Room transaction (audit-log-contract.md §6).
    //
    // `outcome` is one of "DISPATCHED" | "SUCCESS" | "FAILED" | "USER_CANCELLED".
    // `episodeId` is null in v1.1.
    void recordActionInvocation(
        String executionId,
        String proposalId,
        String functionId,
        String outcome,
        String outcomeReason,
        long dispatchedAtMillis,
        long completedAtMillis,
        long latencyMs,
        String episodeId
    );

    // User confirmed a proposal in the chip-row. Flips `state` PROPOSED→CONFIRMED
    // (no-op when already non-PROPOSED to keep concurrent taps safe) and
    // emits an ACTION_CONFIRMED audit row.
    boolean markProposalConfirmed(String proposalId);

    // User dismissed a proposal. Flips `state` PROPOSED→DISMISSED and emits
    // an ACTION_DISMISSED audit row.
    boolean markProposalDismissed(String proposalId);

    // Live feed of non-terminal proposals for an envelope, used by the
    // Diary card chip-row. Parallels `observeDay`/`stopObserving` lifecycle.
    void observeProposalsForEnvelope(String envelopeId, IActionProposalObserver observer);
    void stopObservingProposals(IActionProposalObserver observer);

    // Spec 006 — Orbit action workspace. Compact feed of pending action
    // drafts joined with source envelope and AppFunction display metadata.
    void observePendingActionDrafts(int limit, IActionDraftObserver observer);
    void stopObservingActionDrafts(IActionDraftObserver observer);

    // T044 — ACTION_EXTRACT continuation entry point. Called by
    // [com.orbit.app.ai.extract.ActionExtractionWorker] from the default
    // WorkManager process; the worker binds to :ml's EnvelopeRepositoryService
    // and forwards the envelopeId. The :ml-side implementation runs the
    // [ActionExtractor] (which holds the OrbitDatabase reference) and writes
    // ActionProposal rows + audit rows in one Room transaction.
    //
    // Returns a structured outcome code:
    //   "PROPOSED:<count>"     — N rows inserted
    //   "NO_CANDIDATES"        — extractor produced nothing (success, no row)
    //   "SKIPPED:<reason>"     — kind/sensitivity gate skipped
    //   "FAILED:<reason>"      — Nano timeout / exception → caller retries
    String extractActionsForEnvelope(String envelopeId);

    // Debug-build demo helper. Seeds deterministic proposal rows for known
    // debug demo captures. Release implementation returns "UNAVAILABLE".
    String debugSeedDemoActionProposals();

    // ---- Spec 007 — Memory candidates inspector ----

    // Compact pending memory suggestions for the Orbit tab. Payloads carry
    // only ids and display summaries; no raw screenshots/OCR/prompts/model
    // responses cross Binder.
    void observePendingMemoryCandidates(int limit, IMemoryCandidateObserver observer);
    void stopObservingMemoryCandidates(IMemoryCandidateObserver observer);

    // Compact accepted memory projection for the Orbit tab and later
    // Settings memory surface.
    void observePromotedMemories(int limit, IPromotedMemoryObserver observer);
    void stopObservingPromotedMemories(IPromotedMemoryObserver observer);

    // User decisions on candidate memory. Edited strings are optional and
    // validated in :ml before any promoted memory row is written.
    MemoryDecisionResultParcel acceptMemoryCandidate(
        String candidateId,
        String editedLabel,
        String editedFactText
    );
    MemoryDecisionResultParcel rejectMemoryCandidate(String candidateId, String reason);

    // Debug-build demo helper. Seeds deterministic memory candidates for
    // known debug captures. Release implementation returns "UNAVAILABLE".
    String debugSeedDemoMemoryCandidates();

    // ---- Spec 009 — compact graph provenance projection ----

    // Returns a bounded provenance projection for a local graph target.
    // `targetType` is one of ENTITY | FACT | RELATIONSHIP. The payload
    // carries only ids, labels, dates, and source types; it must not carry
    // raw screenshots, full OCR, prompts, model responses, embeddings, or
    // cloud payloads across Binder.
    GraphWhyThisParcel getGraphWhyThis(String targetType, String targetId);

    // ---- Spec 010 — approval-first agent coordinator ----

    // Deterministic local v1 coordinator. It returns cited plan/question/refusal
    // projections only; it does not execute actions. `allowModelAssist` is
    // accepted for forward compatibility but this path must still work when
    // cloud/model routing is unavailable or disabled.
    AgentPlanParcel planAgentRequest(
        String requestId,
        String query,
        in String[] attachedEnvelopeIds,
        int maxEvidence,
        boolean allowModelAssist
    );

    // T061 — TodoActionHandler local-target dispatch entry point.
    //
    // Inserts one new envelope per parsed item in a single Room transaction:
    //   kind=REGULAR, intent=WANT_IT, intentSource=AUTO_AMBIGUOUS,
    //   todoMetaJson={"items":[{"text":..., "done":false,
    //                            "dueEpochMillis":<long-or-null>}],
    //                 "derivedFromProposalId":"<proposalId>"}.
    // Source envelope is NOT mutated (Principle III).
    //
    // `itemsJson` is a JSON array: each element either a string (text only)
    // or an object {"text":"…","dueEpochMillis":<long>}. Anything malformed
    // is dropped; if the array is empty, no envelope is created.
    //
    // Returns the list of new envelope ids (in input order).
    List<String> createDerivedTodoEnvelope(
        String parentEnvelopeId,
        String itemsJson,
        String proposalId
    );

    // T064 — checkbox toggle for derived-todo envelopes. No-op if the
    // envelope has no `todoMetaJson` or the index is out of range.
    void setTodoItemDone(String envelopeId, int itemIndex, boolean done);

    // T072 / T074 — WeeklyDigestWorker entry point. `targetDayLocal` is
    // the ISO-8601 local Sunday this digest is for (e.g. "2026-04-26").
    // Returns one of:
    //   "GENERATED:<envelopeId>"  — DIGEST envelope inserted
    //   "SKIPPED:too_sparse"      — < 3 source envelopes in window
    //   "SKIPPED:already_exists"  — partial unique index conflict
    //   "FAILED:<short_reason>"   — worker maps to Result.retry()
    String runWeeklyDigest(String targetDayLocal);

    // ---- Spec 002 Phase 11 Block 5 — Cluster surface (T134) ----

    // Live feed of currently-surfaceable clusters (state ∈
    // {SURFACED,TAPPED,ACTING,ACTED,FAILED} && surviving members ≥ 3,
    // FR-037). Mirrors the observeDay/stopObserving lifecycle: binder
    // is the observer key, observer death triggers cleanup, callers
    // pair `observeClusters` with `stopObservingClusters` exactly once.
    //
    // The repository applies two read-side gates beyond what the DAO
    // can express: the runtime kill switch (RuntimeFlags.clusterEmitEnabled,
    // empty list when off) and the model-label lock (FR-030 — rows with
    // a drifted modelLabel are excluded so a label change after the fact
    // hides stale clusters instantly).
    void observeClusters(IClusterObserver observer);
    void stopObservingClusters(IClusterObserver observer);

    // Phase 11 Block 10 (T148 review FU#2) — user-driven dismiss for the
    // diary cluster card. Transitions the cluster row to DISMISSED with
    // dismissedAt = now and writes a CLUSTER_DISMISSED audit row. Idempotent:
    // calling on an already-terminal cluster is a no-op (returns false).
    // Returns true iff a row was actually transitioned.
    boolean markClusterDismissed(String clusterId);

    // Spec 002 Phase 11 Block 13 (T161/T162) — cluster.summarize AppFunction
    // entry point. Resolves [clusterId], transitions to ACTING (forensics
    // before Nano), runs ClusterSummariser, and on success persists a
    // DERIVED IntentEnvelope with derivedVia="cluster_summarize" plus a
    // CLUSTER_SUMMARY_GENERATED audit row in one transaction, then
    // transitions the cluster to ACTED. Returns the same outcome-string
    // contract as runWeeklyDigest: "GENERATED:<envelopeId>" /
    // "FAILED:<reason>". The caller (ClusterSummarizeActionHandler)
    // re-fetches the envelope by id via getEnvelope when needed.
    String summarizeCluster(String clusterId);

    // ---- Spec 004 — Active Intent cleanup queue ----

    // Live compact Active Intent feed for the cleanup surface. The payload is
    // intentionally bounded: no raw screenshots, raw HTML, full OCR bodies,
    // embeddings, prompts, or model responses cross the binder.
    void observeActiveIntents(IActiveIntentObserver observer);
    void stopObservingActiveIntents(IActiveIntentObserver observer);

    // User-driven resolution/archive path for the cleanup surface. Returns
    // true iff a matching row was transitioned.
    boolean resolveActiveIntent(String intentId, String resolutionReason, boolean userConfirmed);

    // Spec 012 — receipt-only cleanup semantics. These hide active follow-ups
    // without claiming the real-world loop is done.
    boolean markActiveIntentNotNow(String intentId);
    boolean snoozeActiveIntent(String intentId, long untilMillis);

    // Audit-only escalation affordance. This records the user's request before
    // any future Smart/Deep queue can dispatch work.
    boolean requestActiveIntentEscalation(String intentId, String mode);
}
