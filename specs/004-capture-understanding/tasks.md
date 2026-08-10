# Tasks: Capture Understanding

**Input**: Design documents from `/Users/richelgomez/dev/capsule-app-spec-004/specs/004-capture-understanding/`
**Prerequisites**: `plan.md`, `spec.md`, `research.md`, `data-model.md`, `quickstart.md`, `contracts/`
**Branch**: `004-capture-understanding`
**Tests**: Required by the feature spec and quickstart. Write story tests first and verify they fail before implementing each story.

**Hard Stop Signs**: Do not implement Ask Orbit, retrieval ranking/citations, KG backend/tables, generic browser automation, generic memory inspector, approval/action runtime, action suggestions, agent coordinator, multi-agent orchestration, cloud-controls management screens, network clients outside `com.capsule.app.net.*`, raw or large Binder payloads, or cross-process raw HTML/screenshots/full text/embeddings/full evidence bundles.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel with other tasks in the same listed parallel group because it touches different files and has no dependency on an incomplete task in that group.
- **[Story]**: User-story label for story phases only.
- Every task names the exact primary file path to create or modify.

---

## Phase 1: Setup (Shared Test And Guard Infrastructure)

**Purpose**: Establish deterministic fixtures and architectural guardrails before implementation starts.

- [ ] T001 Create the 100-case capture understanding fixture manifest covering static URLs, JavaScript-heavy pages, YouTube variants, social/media links, screenshot-only captures, receipts/events, documents, blocked/private/paywalled sources, duplicates, category-only captures, and adversarial low-evidence cases in `app/src/test/resources/capture-understanding/evaluation-fixtures.json`
- [ ] T002 [P] Create the fixture README with retention/deletion rules for synthetic and dogfood content in `app/src/test/resources/capture-understanding/README.md`
- [ ] T003 [P] Create hard-stop architecture guard tests that fail on Ask Orbit, KG backend, memory inspector, action runtime, agent coordinator, or generic browser automation classes in `app/src/test/java/com/capsule/app/architecture/CaptureUnderstandingScopeGuardTest.kt`
- [ ] T004 [P] Create gateway-boundary architecture guard tests that fail on `OkHttpClient`, `HttpURLConnection`, Supabase clients, Anthropic/OpenAI clients, or provider SDK construction outside `app/src/main/java/com/capsule/app/net/` in `app/src/test/java/com/capsule/app/architecture/NetworkBoundaryGuardTest.kt`
- [ ] T005 [P] Create IPC payload architecture guard tests that fail on raw HTML, screenshots, full page text, OCR bulk text, embeddings, vectors, raw prompts, model responses, or full evidence bundles in `app/src/main/aidl/` and `app/src/main/java/com/capsule/app/data/ipc/` in `app/src/test/java/com/capsule/app/architecture/CompactIpcPayloadGuardTest.kt`

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Add the shared domain types, Room schema, DAO surface, migrations, and repository seams required by all stories.

**CRITICAL**: No user story implementation should begin until this phase is complete.

