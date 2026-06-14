# Tasks: Agent Coordinator

**Input**: `spec.md`, `plan.md`, `research.md`, `data-model.md`, `contracts/agent-coordinator-contract.md`

**Tests**: Required. Agent planning touches evidence, action approval, privacy controls, and future model trust boundaries.

## Phase 1: Setup And Artifact Lock

- [x] **T010-001** Create fresh Spec Kit artifacts in `specs/010-agent-coordinator/`.
- [x] **T010-002** Update `docs/orbit-roadmap-queue-2026-06-02.md` and `CODEX_HANDOFF.md` to mark Spec 010 active.

## Phase 2: Coordinator Contract

- [x] **T010-003** [P] Add `AgentModels.kt` under `app/src/main/java/com/orbit/app/agent/`.
- [x] **T010-004** [P] Add pure coordinator contract tests under `app/src/test/java/com/orbit/app/agent/`.
- [x] **T010-005** [P] Add no-raw trace/payload tests.

## Phase 3: Deterministic Local Planner

- [x] **T010-006** [US1] Implement `DeterministicAgentPlanner` for cited plan/refusal/question outcomes.
- [x] **T010-007** [US1] Add evidence validation that rejects uncited plan steps.
- [x] **T010-008** [US2] Add ambiguity handling with cited choice/gap question output.
- [x] **T010-009** [US3] Add local-only/cloud-disabled tests proving no model/gateway dependency.

## Phase 4: Action And KG Integration

- [x] **T010-010** [US1] Read registered AppFunction/action capability summaries through existing delegates.
- [x] **T010-011** [US1] Reference existing action proposals/drafts as approval-required plan steps.
- [x] **T010-012** [US1] Read compact graph `whyThis` projections for graph evidence.
- [x] **T010-013** [US1] Add tests that action steps never execute and always require approval.

## Phase 5: Binder Projection

- [x] **T010-014** [US1] Add compact Agent parcel/AIDL surfaces under `app/src/main/aidl/com/orbit/app/data/ipc/` and `app/src/main/java/com/orbit/app/data/ipc/`.
- [x] **T010-015** [US1] Wire repository/Binder method through `EnvelopeRepositoryImpl` or a narrow agent Binder service.
- [x] **T010-016** [US1] Add source-ready Binder tests for caps and no raw content.

## Phase 6: Orbit Surface

- [ ] **T010-017** [US1] Add minimal Orbit tab plan panel using existing Quiet Almanac components.
- [ ] **T010-018** [US2] Render gap questions/choices without executing actions.
- [ ] **T010-019** [US1] Wire plan action steps to existing capture open/action approval paths only.
- [ ] **T010-020** [US1] Add ViewModel/UI tests for plan/refusal/question states.

## Phase 7: Model Assistance

- [ ] **T010-021** [US4] Add optional model-assisted planning adapter behind existing `LlmProviderRouter`.
- [ ] **T010-022** [US4] Validate model output against local evidence ids/function ids before display.
- [ ] **T010-023** [US4] Add tests for invalid model output forcing deterministic fallback/refusal.
- [ ] **T010-024** [US3] Verify Spec 008 cloud AI controls disable model-assisted planning.

## Phase 8: Validation

- [x] **T010-025** Run focused agent JVM tests.
- [ ] **T010-026** Run full non-phone gate: `./gradlew :app:compileDebugKotlin :app:testDebugUnitTest :build-logic:lint:test :app:lintDebug :app:assembleDebug :app:compileDebugAndroidTestKotlin`.
- [ ] **T010-027** Run `git diff --check`.
- [ ] **T010-028** Update `quickstart.md`, roadmap, and handoff with validation evidence.
- [ ] **T010-029** Commit Spec 010 work without `dist/`, APK outputs, screenshots, `.gbrain-source`, or secrets.

## Deferred

- AppFunctions/Spark/platform-agent interop.
- Persistent chat/workbench session storage.
- A2UI/generative UI runtime.
- BYOM/local model manager.
- Autonomous background planning.
