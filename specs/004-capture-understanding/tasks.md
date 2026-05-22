# Tasks: Screenshot Cleanup + Active Intent

**Input**: `spec.md`, `plan.md`, `data-model.md`
**Branch**: `feature/004-active-intent-cleanup-20260518`
**Status**: Active Intent decision loop implemented; cloud Ask Orbit still deferred

## Format

- `[P]` means parallelizable after prerequisites are complete.
- Each task names the exact file or verification command.
- Do not mark a task complete until code/tests/docs are in the branch and the listed gate passes.

## Phase 0: Planning Lock

- [x] T004-001 Recheck stale Phase 1 audit findings against the current branch and encode relevant constraints in `specs/004-capture-understanding/plan.md`.
- [x] T004-002 Confirm current package is `com.orbit.app`, Room version is 7, debug APK has one launcher, and Basic mode must remain local.
- [x] T004-003 Refresh `specs/004-capture-understanding/spec.md`, `plan.md`, and `data-model.md` for Screenshot Cleanup + Active Intent.

## Phase 1: Room v8 Substrate

**Goal**: Add sidecar storage without touching UI or cloud behavior.

- [x] T004-004 Add domain enums for `IntentCategory`, `CompletionKeyStatus`, `ActiveIntentStatus`, `ResolutionReason`, `UnderstandingMode`, and `UnderstandingStatus` under `app/src/main/java/com/orbit/app/understanding/domain/`. `MAYBE_OLD_OR_INACTIVE` belongs only to `IntentCategory`, not `ActiveIntentStatus`.
- [x] T004-005 Add compact evidence limit constants and allowlisted JSON-key helpers under `app/src/main/java/com/orbit/app/understanding/domain/`.
- [x] T004-006 [P] Add `CaptureUnderstandingEntity` in `app/src/main/java/com/orbit/app/data/entity/CaptureUnderstandingEntity.kt`.
- [x] T004-007 [P] Add `EvidenceBundleEntity` in `app/src/main/java/com/orbit/app/data/entity/EvidenceBundleEntity.kt`.
- [x] T004-008 [P] Add `InvalidationRecordEntity` in `app/src/main/java/com/orbit/app/data/entity/InvalidationRecordEntity.kt`.
- [x] T004-009 [P] Add `ActiveIntentEntity` in `app/src/main/java/com/orbit/app/data/entity/ActiveIntentEntity.kt`.
- [x] T004-010 Add DAOs for the four sidecar tables under `app/src/main/java/com/orbit/app/data/dao/` after the entities compile.
- [x] T004-011 Add entities and DAOs to `OrbitDatabase`; bump Room version from 7 to 8.
- [x] T004-012 Add `MIGRATION_7_8` SQL in `OrbitMigrations.kt`, using the real parent table `intent_envelope(id)` and documenting the tombstone exception for `invalidation_record`.
- [x] T004-013 Generate schema `app/schemas/com.orbit.app.data.OrbitDatabase/8.json`.
- [x] T004-014 Add `OrbitDatabaseMigrationV7toV8Test.kt` under `app/src/androidTest/java/com/orbit/app/data/`.
- [x] T004-015 Gate: run `./gradlew :app:kspDebugKotlin :app:compileDebugKotlin :app:compileDebugAndroidTestKotlin :app:lintDebug`.

## Phase 2: Local Basic Understanding Engine

**Goal**: Convert local capture hints into bounded understanding and completion keys.

- [x] T004-016 [P] Add `SourceIdentity` and `GroundingConstraints` domain models under `app/src/main/java/com/orbit/app/understanding/domain/`.
- [x] T004-017 [P] Add `CompletionKey` domain model and compact JSON serializer helpers.
- [x] T004-018 Add `ContentHasher` for artifact and normalized-text hashes; do not duplicate `CanonicalUrlHasher` URL identity behavior.
- [x] T004-019 Add `IntentCategoryClassifier` using local source/app/category/text hints only.
- [x] T004-020 Add `CompletionKeyExtractor` with first-pass regexes for prices, order IDs, dates, coupon codes, addresses, ingredients, URLs, and QR payloads.
- [x] T004-021 Add `BasicUnderstandingEngine` that never calls network, public fetch, oEmbed, Readability, LLM providers, or VLM.
- [x] T004-022 Add unit tests for classification, completion key extraction, evidence limits, and no-network test doubles.
- [x] T004-023 Gate: run `./gradlew :app:testDebugUnitTest :app:lintDebug`.

## Phase 3: Active Intent Projection

**Goal**: Produce the cleanup queue from Basic understanding.

- [x] T004-024 Add `ActiveIntentProjector` that maps understanding results to active, maybe-old category, or non-actionable rows.
- [x] T004-025 Add `ActiveIntentResolver` for user resolution states: bought, not interested, cooked, read/watched, visited, replied/done, archived, expired, invalidated.
- [x] T004-026 Add `InvalidationService` so capture deletion/correction invalidates understanding and Active Intent sidecars.
- [x] T004-027 Add unit tests for projection, resolution, and invalidation rules.
- [x] T004-028 Gate: run `./gradlew :app:testDebugUnitTest :app:lintDebug`.