- [ ] T006 Create capture understanding enums and value objects for source glyph kind, canonical URL role, evidence kind/status/retention, acquisition depth/method, depth policy, job status, limitation code, feedback kind, invalidation reason, trace outcome, and retry eligibility in `app/src/main/java/com/capsule/app/understanding/CaptureUnderstandingTypes.kt`
- [ ] T007 [P] Create `SourceIdentityEntity` with versioning, provider/app/category labels, evidence IDs, limitation JSON, resolver version, and lifecycle fields in `app/src/main/java/com/capsule/app/data/entity/SourceIdentityEntity.kt`
- [ ] T008 [P] Create `CanonicalUrlEntity` with primary/supporting role, normalized URL, canonical hash, provider family, duplicate eligibility, and lifecycle fields in `app/src/main/java/com/capsule/app/data/entity/CanonicalUrlEntity.kt`
- [ ] T009 [P] Create `EvidenceBundleEntity` with evidence kind, source reference, acquisition depth/method, confidence, content hash/reference, retention class, status, limitations, and lifecycle fields in `app/src/main/java/com/capsule/app/data/entity/EvidenceBundleEntity.kt`
- [ ] T010 [P] Create `UnderstandingJobEntity` with requested/effective depth, policy decision, retry eligibility, attempt count, content-free trace IDs, failure code, user-visible reason, and lifecycle fields in `app/src/main/java/com/capsule/app/data/entity/UnderstandingJobEntity.kt`
- [ ] T011 [P] Create `CaptureUnderstandingEntity` with status, title, compact summary, evidence IDs, source identity ID, confidence, limitations, depth used, extractor provenance, and version lifecycle fields in `app/src/main/java/com/capsule/app/data/entity/CaptureUnderstandingEntity.kt`
- [ ] T012 [P] Create `CorrectionFeedbackEntity` with typed feedback, target reference, optional local note, and resolved-by understanding link in `app/src/main/java/com/capsule/app/data/entity/CorrectionFeedbackEntity.kt`
- [ ] T013 [P] Create `DeletionInvalidationEntity` with reason, affected record references, downstream eligibility, audit trace ID, cloud receipt status, and timestamp in `app/src/main/java/com/capsule/app/data/entity/DeletionInvalidationEntity.kt`
- [ ] T014A [P] Create `UnderstandingDepthPolicyOverrideEntity` for per-capture overrides and domain/source suppression records in `app/src/main/java/com/capsule/app/data/entity/UnderstandingDepthPolicyOverrideEntity.kt`
- [ ] T017 Create failing migration tests covering V7 to V8 table creation, indexes, foreign keys, active primary canonical URL uniqueness, policy override persistence, and deleted-capture ineligibility before implementing the migration in `app/src/androidTest/java/com/capsule/app/data/OrbitDatabaseMigrationV7toV8CaptureUnderstandingTest.kt`
- [ ] T019 Create failing DAO tests for current-version selection, invalidated evidence exclusion, paged evidence summaries, job lifecycle updates, per-capture override lookup, and domain suppression lookup before implementing DAO behavior in `app/src/androidTest/java/com/capsule/app/data/CaptureUnderstandingDaoTest.kt`
- [ ] T014 Create `CaptureUnderstandingDao` with insert, current-version selection, paged evidence summary, policy lookup, job status, feedback, suppression, and invalidation queries in `app/src/main/java/com/capsule/app/data/dao/CaptureUnderstandingDao.kt`
- [ ] T015 Register the new entities and DAO, increment the database version from 7 to 8, and add `MIGRATION_7_8` to the database builder in `app/src/main/java/com/capsule/app/data/OrbitDatabase.kt`
- [ ] T016 Add forward-only `MIGRATION_7_8` for source identities, canonical URLs, evidence bundles, understanding depth policy overrides, understanding jobs, capture understandings, correction feedback, deletion invalidation records, required indexes, and foreign keys in `app/src/main/java/com/capsule/app/data/OrbitMigrations.kt`
- [ ] T018 Export the Room v8 schema after migration tests pass in `app/schemas/com.capsule.app.data.OrbitDatabase/8.json`
- [ ] T020 Add repository/storage backend methods for compact understanding reads, source/evidence writes, job writes, feedback writes, invalidation writes, and current-version selection in `app/src/main/java/com/capsule/app/data/EnvelopeStorageBackend.kt`
- [ ] T021 Wire repository-level capture understanding methods without changing capture behavior yet in `app/src/main/java/com/capsule/app/data/EnvelopeRepositoryImpl.kt`

**Checkpoint**: Foundation ready. All user stories can now be implemented against the same schema and repository seams.

---

## Phase 3: User Story 1 - See What Orbit Knows About A Capture (Priority: P1) MVP

**Goal**: Capture detail shows source identity, canonical URL/domain, evidence used, compact understanding, confidence, depth, and explicit limitations without requiring Ask Orbit or retrieval.

**Independent Test**: Save a public article URL, metadata-only URL, and screenshot-only OCR capture, open detail, and verify source/evidence/summary/limitations are visible and evidence-backed.

### Tests for User Story 1

- [ ] T022 [P] [US1] Add capture-detail Compose tests for readable public text, metadata-only URL, screenshot-only OCR, and limited/failed states in `app/src/androidTest/java/com/capsule/app/diary/EnvelopeDetailUnderstandingScreenTest.kt`
- [ ] T023 [P] [US1] Add view model tests for compact understanding readiness, partial/limited/failed status, confidence, depth, and limitation copy in `app/src/test/java/com/capsule/app/diary/EnvelopeDetailUnderstandingViewModelTest.kt`
- [ ] T024 [P] [US1] Add evidence claim rule tests proving metadata-only, blocked/private/paywalled, visual-only, failed, and suppressed evidence cannot support full-content claims in `app/src/test/java/com/capsule/app/understanding/EvidenceClaimRulesTest.kt`
- [ ] T025 [P] [US1] Add Parcelable round-trip and compact Binder boundary tests for the capture understanding summary parcel, including max string caps, evidence page-size caps, token caps, and a max-filled marshalled size assertion under 32 KiB in `app/src/test/java/com/capsule/app/data/ipc/CaptureUnderstandingSummaryParcelTest.kt`

