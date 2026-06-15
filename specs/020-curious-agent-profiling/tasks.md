# Tasks: Curious Agent Profiling

**Input**: `spec.md`, `plan.md`, `research.md`, `data-model.md`, `contracts/curious-agent-contract.md`

**Tests**: Required. This branch touches user profiling trust boundaries.

## Phase 1: Setup

- [x] **T020-001** Create fresh Spec Kit artifacts in `specs/020-curious-agent-profiling/`.
- [x] **T020-002** Update roadmap and handoff to mark Spec 020 active.

## Phase 2: Pure Domain Foundation

- [ ] **T020-003** Add curious-agent domain models under `app/src/main/java/com/orbit/app/curious/`.
- [ ] **T020-004** Add deterministic question generator with minimum-evidence threshold and max-output cap.
- [ ] **T020-005** Add JVM tests for insufficient evidence, source refs, capped output, dismissed suppression, and no raw text in question copy.

## Phase 3: Persistence/UI Decision

- [ ] **T020-006** Decide whether answer/dismiss persistence belongs in this branch or remains deferred.
- [ ] **T020-007** If persistence is included, add Room/Binder contracts before UI.
- [ ] **T020-008** If UI is included, add Orbit tab surface below higher-priority action/agent panels.

## Phase 4: Validation And Closeout

- [ ] **T020-009** Run focused curious-agent tests.
- [ ] **T020-010** Run compile gates.
- [ ] **T020-011** Run full non-phone gate.
- [ ] **T020-012** Run `git diff --check`.
- [ ] **T020-013** Update quickstart, roadmap, and handoff with validation evidence.
- [ ] **T020-014** Commit without `dist/`, APK outputs, screenshots, `.gbrain-source`, or secrets.
