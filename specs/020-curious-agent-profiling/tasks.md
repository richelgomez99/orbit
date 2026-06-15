# Tasks: Curious Agent Profiling

**Input**: `spec.md`, `plan.md`, `research.md`, `data-model.md`, `contracts/curious-agent-contract.md`

**Tests**: Required. This branch touches user profiling trust boundaries.

## Phase 1: Setup

- [x] **T020-001** Create fresh Spec Kit artifacts in `specs/020-curious-agent-profiling/`.
- [x] **T020-002** Update roadmap and handoff to mark Spec 020 active.

## Phase 2: Pure Domain Foundation

- [x] **T020-003** Add curious-agent domain models under `app/src/main/java/com/orbit/app/curious/`.
- [x] **T020-004** Add deterministic question generator with minimum-evidence threshold and max-output cap.
- [x] **T020-005** Add JVM tests for insufficient evidence, source refs, capped output, dismissed suppression, and no raw text in question copy.

## Phase 3: Persistence/UI Decision

- [x] **T020-006** Decide whether answer/dismiss persistence belongs in this branch or remains deferred.
- [x] **T020-007** Document answer/dismiss persistence as deferred until repository-backed evidence integration.
- [x] **T020-008** Document Orbit tab UI as deferred until persisted question state exists.

## Phase 4: Validation And Closeout

- [x] **T020-009** Run focused curious-agent tests.
- [x] **T020-010** Run compile gates.
- [x] **T020-011** Run full non-phone gate.
- [x] **T020-012** Run `git diff --check`.
- [x] **T020-013** Update quickstart, roadmap, and handoff with validation evidence.
- [x] **T020-014** Commit without `dist/`, APK outputs, screenshots, `.gbrain-source`, or secrets.