### Implementation for User Story 1

- [ ] T026 [US1] Implement evidence claim validation and limitation mapping in `app/src/main/java/com/capsule/app/understanding/EvidenceClaimPolicy.kt`
- [ ] T027 [US1] Create `CaptureUnderstandingSummaryParcel` with IDs, compact labels, status, confidence band, limitation codes, depth, retry/correction flags, counts, optional evidence page token only, and enforced caps from `compact-ipc-payload-contract.md` in `app/src/main/java/com/capsule/app/data/ipc/CaptureUnderstandingSummaryParcel.kt`
- [ ] T028 [US1] Extend `EnvelopeViewParcel` with compact capture understanding summary references while preserving existing parcel ordering compatibility in `app/src/main/java/com/capsule/app/data/ipc/EnvelopeViewParcel.kt`
- [ ] T029 [US1] Update `EnvelopeRepositoryImpl.getEnvelope` mapping to prefer current `CaptureUnderstandingEntity` over legacy hydration fields when available and fall back to existing hydration data otherwise in `app/src/main/java/com/capsule/app/data/EnvelopeRepositoryImpl.kt`
- [ ] T030 [US1] Extend the detail UI state with source identity, evidence summary count, compact summary, depth used, confidence, limitations, and retry/correction availability in `app/src/main/java/com/capsule/app/diary/EnvelopeDetailUiState.kt`
- [ ] T031 [US1] Load compact understanding state and evidence summary metadata in `app/src/main/java/com/capsule/app/diary/EnvelopeDetailViewModel.kt`
- [ ] T032 [US1] Render the source/evidence/summary/limitations insight section and keep Ask/retrieval/KG/action entry points absent in `app/src/main/java/com/capsule/app/diary/ui/EnvelopeDetailScreen.kt`

**Checkpoint**: User Story 1 is independently testable as the MVP trust surface.

---

## Phase 4: User Story 2 - Control How Hard Orbit Tries To Understand (Priority: P1)

**Goal**: Basic, Smart, Deep, and per-capture overrides produce recorded policy decisions, allowed evidence paths, bounded cloud trace attempts, and visible limitations.

**Independent Test**: Change global depth and repeat the same URL capture under Basic, Smart, and Deep; verify selected depth, allowed evidence path, skipped work, trace metadata, and limitations.

### Tests for User Story 2

- [ ] T033 [P] [US2] Add depth policy evaluator tests for Basic, Smart, Deep, `SUMMARIZE_ONLY_SAVED`, screenshot-only, domain suppression, budget block, sensitive-source block, and generic browser automation suppression in `app/src/test/java/com/capsule/app/understanding/UnderstandingDepthPolicyTest.kt`
- [ ] T034 [P] [US2] Add cloud trace no-content contract tests proving trace defaults include IDs, bounded policy/outcome fields, latency/cost/failure codes, and exclude raw prompts/page text/HTML/screenshots/embeddings/full evidence bundles in `app/src/test/java/com/capsule/app/understanding/CloudEnrichmentTraceContractTest.kt`
- [ ] T035 [P] [US2] Add preferences tests for global Basic/Smart/Deep depth, local-mode fallback, cloud enabled/disabled, and per-capture override persistence in `app/src/test/java/com/capsule/app/settings/PrivacyPreferencesUnderstandingDepthTest.kt`

### Implementation for User Story 2

- [ ] T036 [US2] Implement `UnderstandingDepthPolicyEvaluator` with user depth, local-mode, budget, sensitivity, domain suppression, source availability, and auditability decisions in `app/src/main/java/com/capsule/app/understanding/UnderstandingDepthPolicyEvaluator.kt`
- [ ] T037 [US2] Extend `PrivacyPreferences` with global understanding depth, cloud enrichment enablement, and local fallback settings in `app/src/main/java/com/capsule/app/settings/PrivacyPreferences.kt`
- [ ] T038 [US2] Add capture override persistence and effective-policy lookup methods to `CaptureUnderstandingDao` in `app/src/main/java/com/capsule/app/data/dao/CaptureUnderstandingDao.kt`
- [ ] T039 [US2] Add settings UI controls for Basic, Smart, and Deep without adding a cloud-controls management screen in `app/src/main/java/com/capsule/app/settings/SettingsScreen.kt`
- [ ] T040 [US2] Add per-capture controls for get more context, summarize only what I saved, refresh source evidence, screenshot-only, and suppress future domain fetching in `app/src/main/java/com/capsule/app/diary/ui/EnvelopeDetailScreen.kt`
- [ ] T041 [US2] Record content-free acquisition, suppression, and cloud/model attempt traces through bounded audit metadata in `app/src/main/java/com/capsule/app/audit/AuditLogWriter.kt`
- [ ] T042 [US2] Route any Smart/Deep cloud/model request through the existing `INetworkGateway.callLlmGateway` boundary and compact request IDs only in `app/src/main/java/com/capsule/app/understanding/CloudEnrichmentGateway.kt`

