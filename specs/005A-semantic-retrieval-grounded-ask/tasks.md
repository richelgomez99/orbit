# Tasks: Semantic Retrieval And Grounded Ask

**Input**: `spec.md`, `plan.md`, `research.md`, `data-model.md`, `contracts/`
**Branch**: `feature/005a-semantic-retrieval-grounded-ask-20260603`
**Status**: Draft tasks - planning artifacts created. Implementation is paused until Spec 005 closeout is accepted/committed.

## Format

- `[P]` means parallelizable after prerequisites are complete.
- `[US1]`, `[US2]`, `[US3]`, `[US4]` map to user stories in `spec.md`.
- Every task names exact files or commands.

## Phase 0: Speckit Lock

- [x] T005A-001 Create `specs/005A-semantic-retrieval-grounded-ask/`.
- [x] T005A-002 Add `spec.md`, `plan.md`, `research.md`, `data-model.md`, `quickstart.md`, and contracts.
- [x] T005A-003 Add this `tasks.md`.
- [x] T005A-004 Create branch `feature/005a-semantic-retrieval-grounded-ask-20260603` as a stacked branch from the current dirty Spec 005 state.
- [x] T005A-005 Run stale-doc scan: `rg -n "Spec 006|005A|semantic retrieval|grounded Ask" docs CODEX_HANDOFF.md specs`.

## Phase 1: Backend Embedding Policy Foundation

**Goal**: Lock compact embedding input and provider metadata before any vector queries.

- [ ] T005A-006 Add embedding config constants in `supabase/functions/memory_gateway/lib/embeddingPolicy.ts`.
- [ ] T005A-007 Extend `supabase/functions/memory_gateway/types.ts` with embedding metadata fields and new request/response types.
- [ ] T005A-008 Extend `supabase/functions/memory_gateway/lib/schemas.ts` for embedding metadata, `memory_embed_stale`, `memory_semantic_search`, and `memory_grounded_ask`.
- [ ] T005A-009 Create `supabase/functions/memory_gateway/lib/embeddingInput.ts` that builds capped compact embedding input from allowed fields only.
- [ ] T005A-010 [P] Add `supabase/functions/memory_gateway/test/embedding_policy.test.ts` proving raw/banned fields cannot enter embedding input.
- [ ] T005A-011 [P] Add schema tests proving embedding model/dimension mismatch and banned provider fields are rejected.

## Phase 2: Embedding Provider And Atlas Vector Index

**Goal**: Generate vectors for compact records and prepare Atlas search.

- [ ] T005A-012 Create `supabase/functions/memory_gateway/lib/embeddings.ts` using backend-only `OPENAI_API_KEY` and `text-embedding-3-small`.
- [ ] T005A-013 Add provider tests with mocked embedding responses and dimension validation in `supabase/functions/memory_gateway/test/embeddings.test.ts`.
- [ ] T005A-014 Extend `supabase/functions/memory_gateway/lib/atlas.ts` to find stale embedding records and update embedding metadata.
- [ ] T005A-015 Create `supabase/functions/memory_gateway/scripts/create-vector-index.mjs`.
- [ ] T005A-016 Create `supabase/functions/memory_gateway/scripts/embed-stale.mjs`.
- [ ] T005A-017 Add package scripts `vector:index` and `memory:embed-stale` in `supabase/functions/memory_gateway/package.json`.
- [ ] T005A-018 Gate: run `cd supabase/functions/memory_gateway && npm run typecheck && npm run test:unit`.

## Phase 3: User Story 1 - Semantic Library Retrieval (P1)

**Goal**: Library can retrieve meaning/context, not only exact tokens.

**Independent Test**: Fixture search ranks expected local-backed captures for `qr code`, `startup event`, `flight receipt`, `recipe`, `reschedule`, and `rescheduling`.

- [ ] T005A-019 [US1] Create `supabase/functions/memory_gateway/lib/hybridSearch.ts`.
- [ ] T005A-020 [US1] Add Atlas `$vectorSearch` path with user/tombstone/filter constraints in `lib/atlas.ts`.
- [ ] T005A-021 [US1] Merge vector and lexical results deterministically with context-note and exact-phrase bonuses.
- [ ] T005A-022 [US1] Add `memory_semantic_search` dispatch path in `supabase/functions/memory_gateway/index.ts`.
- [ ] T005A-023 [US1] Add backend tests in `supabase/functions/memory_gateway/test/hybrid_search.test.ts`.
- [ ] T005A-024 [US1] Extend Kotlin DTOs in `app/src/main/java/com/orbit/app/memory/MemoryModels.kt` and `MemoryGatewayDtos.kt`.
- [ ] T005A-025 [US1] Update `app/src/main/java/com/orbit/app/library/LibraryRepository.kt` to prefer semantic search when enabled and fall back locally.
- [ ] T005A-026 [US1] Add/extend `app/src/test/java/com/orbit/app/library/*` tests for semantic result ordering and local-existence filtering.

