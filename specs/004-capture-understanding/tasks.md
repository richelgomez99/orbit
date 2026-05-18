# Tasks: Capture Understanding

**Input**: Design documents from `specs/004-capture-understanding/`
**Prerequisites**: spec.md ✅, plan.md ✅, data-model.md ✅ (contracts/ and quickstart.md pending refresh)
**Status**: Reframed. T001-T053 capture the v8 understanding foundation. T056+ are the Active Intent cleanup pivot.

**Strategic pivot**: Spec 004 is now **Capture Understanding - Active Intent Cleanup**. The v8 implementation remains useful foundation work: source identity, content hashing, evidence bundles, escalation, invalidation, and compact UI. The next implementation pass must add completion keys, Active Intent rows, and the cleanup UI so Basic mode answers: "Which screenshots still need something from me?"

**Existing infrastructure (DO NOT recreate)**:
- `app/src/main/java/com/orbit/app/net/CanonicalUrlHasher.kt` — URL canonicalization + SHA-256; handles Google AMP, YouTube redirects, UTM stripping (T066a from spec 001)
- `app/src/main/java/com/orbit/app/net/ProviderMetadataResolver.kt` — YouTube oEmbed resolver; use only after user-triggered Smart/Deep escalation, never in Basic mode
- `app/src/main/java/com/orbit/app/net/ReadabilityExtractor.kt` — Readability4J + jsoup HTML extraction (200KB max)
- `app/src/main/java/com/orbit/app/net/SafeOkHttpClient.kt` — HTTP client
- `app/src/main/java/com/orbit/app/capture/AppCategoryDictionary.kt` — app category lookup
- `app/src/main/java/com/orbit/app/capture/StateSnapshotCollector.kt` — foreground app detection
- `app/src/main/java/com/orbit/app/ui/primitives/SourceIdentityResolver.kt` — UI glyph helper (separate from the domain resolver created in US3)

**Room DB**: currently version 7 → next migration is v7→v8 (this feature)

**Tests**: JVM unit tests (`app/src/test/java/com/orbit/app/understanding/`) for engine logic and domain rules. Instrumented migration test (`app/src/androidTest/java/com/orbit/app/data/`). Physical device checkpoints per user story.

**Organization**: 5 user stories in the reframed spec. Phases 1–7 record the v8 understanding foundation. Phase 8 adds the Active Intent cleanup layer required by the reframed product wedge.

## Format: `[ID] [P?] [Story?] Description with exact file path`

- **[P]**: Can run in parallel (targets different files, no incomplete-task dependency)
- **[Story]**: Which user story (US1–US4); only applied in user story phases (3–6)
- All paths relative to repository root

---

## Phase 1: Setup

**Purpose**: Create the `understanding` package tree so user story phases can target exact file paths without directory errors.

- [x] T001 Create package skeleton — add `package-info.kt` stubs in `app/src/main/java/com/orbit/app/understanding/`, `app/src/main/java/com/orbit/app/understanding/domain/`, and `app/src/main/java/com/orbit/app/understanding/engine/`
- [x] T002 Compile gate — `./gradlew assembleDebug` must succeed with zero errors before proceeding

**Checkpoint**: New package directories exist and compile cleanly.

---

## Phase 2: Foundational — Data Layer

**Purpose**: All four user stories depend on these entities, DAOs, and the DB migration. No story work can begin until T016 passes.

**⚠️ CRITICAL**: Complete this phase before any Phase 3–6 task.