**Checkpoint**: User Story 2 depth and trace behavior is independently testable without adding deferred cloud-controls screens.

---

## Phase 5: User Story 3 - Trust Source Identity And Duplicate Handling (Priority: P1)

**Goal**: Provider URL, foreground app label, generic category, canonical URL, and duplicate handling produce trustworthy source identity without overclaiming.

**Independent Test**: Save YouTube URLs from multiple app contexts and a category-only video capture; provider-backed captures show YouTube plus origin context, category-only captures stay generic, and duplicate canonical URLs use Already Saved.

### Tests for User Story 3

- [ ] T043 [P] [US3] Add source identity hierarchy tests for provider URL over app label, app label over category, category-only generic identity, unknown fallback, provider plus origin-app copy, and branded glyph guardrails in `app/src/test/java/com/capsule/app/understanding/SourceIdentityResolverTest.kt`
- [ ] T044 [P] [US3] Add canonical URL and evidence behavior tests for tracking parameters, fragments, host casing, mobile subdomains, primary/supporting URL roles, YouTube-family variants, and scheme-less links in `app/src/test/java/com/capsule/app/understanding/CanonicalUrlEvidenceTest.kt`
- [ ] T045 [P] [US3] Extend duplicate contract tests for pre-hydration duplicates, post-hydration duplicates, deleted-capture exclusion, and `Already saved` results tied to canonical URL records in `app/src/androidTest/java/com/capsule/app/data/UrlHashDedupeContractTest.kt`

### Implementation for User Story 3

- [ ] T046 [US3] Implement source identity resolution using provider URL evidence, foreground app label evidence, generic category, and unknown fallback in `app/src/main/java/com/capsule/app/understanding/SourceIdentityResolver.kt`
- [ ] T047 [US3] Extend YouTube-family recognition for `youtu.be`, `youtube.com` subdomains, `youtube-nocookie.com`, Shorts/live/watch/embed paths, and scheme-less links in `app/src/main/java/com/capsule/app/net/ProviderMetadataResolver.kt`
- [ ] T048 [US3] Implement canonical URL evidence building with primary/supporting URL roles and normalization version compatibility with `CanonicalUrlHasher` in `app/src/main/java/com/capsule/app/understanding/CanonicalUrlEvidenceBuilder.kt`
- [ ] T049 [US3] Preserve foreground app label and durable category as source evidence inputs during state collection in `app/src/main/java/com/capsule/app/capture/StateSnapshotCollector.kt`
- [ ] T050 [US3] Wire seal-time canonical URL, evidence bundle, and source identity creation into duplicate lookup without replacing existing `Already saved` behavior in `app/src/main/java/com/capsule/app/data/EnvelopeRepositoryImpl.kt`
- [ ] T051 [US3] Update URL hydration to write readable public text, metadata-only, failure, and canonical URL evidence beside existing continuation results in `app/src/main/java/com/capsule/app/continuation/UrlHydrateWorker.kt`

**Checkpoint**: User Story 3 is independently testable for source identity and duplicates.

---

## Phase 6: User Story 5 - Delete Or Invalidate Derived Understanding (Priority: P1)

**Goal**: Deleting or invalidating a capture makes source identities, evidence, understanding jobs/results, and future downstream references ineligible while retaining content-free lifecycle audit metadata.

**Independent Test**: Save a capture, let source/evidence/understanding records exist, delete it, and verify derived records are no longer displayed or eligible within 30 seconds.

### Tests for User Story 5

- [ ] T052 [P] [US5] Add deletion/invalidation DAO tests for source identity, canonical URL eligibility, evidence, jobs, understandings, feedback versions, and downstream eligibility in `app/src/androidTest/java/com/capsule/app/data/DeletionInvalidationDaoTest.kt`
- [ ] T053 [P] [US5] Add repository lifecycle tests proving delete invalidates derived data, deleted captures are ignored for duplicate matching, and content-free audit events remain in `app/src/test/java/com/capsule/app/data/CaptureUnderstandingDeletionLifecycleTest.kt`
- [ ] T054 [P] [US5] Add cloud receipt reconciliation tests proving pending or failed cloud receipts never re-enable local derived data in `app/src/test/java/com/capsule/app/understanding/CloudDeletionReceiptTest.kt`

