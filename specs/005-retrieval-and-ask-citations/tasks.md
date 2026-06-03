# Tasks: Atlas Memory Index + Cited Library

**Input**: `spec.md`, `plan.md`, `research.md`, `data-model.md`, `contracts/`
**Branch**: `feature/005-atlas-memory-index-search-20260530`
**Status**: MVP proven on S24 for Library search -> local capture detail; Ask Orbit exists as a limited local cited retrieval preview; semantic/vector Ask moves to Spec 005A.

## Format

- `[P]` means parallelizable after prerequisites are complete.
- `[US1]`, `[US2]`, `[US3]`, `[US4]` map to user stories in `spec.md`.
- Every task names exact files or commands.

## Phase 0: Speckit Lock

- [x] T005-001 Create branch `feature/005-atlas-memory-index-search-20260530`.
- [x] T005-002 Replace placeholder `specs/005-retrieval-and-ask-citations/spec.md` with Atlas Memory Index + Cited Library MVP.
- [x] T005-003 Add `specs/005-retrieval-and-ask-citations/plan.md`.
- [x] T005-004 Add `specs/005-retrieval-and-ask-citations/research.md`.
- [x] T005-005 Add `specs/005-retrieval-and-ask-citations/data-model.md`.
- [x] T005-006 Add contracts under `specs/005-retrieval-and-ask-citations/contracts/`.
- [x] T005-007 Add `specs/005-retrieval-and-ask-citations/quickstart.md`.

## Phase 1: Backend Memory Gateway Foundation

**Goal**: Create a server-side Atlas gateway with auth and payload safety.

- [x] T005-008 Create `supabase/functions/memory_gateway/package.json` with TypeScript, Vitest, Zod, jose, and mongodb dependencies.
- [x] T005-009 Create `supabase/functions/memory_gateway/tsconfig.json` and `vitest.config.ts`.
- [x] T005-010 [P] Create `supabase/functions/memory_gateway/types.ts` with request/response/entity types from `data-model.md`.
- [x] T005-011 [P] Create `supabase/functions/memory_gateway/lib/errors.ts` and `lib/response.ts` matching the wire shape in `contracts/memory-gateway-api.md`.
- [x] T005-012 Create `supabase/functions/memory_gateway/lib/auth.ts` by adapting the verified Supabase JWT pattern from `supabase/functions/llm_gateway/lib/auth.ts`.
- [x] T005-013 Create `supabase/functions/memory_gateway/lib/schemas.ts` with Zod validation, field caps, and recursive banned-field rejection.
- [x] T005-014 Create `supabase/functions/memory_gateway/lib/atlas.ts` with lazy `MongoClient`, env parsing, collection selection, and user-scoped helpers.
- [x] T005-015 Create `supabase/functions/memory_gateway/index.ts` router for `memory_upsert`, `memory_tombstone`, and `memory_search`; `memory_ask` may be included as a stretch endpoint only if it remains retrieval-grounded.
- [x] T005-016 Add `.env.local.example` for non-secret variable names only in `supabase/functions/memory_gateway/.env.local.example`.

## Phase 2: Backend Tests

**Goal**: Prove auth, isolation, and payload safety before Android integration.

- [x] T005-017 [P] Add auth tests in `supabase/functions/memory_gateway/test/auth.test.ts`.
- [x] T005-018 [P] Add banned-field schema tests in `supabase/functions/memory_gateway/test/payload_safety.test.ts`.
- [x] T005-019 [P] Add Atlas helper tests with mocked collection in `supabase/functions/memory_gateway/test/atlas.test.ts`.
- [x] T005-020 Add router tests in `supabase/functions/memory_gateway/test/router.test.ts`, with `memory_ask` covered only if implemented.
- [x] T005-021 Gate: run `cd supabase/functions/memory_gateway && npm install && npm run typecheck && npm run test:unit`.

## Phase 3: Android Compact Memory Payload

**Goal**: Build safe index records in `:ml` without network.

- [x] T005-022 Create domain DTOs under `app/src/main/java/com/orbit/app/memory/` for `MemoryIndexItem`, `MemoryEvidenceSnippet`, `MemorySearchResult`, and `AskOrbitAnswer`.
- [x] T005-023 Create `app/src/main/java/com/orbit/app/memory/MemoryPayloadCaps.kt` with caps and banned-key constants matching `data-model.md`.
- [x] T005-024 Create `app/src/main/java/com/orbit/app/memory/CompactMemoryIndexBuilder.kt` that maps local envelope/continuation/note/understanding sidecars into safe payloads.
- [x] T005-025 Add unit tests in `app/src/test/java/com/orbit/app/memory/CompactMemoryIndexBuilderTest.kt` proving caps and banned fields.
- [x] T005-026 Add local audit event helpers for memory operations in the existing audit/repository seam.

## Phase 4: Android Network Boundary

**Goal**: Send memory requests only through `:net`.