- [x] T003 [P] Create `UnderstandingMode.kt` enum (BASIC, SMART, DEEP) in `app/src/main/java/com/orbit/app/understanding/domain/UnderstandingMode.kt`
- [x] T004 [P] Create `UnderstandingStatus.kt` with `UnderstandingStatus` enum (READY, LIMITED, FAILED) and `EvidenceLevel` enum (FULL, PARTIAL, METADATA_ONLY, VISUAL_ONLY) in `app/src/main/java/com/orbit/app/understanding/domain/UnderstandingStatus.kt`
- [x] T005 [P] Create `GroundingConstraints.kt` data class (constraints: List<String>, evidenceLevel: EvidenceLevel) in `app/src/main/java/com/orbit/app/understanding/domain/GroundingConstraints.kt` — add KDoc noting forward contract for specs 005 retrieval/Ask, 006 approval actions, 007 memory inspector, 008 cloud controls, 009 KG, and 010 agent coordinator
- [x] T006 [P] Create `CaptureUnderstandingEntity.kt` Room entity with columns: captureId (FK to IntentEnvelopeEntity), mode (UnderstandingMode), status (UnderstandingStatus), title (String?), summaryText (String?), groundingConstraintsJson (String), contentHashHex (String?), canonicalUrl (String?), sourceIdentityJson (String?), createdAt (Long), updatedAt (Long), invalidatedAt (Long? nullable) in `app/src/main/java/com/orbit/app/data/entity/CaptureUnderstandingEntity.kt`
- [x] T007 [P] Create `EvidenceBundleEntity.kt` Room entity with columns: id (UUID), captureId (FK), bundleType (String enum: OCR/URL_METADATA/PUBLIC_FETCH/PARSER_OUTPUT/MODEL_ATTEMPT/SKIPPED/FAILURE), payloadJson (String — MUST NOT contain raw HTML body, full screenshots, or embedding vectors), createdAt (Long) in `app/src/main/java/com/orbit/app/data/entity/EvidenceBundleEntity.kt`
- [x] T008 [P] Create `InvalidationRecordEntity.kt` Room entity with columns: captureId (String PK), invalidatedAt (Long), reason (String enum: CAPTURE_DELETED/USER_REQUESTED/CORRECTION_APPLIED) in `app/src/main/java/com/orbit/app/data/entity/InvalidationRecordEntity.kt`
- [x] T009 [P] Create `CorrectionFeedbackEntity.kt` Room entity with columns: id (UUID), captureId (FK), feedbackType (String enum: WRONG_SOURCE/WRONG_SUMMARY/BAD_RELEVANCE), note (String?), createdAt (Long) in `app/src/main/java/com/orbit/app/data/entity/CorrectionFeedbackEntity.kt`
- [x] T010 [P] Create `CaptureUnderstandingDao.kt` with: `getByCapture(captureId): Flow<CaptureUnderstandingEntity?>`, `upsert(entity)`, `getByContentHash(hex: String): List<CaptureUnderstandingEntity>`, `markInvalidated(captureId: String, invalidatedAt: Long)`, `deleteByCapture(captureId: String)` in `app/src/main/java/com/orbit/app/data/dao/CaptureUnderstandingDao.kt`
- [x] T011 [P] Create `EvidenceBundleDao.kt` with: `getByCaptureId(captureId: String): List<EvidenceBundleEntity>`, `insert(entity)`, `deleteByCapture(captureId: String)` in `app/src/main/java/com/orbit/app/data/dao/EvidenceBundleDao.kt`
- [x] T012 [P] Create `InvalidationRecordDao.kt` with: `getById(captureId: String): InvalidationRecordEntity?`, `insert(entity)`, `existsForCapture(captureId: String): Boolean` in `app/src/main/java/com/orbit/app/data/dao/InvalidationRecordDao.kt`
- [x] T013 [P] Create `CorrectionFeedbackDao.kt` with: `insert(entity)`, `getByCaptureId(captureId: String): List<CorrectionFeedbackEntity>` in `app/src/main/java/com/orbit/app/data/dao/CorrectionFeedbackDao.kt`
- [x] T014 Add `MIGRATION_7_8` val to `app/src/main/java/com/orbit/app/data/OrbitDatabase.kt` — SQL creates tables: `capture_understanding`, `evidence_bundle`, `invalidation_record`, `correction_feedback`; bump `@Database(version = 8)`; add four new entities to `@Database(entities = [...])` and expose four new DAOs
- [x] T015 Run `./gradlew :app:generateDebugRoomSchemas` to produce `app/schemas/com.orbit.app.data.OrbitDatabase/8.json`; write `OrbitDatabaseMigrationV7to8Test.kt` in `app/src/androidTest/java/com/orbit/app/data/` verifying migration from v7 preserves existing rows and creates the four new tables
- [x] T016 Compile gate — `./gradlew assembleDebug` must succeed before any Phase 3–6 task starts

**Checkpoint**: Room v8 schema file exists, migration test written, debug build passes.

---

## Phase 3: User Story 1 — See What Orbit Knows About A Capture (Priority: P1) 🎯 MVP

**Goal**: Save a public article URL → capture detail view shows source identity, canonical URL, evidence used, compact summary, and known limitations.

**Independent Test**: Save a public article URL from a browser, open the capture detail, and verify source identity chip, evidence type row, compact summary card, and limitations banner are all shown — without Ask Orbit or any cloud feature.