### Implementation for User Story 5

- [ ] T055 [US5] Implement deletion/invalidation cascade planning for source identities, canonical URLs, evidence, jobs, understandings, correction versions, and future downstream references in `app/src/main/java/com/capsule/app/understanding/DeletionInvalidationCascade.kt`
- [ ] T056 [US5] Add DAO invalidation queries for affected records, downstream eligibility, canonical URL duplicate eligibility, and current-version exclusion in `app/src/main/java/com/capsule/app/data/dao/CaptureUnderstandingDao.kt`
- [ ] T057 [US5] Update capture delete/archive/restore paths to call the invalidation cascade and keep active duplicate keys consistent in `app/src/main/java/com/capsule/app/data/EnvelopeRepositoryImpl.kt`
- [ ] T058 [US5] Update `IntentEnvelopeDao` duplicate and lifecycle queries to coordinate with derived canonical URL eligibility and deleted-capture exclusion in `app/src/main/java/com/capsule/app/data/dao/IntentEnvelopeDao.kt`
- [ ] T059 [US5] Add content-free deletion and invalidation audit events with affected record IDs/types only in `app/src/main/java/com/capsule/app/audit/AuditLogWriter.kt`
- [ ] T060 [US5] Hide invalidated source/evidence/understanding data from capture detail while showing only lifecycle-safe limitation state in `app/src/main/java/com/capsule/app/diary/EnvelopeDetailViewModel.kt`

**Checkpoint**: User Story 5 lifecycle safety is independently testable before future Ask/retrieval/memory/KG/action/agent features consume derived data.

---

## Phase 7: User Story 4 - Correct Bad Understanding (Priority: P2)

**Goal**: User corrections for wrong source, wrong summary, relevance, context breadth, and domain/source suppression are recorded, versioned, and applied to refresh behavior.

**Independent Test**: Save a capture, mark source wrong, refresh understanding, and verify feedback is recorded, a corrected version is selected, and audit history is preserved.

### Tests for User Story 4

- [ ] T061 [P] [US4] Add correction feedback tests for wrong source, wrong summary, not relevant, too much context, suppress domain, suppress source, version linking, and local-only notes in `app/src/test/java/com/capsule/app/understanding/CorrectionFeedbackRecorderTest.kt`
- [ ] T062 [P] [US4] Add capture detail correction UI tests for feedback controls, recorded state, refresh trigger, and history-preserving updated summary/source display in `app/src/androidTest/java/com/capsule/app/diary/EnvelopeDetailCorrectionUiTest.kt`

### Implementation for User Story 4

- [ ] T063 [US4] Implement typed correction feedback recording and suppression target normalization in `app/src/main/java/com/capsule/app/understanding/CorrectionFeedbackRecorder.kt`
- [ ] T064 [US4] Add correction feedback insertion, resolution, domain suppression, source suppression, and version-linking queries in `app/src/main/java/com/capsule/app/data/dao/CaptureUnderstandingDao.kt`
- [ ] T065 [US4] Wire correction actions from the detail view model to feedback recording and refresh scheduling in `app/src/main/java/com/capsule/app/diary/EnvelopeDetailViewModel.kt`
- [ ] T066 [US4] Render wrong source, wrong summary, not relevant, too much context, suppress domain, and refresh controls without adding actions/memory/agent coordinator features in `app/src/main/java/com/capsule/app/diary/ui/EnvelopeDetailScreen.kt`
- [ ] T067 [US4] Link refreshed source identity and understanding versions back to correction feedback without mutating historical versions in place in `app/src/main/java/com/capsule/app/understanding/CaptureUnderstandingVersionSelector.kt`

**Checkpoint**: User Story 4 correction and versioning behavior is independently testable.

---

## Phase 8: User Story 6 - Stay Useful When Cloud Or Deep Extraction Is Unavailable (Priority: P2)

**Goal**: Captures save successfully and show local/basic or limited understanding when cloud, network, budget, policy, or deep extraction is unavailable.

**Independent Test**: Disable cloud or simulate network failure, save a public URL and screenshot-only capture, and verify save success, local evidence, bounded job status, retry eligibility, and visible limitations.

### Tests for User Story 6

