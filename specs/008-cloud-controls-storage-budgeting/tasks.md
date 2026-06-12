# Tasks: Cloud Controls, Storage, And Budgeting

**Input**: Design documents from `/specs/008-cloud-controls-storage-budgeting/`  
**Prerequisites**: `plan.md`, `spec.md`, `research.md`, `data-model.md`, `contracts/cloud-controls-contract.md`

**Tests**: Required. This branch changes privacy controls and cloud routing behavior.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel with nearby tasks because it touches different files.
- **[Story]**: Maps to user stories in `spec.md`.
- Each task names exact files.

## Phase 1: Setup And Artifact Lock

**Purpose**: Make Spec 008 concrete before implementation.

- [x] **T008-001** Create fresh Spec Kit artifacts in `specs/008-cloud-controls-storage-budgeting/`.
- [x] **T008-002** [P] Update `docs/orbit-roadmap-queue-2026-06-02.md` to mark Spec 008 active and preserve Spec 007 phone-only deferred status.
- [x] **T008-003** [P] Update `CODEX_HANDOFF.md` with Spec 008 branch state, policy decisions, and next validation gates.

---

## Phase 2: Foundational Policy And Receipt Model

**Purpose**: Add local, testable policy primitives before wiring any cloud call sites.

- [x] **T008-004** [P] [US1/US2/US3] Add `CloudCapability`, `BudgetDecision`, `BudgetDecisionReason`, `FallbackMode`, and `CloudUsageOutcome` in `app/src/main/java/com/orbit/app/cloud/CloudControlModels.kt`.
- [x] **T008-005** [P] [US1/US2/US3] Add `CloudControlPolicy` in `app/src/main/java/com/orbit/app/cloud/CloudControlPolicy.kt` that reads local preferences and returns pure decisions.
- [x] **T008-006** [US1/US2/US3] Extend `app/src/main/java/com/orbit/app/settings/PrivacyPreferences.kt` with `cloudAskSynthesisEnabled`, `cloudAiRoutingEnabled`, and `dailyCloudBudgetCents`.
- [x] **T008-007** [P] [US4] Add bounded receipt builder helpers in `app/src/main/java/com/orbit/app/cloud/CloudUsageReceiptWriter.kt` or extend `app/src/main/java/com/orbit/app/memory/MemoryAudit.kt` if smaller.
- [x] **T008-008** [P] [US1/US2/US3/US4] Add policy unit tests in `app/src/test/java/com/orbit/app/cloud/CloudControlPolicyTest.kt`.
- [x] **T008-009** [P] [US4] Add forbidden-key receipt tests in `app/src/test/java/com/orbit/app/cloud/CloudUsageReceiptTest.kt`.

**Checkpoint**: Policy tests pass without touching gateway behavior.

---

## Phase 3: User Story 1 - Compact Index Control And Receipts (Priority: P1)

**Goal**: Existing compact memory index control is normalized under policy and auditable as a cloud decision.

**Independent Test**: Disable compact indexing, sync/tombstone a fake envelope, and verify no fake gateway call plus skipped receipt.

### Tests

- [x] **T008-010** [P] [US1] Expand `app/src/test/java/com/orbit/app/memory/MemoryIndexSyncCoordinatorTest.kt` to assert disabled upsert and tombstone make zero gateway calls and write bounded skip receipts.
- [x] **T008-011** [P] [US1] Add/expand `app/src/test/java/com/orbit/app/settings/PrivacyPreferencesTest.kt` for compact index preference compatibility. No JVM SharedPreferences test added because this repo intentionally does not use Robolectric; compatibility is covered by preserving `KEY_MEMORY_INDEXING_ENABLED` and compiling Settings/Activity wiring.

### Implementation

- [x] **T008-012** [US1] Update `app/src/main/java/com/orbit/app/memory/MemoryIndexSyncCoordinator.kt` to use `CloudControlPolicy` or emit compatible `CloudCapability` receipt metadata.
- [x] **T008-013** [US1] Update `app/src/main/java/com/orbit/app/memory/MemoryIndexSyncDelegate.kt` only if needed to pass policy dependencies from preferences. No delegate change required; it already passes `PrivacyPreferences.memoryIndexingEnabled` into `MemoryIndexSyncCoordinator`.
- [x] **T008-014** [US1] Update Settings copy in `app/src/main/java/com/orbit/app/settings/SettingsScreen.kt` so compact index wording covers search mirror and raw-data exclusions.

**Checkpoint**: US1 can ship independently.

---

## Phase 4: User Story 2 - Cloud Ask Synthesis Toggle (Priority: P2)

**Goal**: Ask Orbit can run without cloud answer synthesis while preserving local cited retrieval/refusal.

**Independent Test**: Disable cloud Ask in repository tests; assert no `GroundedAsk` gateway call and local answer/refusal still works.

### Tests

- [x] **T008-015** [P] [US2] Expand `app/src/test/java/com/orbit/app/orbit/AskOrbitRepositoryTest.kt` with cloud-Ask-disabled no-gateway-call coverage.
- [x] **T008-016** [P] [US2] Add sensitive-refusal copy coverage in `app/src/test/java/com/orbit/app/orbit/AskOrbitRepositoryTest.kt`.