- [x] T005-027 Decide whether to extend `app/src/main/aidl/com/orbit/app/net/ipc/INetworkGateway.aidl` or add `IMemoryGateway.aidl`; document decision in `plan.md`.
- [x] T005-028 Add request/response parcel classes under `app/src/main/java/com/orbit/app/net/ipc/` if a new AIDL surface is used.
- [x] T005-029 Create `app/src/main/java/com/orbit/app/net/MemoryGatewayClient.kt` using `SafeOkHttpClient` and existing Supabase auth state.
- [x] T005-030 Wire the memory client in `app/src/main/java/com/orbit/app/net/NetworkGatewayImpl.kt` and `NetworkGatewayService.kt`.
- [x] T005-031 Add tests proving no MongoDB/Atlas dependency or URI appears under `app/src`.
- [x] T005-032 Gate: run `./gradlew :build-logic:lint:test :app:testDebugUnitTest :app:lintDebug`.

## Phase 5: User Story 3 - Compact Cloud Index Sync (P1)

**Goal**: Upsert/tombstone compact memory records from local captures.

**Independent Test**: Capture or seed records, run sync, inspect Atlas for compact fields only.

- [x] T005-033 [US3] Create sync coordinator under `app/src/main/java/com/orbit/app/memory/MemoryIndexSyncCoordinator.kt`.
- [x] T005-034 [US3] Add WorkManager job under `app/src/main/java/com/orbit/app/memory/MemoryIndexSyncWorker.kt`.
- [x] T005-035 [US3] Trigger sync after seal/understanding refresh without blocking capture.
- [x] T005-036 [US3] Tombstone Atlas records when local envelopes are deleted or invalidated.
- [x] T005-037 [US3] Add tests for disabled cloud indexing and Atlas unavailable behavior.

## Phase 6: User Story 1 - Library Search (P1)

**Goal**: Make Library usable as the second pillar.

**Independent Test**: Search indexed captures and open local envelope detail.

- [x] T005-038 [US1] Create `app/src/main/java/com/orbit/app/library/LibraryRepository.kt`.
- [x] T005-039 [US1] Create `app/src/main/java/com/orbit/app/library/LibraryViewModel.kt`.
- [x] T005-040 [US1] Create `app/src/main/java/com/orbit/app/library/LibraryUiState.kt`.
- [x] T005-041 [US1] Create Compose search UI under `app/src/main/java/com/orbit/app/library/ui/LibraryScreen.kt`.
- [x] T005-042 [US1] Add result row with citation metadata and View Capture action.
- [x] T005-043 [US1] Add tests for loading, results, empty, unavailable, and open-capture states.

## Phase 7: User Story 3 - Three-Pillar Entry Point (P1)

**Goal**: Make Library a real pillar without waiting for full Spec 019.

**Independent Test**: Open the app, move between Diary and Library, search, and return from a result to Library state.

- [x] T005-044 [US3] Add minimal Diary/Library/Orbit navigation shell or entry point in existing Diary activity files, scoped to demo needs.
- [x] T005-045 [US3] Route Library entry to `app/src/main/java/com/orbit/app/library/ui/LibraryScreen.kt`.
- [x] T005-046 [US3] Route Orbit entry to existing Active Intent cleanup surface until Spec 019 fully lands.
- [x] T005-047 [US3] Add UI tests for navigation state and Library result back behavior.
  - 2026-05-30 note: manual S24 screenshots verified Diary/Library/Orbit nav, Library search, and result -> detail. Automated UI coverage now compiles for bottom navigation selection and Library result -> capture open state.

## Phase 8: User Story 4 - Ask Orbit With Citations (P2 Stretch)

**Goal**: Retrieval-grounded Ask, not generic chat.

**Independent Test**: Ask scripted questions and verify answers cite source envelopes or refuse.

- [x] T005-048 [US4] Create `app/src/main/java/com/orbit/app/orbit/AskOrbitRepository.kt`.
- [x] T005-049 [US4] Create `app/src/main/java/com/orbit/app/orbit/AskOrbitViewModel.kt`.
- [x] T005-050 [US4] Create cited answer UI under `app/src/main/java/com/orbit/app/orbit/ui/AskOrbitPanel.kt`.
- [x] T005-051 [US4] Add insufficient-evidence fallback UI.
- [x] T005-052 [US4] Add tests proving citations are required for answered responses.
  - 2026-05-30 note: Orbit tab now includes a thin cited Ask panel. It accepts answers only with local-backed citations, falls back to local search when cloud records are missing, and exposes citation rows that open capture detail.

## Phase 9: User Story 5 - Audit Cloud Memory Operations (P2)

**Goal**: Make cloud memory operations locally auditable.

- [x] T005-053 [US5] Add audit row creation for implemented upsert/tombstone/search request outcomes. Ask uses the same helper when P2 lands.
- [x] T005-054 [US5] Add audit tests proving only digests/counts/outcomes are stored, not raw memory text.
- [x] T005-055 [US5] Verify audit UI can display the new event names without crashing.