- [ ] T068 [P] [US6] Add WorkManager tests for pending, running, ready, limited, failed, canceled, invalidated, retryable, not-retryable, and user-action-required understanding jobs in `app/src/androidTest/java/com/capsule/app/continuation/CaptureUnderstandingWorkerTest.kt`
- [ ] T069 [P] [US6] Add local fallback tests for cloud disabled, network unavailable, budget exhausted, domain suppressed, sensitive screenshot, blocked/private source, and failed extractor states in `app/src/test/java/com/capsule/app/understanding/LocalFallbackUnderstandingTest.kt`
- [ ] T070 [P] [US6] Add save-success regression tests proving worker, gateway, extraction, and policy failures never fail the capture seal path in `app/src/androidTest/java/com/capsule/app/data/CaptureUnderstandingSaveReliabilityTest.kt`

### Implementation for User Story 6

- [ ] T071 [US6] Implement `LocalEvidenceExtractor` for saved text, saved URL, URL metadata, OCR text references, screenshot references, and local limitation evidence in `app/src/main/java/com/capsule/app/understanding/LocalEvidenceExtractor.kt`
- [ ] T072 [US6] Implement `CaptureUnderstandingWorker` with bounded retries, visible terminal limited/failed states, local fallback, and compact trace IDs in `app/src/main/java/com/capsule/app/continuation/CaptureUnderstandingWorker.kt`
- [ ] T073 [US6] Extend `ContinuationEngine` to enqueue capture understanding jobs after seal/hydration using existing constraints and tags without adding autonomous agents in `app/src/main/java/com/capsule/app/continuation/ContinuationEngine.kt`
- [ ] T074 [US6] Ensure `EnvelopeRepositoryImpl.seal` returns save success when understanding scheduling, local extraction, policy evaluation, network, or cloud enrichment fails in `app/src/main/java/com/capsule/app/data/EnvelopeRepositoryImpl.kt`
- [ ] T075 [US6] Map gateway, model, network, timeout, policy, sensitivity, budget, domain suppression, malformed response, and extractor failures to bounded job states and limitation codes in `app/src/main/java/com/capsule/app/understanding/UnderstandingFailureMapper.kt`
- [ ] T076 [US6] Add retry and refresh eligibility wiring for limited/failed understanding states in `app/src/main/java/com/capsule/app/diary/EnvelopeDetailViewModel.kt`

**Checkpoint**: User Story 6 fallback behavior is independently testable and save reliability remains intact.

---

## Phase 9: Polish & Cross-Cutting Validation

**Purpose**: Evaluate coverage, run full validation gates, and document remaining dogfood evidence without expanding feature scope.

- [ ] T077 [P] Add the 100-case evaluation runner and success-criteria assertions for source identity accuracy, YouTube coverage, readable public-text summary within 30 seconds for at least 90% of eligible fixtures, limitation correctness, policy compliance, duplicate handling, deletion invalidation, local fallback, and architecture guards in `app/src/test/java/com/capsule/app/understanding/CaptureUnderstandingEvaluationTest.kt`
- [ ] T078 [P] Create the physical dogfood QA checklist and scoring form for public article, metadata-only URL, JavaScript-heavy page, YouTube variants, provider plus origin app, category-only capture, duplicate URL, screenshot-only OCR, sensitive screenshot, blocked/private/paywalled source, offline/cloud-disabled, correction, domain suppression, and deletion cases; include an 8-of-10 tester pass threshold for identifying evidence used and where Orbit stopped in `docs/capture-understanding-dogfood-qa.md`
- [ ] T078A Execute the 10-tester dogfood review using `docs/capture-understanding-dogfood-qa.md`, record anonymized scores/evidence in the same document, and fail the release gate unless at least 8 of 10 testers correctly identify evidence used and where Orbit stopped in `docs/capture-understanding-dogfood-qa.md`
- [ ] T079 Run Android compile, unit tests, lint, and connected tests for the implemented feature using `./gradlew compileDebugKotlin testDebugUnitTest lintDebug connectedDebugAndroidTest` and review reports under `app/build/reports/`
- [ ] T080 Run Room migration and schema validation for V7 to V8 using `./gradlew connectedDebugAndroidTest --tests '*OrbitDatabaseMigrationV7toV8CaptureUnderstandingTest*'` and verify exported schema `app/schemas/com.capsule.app.data.OrbitDatabase/8.json`
- [ ] T081 Run architecture validation greps for network clients outside `app/src/main/java/com/capsule/app/net/` and forbidden raw/large IPC payload fields in `app/src/main/aidl/` plus `app/src/main/java/com/capsule/app/data/ipc/`
- [ ] T082 Run gateway TypeScript validation because cloud trace contracts are touched using `npm run typecheck` and `npm run test:unit` from `supabase/functions/llm_gateway/package.json`
- [ ] T083 Scan for unresolved clarification and template markers in `specs/004-capture-understanding/tasks.md`, `specs/004-capture-understanding/spec.md`, `specs/004-capture-understanding/plan.md`, `specs/004-capture-understanding/research.md`, `specs/004-capture-understanding/data-model.md`, `specs/004-capture-understanding/quickstart.md`, and `specs/004-capture-understanding/contracts/`