- [x] T017 [P] [US1] Create `ContentHasher.kt` in `app/src/main/java/com/orbit/app/understanding/engine/ContentHasher.kt` — `fun hash(bytes: ByteArray): String` returns SHA-256 of raw artifact bytes as lowercase hex; add `fun normalizedTextHash(text: String): String` that lowercases, trims, collapses whitespace, and hashes normalized article/text content for syndicated-content matching; NOTE: this hashes artifact/content bytes, NOT canonical URLs — see CanonicalUrlHasher for URL-based hashing
- [x] T018 [P] [US1] Create `UnderstandingResult.kt` data class with fields: captureId, status (UnderstandingStatus), mode (UnderstandingMode), title (String?), summaryText (String?), groundingConstraints (GroundingConstraints), sourceIdentityJson (String?), contentHashHex (String?), canonicalUrl (String?), evidenceBundleIds (List<String>), duplicateMatch (DuplicateMatch? nullable) in `app/src/main/java/com/orbit/app/understanding/domain/UnderstandingResult.kt`; add `DuplicateMatch` data class (existingCaptureId: String, matchType: MatchType enum EXACT_HASH/CANONICAL_URL/BOTH) in same file
- [x] T019 [US1] Create `BasicUnderstandingEngine.kt` in `app/src/main/java/com/orbit/app/understanding/engine/BasicUnderstandingEngine.kt` — for URL captures: call `ContentHasher.hash()` on captured URL bytes as the Basic artifact hash; call `CanonicalUrlHasher.hash(url)` for canonical URL identity; resolve provider/source using local host/app/category rules only; return UnderstandingResult(status=READY, mode=BASIC); for non-URL (screenshot/text-only): status=LIMITED, evidenceLevel=VISUAL_ONLY; NO network, cloud, oEmbed, ReadabilityExtractor, or public fetch calls in Basic; raw HTML NEVER stored in EvidenceBundleEntity payloadJson
- [x] T020 [P] [US1] Create `UnderstandingRepository.kt` interface and `UnderstandingRepositoryImpl.kt` in `app/src/main/java/com/orbit/app/understanding/` — `getUnderstanding(captureId: String): Flow<CaptureUnderstandingEntity?>`, `saveUnderstanding(result: UnderstandingResult)`, `requestEscalation(request: EscalationRequest)`, `getEvidenceBundles(captureId: String): List<EvidenceBundleEntity>`; interface KDoc explicitly excludes: raw HTML, full screenshots, embedding vectors from return types
- [x] T021 [P] [US1] Create `CaptureDetailViewModel.kt` in `app/src/main/java/com/orbit/app/ui/understanding/CaptureDetailViewModel.kt` — `StateFlow<CaptureDetailUiState>`; loads understanding and evidence bundles by captureId; routes through `InvalidationGuard.assertNotInvalidated(captureId)` before exposing data; does NOT surface raw evidence payloads to UI state
- [x] T022 [US1] Create `CaptureDetailScreen.kt` Composable in `app/src/main/java/com/orbit/app/ui/understanding/CaptureDetailScreen.kt` — renders: `SourceIdentityChip` (provider/app label + category icon), `EvidenceTypeSummaryRow` (OCR / URL_METADATA / PUBLIC_FETCH badges), `CompactSummaryCard` (title + summaryText), `GroundingConstraintBanner` (shown when status=LIMITED or groundingConstraints non-empty), "Get More Context" button (opens EscalationSheet from US2)
- [x] T023 [P] [US1] Write unit test `ContentHasherTest.kt` in `app/src/test/java/com/orbit/app/understanding/ContentHasherTest.kt` — identical byte arrays produce identical hex; change one byte → different hex; SHA-256 of known test vector matches expected hex
- [x] T024 [P] [US1] Write unit test `BasicUnderstandingEngineTest.kt` in `app/src/test/java/com/orbit/app/understanding/BasicUnderstandingEngineTest.kt` — HTTPS URL → status=READY, non-null canonicalUrl, non-null contentHashHex; screenshot-only capture (no URL) → status=LIMITED with EvidenceLevel=VISUAL_ONLY; verify no network calls, ProviderMetadataResolver calls, ReadabilityExtractor calls, or cloud calls are made in BASIC mode (use throwing test doubles)
- [ ] T025 [US1] Verify on physical device — save a public article URL from any browser; navigate to capture detail; confirm: source identity chip (provider name visible), evidence type row (at least URL_METADATA badge), compact summary card renders without crash, limitations banner shows if evidence is metadata-only; no ANR or crash in logcat

**Checkpoint**: Capture detail view shows source, evidence, summary, and limitations. US1 independently testable.

---

## Phase 4: User Story 2 — Control How Hard Orbit Tries To Understand (Priority: P1)

**Goal**: Basic understanding by default. Smart/Deep are explicit user-triggered escalations, each logged as a user-intent audit event before engine dispatch.

**Independent Test**: Save a URL → verify default=BASIC. Tap "Get More Context" → SMART → confirm audit entry in DiagnosticsActivity. Enable airplane mode → tap again → confirm LIMITED result with "cloud-unavailable" constraint.

