# Tasks: Generative UI Runtime

**Input**: `spec.md`, `plan.md`, `research.md`, `data-model.md`, `contracts/generative-ui-runtime-contract.md`

**Tests**: Required. This branch establishes an agent-output trust boundary.

## Phase 1: Setup

- [x] **T021-001** Create fresh Spec Kit artifacts in `specs/021-generative-ui-runtime/`.
- [x] **T021-002** Update roadmap and handoff to mark Spec 021 active.

## Phase 2: Typed Runtime Foundation

- [x] **T021-003** Add typed Orbit agent UI document/component models under `app/src/main/java/com/orbit/app/generativeui/`.
- [x] **T021-004** Add `AgentPlanParcel` adapter with display caps and evidence preservation.
- [x] **T021-005** Add deterministic plain-text fallback renderer.
- [x] **T021-006** Add JVM tests for caps, citation preservation, approval wording, and fallback output.

## Phase 3: Compose Renderer

- [x] **T021-007** Add Compose renderer for title, body, questions, steps, limitations, and evidence rows.
- [x] **T021-008** Wire `OrbitCleanupScreen` agent-plan result to the typed renderer.
- [x] **T021-009** Preserve existing evidence-open behavior.

## Phase 4: Validation And Closeout

- [x] **T021-010** Run focused generative UI tests.
- [x] **T021-011** Run compile gates.
- [x] **T021-012** Run full non-phone gate.
- [x] **T021-013** Run `git diff --check`.
- [x] **T021-014** Update quickstart, roadmap, and handoff with validation evidence.
- [ ] **T021-015** Commit without `dist/`, APK outputs, screenshots, `.gbrain-source`, or secrets.