---

## Dependencies & Execution Order

### Phase Dependencies

- **Phase 1 Setup**: No dependencies; T002-T005 can run in parallel after T001 is understood.
- **Phase 2 Foundational**: Depends on Phase 1; blocks all user stories.
- **Phase 3 US1**: Depends on Phase 2; MVP detail insight surface.
- **Phase 4 US2**: Depends on Phase 2 and integrates with US1 detail controls where UI tasks touch the same files.
- **Phase 5 US3**: Depends on Phase 2; can run in parallel with US2 except for `EnvelopeRepositoryImpl.kt` coordination.
- **Phase 6 US5**: Depends on Phase 2 and should land before any future downstream features consume understanding data.
- **Phase 7 US4**: Depends on US1, US2, US3, and US5 because correction refresh must preserve display, policy, source identity, and invalidation history.
- **Phase 8 US6**: Depends on US1, US2, and US3 because fallback jobs need display, policy, and source/evidence creation surfaces.
- **Phase 9 Polish**: Depends on all desired stories for the release slice.

### User Story Dependencies

- **US1 (P1)**: MVP after foundation; no dependency on US2/US3/US5 for its fallback display path.
- **US2 (P1)**: After foundation; depends on US1 only for the visible per-capture controls in `EnvelopeDetailScreen.kt`.
- **US3 (P1)**: After foundation; no dependency on US1/US2 for resolver and duplicate behavior.
- **US5 (P1)**: After foundation; no dependency on US1/US2/US3, but should be validated before P2 stories and any downstream feature work.
- **US4 (P2)**: Depends on US1 display, US2 policy overrides, US3 source identity, and US5 invalidation/versioning.
- **US6 (P2)**: Depends on US1 display, US2 policy decisions, and US3 source/evidence creation.

### Within Each User Story

- Tests first; verify they fail before implementation.
- Domain/resolver/policy code before repository wiring.
- Repository/storage before Binder/UI mapping.
- Worker/job scheduling before retry UI.
- Story checkpoint validation before moving to the next priority when working sequentially.

---

## Parallel Execution Examples

### Setup Parallel Group

```text
T002 Create fixture README in app/src/test/resources/capture-understanding/README.md
T003 Create scope guard test in app/src/test/java/com/capsule/app/architecture/CaptureUnderstandingScopeGuardTest.kt
T004 Create network boundary guard test in app/src/test/java/com/capsule/app/architecture/NetworkBoundaryGuardTest.kt
T005 Create compact IPC guard test in app/src/test/java/com/capsule/app/architecture/CompactIpcPayloadGuardTest.kt
```

### Foundational Entity Parallel Group

```text
T007 Create SourceIdentityEntity in app/src/main/java/com/capsule/app/data/entity/SourceIdentityEntity.kt
T008 Create CanonicalUrlEntity in app/src/main/java/com/capsule/app/data/entity/CanonicalUrlEntity.kt
T009 Create EvidenceBundleEntity in app/src/main/java/com/capsule/app/data/entity/EvidenceBundleEntity.kt
T010 Create UnderstandingJobEntity in app/src/main/java/com/capsule/app/data/entity/UnderstandingJobEntity.kt
T011 Create CaptureUnderstandingEntity in app/src/main/java/com/capsule/app/data/entity/CaptureUnderstandingEntity.kt
T012 Create CorrectionFeedbackEntity in app/src/main/java/com/capsule/app/data/entity/CorrectionFeedbackEntity.kt
T013 Create DeletionInvalidationEntity in app/src/main/java/com/capsule/app/data/entity/DeletionInvalidationEntity.kt
T014A Create UnderstandingDepthPolicyOverrideEntity in app/src/main/java/com/capsule/app/data/entity/UnderstandingDepthPolicyOverrideEntity.kt
```

### US1 Test Parallel Group