- [x] T026 [P] [US2] Create `EscalationRequest.kt` data class (captureId: String, requestedMode: UnderstandingMode, requestedAt: Long) in `app/src/main/java/com/orbit/app/understanding/domain/EscalationRequest.kt`
- [x] T027 [P] [US2] Create `EscalationAuditWriter.kt` in `app/src/main/java/com/orbit/app/understanding/engine/EscalationAuditWriter.kt` — `fun writeEscalationRequested(captureId: String, mode: UnderstandingMode)`: inserts UNDERSTANDING_ESCALATION_REQUESTED AuditLogEntry via AuditLogDao; MUST be called before any engine dispatch
- [x] T028 [P] [US2] Create `UnderstandingMetadataFetcher.kt` in `app/src/main/java/com/orbit/app/net/UnderstandingMetadataFetcher.kt` — uses SafeOkHttpClient; strips cookies and auth headers; max 128KB response; extracts only Open Graph tags (og:title, og:description, og:url, og:type) and canonical link tag via jsoup; calls ReadabilityExtractor if Smart mode; computes `normalizedContentHashHex` from normalized extracted readable text when available to match syndicated content across different URLs/apps; raw HTML body is NEVER retained after extraction; returns `UrlMetadata(title: String?, description: String?, canonicalUrl: String?, ogType: String?, normalizedContentHashHex: String?)`
- [x] T029 [US2] Create `SmartUnderstandingEngine.kt` in `app/src/main/java/com/orbit/app/understanding/engine/SmartUnderstandingEngine.kt` — asserts EscalationAuditWriter has logged escalation first; calls UnderstandingMetadataFetcher for URL metadata and normalized content hash; calls ProviderMetadataResolver for YouTube only after escalation (reuse existing); on any network failure delegates to LocalFallbackGuard and returns LIMITED result; returns UnderstandingResult(mode=SMART) with normalized content hash preferred over URL-byte artifact hash when available for duplicate matching
- [x] T030 [P] [US2] Create `LocalFallbackGuard.kt` in `app/src/main/java/com/orbit/app/understanding/engine/LocalFallbackGuard.kt` — `fun check(context: Context): LocalFallbackResult?`: queries ConnectivityManager and cloud-enabled SharedPreference; if offline or cloud disabled, returns LIMITED UnderstandingResult with "cloud-unavailable" grounding constraint and prevents all outbound calls; returns null (proceed) when online + cloud enabled
- [x] T031 [US2] Create `EscalationSheet.kt` Composable in `app/src/main/java/com/orbit/app/ui/understanding/EscalationSheet.kt` — bottom sheet with two action rows: "Quick context (Smart)" and "Full analysis (Deep — uses cloud, may incur cost)"; on selection: calls `UnderstandingRepository.requestEscalation()` → EscalationAuditWriter → engine dispatch; shows CircularProgressIndicator while in-flight; updates CaptureDetailScreen via shared StateFlow on completion
- [x] T032 [P] [US2] Write unit test `EscalationAuditWriterTest.kt` in `app/src/test/java/com/orbit/app/understanding/EscalationAuditWriterTest.kt` — SMART escalation writes audit row with captureId and mode=SMART; DEEP escalation writes row with mode=DEEP; audit write happens before engine dispatch (verify using test double ordering)
- [x] T033 [P] [US2] Write unit test `LocalFallbackGuardTest.kt` in `app/src/test/java/com/orbit/app/understanding/LocalFallbackGuardTest.kt` — offline state (mocked ConnectivityManager) → returns LIMITED result with "cloud-unavailable" constraint; cloud-disabled flag → same; online + cloud enabled → returns null
- [ ] T034 [US2] Verify on physical device — (1) save a URL capture → verify detail shows mode=BASIC by default; (2) tap "Get More Context" → select Smart → check DiagnosticsActivity audit log for UNDERSTANDING_ESCALATION_REQUESTED entry with correct captureId; (3) enable airplane mode → tap "Get More Context" again → verify result shows LIMITED with "cloud-unavailable" grounding constraint visible

**Checkpoint**: Escalation flow is user-triggered, audit-logged before dispatch, and locally resilient. US2 independently testable.

---

## Phase 5: User Story 3 — Trust Source Identity And Duplicate Handling (Priority: P1)

**Goal**: Every capture has a source identity (provider, app label, category). Content-hash and canonical URL matching surfaces duplicates and offers to link evidence.

**Independent Test**: Save the same article URL twice (second time with `?utm_source=google`). Verify Orbit identifies them as a CANONICAL_URL match and offers to link evidence.

