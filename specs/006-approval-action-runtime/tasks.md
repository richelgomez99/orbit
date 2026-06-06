# Tasks: Approval Action Runtime

**Input**: `spec.md`, `plan.md`, `research.md`, `data-model.md`, `contracts/`
**Branch**: `feature/006-approval-action-runtime-20260603`
**Status**: Implementation and local gates are complete. Contract reconciliation, Orbit action draft projection/UI, typed Calendar and grouped local list approval, visible outcomes/failures, lifecycle audits, permission regression, debug seeding, APK build, S24 install/seed, and local Android gates are complete. Remaining work is final S24 manual demo confirmation, closeout docs, and commit.

## Format

- `[P]` means parallelizable after prerequisites.
- `[US1]`, `[US2]`, `[US3]`, `[US4]` map to user stories in `spec.md`.
- Every task names exact files or commands.

## Phase 0: Spec Kit Lock

- [x] T006-001 Create/refine `specs/006-approval-action-runtime/` artifacts from the validated Spec 005A/005B baseline.
- [x] T006-002 Reconcile current code reality by scanning `app/src/main/java/com/orbit/app/action`, `app/src/main/java/com/orbit/app/diary`, `app/src/main/java/com/orbit/app/data`, and `specs/003-orbit-actions/`.
- [x] T006-003 Run a pre-implementation gstack plan review or equivalent engineering review against this plan; recorded pivots: no fake Calendar undo, proposal id injected after persistence, failure outcome projection required, debug proposal seeding must stay debug-only/process-safe.

## Phase 1: Contract Reconciliation Foundation

**Goal**: Make schemas, handlers, extraction output, and fixtures agree before adding UI.

- [x] T006-004 [P] Add/extend tests for `BuiltInAppFunctionSchemas.TASKS_CREATE_TODO` proving the model-facing schema accepts non-empty `items`, optional `parentEnvelopeId`, and optional `target`, and does not require model-generated `proposalId`.
- [x] T006-005 Update `app/src/main/java/com/orbit/app/action/BuiltInAppFunctionSchemas.kt` so `tasks.createTodo` matches `TodoActionHandler`.
- [x] T006-006 [P] Add/extend `TodoActionHandler` tests for local target success, missing parent, missing proposal id, empty items, and external chooser fallback. Existing `TodoAddHandlerLocalTest`/`TodoAddHandlerExternalTest` already cover these executor-facing paths.
- [x] T006-007 Update `app/src/main/java/com/orbit/app/action/handler/TodoActionHandler.kt` only if tests prove handler/schema mismatch remains after schema fix. No handler change required; runtime now injects `proposalId` after persistence.
- [x] T006-008 [P] Add `ActionExtractor`/provider fixture tests proving todo candidates use the aligned model-facing contract and stale/unknown schemas are dropped. Updated provider contract fixture and added `ActionApprovalArgsTest` for runtime injection.
- [x] T006-009 Run focused contract gate: `./gradlew :app:testDebugUnitTest --tests "com.orbit.app.data.SchemaValidationTest" --tests "com.orbit.app.ai.LlmProviderExtractActionsContractTest" --tests "com.orbit.app.diary.ActionApprovalArgsTest" --tests "com.orbit.app.diary.DiaryViewModelTest" :app:compileDebugKotlin`.

## Phase 2: User Story 1 - Orbit Action Draft Workspace (P1)

**Goal**: Pending action drafts are visible and reviewable in Orbit with source evidence.

**Independent Test**: Seed proposals, open Orbit, verify pending drafts appear, open source capture, dismiss a draft, and confirm it disappears.

- [x] T006-010 [US1] Add `ActionDraftParcel` and observer/list AIDL under `app/src/main/aidl/com/orbit/app/data/ipc/`.
- [x] T006-011 [US1] Add matching Kotlin parcel classes under `app/src/main/java/com/orbit/app/data/ipc/`.
- [x] T006-012 [US1] Add pending draft projection query in `app/src/main/java/com/orbit/app/data/dao/ActionProposalDao.kt` or a DAO-backed delegate method.
- [x] T006-013 [US1] Expose pending draft projection from `app/src/main/java/com/orbit/app/data/ActionsRepositoryDelegate.kt` and `EnvelopeRepositoryImpl.kt`.
- [x] T006-014 [US1] Add UI repository/view-model path for pending action drafts via `DiaryRepository`, `BinderDiaryRepository`, and `DiaryViewModel`.
- [x] T006-015 [US1] Render Action Drafts section/cards in the Orbit tab using existing styles; cards show action title, source app/title/day, side-effect label, and Open Source.
- [x] T006-016 [US1] Wire Dismiss from Orbit draft cards to `markProposalDismissed`.
- [x] T006-017 [US1] Add JVM/Compose tests for draft projection and rendering with no raw JSON. `ActionDraftParcelTest` covers projection-to-proposal mapping; grouped checklist Compose coverage added for derived list rendering.

## Phase 3: User Story 2 - Calendar Approval Runtime (P1)

**Goal**: Calendar drafts can be edited and approved, then the system Calendar insert UI opens.

