# Tasks: Manual Compose And Capture Context

**Input**: `spec.md`, `plan.md`, `research.md`, `data-model.md`, `contracts/manual-compose-contract.md`

**Tests**: Required. This feature touches capture, note persistence, duplicate behavior, Binder process boundaries, and manual user-entered content.

## Phase 1: Setup And Artifact Lock

- [x] **T011-001** Create fresh Spec Kit artifacts in `specs/011-manual-compose/`.
- [x] **T011-002** Update `docs/orbit-roadmap-queue-2026-06-02.md` and `CODEX_HANDOFF.md` to mark Spec 011 active.

## Phase 2: Capture Context Foundation

- [x] **T011-003** [P] Add/extend overlay UI tests for post-capture context affordance in `app/src/test/java/com/orbit/app/overlay/`.
- [x] **T011-004** Add `Add context` action to new-capture post-save UI without removing undo behavior.
- [x] **T011-005** Route new-capture context action from `OverlayViewModel`/`OrbitOverlayService` to `EnvelopeDetailActivity` with `startNote = true`.
- [x] **T011-006** Verify duplicate-capture context still targets the existing envelope and does not create a new one.

## Phase 3: Manual Compose Repository Seam

- [x] **T011-007** [P] Add manual compose result/domain models under `app/src/main/java/com/orbit/app/diary/`.
- [x] **T011-008** Add `DiaryRepository`/`BinderDiaryRepository` method for manual text compose using existing seal/Binder path.
- [x] **T011-009** Add tests proving blank manual body blocks before repository calls.
- [x] **T011-010** Add tests proving save body first, then attach optional context note.
- [x] **T011-011** Add duplicate manual compose test using existing `SealResultParcel.alreadySaved` behavior.

## Phase 4: Manual Compose UI

- [ ] **T011-012** [P] Add `ManualComposeViewModel` with body/context/day state and save result states.
- [ ] **T011-013** [P] Add `ManualComposeScreen` using Quiet Almanac controls.
- [ ] **T011-014** Wire Diary entry point to open manual compose for the current/selected day.
- [ ] **T011-015** On successful save, open or highlight the saved capture through existing detail/diary paths.
- [ ] **T011-016** Add ViewModel/UI tests for blank body, successful save, context save, duplicate result, and failure copy.

## Phase 5: Downstream Context Verification

- [ ] **T011-017** Verify Library local search still matches note-only terms and cites `Context`.
- [ ] **T011-018** Verify hydration context packet includes capped latest note and excludes banned raw fields.
- [ ] **T011-019** Verify compact memory index builder includes note context through existing snapshot path.
- [ ] **T011-020** Add or update tests only where existing coverage does not prove these paths.

## Phase 6: Validation And Closeout

- [ ] **T011-021** Run focused overlay/diary/manual compose JVM tests.
- [x] **T011-022** Run compile gates: `./gradlew :app:compileDebugKotlin :app:compileDebugAndroidTestKotlin`.
- [ ] **T011-023** Run full non-phone gate: `./gradlew :app:compileDebugKotlin :app:testDebugUnitTest :build-logic:lint:test :app:lintDebug :app:assembleDebug :app:compileDebugAndroidTestKotlin`.
- [ ] **T011-024** Run `git diff --check`.
- [ ] **T011-025** Update `quickstart.md`, roadmap, and handoff with validation evidence.
- [ ] **T011-026** Commit Spec 011 work without `dist/`, APK outputs, screenshots, `.gbrain-source`, or secrets.

## Deferred

- Transparent pre-seal Clarify activity if post-save context path proves insufficient.
- Voice compose.
- Share sheet compose.
- Rich media/manual attachment compose.
- A2UI/generative UI compose.
- BYOM/local model inference.