- [x] T035 [P] [US3] Create `SourceIdentity.kt` data class (provider: String?, appLabel: String?, category: AppCategory, confidence: Float) in `app/src/main/java/com/orbit/app/understanding/domain/SourceIdentity.kt`; add `DomainBrandMap` object with at minimum 12 entries: YouTube, Twitter/X, Instagram, Reddit, GitHub, LinkedIn, TikTok, Spotify, Substack, Medium, BBC, NYT — maps hostname → provider name + category
- [x] T036 [P] [US3] Create `SourceIdentityDomainResolver.kt` in `app/src/main/java/com/orbit/app/understanding/engine/SourceIdentityDomainResolver.kt` — priority chain: (1) match canonicalUrl host against DomainBrandMap; (2) foreground app label via AppCategoryDictionary (reuse existing); (3) generic AppCategory fallback (SOCIAL/NEWS/VIDEO/LINK/IMAGE/TEXT/UNKNOWN); returns SourceIdentity; NOTE: the existing `SourceIdentityResolver` in `ui/primitives/` is a UI glyph helper and remains unchanged — this is the domain-level resolver
- [x] T037 [P] [US3] Create `YoutubeUrlCanonicalizer.kt` in `app/src/main/java/com/orbit/app/capture/YoutubeUrlCanonicalizer.kt` — wraps `CanonicalUrlHasher.canonicalize()` and adds YouTube-specific short-URL expansion NOT yet covered by CanonicalUrlHasher: `youtu.be/{ID}` → `youtube.com/watch?v={ID}`; `youtube.com/shorts/{ID}` → `youtube.com/watch?v={ID}`; returns null without throwing for blank/invalid input; non-YouTube URLs pass through unchanged
- [x] T038 [P] [US3] Create `DuplicateDetectionService.kt` in `app/src/main/java/com/orbit/app/understanding/engine/DuplicateDetectionService.kt` — `fun detectDuplicate(captureId: String, contentHashHex: String?, canonicalUrl: String?): DuplicateMatch?`: queries CaptureUnderstandingDao.getByContentHash() → EXACT_HASH, using normalized content hashes when Smart/Deep produced them and Basic artifact hashes otherwise; queries by canonicalUrl → CANONICAL_URL; returns BOTH when both match; excludes self (captureId) and captures where invalidatedAt != null; returns null when no match
- [x] T039 [P] [US3] Write unit test `YoutubeUrlCanonicalizerTest.kt` in `app/src/test/java/com/orbit/app/capture/YoutubeUrlCanonicalizerTest.kt` — `youtu.be/dQw4w9WgXcQ` → `youtube.com/watch?v=dQw4w9WgXcQ`; `youtube.com/shorts/dQw4w9WgXcQ` → `youtube.com/watch?v=dQw4w9WgXcQ`; tracking params stripped (reuses CanonicalUrlHasher); non-YouTube URL → returned unchanged; blank input → null without throw
- [x] T040 [P] [US3] Write unit test `SourceIdentityDomainResolverTest.kt` in `app/src/test/java/com/orbit/app/understanding/SourceIdentityDomainResolverTest.kt` — `youtube.com` URL → provider="YouTube", category=VIDEO; `twitter.com` URL → provider="Twitter/X", category=SOCIAL; unknown domain → provider=null, category=LINK; foreground app=`com.instagram.android`, no URL → appLabel="Instagram"; all null inputs → category=UNKNOWN
- [x] T041 [P] [US3] Write unit test `DuplicateDetectionServiceTest.kt` in `app/src/test/java/com/orbit/app/understanding/DuplicateDetectionServiceTest.kt` — same normalized contentHashHex across different URLs → EXACT_HASH; same Basic artifact hash → EXACT_HASH; same canonicalUrl → CANONICAL_URL; both match → BOTH; no match → null; self-captureId excluded; invalidated capture (invalidatedAt non-null) excluded from results
- [x] T042 [US3] Integrate DuplicateDetectionService into BasicUnderstandingEngine save path in `app/src/main/java/com/orbit/app/understanding/engine/BasicUnderstandingEngine.kt` — after computing hash and canonicalUrl, call detectDuplicate; include DuplicateMatch in UnderstandingResult; update `app/src/main/java/com/orbit/app/ui/understanding/CaptureDetailScreen.kt` to render `DuplicateWarningBanner` with "Link evidence" and "Keep separate" actions when duplicateMatch is non-null
- [ ] T043 [US3] Verify on physical device — (1) save an article URL from browser; (2) save same article URL with `?utm_source=google` appended; (3) open second capture detail; (4) verify DuplicateWarningBanner appears showing CANONICAL_URL match; (5) tap "Link evidence" and verify both captures reflect linked state

**Checkpoint**: Source identity resolves for common providers and app launches. Duplicate detection surfaces content matches. US3 independently testable.

---

## Phase 6: User Story 4 — Delete Or Invalidate Derived Understanding (Priority: P1)

**Goal**: Deleting a capture triggers a mechanical invalidation of all derived understanding within 1 second. Future retrieval features cannot serve data from invalidated captures.

**Independent Test**: Save a capture, generate understanding, delete the capture, re-trigger any retrieval for that captureId, and verify "No evidence available" — not the previous summary.

