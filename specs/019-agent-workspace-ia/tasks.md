# Tasks: Agent Workspace IA

**Input**: `spec.md`, `plan.md`, `research.md`, `data-model.md`, `contracts/agent-workspace-ia-contract.md`

**Tests**: Required. This branch changes navigation state semantics.

## Phase 1: Setup

- [x] **T019-001** Create fresh Spec Kit artifacts in `specs/019-agent-workspace-ia/`.
- [x] **T019-002** Update roadmap and handoff to mark Spec 019 active.

## Phase 2: Library Reset Semantics

- [ ] **T019-003** Add `LibraryViewModel.reset()` that cancels in-flight search and restores `LibraryUiState()`.
- [ ] **T019-004** Add JVM tests proving reset clears query/results/errors/loading and cancels in-flight search.

## Phase 3: Tab Boundary Wiring

- [ ] **T019-005** Update `DiaryActivity` tab selection to reset Library when leaving Library.
- [ ] **T019-006** Preserve existing Ask reset when leaving Orbit.
- [ ] **T019-007** Add source-level or extracted controller test for tab-exit reset contract.

## Phase 4: Validation And Closeout

- [ ] **T019-008** Run focused Library/IA tests.
- [ ] **T019-009** Run compile gates.
- [ ] **T019-010** Run full non-phone gate.
- [ ] **T019-011** Run `git diff --check`.
- [ ] **T019-012** Update quickstart, roadmap, and handoff with validation evidence.
- [ ] **T019-013** Commit without `dist/`, APK outputs, screenshots, `.gbrain-source`, or secrets.
