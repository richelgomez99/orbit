# Tasks: Capture Context Affordance

**Input**: `spec.md`, `plan.md`, `research.md`, `data-model.md`, `contracts/capture-context-affordance-contract.md`

**Tests**: Required. This feature touches overlay UX, Binder note persistence, duplicate context targeting, and process-boundary safety.

## Phase 1: Setup And Artifact Lock

- [x] **T018-001** Create fresh Spec Kit artifacts in `specs/018-capture-context-affordance/`.
- [x] **T018-002** Update roadmap and handoff to mark Spec 018 active.
- [x] **T018-003** Audit current Spec 011 post-capture context code and record whether full-detail fallback remains.

## Phase 2: Context Save Seam

- [ ] **T018-004** [P] Add focused context saver/domain seam under `app/src/main/java/com/orbit/app/diary/` or `app/src/main/java/com/orbit/app/overlay/`.
- [ ] **T018-005** [P] Add JVM tests for blank text, successful save, Binder unavailable/failure, and no raw payload leakage.
- [ ] **T018-006** Reuse existing `createOrUpdateLatestNote` path; add no new Room table or migration.

## Phase 3: Focused Post-Capture UI

- [ ] **T018-007** Add focused context entry UI for new-capture `SilentWrapPill`/`UndoPill` actions.
- [ ] **T018-008** Add focused context entry UI for duplicate `AlreadySaved` actions targeting `existingEnvelopeId`.
- [ ] **T018-009** Preserve cancel, retry, save, and compact confirmation states.
- [ ] **T018-010** Keep full detail note entry as fallback only if focused surface cannot open or if user explicitly opens detail.

## Phase 4: Overlay/Service Wiring

- [ ] **T018-011** Wire `OverlayViewModel` context callbacks to open focused context instead of immediately launching detail.
- [ ] **T018-012** Wire `OrbitOverlayService` to host the focused context surface without direct Room access.
- [ ] **T018-013** Verify overlay window flags allow text input where needed and do not swallow unrelated app touches after dismissal.
- [ ] **T018-014** Preserve undo and duplicate recapture behavior while context entry is open.

## Phase 5: Downstream Verification

- [ ] **T018-015** Verify context saved through focused entry appears in capture detail.
- [ ] **T018-016** Verify Library local search finds note-only terms with `Context` evidence.
- [ ] **T018-017** Verify duplicate capture context updates the existing envelope and creates no duplicate.
- [ ] **T018-018** Verify no network/model path is invoked for context entry.

## Phase 6: Validation And Closeout

- [ ] **T018-019** Run focused overlay/context JVM tests.
- [ ] **T018-020** Run compile gates: `./gradlew :app:compileDebugKotlin :app:compileDebugAndroidTestKotlin`.
- [ ] **T018-021** Run full non-phone gate: `./gradlew :app:compileDebugKotlin :app:testDebugUnitTest :build-logic:lint:test :app:lintDebug :app:assembleDebug :app:compileDebugAndroidTestKotlin`.
- [ ] **T018-022** Run `git diff --check`.
- [ ] **T018-023** Update `quickstart.md`, roadmap, and handoff with validation evidence.
- [ ] **T018-024** Commit Spec 018 work without `dist/`, APK outputs, screenshots, `.gbrain-source`, or secrets.

## Deferred

- True pre-seal transparent Clarify activity.
- Voice context.
- Share-sheet capture context.
- LLM-generated titles from context.
- KG/profile extraction from context beyond existing downstream consumers.