- [x] T044 [P] [US4] Create `InvalidationService.kt` in `app/src/main/java/com/orbit/app/understanding/engine/InvalidationService.kt` — `fun invalidateForCapture(captureId: String, reason: InvalidationReason)`: single Room transaction: (1) insert InvalidationRecordEntity; (2) call CaptureUnderstandingDao.markInvalidated(captureId, now); (3) call EvidenceBundleDao.deleteByCapture(captureId); (4) write UNDERSTANDING_INVALIDATED AuditLogEntry via AuditLogDao; entire transaction must complete in ≤1 second; second call on same captureId is idempotent; returns InvalidationResult(captureId, invalidatedAt)
- [x] T045 [P] [US4] Create `InvalidationGuard.kt` in `app/src/main/java/com/orbit/app/understanding/engine/InvalidationGuard.kt` — `fun assertNotInvalidated(captureId: String)`: queries InvalidationRecordDao.existsForCapture(captureId); throws `InvalidatedCaptureException` if true; add KDoc forward contract: "All future retrieval, action, memory, cloud, KG, and agent systems (specs 005 through 010) MUST call assertNotInvalidated before serving cached or derived data for a captureId"
- [x] T046 [US4] Wire InvalidationService into capture deletion — locate the capture deletion call-site in the codebase (search IntentEnvelopeDao, any CaptureRepository, or CaptureListViewModel delete action); atomically call `InvalidationService.invalidateForCapture(captureId, InvalidationReason.CAPTURE_DELETED)` before or within the same transaction as the row delete; if no single deletion entry point exists, create `app/src/main/java/com/orbit/app/data/CaptureRepository.kt` with `suspend fun deleteCapture(captureId: String)` that coordinates both
- [x] T047 [P] [US4] Write unit test `InvalidationServiceTest.kt` in `app/src/test/java/com/orbit/app/understanding/InvalidationServiceTest.kt` — after `invalidateForCapture`: (1) InvalidationRecordDao.existsForCapture() returns true; (2) CaptureUnderstandingEntity.invalidatedAt is non-null; (3) EvidenceBundleDao.getByCaptureId() returns empty; (4) audit log contains UNDERSTANDING_INVALIDATED entry with correct captureId; (5) second call on same captureId does not throw (idempotent)
- [x] T048 [P] [US4] Write unit test `InvalidationGuardTest.kt` in `app/src/test/java/com/orbit/app/understanding/InvalidationGuardTest.kt` — non-invalidated captureId → assertNotInvalidated() completes without exception; invalidated captureId → throws InvalidatedCaptureException; confirm CaptureDetailViewModel propagates exception to error UiState rather than serving stale data
- [ ] T049 [US4] Verify on physical device — (1) save a capture and trigger Smart understanding; (2) note the summary shown in detail view; (3) delete the capture from the list view; (4) attempt to re-open or re-query that captureId via any retrieval path; (5) verify the response is "No evidence available" or equivalent empty state — NOT the previous summary; (6) open DiagnosticsActivity audit log and confirm UNDERSTANDING_INVALIDATED entry is present; (7) confirm total time from delete tap to invalidated state is ≤1 second

**Checkpoint**: Deletion mechanically tombstones all derived data. Future retrieval will block on the guard. US4 independently testable.

---

## Phase 7: Polish & Cross-Cutting Concerns

- [x] T050 [P] Create `SummaryLimiter.kt` in `app/src/main/java/com/orbit/app/understanding/engine/SummaryLimiter.kt` — `fun enforce(result: UnderstandingResult): UnderstandingResult`: if evidenceLevel=METADATA_ONLY, remove any summaryText sentences making full-content claims ("the article says", "according to the text", etc.) and add "Orbit read only metadata, not the full page" grounding constraint; if evidenceLevel=VISUAL_ONLY, strip text-content claims and add "Orbit read only visual content" constraint; enforces SC-004-005 (zero hallucinated full-content claims on metadata-only evidence)
- [x] T051 Add `@ForFutureFeature` marker comments to forward-contract KDoc in `app/src/main/java/com/orbit/app/understanding/domain/GroundingConstraints.kt` and `app/src/main/java/com/orbit/app/understanding/engine/InvalidationGuard.kt` — document that specs 005 (`005-retrieval-and-ask-citations`), 006 (`006-approval-action-runtime`), 007 (`007-memory-candidates-inspector`), 008 (`008-cloud-controls-storage-budgeting`), 009 (`009-kg-backend-poc`), and 010 (`010-agent-coordinator`) MUST respect these boundaries
- [x] T052 IPC boundary audit — run `grep -r "ByteArray\|Bitmap" app/src/main/java/com/orbit/app/data/ipc/` and confirm no new Parcelable fields carry raw content across Binder; verify `EvidenceBundleEntity.payloadJson` does not serialize raw HTML, screenshots, or embeddings; add `// IPC-SAFE: payloadJson carries metadata only, never raw content` comment to `app/src/main/java/com/orbit/app/data/entity/EvidenceBundleEntity.kt`; run `./gradlew :app:lintDebug` — zero new baseline lint entries
- [x] T053 Run full unit test suite — `./gradlew :app:testDebugUnitTest` — all tests in `com.orbit.app.understanding.*` pass; total test count does not regress from pre-feature baseline
- [ ] T054 Final compile and size check — `./gradlew :app:assembleDebug` produces BUILD SUCCESSFUL; compare debug APK size to pre-feature baseline and confirm increase is ≤50KB
- [ ] T055 [P] Physical device validation sweep — execute all four story device checkpoints (T025, T034, T043, T049) in a single device session; record pass/fail per checkpoint; write results to `specs/004-capture-understanding/quickstart.md` as the device verification record