**Independent Test**: Approve seeded calendar draft on S24; Calendar insert screen opens with expected fields.

- [x] T006-018 [US2] Add/extend tests for `parseCalendarArgs` and confirm validation in `ActionPreviewCardUI.kt`.
- [x] T006-019 [US2] Ensure Calendar approval sheet is reachable from both Diary chips and Orbit draft cards.
- [x] T006-020 [US2] Harden double-confirm behavior in `DiaryViewModel`/Orbit action VM so one proposal cannot dispatch twice.
- [x] T006-021 [US2] Add/extend instrumented tests for `CalendarActionHandler` intent extras and no calendar permissions. Existing `CalendarInsertHandlerTest` covers extras/failure paths; `ActionPermissionRegressionTest` covers no calendar/contact/storage-write permissions.
- [x] T006-022 [US2] Add user-facing failure copy for no handler/security/schema failure; do not leave only an undo toast with raw reason.
- [x] T006-023 [US2] Remove or suppress generic undo affordance for `calendar.createEvent`; Calendar now executes with `withUndo=false` and no undo toast, while local todo remains undo-eligible.

## Phase 4: User Story 3 - Local Todo/List Approval Runtime (P2)

**Goal**: Local list/task drafts create one derived Orbit list envelope and remain searchable.

**Independent Test**: Approve seeded shopping-list draft; one derived list envelope appears in Diary/Library with multiple checklist items and toggles work.

- [x] T006-024 [US3] Replace read-only raw JSON fallback in `ActionPreviewCardUI.kt` with typed todo/list fields for `tasks.createTodo`.
- [x] T006-025 [US3] Ensure confirm sheet/runtime injects `parentEnvelopeId` and `proposalId` for local todo execution after proposal persistence.
- [x] T006-026 [US3] Add tests proving approved local todo creates one derived list envelope with `todoMetaJson.items[]` and audit provenance.
- [x] T006-027 [US3] Add/extend UI tests for derived todo checkbox rendering/toggling in Diary.
- [x] T006-028 [US3] Verify Library search can surface derived list envelopes without special cloud requirements.

## Phase 5: User Story 4 - Outcome, Audit, Failure, Undo (P2)

**Goal**: Every action outcome is trustworthy and auditable.

**Independent Test**: Exercise success, dismiss, schema mismatch, no handler, binder unavailable, and undo/cancel paths.

- [x] T006-029 [US4] Add tests for proposal lifecycle audit rows in `ActionsRepositoryDelegate`.
- [x] T006-030 [US4] Add tests for `ActionExecutorService` schema mismatch and unknown skill paths without firing intents.
- [x] T006-031 [US4] Add UI state/projection for execution failure/success that does not rely solely on transient toast.
- [x] T006-032 [US4] Validate `skill_usage` updates on success/failure/cancel and does not double-count duplicate confirm.
- [x] T006-033 [US4] Add permission regression test proving no `READ_CALENDAR`, `WRITE_CALENDAR`, contacts, or storage write permissions were added.
- [x] T006-034 [US4] Run network boundary scan: `rg -n "OkHttpClient\\(|HttpURLConnection|Socket\\(|HttpClient\\(" app/src/main/java app/src/debug/java app/src/release/java || true`. Result: no hits in scanned app source.

## Phase 6: Debug Demo And S24 Validation

**Goal**: Deterministic device demo for close-loop actions.

- [x] T006-035 Add debug-only deterministic action proposal seeding in `app/src/debug/java/com/orbit/app/debug/DebugDemoSeedReceiver.kt` or a dedicated debug receiver; release implementation returns `UNAVAILABLE` and no arbitrary production proposal-insert API was added.
- [x] T006-036 Add install/seed helper script under `specs/006-approval-action-runtime/scripts/`.
- [x] T006-037 Build APK and copy to `dist/orbit-mvp-debug-20260603-006.apk`; record SHA in `quickstart.md` (`05cb7026b5738b7c55f3cba7a0d168a4dabcb182270b2f18ad05863d7fad71c5`).
- [x] T006-038 Run full Android gate: `./gradlew :app:compileDebugKotlin :app:testDebugUnitTest :build-logic:lint:test :app:lintDebug :app:assembleDebug :app:compileDebugAndroidTestKotlin`. Passed on 2026-06-04.
- [ ] T006-039 Run S24 demo: Orbit Action Drafts, calendar approval, no fake calendar undo, local todo approval, dismiss, and failure path.
- [x] T006-040 Update `quickstart.md`, `docs/orbit-roadmap-queue-2026-06-02.md`, and `CODEX_HANDOFF.md` with evidence.
- [ ] T006-041 Commit the completed Spec 006 branch without generated APK artifacts.

## Deferred Explicitly Out Of Scope

- [ ] T006-D01 AppFunctions/Spark/platform-agent interop.
- [ ] T006-D02 Autonomous agent planning. Spec 010 owns this.
- [ ] T006-D03 Knowledge graph facts/entities. Spec 009 owns this.
- [ ] T006-D04 BYOM/local model manager. Spec 022 owns this.
- [ ] T006-D05 Direct third-party app API integrations.