## Phase 4: Repository Boundary

**Goal**: Expose compact Active Intent data without weakening process boundaries.

- [x] T004-029 Inspect existing `EnvelopeRepositoryService`, AIDL parcels, and UI repository usage before choosing the access seam.
- [x] T004-030 Implement compact repository access in `:ml`; do not add new direct Room calls from `:ui` or `:capture`.
- [x] T004-031 Add contract tests proving Binder-facing payloads exclude raw screenshots, raw HTML, full OCR bodies, embeddings, prompts, and model responses.
- [x] T004-032 Gate: run `./gradlew :app:compileDebugKotlin :app:testDebugUnitTest :app:compileDebugAndroidTestKotlin`.

## Phase 5: Minimal Cleanup UI

**Goal**: Make the product loop visible.

- [x] T004-033 Add Active Intent UI state model grouped by category and lifecycle status.
- [x] T004-034 Add minimal Compose card/list surface for active items, evidence basis, completion key status, and resolve/archive controls.
- [x] T004-035 Add explicit escalation affordance for missing context; do not dispatch Smart/Deep in this slice unless T004-036 is complete.
- [x] T004-036 Add escalation audit writer before any Smart/Deep request is queued.
- [x] T004-037 Add UI tests for empty state, active grouping, maybe-old category grouping, and resolution actions.
- [x] T004-038 Gate: run `./gradlew :app:testDebugUnitTest :app:lintDebug :app:assembleDebug`.

## Phase 6: Device Validation

- [x] T004-039 Install debug APK on S24. Tab S9 verification intentionally skipped for this slice by product decision on 2026-05-19.
- [x] T004-040 Verify one launcher icon, Quiet Almanac visual default, capture flow, and Active Intent surface on S24. Completed on 2026-05-19: launcher/package, Quiet Almanac empty Diary, empty-state setup detour removal, real screenshot observer seal, screenshot OCR worker success, and populated Active Intent surface verified on S24.
- [x] T004-041 Seed or capture at least 20 screenshots across the initial categories and record the full biopsy output in `quickstart.md`: category distribution, active/unresolved percentage, completion-key extraction rate, actions taken, trust notes, top failures, and product decision. Completed with 21 deterministic seeded captures on 2026-05-19; real Gallery biopsy remains useful follow-up for T004-040/manual dogfood.
- [x] T004-042 Wire Basic understanding persistence into the `:ml` seal/OCR path so captured screenshots and text captures create/update cleanup sidecars automatically.
- [x] T004-043 Refresh active Basic rows from compact sidecars on `:ml` startup so classifier fixes can relabel still-active rows without resurrecting resolved or archived user decisions.
- [x] T004-044 Improve Active Intent card clarity by preferring compact excerpt evidence, showing source/time context, making `Add context` primary when needed, and tightening follow-up summary copy.
- [x] T004-045 Fix saved-detail intent picker clipping by allowing intent chips to wrap and use two-line labels on narrow phone widths.
- [x] T004-046 Make Active Intent cards demo-readable by replacing internal evidence labels with explicit `Source`, `Clue`, and `Why` lines and user-facing section copy.
- [x] T004-047 Add an Ask Orbit decision-brief loop that audits the request in `:ml`, writes a compact local decision review onto the active row, exposes Ask Orbit on every cleanup card, and preserves the brief across active Basic refreshes.
- [x] T004-048 Add an Ask Orbit decision dialog so each cleanup card opens a focused source/clue/why surface with review, handled, and not-needed choices before dispatching or resolving.
- [x] T004-049 Add a compact Ask Orbit review-context packet for future cloud dispatch and audit inclusion, with contract coverage proving raw screenshots, OCR bodies, HTML, embeddings, prompts, and model responses are stripped.
- [x] T004-050 Add the cloud gateway `active_intent_review` request/response contract, Anthropic Haiku handler, router validation, and matching Android `:net` DTOs so real Ask Orbit can plug into the existing gateway path.
- [x] T004-051 Ground the Active Intent cleanup flow in the source capture: add `View capture` and `Add context` actions before Ask Orbit, deep-link context entry into the existing capture detail note store, and surface context notes in the current Quiet Almanac detail view.
- [x] T004-052 Make the Active Intent cleanup queue demo-manageable with a collapsible panel and local filters for all, needs-context, ready, and maybe-old rows.
- [x] T004-053 Make capture detail inspection demo-safe by adding fallback titles for unhydrated/long-text captures and rendering screenshots uncropped with tap-to-expand.

## Deferred Explicitly Out Of Scope

- [ ] T004-D01 Backup and data extraction rules cleanup for encrypted capture data.
- [ ] T004-D02 Existing direct DB calls outside `:ml` audit/remediation.
- [ ] T004-D03 AppFunctions dependency upgrade.
- [ ] T004-D04 Wire Android Active Intent review dispatch through `:net` once Supabase Auth/session and provider keys are configured outside chat.
- [ ] T004-D05 KG/backend POC.
- [ ] T004-D06 Add confirmed action drafting from reviewed captures, starting with calendar draft, generic to-do draft, and save-for-later draft proposals that always require user confirmation before external writes.