**Checkpoint**: All unit tests green, lint clean, APK size within budget, four device-story checkpoints documented.

---

## Phase 8: Active Intent Cleanup Pivot (Priority: P1)

**Goal**: Turn the v8 capture-understanding foundation into the product loop: screenshot/capture -> intent category -> completion key -> Active Intent card -> user resolves, archives, expires, or escalates.

**Independent Test**: Load a mixed fixture corpus covering the 10 Dead Intent categories. Verify Active Intent shows grouped unresolved saves, each card exposes the completion key or a clear missing-evidence limitation, and resolution removes the item from Active while keeping it searchable.

- [x] T056 [P] Create `IntentCategory.kt` in `app/src/main/java/com/orbit/app/understanding/domain/IntentCategory.kt` with values: BUY_LATER_PRODUCT, RECIPE, QR_OR_BARCODE, RECEIPT_OR_ORDER, EVENT_TICKET_RESERVATION, COUPON_OR_PROMO, READ_OR_WATCH_LATER, PLACE_OR_TRAVEL_IDEA, GIFT_IDEA, CHAT_ACTION, MAYBE_OLD_OR_INACTIVE, UNKNOWN.
- [x] T057 [P] Create `CompletionKey.kt` in `app/src/main/java/com/orbit/app/understanding/domain/CompletionKey.kt` with sealed/typed shapes for product, recipe, QR/barcode, receipt/order, event/ticket, coupon, read/watch later, place, gift, and chat action keys; include `CompletionKeyStatus` FOUND/MISSING/NOT_ACTIONABLE and missing-field reasons.
- [x] T058 [P] Extend source identity domain model with `SourceTrustLevel` and `SourceEvidenceBasis` in `app/src/main/java/com/orbit/app/understanding/domain/SourceIdentity.kt`; preserve existing provider/app/category fields and JSON compatibility.
- [x] T059 Add Room v9 sidecar `ActiveIntentEntity.kt` in `app/src/main/java/com/orbit/app/data/entity/ActiveIntentEntity.kt` with fields: intentId, captureId, intentType, status, primaryEvidenceJson, primaryAction, dueAt, expiresAt, resolutionReason, resolvedAt, userConfirmed, createdAt, updatedAt.
- [x] T060 Add `ActiveIntentDao.kt` in `app/src/main/java/com/orbit/app/data/dao/ActiveIntentDao.kt` with active list, by-capture, upsert, resolve/archive/expire/invalidate transitions, and delete-by-capture helpers.
- [x] T061 Add `MIGRATION_8_9` and schema export for `active_intent`; create `OrbitDatabaseMigrationV8toV9Test.kt` verifying existing v8 rows survive, Active Intent inserts work, and capture delete invalidates or cascades sidecars according to policy.
- [x] T062 [P] Create `IntentCategoryClassifier.kt` in `app/src/main/java/com/orbit/app/understanding/engine/IntentCategoryClassifier.kt` using local evidence only: foreground app/category, OCR tokens, detected URLs, dates, prices, order IDs, coupon-code patterns, QR/barcode payload hints, address/place hints, and chat-action verbs.
- [x] T063 [P] Create `CompletionKeyExtractor.kt` in `app/src/main/java/com/orbit/app/understanding/engine/CompletionKeyExtractor.kt`; extract completion keys per category and return LIMITED/missing-field explanations when required evidence is absent.
- [x] T064 [P] Create `SourceEvidenceRanker.kt` in `app/src/main/java/com/orbit/app/understanding/engine/SourceEvidenceRanker.kt`; enforce hierarchy: URL/deeplink, foreground app/package, OCR logo/text, visible product/source text, model inference, category-only, unknown.
- [x] T065 Create `ActiveIntentResolver.kt` in `app/src/main/java/com/orbit/app/understanding/engine/ActiveIntentResolver.kt`; combine UnderstandingResult + category + completion key + source evidence into an ActiveIntent candidate without network calls in BASIC mode.
- [x] T066 Integrate ActiveIntentResolver into the BasicUnderstandingEngine save path through `UnderstandingRepositoryImpl` or a dedicated `ActiveIntentRepository`; Basic save should create/update Active Intent rows only when intent is active or likely recoverable.
- [x] T067 Wire invalidation into ActiveIntent rows in `LocalRoomBackend` and `InvalidationService`: deleted source captures mark sidecars INVALIDATED; user-resolved/user-archived rows remain searchable unless the capture itself is deleted.
- [x] T068 [P] Create `ActiveIntentRepository.kt` in `app/src/main/java/com/orbit/app/understanding/ActiveIntentRepository.kt` with active grouped list, resolve, archive, expire, mark not interested, and by-capture lookup APIs; return compact UI-safe models only.
- [x] T069 Create `ActiveIntentViewModel.kt` in `app/src/main/java/com/orbit/app/ui/understanding/ActiveIntentViewModel.kt`; expose grouped Active Intent state and resolution actions.
- [x] T070 Create `ActiveIntentScreen.kt` in `app/src/main/java/com/orbit/app/ui/understanding/ActiveIntentScreen.kt`; render grouped categories, cards with thumbnail/source/evidence/confidence, primary action, resolve/archive/not interested controls, and quiet empty states.
- [x] T071 Update `CaptureDetailScreen.kt` to prioritize completion key and Active Intent status above generic summary; source claims must show evidence basis when not high-confidence.
- [x] T072 [P] Write classifier fixture tests in `app/src/test/java/com/orbit/app/understanding/IntentCategoryClassifierTest.kt` covering all 10 Dead Intent categories plus maybe-old/inactive and unknown.
- [x] T073 [P] Write completion key tests in `app/src/test/java/com/orbit/app/understanding/CompletionKeyExtractorTest.kt` verifying extracted keys and LIMITED missing-field reasons for product, recipe, QR, receipt, event, coupon, place, gift, read/watch, and chat action fixtures.
- [x] T074 [P] Write Active Intent transition tests in `app/src/test/java/com/orbit/app/understanding/ActiveIntentResolverTest.kt` and `ActiveIntentRepositoryTest.kt`: active -> resolved, active -> archived, active -> expired, active -> invalidated, resolved remains searchable, source delete blocks downstream use.
- [x] T075 Run screenshot biopsy planning pass: write `specs/004-capture-understanding/screenshot-biopsy.md` with recruit criteria, labeling rubric, 100-screenshot worksheet, and pass/fail thresholds from SC-004-001 through SC-004-005.
- [x] T076 Run validation gate: `./gradlew :app:kspDebugKotlin :app:compileDebugAndroidTestKotlin :app:testDebugUnitTest :app:lintDebug :app:assembleDebug`; run IPC raw-content scan and old-name scan.
- [ ] T077 Physical/device validation: run Active Intent on a real Android screenshot pile; record category counts, key extraction misses, resolution actions taken, and user trust notes in `specs/004-capture-understanding/quickstart.md`.

