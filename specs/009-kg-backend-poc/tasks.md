# Tasks: KG Backend POC

**Input**: Design documents from `/specs/009-kg-backend-poc/`  
**Prerequisites**: `plan.md`, `spec.md`, `research.md`, `data-model.md`, `contracts/graph-backend-adapter-contract.md`

**Tests**: Required. KG storage changes privacy, deletion, provenance, and future agent behavior.

## Phase 1: Setup And Artifact Lock

- [x] **T009-001** Create fresh Spec Kit artifacts in `specs/009-kg-backend-poc/`.
- [x] **T009-002** Update `docs/orbit-roadmap-queue-2026-06-02.md` and `CODEX_HANDOFF.md` to mark Spec 009 active.

## Phase 2: Foundation Contract

- [x] **T009-003** [P] Add graph enums/drafts/results in `app/src/main/java/com/orbit/app/graph/GraphModels.kt`.
- [x] **T009-004** [P] Add `GraphBackendAdapter` in `app/src/main/java/com/orbit/app/graph/GraphBackendAdapter.kt`.
- [x] **T009-005** [P] Add adapter contract tests in `app/src/test/java/com/orbit/app/graph/GraphBackendAdapterContractTest.kt`.
- [x] **T009-006** [P] Add no-raw export/payload tests in `app/src/test/java/com/orbit/app/graph/GraphExportPolicyTest.kt`.

## Phase 3: Room Baseline

- [x] **T009-007** Add Room v10 graph entities in `app/src/main/java/com/orbit/app/data/entity/`.
- [x] **T009-008** Add graph DAOs in `app/src/main/java/com/orbit/app/data/dao/`.
- [x] **T009-009** Add `MIGRATION_9_10` and export schema `app/schemas/com.orbit.app.data.OrbitDatabase/10.json`.
- [x] **T009-010** Add migration test source `app/src/androidTest/java/com/orbit/app/data/OrbitDatabaseMigrationV9toV10Test.kt`.
- [x] **T009-011** Implement `RoomGraphBackendAdapter` in `app/src/main/java/com/orbit/app/graph/RoomGraphBackendAdapter.kt`.

## Phase 4: Provenance And Invalidation

- [x] **T009-012** [US1] Enforce no-provenance fact rejection in `RoomGraphBackendAdapter`.
- [x] **T009-013** [US1] Enforce no-provenance relationship rejection in `RoomGraphBackendAdapter`.
- [x] **T009-014** [US2] Implement `invalidateBySource` preserving facts with surviving provenance.
- [x] **T009-015** [US2] Add repository tests for single-source invalidation and multi-source survival.
- [x] **T009-016** [US2] Add user correction/rejection feedback model and tests.

## Phase 5: Spec 007 Projection

- [x] **T009-017** [US1] Add deterministic promoted-memory-to-fact projection in `GraphRepositoryDelegate`.
- [x] **T009-018** [US1] Prove pending/rejected memory candidates do not become active KG facts.
- [ ] **T009-019** [US1] Add audit rows for KG fact proposed/promoted/rejected/invalidated if new audit actions are needed.

## Phase 6: Binder And Why-This Projection

- [ ] **T009-020** [US1] Add compact graph projection parcels/AIDL methods or a narrow graph Binder surface.
- [ ] **T009-021** [US1] Implement `whyThis` projection with source envelope/user-confirmation ids.
- [ ] **T009-022** [US1] Add Binder payload cap/source-ready tests.

## Phase 7: Adapter Evaluation Closeout

- [ ] **T009-023** [US3] Document adapter evaluation verdicts for Room baseline and deferred external candidates in `research.md`.
- [ ] **T009-024** [US4] Verify Spec 008 cloud controls gate any compact graph mirror work; defer remote mirror if not implemented.
- [ ] **T009-025** Update `quickstart.md`, roadmap, and handoff with validation evidence.

## Phase 8: Validation

- [x] **T009-026** Run focused graph JVM tests.
- [x] **T009-027** Run full non-phone gate: `./gradlew :app:compileDebugKotlin :app:testDebugUnitTest :build-logic:lint:test :app:lintDebug :app:assembleDebug :app:compileDebugAndroidTestKotlin`.
- [x] **T009-028** Run `git diff --check`.
- [ ] **T009-029** Commit Spec 009 implementation without `dist/`, APK outputs, screenshots, `.gbrain-source`, or secrets.

## Notes

- Do not wire external graph vendors in production in this branch.
- Do not create autonomous agent behavior.
- Do not store pending/rejected candidates as active facts.
- Do not bypass `:ml`/Binder process boundaries.