## Phase 4: User Story 2 - Grounded Ask Ranking And Refusal (P1)

**Goal**: Ask answers only with strong cited evidence and refuses sensitive unsupported questions.

**Independent Test**: Screenshot-derived Ask fixtures pass.

- [ ] T005A-027 [US2] Create `supabase/functions/memory_gateway/lib/sensitiveQuestionPolicy.ts`.
- [ ] T005A-028 [US2] Create `supabase/functions/memory_gateway/lib/groundedAsk.ts`.
- [ ] T005A-029 [US2] Add `memory_grounded_ask` dispatch path in `supabase/functions/memory_gateway/index.ts`.
- [ ] T005A-030 [US2] Add backend tests in `supabase/functions/memory_gateway/test/grounded_ask.test.ts` for startup, flight, recipe, and passport refusal.
- [ ] T005A-031 [US2] Update `app/src/main/java/com/orbit/app/orbit/AskOrbitRepository.kt` to prefer grounded Ask when enabled and fall back locally.
- [ ] T005A-032 [US2] Update `app/src/test/java/com/orbit/app/orbit/AskOrbitRepositoryTest.kt` with semantic/grounded gateway fixtures.
- [ ] T005A-033 [US2] Ensure `AskOrbitPanel.kt` handles `sensitive_refusal`, `provider_unavailable`, and limitations without visual drift.

## Phase 5: User Story 3 - Compact Embedding Sync And Audit (P1)

**Goal**: Semantic indexing remains auditable and compact.

- [ ] T005A-034 [US3] Extend `app/src/main/java/com/orbit/app/memory/MemoryAudit.kt` with semantic search, embedding, fallback, and grounded Ask audit actions.
- [ ] T005A-035 [US3] Add audit tests in `app/src/test/java/com/orbit/app/memory/MemoryAuditTest.kt`.
- [ ] T005A-036 [US3] Ensure `CompactMemoryIndexBuilder.kt` includes context/note fields in compact text while preserving caps.
- [ ] T005A-037 [US3] Add tests proving compact embedding input excludes raw OCR/full screenshot/prompt/model response fields.
- [ ] T005A-038 [US3] Verify `rg -n "OPENAI_API_KEY|MONGODB_ATLAS_URI|mongodb\\+srv|MongoClient" app/src || true` returns no Android secret/client hits.

## Phase 6: User Story 4 - Retrieval Quality Evaluation Harness (P2)

**Goal**: Make retrieval quality measurable before action runtime.

- [ ] T005A-039 [US4] Add fixture JSONL under `supabase/functions/memory_gateway/test/fixtures/retrieval-eval.jsonl`.
- [ ] T005A-040 [US4] Create `supabase/functions/memory_gateway/scripts/eval-retrieval.mjs`.
- [ ] T005A-041 [US4] Add package script `eval:retrieval`.
- [ ] T005A-042 [US4] Add Android-side fixture tests for local fallback and dead-envelope filtering.
- [ ] T005A-043 [US4] Document fixture outcomes in `quickstart.md`.

## Phase 7: Validation And APK

- [ ] T005A-044 Run backend gate: `cd supabase/functions/memory_gateway && npm run typecheck && npm run test:unit`.
- [ ] T005A-045 Run retrieval eval: `cd supabase/functions/memory_gateway && npm run eval:retrieval`.
- [ ] T005A-046 Run focused Android gate: `./gradlew :app:testDebugUnitTest --tests "com.orbit.app.library.*" --tests "com.orbit.app.orbit.*" --tests "com.orbit.app.memory.*" :app:compileDebugKotlin`.
- [ ] T005A-047 Run lint/compile gate: `./gradlew :build-logic:lint:test :app:lintDebug :app:compileDebugAndroidTestKotlin`.
- [ ] T005A-048 Build APK: `./gradlew :app:assembleDebug`.
- [ ] T005A-049 Copy APK to `dist/orbit-mvp-debug-20260603-005a.apk` and record SHA-256 in `quickstart.md`.
- [ ] T005A-050 Manual S24 demo for Library semantic search and grounded Ask refusal.
- [ ] T005A-051 Update `CODEX_HANDOFF.md`, `docs/orbit-roadmap-queue-2026-06-02.md`, and this `tasks.md` with closeout status.

## Deferred Explicitly Out Of Scope

- [ ] T005A-D01 Local/BYOM embedding model manager. Spec 022 owns this.
- [ ] T005A-D02 Full LLM conversational Ask beyond grounded cited answers.
- [ ] T005A-D03 Knowledge graph entities/relationships. Spec 009 owns this.
- [ ] T005A-D04 Approval action runtime. Spec 006 owns this after 005A passes.
- [ ] T005A-D05 AppFunctions/Spark interop. Deferred until approval exists.