```text
T022 Add capture-detail Compose tests in app/src/androidTest/java/com/capsule/app/diary/EnvelopeDetailUnderstandingScreenTest.kt
T023 Add view model tests in app/src/test/java/com/capsule/app/diary/EnvelopeDetailUnderstandingViewModelTest.kt
T024 Add evidence claim rule tests in app/src/test/java/com/capsule/app/understanding/EvidenceClaimRulesTest.kt
T025 Add parcel boundary tests in app/src/test/java/com/capsule/app/data/ipc/CaptureUnderstandingSummaryParcelTest.kt
```

### US2 Test Parallel Group

```text
T033 Add depth policy evaluator tests in app/src/test/java/com/capsule/app/understanding/UnderstandingDepthPolicyTest.kt
T034 Add cloud trace no-content contract tests in app/src/test/java/com/capsule/app/understanding/CloudEnrichmentTraceContractTest.kt
T035 Add preferences tests in app/src/test/java/com/capsule/app/settings/PrivacyPreferencesUnderstandingDepthTest.kt
```

### US3 Test Parallel Group

```text
T043 Add source identity resolver tests in app/src/test/java/com/capsule/app/understanding/SourceIdentityResolverTest.kt
T044 Add canonical URL evidence tests in app/src/test/java/com/capsule/app/understanding/CanonicalUrlEvidenceTest.kt
T045 Extend duplicate contract tests in app/src/androidTest/java/com/capsule/app/data/UrlHashDedupeContractTest.kt
```

### US5 Test Parallel Group

```text
T052 Add deletion/invalidation DAO tests in app/src/androidTest/java/com/capsule/app/data/DeletionInvalidationDaoTest.kt
T053 Add repository lifecycle tests in app/src/test/java/com/capsule/app/data/CaptureUnderstandingDeletionLifecycleTest.kt
T054 Add cloud receipt reconciliation tests in app/src/test/java/com/capsule/app/understanding/CloudDeletionReceiptTest.kt
```

### US4 Test Parallel Group

```text
T061 Add correction feedback tests in app/src/test/java/com/capsule/app/understanding/CorrectionFeedbackRecorderTest.kt
T062 Add capture detail correction UI tests in app/src/androidTest/java/com/capsule/app/diary/EnvelopeDetailCorrectionUiTest.kt
```

### US6 Test Parallel Group

```text
T068 Add WorkManager tests in app/src/androidTest/java/com/capsule/app/continuation/CaptureUnderstandingWorkerTest.kt
T069 Add local fallback tests in app/src/test/java/com/capsule/app/understanding/LocalFallbackUnderstandingTest.kt
T070 Add save-success regression tests in app/src/androidTest/java/com/capsule/app/data/CaptureUnderstandingSaveReliabilityTest.kt
```

### Polish Parallel Group

```text
T077 Add evaluation runner in app/src/test/java/com/capsule/app/understanding/CaptureUnderstandingEvaluationTest.kt
T078 Create dogfood QA checklist in docs/capture-understanding-dogfood-qa.md
```

---

## Implementation Strategy

### MVP First (US1 Only)

1. Complete Phase 1 setup and Phase 2 foundation.
2. Complete Phase 3 US1 tests and implementation.
3. Stop and validate capture detail independently with public article, metadata-only URL, and screenshot-only OCR fixtures.
4. Confirm no Ask Orbit, retrieval, KG, memory, action, or agent affordances were introduced.

### Priority-Ordered Delivery

1. Foundation: schema, DAO, compact contracts, migration tests, and architecture guards.
2. US1: user-visible capture detail insight MVP.
3. US2: Basic/Smart/Deep controls and content-free trace policy.
4. US3: source identity hierarchy and duplicate/canonical URL behavior.
5. US5: deletion/invalidation lifecycle gate before downstream consumers.
6. US4: correction feedback and versioning.
7. US6: local fallback and save reliability under cloud/network/policy failure.
8. Polish: evaluation set, dogfood QA, Android validation, migration/schema validation, architecture greps, and gateway TypeScript validation.

### Parallel Team Strategy

After Phase 2, teams can split by story with coordination on shared files:

- Developer A: US1 detail surface and compact IPC.
- Developer B: US2 policy controls and content-free traces.
- Developer C: US3 source identity/canonical URL/duplicate behavior.
- Developer D: US5 deletion/invalidation lifecycle.

Coordinate any edits to `app/src/main/java/com/capsule/app/data/EnvelopeRepositoryImpl.kt`, `app/src/main/java/com/capsule/app/data/dao/CaptureUnderstandingDao.kt`, and `app/src/main/java/com/capsule/app/diary/ui/EnvelopeDetailScreen.kt` because multiple stories touch them.