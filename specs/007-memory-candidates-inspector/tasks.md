# Tasks: Memory Candidates Inspector

**Input**: `spec.md`, `plan.md`, `research.md`, `data-model.md`, `contracts/`
**Branch**: `feature/007-memory-candidates-inspector-20260605`
**Status**: Room v9 foundation, compact Binder/repository layer, Orbit Memory Review UI, debug seeding, ViewModel decision-flow coverage, and non-phone local gates are implemented. Remaining work focuses on Compose UI tests, explicit Ask/action/cloud exclusion tests, connected migration/repository execution when a device/emulator is available, docs closeout, and commit.

## Phase 0: Spec Kit Lock

- [x] T007-001 Replace placeholder spec with fresh Spec Kit artifacts grounded in current Spec 005A/006 code.
- [x] T007-002 Reconcile vision/architecture docs: memory ladder, inspector, promotion policy, local-first boundaries.
- [x] T007-003 Run a gstack-style plan/engineering review or equivalent before schema implementation. Pivots recorded in `plan.md`: add support junction tables, keep Settings memory manager out of scope, invalidate source-less memories conservatively, and defer cloud memory sync shape.

## Phase 1: Room Foundation

- [x] T007-004 Add `MemoryCandidateEntity`, `MemoryCandidateSupportEntity`, `PromotedMemoryEntity`, and `PromotedMemorySupportEntity` under `app/src/main/java/com/orbit/app/data/entity/`.
- [x] T007-005 Add candidate/promoted memory DAOs and support DAOs under `app/src/main/java/com/orbit/app/data/dao/`.
- [x] T007-006 Add enums/models for candidate kind/state/sensitivity/source and promoted memory kind/state/source.
- [x] T007-007 Bump `OrbitDatabase` to v9, register DAOs/entities, and add `MIGRATION_8_9`.
- [x] T007-008 Export Room schema and add v8->v9 migration test. Source compile passes; connected migration execution deferred until a device/emulator is available.
- [ ] T007-009 Add DAO tests for pending list, duplicate active candidate suppression, accept idempotency, reject idempotency, and source deletion invalidation. Repository decision androidTest source exists; remaining: focused DAO/source-deletion coverage and connected execution.

## Phase 2: Binder And Repository

- [x] T007-010 Add AIDL parcels/observers for `MemoryCandidateParcel`, `PromotedMemoryParcel`, and `MemoryDecisionResultParcel`.
- [x] T007-011 Extend `IEnvelopeRepository` with compact candidate/promoted list and decision methods.
- [x] T007-012 Add `MemoryRepositoryDelegate` in `:ml` for projection and decision logic.
- [x] T007-013 Wire delegate through `EnvelopeRepositoryImpl` and `EnvelopeRepositoryService`.
- [x] T007-014 Add audit rows for accept/reject/edit/invalidate decisions with compact extra JSON.
- [x] T007-015 Add JVM/unit tests for parcel mapping and repository decision semantics. JVM parcel test passes; repository decision androidTest source compiles and awaits connected execution.

## Phase 3: Orbit Memory Review UI

- [x] T007-016 Extend `DiaryRepository`, `BinderDiaryRepository`, and `DiaryViewModel` with candidate/promoted memory state and ViewModel-level flow/decision coverage.
- [x] T007-017 Add Memory Review section/cards to `OrbitCleanupScreen` after Action Drafts.
- [x] T007-018 Add source-open, accept, reject, and edit sheet flows.
- [x] T007-019 Add user-facing failure/success state that does not rely only on transient toast.
- [ ] T007-020 Add Compose/UI tests for pending candidates, no-empty-state noise, accept/reject callbacks, and edited acceptance validation.

## Phase 4: Debug Demo And Boundary Tests

- [x] T007-021 Add debug-only deterministic memory candidate seeding.
- [x] T007-022 Ensure release build has no arbitrary memory candidate insert API.
- [ ] T007-023 Add tests proving pending/rejected candidates are not used as facts by Ask/action answer code.
- [ ] T007-024 Add tests proving sensitive/local-only candidates are excluded from compact memory/Atlas sync payloads.
- [x] T007-025 Run secret scan and network-boundary scan.

## Phase 5: Validation And Closeout

- [x] T007-026 Run focused gates for memory DAO/repository/UI tests. Latest focused ViewModel coverage: `./gradlew :app:testDebugUnitTest --tests "com.orbit.app.diary.DiaryViewModelTest"`.
- [x] T007-027 Run full local Android gate: `./gradlew :app:compileDebugKotlin :app:testDebugUnitTest :build-logic:lint:test :app:lintDebug :app:assembleDebug :app:compileDebugAndroidTestKotlin`.
- [x] T007-028 Build APK and record SHA in `quickstart.md`.
- [x] T007-029 Update roadmap and `CODEX_HANDOFF.md` with evidence.
- [ ] T007-030 Commit branch without generated APK/screenshot artifacts.

## Deferred Explicitly Out Of Scope

- [ ] T007-D01 Knowledge graph backend/entities/edges. Spec 009 owns this.
- [ ] T007-D02 Curious Agent profile questions. Spec 020 owns this.
- [ ] T007-D03 Agent planning over memory. Spec 010 owns this.
- [ ] T007-D04 Cloud sync of memory facts. Spec 008 and Spec 009 own policy/backend.
- [ ] T007-D05 Automatic promotion from repeated behavior without explicit user acceptance.