**Checkpoint**: Active Intent is the primary Basic-mode product loop, not a generic capture-summary view.

---

## Dependencies

```
Phase 1 (Setup)
  └── Phase 2 (Data Layer: T003–T016)
        ├── Phase 3 US1 (T017–T025)   [MVP]
        ├── Phase 4 US2 (T026–T034)   depends on US1 EscalationRequest + BasicUnderstandingEngine
        ├── Phase 5 US3 (T035–T043)   depends on US1 BasicUnderstandingEngine save path (T042)
        └── Phase 6 US4 (T044–T049)   depends on US1 UnderstandingRepository (T020) + CaptureDetailViewModel (T021)
              └── Phase 7 Polish (T050–T055)
                    └── Phase 8 Active Intent Cleanup Pivot (T056–T077)
```

US2, US3, US4 can begin in parallel once Phase 2 and Phase 3 are complete.

## Parallel Execution Within Phases

**Phase 2**: T003–T013 can all run in parallel (different files). T014 must follow T003–T013. T015 must follow T014.

**Phase 3**: T017, T018, T020, T021, T023, T024 can run in parallel. T019 depends on T017 and T018. T022 depends on T019 and T021. T025 follows T022.

**Phase 4**: T026, T027, T028, T030, T032, T033 can run in parallel. T029 depends on T027, T028, T030. T031 depends on T029.

**Phase 5**: T035, T037, T038, T039, T040, T041 can run in parallel. T036 depends on T035. T042 depends on T036, T038 (integration task). T043 follows T042.

**Phase 6**: T044, T045, T047, T048 can run in parallel. T046 depends on T044, T045. T049 follows T046.

## Implementation Strategy

**Suggested MVP scope**: The v8 foundation (T001-T053) is complete enough to support the pivot, but it is not yet the product wedge. The next MVP is Phase 8: Active Intent sidecar rows, completion keys, and the grouped cleanup UI. Ask/search/agent work should wait until Active Intent proves users actually clear open loops.

**Incremental delivery**:
1. Preserve v8 foundation: Basic understanding, evidence, source identity, escalation, duplicate detection, invalidation, lint/test gates.
2. Phase 8 data: add ActiveIntent sidecar, category/completion-key domain, migration, DAOs, and repository.
3. Phase 8 engine: local classifier, completion-key extractor, source evidence ranker, resolver, and invalidation policy.
4. Phase 8 UI: grouped Active Intent screen and completion-key-first detail view.
5. Screenshot biopsy: validate real user screenshot piles before adding deeper Smart/Deep category behavior.

**Total tasks**: 77
**Parallelizable tasks**: 40 ([P] marked)
**Device verification checkpoints**: 6 (T025, T034, T043, T049, T055, T077)
**Unit test tasks**: 12 (T023, T024, T032, T033, T039, T040, T041, T047, T048, T072, T073, T074)
**Instrumented test tasks**: 2 (T015, T061)
