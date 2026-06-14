# Tasks: Resolution Semantics

**Input**: `spec.md`, `plan.md`, `research.md`, `data-model.md`, `contracts/resolution-semantics-contract.md`

**Tests**: Required. This feature changes lifecycle semantics, Room schema, repository behavior, and Follow-up surfacing.

## Phase 1: Setup And Artifact Lock

- [x] **T012-001** Create fresh Spec Kit artifacts in `specs/012-resolution-semantics/`.
- [x] **T012-002** Update roadmap and handoff to mark Spec 012 active.

## Phase 2: Receipt Domain And Schema

- [x] **T012-003** [P] Add resolution domain enums and verdict model under `app/src/main/java/com/orbit/app/resolution/`.
- [x] **T012-004** [P] Add `ResolutionReceiptEntity` and `ResolutionReceiptDao`.
- [x] **T012-005** Add Room v11 migration for `resolution_receipt` and export schema.
- [x] **T012-006** Add migration/schema tests for v10 to v11.
- [x] **T012-007** Add validation tests that receipts reject blank targets, invalid snooze windows, oversized metadata, and banned keys.

## Phase 3: Repository Semantics

- [x] **T012-008** Add `ResolutionRepository` helper for validated receipt writes and verdict queries.
- [x] **T012-009** Add `ResolutionVerdictResolver` tests for active, dismissed, not-now, snoozed, done, reopened, invalidated, stale, and source-deleted precedence.
- [ ] **T012-010** Add Binder parcels/methods only if UI or cross-process callers need direct receipt/verdict access.

## Phase 4: Duplicate Hooks

- [ ] **T012-011** Hook exact duplicate `sealWithResult` paths to write `DUPLICATE_RECAPTURE` receipts.
- [ ] **T012-012** Hook Basic-understanding duplicate suppression to write `DUPLICATE_RECAPTURE` receipts.
- [ ] **T012-013** Add tests proving duplicate receipt metadata carries ids/matched-by/content-hash reason only and no raw text.

## Phase 5: Action And Todo Hooks

- [ ] **T012-014** Hook action proposal dismiss to write `DISMISSED` receipt.
- [ ] **T012-015** Hook action schema/runtime invalidation to write `INVALIDATED` or `STALE` receipt.
- [ ] **T012-016** Hook derived todo aggregate completion to write `DONE` when all items become done.
- [ ] **T012-017** Hook derived todo reopen to write `REOPENED` when a completed list becomes incomplete.
- [ ] **T012-018** Add repository tests for action/todo receipt hooks.

## Phase 6: Follow-Up Surfacing

- [ ] **T012-019** Add Active Intent/Follow-up surfacing filter using resolution verdicts.
- [ ] **T012-020** Add minimal user affordances for dismiss, not-now, and snooze if current UI cannot produce those receipts.
- [ ] **T012-021** Add ViewModel/UI tests for dismiss/not-now/snooze surfacing behavior.

## Phase 7: Validation And Closeout

- [ ] **T012-022** Run focused resolution/duplicate/action/todo tests.
- [x] **T012-023** Run compile gates: `./gradlew :app:compileDebugKotlin :app:compileDebugAndroidTestKotlin`.
- [ ] **T012-024** Run full non-phone gate: `./gradlew :app:compileDebugKotlin :app:testDebugUnitTest :build-logic:lint:test :app:lintDebug :app:assembleDebug :app:compileDebugAndroidTestKotlin`.
- [ ] **T012-025** Run `git diff --check`.
- [ ] **T012-026** Update `quickstart.md`, roadmap, and handoff with validation evidence.
- [ ] **T012-027** Commit Spec 012 work without `dist/`, APK outputs, screenshots, `.gbrain-source`, or secrets.

## Deferred

- Full graph/profile conflict correction UI.
- Cloud sync of resolution receipts.
- Agent-authored resolution without explicit user approval.
- Platform-agent/AppFunctions interop.