## Phase 10: MVP Validation

- [x] T005-056 Run backend gates from `quickstart.md`.
- [x] T005-057 Run Android gates from `quickstart.md`.
- [x] T005-058 Seed or capture 20 demo envelopes and index them.
  - 2026-05-30: debug Settings seed creates 20 real local Room envelopes through `EnvelopeRepositoryService`. Backend seed also upserts 20 Atlas demo records; Android now filters cloud-only records and falls back to local Room search when Atlas sync lags.
- [x] T005-059 Inspect Atlas documents for banned fields.
  - 2026-05-30: `npm run demo:seed` reported `atlas_banned_field_count=0`.
- [x] T005-060 Demo Library search for at least 3 queries.
  - 2026-05-30: S24 screenshots proved `startup` Library results open local capture detail. Follow-up S24 testing proved `reschedule` and `rescheduling` both surface the expected captures, repeated dentist seed captures collapse to one row, and that row opens capture detail. Backend seed independently proved `startup event`, `flight receipt`, and `recipe` gateway searches.
- [x] T005-061 Optional stretch: demo Ask Orbit for at least 3 cited answers and 1 refusal.
  - 2026-06-02: `AskOrbitRepositoryTest.demoQuestionsReturnCitedAnswersAndUnsupportedQuestionRefuses` proves the current local-cited Ask preview answers controlled `startup event`, `flight receipt`, and `recipe` questions with source-envelope citations, and refuses an unsupported passport-number question. Focused Orbit unit tests, backend memory-gateway tests, and the full Android gate passed. A fresh physical S24 pass could not run because no ADB devices were attached.
  - 2026-06-03 correction: screenshot testing showed the token-only path can still rank weak keyword matches in real UI use. Treat this task as validation of the limited cited retrieval preview plus false-positive guardrails, not as proof of reliable semantic/vector/LLM Ask.
- [x] T005-062 Update `quickstart.md` with actual demo results and any Monday MVP cuts.

## Phase 11: MVP Duplicate Safety Layer

**Goal**: Prevent repeated captures from becoming repeated user work while avoiding a risky canonical-memory migration on this branch.

**Independent Test**: Recapturing the same coupon/order/event/message may still preserve local capture evidence, but Library, Ask, and Follow-ups show one representative memory/follow-up unless the capture is materially different.

- [x] T005-063 [US1] Preserve seal-time hard duplicate behavior for exact text and canonical URL (`Already saved`) in `EnvelopeRepositoryImpl`; do not replace this path with fuzzy logic.
- [x] T005-064 [US4] Add user-facing Follow-ups dedupe in `ActiveIntentUiState` by normalized clue/category/action, keeping the newest representative row.
- [x] T005-065 [US4] Add tests proving duplicate Follow-up content collapses to one visible row in `ActiveIntentUiStateTest` and `ActiveIntentCleanupPanelTest`.
- [x] T005-066 [US4] Add post-Basic-understanding duplicate protection in `BasicUnderstandingWriter`: duplicate normalized content may keep compact understanding sidecars but must not project another Active Intent.
- [x] T005-067 [US4] Add writer-level tests proving duplicate screenshot/OCR-derived understanding content does not create a new Active Intent.
- [x] T005-068 [US5] Add a compact audit/debug marker for post-understanding duplicate suppression without storing raw text.
- [x] T005-069 [US5] Add tests proving the duplicate-suppression audit marker stores only hashes/ids/outcome metadata.
- [x] T005-070 Update `quickstart.md`, `CODEX_HANDOFF.md`, and demo APK hash after duplicate-safety validation.
  - 2026-06-03 closeout cleanup: paused 005A implementation, returned to `feature/005-atlas-memory-index-search-20260530`, reran backend typecheck/unit tests and the full Android gate. Spec 005 remains a compact-index + Library + limited local cited Ask preview; semantic/vector Ask remains deferred to Spec 005A.

## Deferred Explicitly Out Of Scope

- [ ] T005-D01 Atlas Vector Search index after embedding field policy is locked.
- [ ] T005-D02 Full knowledge graph collections.
- [ ] T005-D03 3-pillar bottom navigation shell if handled by Spec 019.
- [ ] T005-D04 A2UI runtime.
- [ ] T005-D05 BYOM/local model manager.
- [ ] T005-D06 Multi-device conflict resolution and export/delete settings UI.
- [ ] T005-D07 Full canonical duplicate relationship model (`duplicateOf`, `seenCount`, `lastSeenAt`) for Spec 012 / resolution semantics.
- [ ] T005-D08 Semantic retrieval and grounded Ask enablement moved to Spec 005A: embedding provider/model/dimensions, Atlas Vector Search, hybrid retrieval, grounded answer/refusal thresholds, and local fallback.