### Implementation

- [x] **T008-017** [US2] Update `app/src/main/java/com/orbit/app/orbit/AskOrbitRepository.kt` to accept/read cloud Ask policy before `requestGroundedAsk`.
- [x] **T008-018** [US2] Record a bounded skip/fallback receipt when cloud Ask is disabled or gateway fallback is used.
- [x] **T008-019** [US2] Update user-facing insufficient/sensitive evidence copy in `AskOrbitRepository.kt` or downstream UI model if necessary.

**Checkpoint**: Ask remains demoable with cloud Ask disabled.

---

## Phase 5: User Story 3 - Cloud AI Routing And Budget Gate (Priority: P3)

**Goal**: User preference can prevent `CloudLlmProvider` selection and budget-denied calls fail closed.

**Independent Test**: Disable cloud AI in router tests; assert no `CloudLlmProvider` and graceful unavailable/local provider behavior.

### Tests

- [x] **T008-020** [P] [US3] Expand `app/src/test/java/com/orbit/app/ai/LlmProviderRouterTest.kt` with cloud-AI-disabled decisions.
- [x] **T008-021** [P] [US3] Add budget-denied policy coverage in `app/src/test/java/com/orbit/app/cloud/CloudControlPolicyTest.kt`.
- [x] **T008-022** [P] [US3] Add receipt coverage for LLM capability metadata without prompts/model responses.

### Implementation

- [x] **T008-023** [US3] Update `app/src/main/java/com/orbit/app/ai/LlmProviderRouter.kt` to accept a cloud-routing decision/predicate while preserving current call sites.
- [x] **T008-024** [US3] Update necessary LLM call sites to pass durable cloud-routing policy from `PrivacyPreferences`.
- [x] **T008-025** [US3] Ensure disabled cloud AI returns deterministic fallback or local-unavailable copy instead of throwing a raw router exception.

**Checkpoint**: Cloud LLM routing is user-controllable.

---

## Phase 6: User Story 4 - Settings Surface And Receipt Visibility (Priority: P4)

**Goal**: Settings clearly shows three distinct controls and points users at bounded cloud activity.

**Independent Test**: Compose renders compact index, cloud Ask, and cloud AI rows with independent toggles and state preservation.

### Tests

- [x] **T008-026** [P] [US4] Expand `app/src/androidTest/java/com/orbit/app/settings/SettingsScreenTest.kt` for three separate cloud controls.
- [x] **T008-027** [P] [US4] Add JVM UI-state tests if existing Settings state can be covered without device. No JVM UI-state seam exists; Settings state is covered by `SettingsScreenTest` androidTest source and `compileDebugAndroidTestKotlin`.

### Implementation

- [x] **T008-028** [US4] Update `app/src/main/java/com/orbit/app/settings/SettingsScreen.kt` parameters and Quiet Settings layout for three controls.
- [x] **T008-029** [US4] Update `app/src/main/java/com/orbit/app/settings/SettingsActivity.kt` to read/write the new preferences.
- [x] **T008-030** [US4] Add a concise cloud activity status row or audit-log link copy in Settings without creating a separate dashboard unless necessary.

**Checkpoint**: Settings makes cloud behavior legible.

---

## Phase 7: Validation And Closeout

**Purpose**: Prove the branch is locally safe before moving to Spec 009.

- [x] **T008-031** Run focused JVM tests: `./gradlew :app:testDebugUnitTest --tests "com.orbit.app.cloud.*" --tests "com.orbit.app.memory.*" --tests "com.orbit.app.orbit.*" --tests "com.orbit.app.ai.*"`.
- [x] **T008-032** Run compile/lint/build gate: `./gradlew :app:compileDebugKotlin :app:compileDebugAndroidTestKotlin :build-logic:lint:test :app:lintDebug :app:assembleDebug`.
- [x] **T008-033** Verify no forbidden cloud receipt keys using tests and targeted `rg` review.
- [x] **T008-034** Update `specs/008-cloud-controls-storage-budgeting/quickstart.md` with actual validation results and known phone-deferred checks.
- [x] **T008-035** Update `docs/orbit-roadmap-queue-2026-06-02.md` and `CODEX_HANDOFF.md` with branch status.
- [x] **T008-036** Commit Spec 008 implementation; do not add `dist/`, APK outputs, `screenshots/`, or local secrets.

## Dependencies & Execution Order

1. Phase 1 locks artifacts.
2. Phase 2 blocks all implementation stories.
3. Phase 3 ships the already-existing index control safely.
4. Phase 4 gates Ask synthesis separately.
5. Phase 5 gates cloud LLM routing.
6. Phase 6 exposes the controls in UI.
7. Phase 7 validates and closes.

## Notes

- Do not add BYOC/BYOK in this branch.
- Do not change MongoDB/Atlas into source-of-truth storage.
- Do not introduce network clients outside `com.orbit.app.net`.
- Keep local fallback behavior working at every checkpoint.
