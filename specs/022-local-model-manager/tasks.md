# Tasks: Local Model Manager

**Input**: `spec.md`, `plan.md`, `research.md`, `data-model.md`, `contracts/local-model-manager-contract.md`

**Tests**: Required. This branch changes local/cloud inference routing semantics.

## Phase 1: Setup

- [x] **T022-001** Create fresh Spec Kit artifacts in `specs/022-local-model-manager/`.
- [x] **T022-002** Update roadmap and handoff to mark Spec 022 active.

## Phase 2: Local Model Policy

- [x] **T022-003** Add local model domain models under `app/src/main/java/com/orbit/app/ai/local/`.
- [x] **T022-004** Add deterministic `LocalModelSelectionPolicy`.
- [x] **T022-005** Add JVM tests for Speed, Intelligence, legacy Nano, Cloud, and Unavailable outcomes.

## Phase 3: Router Seam

- [x] **T022-006** Add optional BYOM/local selection seam to `LlmProviderRouter.resolve` without changing production defaults.
- [x] **T022-007** Add router tests proving selected BYOM provider can win in local mode and unavailable fails closed when cloud is disabled.
- [x] **T022-008** Update comments/docs to label Nano as current/legacy, not final architecture.

## Phase 4: Validation And Closeout

- [x] **T022-009** Run focused local-model/router tests.
- [x] **T022-010** Run compile gates.
- [x] **T022-011** Run full non-phone gate.
- [x] **T022-012** Run `git diff --check`.
- [x] **T022-013** Update quickstart, roadmap, and handoff with validation evidence.
- [ ] **T022-014** Commit without `dist/`, APK outputs, screenshots, `.gbrain-source`, or secrets.
