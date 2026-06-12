# CODEX_HANDOFF.md

This handoff was written for a session restart. It captures the repo orientation, the current product/spec truth, the MongoDB Atlas decision, and the proposed next branches/spec workflow.

## Current Status - 2026-06-12 Spec 009 Active

## Previous Status - 2026-06-12 Spec 008 Checkpoint

Authoritative branch:

- `feature/009-kg-backend-poc-20260612`

Base checkpoint:

- Branched from completed Spec 008 branch `feature/008-cloud-controls-storage-budgeting-20260612`.
- Latest Spec 008 commits in stack:
  - `22474aa docs(spec-008): add cloud controls spec artifacts`
  - `84fc216 feat(spec-008): add cloud controls policy gates`
  - `e56dc49 feat(spec-008): expose cloud activity settings`
  - `b03c712 docs(spec-008): record full local gate`
  - `743826c docs: record gbrain source sync state`

Active Spec 009 truth:

- Fresh Spec Kit artifacts now exist under `specs/009-kg-backend-poc/`:
  - `spec.md`
  - `research.md`
  - `data-model.md`
  - `contracts/graph-backend-adapter-contract.md`
  - `plan.md`
  - `tasks.md`
  - `quickstart.md`
- Spec 009 is a local-first KG foundation, not a remote graph-vendor branch.
- First implementation should use Room/SQLCipher canonical state in `:ml` behind a `GraphBackendAdapter`.
- Graphiti/Zep/Mem0/Supabase are adapter candidates only and must pass Orbit's contract before any production wiring.
- No active `GraphFact` or `GraphRelationship` may be written without provenance.
- Pending/rejected memory candidates from Spec 007 must not become active graph facts.
- Spec 008 cloud controls govern any compact graph mirror; cloud mirrors remain optional and non-authoritative.

Immediate Spec 009 task order:

1. Commit Spec 009 artifacts and continuity docs.
2. Add pure graph models and `GraphBackendAdapter` contract tests.
3. Add Room v10 graph tables/DAOs/migration and source-ready migration test.
4. Implement provenance-required writes and source invalidation.
5. Project promoted memories only, never pending/rejected candidates.
6. Add compact Binder/`why this?` projection.
7. Run focused graph tests and full non-phone gate.

Important Spec 009 constraints:

- Do not start autonomous agent planning here.
- Do not add AppFunctions/Spark/platform-agent sharing here.
- Do not make remote graph storage authoritative.
- Do not bypass `:ml`/Binder boundaries.
- Do not store raw screenshots, full OCR, prompts, embeddings, model responses, or secrets in graph export/mirror payloads.

Authoritative current branch:

- `feature/008-cloud-controls-storage-budgeting-20260612`

Base checkpoint:

- Branched from committed Spec 007 checkpoint `0e2bf58` (`docs(spec-007): record clean branch checkpoint`).
- Spec 007 is repo-side complete and clean except phone/emulator execution for connected tests/manual demo.
- `screenshots/` remains untracked and must not be committed.

Active Spec 008 truth:

- Fresh Spec Kit artifacts now exist under `specs/008-cloud-controls-storage-budgeting/`:
  - `spec.md`
  - `research.md`
  - `data-model.md`
  - `contracts/cloud-controls-contract.md`
  - `plan.md`
  - `tasks.md`
  - `quickstart.md`
- Spec 008 is not a generic "cloud on/off" settings branch. It must implement three independent user controls:
  1. Compact memory index sync/search mirror.
  2. Cloud Ask synthesis / `MemoryGatewayRequest.GroundedAsk`.
  3. Cloud AI routing / `CloudLlmProvider`.
- The GStack engineering review conclusion was to split these controls because they send different payloads and have different product/privacy meaning.
- MongoDB Atlas remains a compact retrieval mirror only. Room/SQLCipher on device remains the source of truth.
- Audit/receipt data remains local-only and bounded. Do not store raw screenshots, full OCR, raw questions, prompts, embeddings, raw model responses, tokens, API keys, JWTs, or cookies in receipts.

Spec 008 implemented so far:

- Added `com.orbit.app.cloud` policy primitives:
  - `CloudCapability`
  - `BudgetDecision`
  - `BudgetDecisionReason`
  - `FallbackMode`
  - `CloudUsageOutcome`
  - `CloudControlSettings`
  - `CloudControlPolicy`
  - `CloudUsageReceiptWriter`
- Extended `PrivacyPreferences` with:
  - `cloudAskSynthesisEnabled` (default true)
  - `cloudAiRoutingEnabled` (default true)
  - `dailyCloudBudgetCents` (`null` means no local cap)
- Added `AuditAction.CLOUD_USAGE_RECORDED`.
- Compact memory index audit rows now carry bounded Spec 008 metadata:
  - `capability`
  - `cloudOutcome`
  - `cloudReason` where applicable
- `MemoryIndexSyncCoordinator` disabled upsert/tombstone paths still make zero gateway calls and now record capability-specific skipped receipts.
- `BinderAskOrbitRepository` now checks `PrivacyPreferences.cloudAskSynthesisEnabled` before `GroundedAsk`.
  - When disabled, it skips `GroundedAsk` entirely.
  - It records a best-effort local `CLOUD_USAGE_RECORDED` receipt through `BinderAuditLogClient` when a real context exists.
  - It keeps local cited retrieval/refusal behavior.
  - Sensitive identifier insufficient-evidence copy is now more user-friendly.
- `LlmProviderRouter` now reads `PrivacyPreferences.cloudAiRoutingEnabled`.
  - If cloud AI is disabled and local Nano is not available, it returns `UnavailableLlmProvider`.
  - If future local Nano hardware is available and `useLocalAi` is true, it still returns `NanoLlmProvider`.
- Added `UnavailableLlmProvider` as a non-network, fail-closed fallback.
- Settings now shows three distinct controls in both UI variants:
  - Compact memory index.
  - Cloud Ask synthesis.
  - Cloud AI routing.
- Settings test tags are now distinct per toggle instead of every switch reusing the pause tag.

Spec 008 code seams already identified:

- `PrivacyPreferences.memoryIndexingEnabled` already exists and defaults to false.
- `SettingsScreen` and `SettingsActivity` already expose "Cloud memory index".
- `MemoryIndexSyncCoordinator` already accepts `indexingEnabled` and writes skipped audit rows.
- `BinderAskOrbitRepository` currently attempts `GroundedAsk(... allowSynthesis = true)` before local fallback and needs a cloud-Ask preference gate.
- `LlmProviderRouter` currently defaults to `CloudLlmProvider` unless `RuntimeFlags.useLocalAi` and hardware capability both allow local Nano; this needs a durable cloud-AI routing policy before deeper agent work.
- `MemoryAudit` already stores request IDs, digests, counts, latency, outcomes, and avoids raw text; Spec 008 should generalize that receipt discipline.

Spec 008 completed task order:

1. Fresh Spec Kit artifacts landed in `22474aa`.
2. Policy gates, receipts, compact index metadata, Ask synthesis gate, cloud AI routing gate, and Settings controls landed in `84fc216`.
3. Cloud activity Settings entry point and task cleanup landed in `e56dc49`.
4. Full non-phone gate passed after the final polish. `dist/`, APK outputs, screenshots, and secrets were not committed.

Spec 008 validation:

```text
cloud_policy_tests=PASS 2026-06-12 `./gradlew :app:testDebugUnitTest --tests "com.orbit.app.cloud.*"`
cloud_receipt_forbidden_key_tests=PASS 2026-06-12 `CloudUsageReceiptTest`
memory_index_disabled_no_gateway_test=PASS 2026-06-12 `./gradlew :app:testDebugUnitTest --tests "com.orbit.app.memory.*"`
ask_cloud_disabled_no_groundedask_test=PASS 2026-06-12 `./gradlew :app:testDebugUnitTest --tests "com.orbit.app.orbit.*"`
llm_router_cloud_disabled_test=PASS 2026-06-12 `./gradlew :app:testDebugUnitTest --tests "com.orbit.app.ai.LlmProviderRouterTest"`
settings_three_controls_test=SOURCE_READY 2026-06-12 `./gradlew :app:compileDebugAndroidTestKotlin`
focused_jvm_gate=PASS 2026-06-12 `./gradlew :app:testDebugUnitTest --tests "com.orbit.app.cloud.*" --tests "com.orbit.app.memory.*" --tests "com.orbit.app.orbit.*" --tests "com.orbit.app.ai.*"`
lint_build_gate=PASS 2026-06-12 `./gradlew :build-logic:lint:test :app:lintDebug :app:assembleDebug`
compile_gate=PASS 2026-06-12 `./gradlew :app:compileDebugKotlin :app:compileDebugAndroidTestKotlin`
full_non_phone_gate=PASS 2026-06-12 `./gradlew :app:compileDebugKotlin :app:testDebugUnitTest :build-logic:lint:test :app:lintDebug :app:assembleDebug :app:compileDebugAndroidTestKotlin`
diff_whitespace=PASS 2026-06-12 `git diff --check`
receipt_forbidden_key_review=PASS 2026-06-12 targeted rg found raw/private words only as local variable names/test fixture assertions; persisted receipt fields are digest/count/capability/outcome metadata.
apk_path=app/build/outputs/apk/debug/app-debug.apk
apk_sha256=4c9caa89658c937195ba2edd851ed66d06cef2dd913a7e01dd805f741918bac5
```

GStack/GBrain continuity status:

- Ran GStack `sync-gbrain` preamble after Spec 008 closeout.
- Initial `gstack-gbrain-sync --incremental` partially succeeded:
  - memory/artifact ingest succeeded (`7 imported, 0 unchanged, 0 failed`);
  - curated artifact sync succeeded;
  - code sync failed because local GBrain PGLite is configured for `zeroentropyai:zembed-1` and `ZEROENTROPY_API_KEY` is not set.
- Did not wipe/re-init GBrain to switch embedding dimensions. That requires explicit user approval because GBrain reports PGLite embedding model changes need a schema-sized re-init.
- Ran non-destructive code import with embeddings disabled:
  - `gbrain sync --strategy code --source gstack-code-orbit-015537d5 --no-embed --yes`
  - imported `1031` code files and created `3783` chunks.
- Attached this worktree to the imported source:
  - source id: `gstack-code-orbit-015537d5`
  - `.gbrain-source` contains that id and is ignored by `.gitignore` so the pin remains local.
- Smoke `gbrain search "Cloud Ask synthesis" --source gstack-code-orbit-015537d5` timed out waiting for the PGLite lock. Treat GBrain search as not yet reliable in this worktree until the lock clears and/or embeddings are configured.

Important Spec 008 constraints:

- Do not add BYOC/BYOK UI in this branch.
- Do not change Atlas into backup/source-of-truth storage.
- Do not add server-side consent/audit ledger.
- Do not introduce network clients outside `com.orbit.app.net`.
- Do not make cloud Ask disabled mean `allowSynthesis=false`; it must skip `GroundedAsk` entirely.
- Do not overbuild a dashboard before policy gates and tests exist.

## Previous Status - 2026-06-12 Spec 007 Checkpoint

Authoritative current branch:

- `feature/007-memory-candidates-inspector-20260605`

Base checkpoint:

- Spec 006 was committed as `ba2dcf7` (`feat(spec-006): add approval action runtime`) on `feature/006-approval-action-runtime-20260603`.
- Spec 006 still has phone-only final demo work open, but its implementation/local gates are preserved in git. User confirmed the grouped shopping-list fix on S24: one envelope with multiple checklist items.

Spec 007 implemented so far:

- Fresh Spec Kit artifacts under `specs/007-memory-candidates-inspector/`.
- GStack-style engineering review pivot: add support/provenance junction tables now; do not rely only on JSON support arrays.
- Room v9 schema:
  - `memory_candidate`
  - `memory_candidate_support`
  - `promoted_memory`
  - `promoted_memory_support`
- New enums in `MemoryModels.kt`.
- New DAOs and exported schema `app/schemas/com.orbit.app.data.OrbitDatabase/9.json`.
- `MIGRATION_8_9` plus `OrbitDatabaseMigrationV8toV9Test` source.
- Expanded `MemoryRepositoryDelegateTest` androidTest source covers pending projection ordering, duplicate active fact-key suppression, accept/reject idempotency, edited promotion, and source-less invalidation. Source compiles; connected execution is still deferred.
- Compact Binder parcels/observers:
  - `MemoryCandidateParcel`
  - `PromotedMemoryParcel`
  - `MemoryDecisionResultParcel`
  - `IMemoryCandidateObserver`
  - `IPromotedMemoryObserver`
- `MemoryRepositoryDelegate` in `:ml` for pending/promoted projections, accept/reject decisions, compact audit rows, and debug seeding.
- `IEnvelopeRepository` extended with memory candidate observation and decision methods.
- `EnvelopeRepositoryService` wires the memory delegate in production.
- `DiaryRepository`, `BinderDiaryRepository`, and `DiaryViewModel` expose memory candidate/promoted flows and accept/reject commands.
- `DiaryViewModelTest` now covers memory candidate flow observation plus accept/reject command delegation and user-visible notices.
- `OrbitCleanupScreen` has a Memory Review section after Action Drafts, with Open, Reject, Edit, and Accept.
- `OrbitCleanupMemoryReviewTest` androidTest source covers no-empty-state noise, candidate rendering, Open/Reject/Accept callbacks, and edit validation. Source compiles; connected execution is still deferred.
- `DebugDemoSeedReceiver` now seeds deterministic memory candidates after demo captures/action proposals.
- `MemoryPayloadCaps` explicitly rejects memory-review payload keys (`memoryCandidate`, `promotedMemory`, `memory_candidate`, `promoted_memory`) before compact Atlas sync.
- `MemoryBoundaryPolicyTest` proves Ask/action and compact-memory code do not read memory candidate/promoted memory review tables as facts.

Latest Spec 007 validation:

```text
focused_memory_parcel_test=PASS 2026-06-05 `./gradlew :app:testDebugUnitTest --tests "com.orbit.app.data.ipc.MemoryParcelTest" :app:compileDebugAndroidTestKotlin`
focused_memory_viewmodel_test=PASS 2026-06-12 `./gradlew :app:testDebugUnitTest --tests "com.orbit.app.diary.DiaryViewModelTest"`
focused_memory_boundary_tests=PASS 2026-06-12 `./gradlew :app:testDebugUnitTest --tests "com.orbit.app.memory.*"`
memory_review_compose_tests=SOURCE_READY 2026-06-12 `OrbitCleanupMemoryReviewTest`; `./gradlew :app:compileDebugKotlin :app:compileDebugAndroidTestKotlin` passes; connected execution deferred
memory_repository_dao_tests=SOURCE_READY 2026-06-12 expanded `MemoryRepositoryDelegateTest`; `./gradlew :app:compileDebugKotlin :app:compileDebugAndroidTestKotlin` passes; connected execution deferred
full_non_phone_gate=PASS 2026-06-12 `./gradlew :app:compileDebugKotlin :app:testDebugUnitTest :build-logic:lint:test :app:lintDebug :app:assembleDebug :app:compileDebugAndroidTestKotlin`
diff_whitespace=PASS 2026-06-12 `git diff --check`
android_secret_scan=PASS 2026-06-12 no OPENAI_API_KEY/MONGODB_ATLAS_URI/mongodb+srv/MongoClient/ANTHROPIC_API_KEY/ZEROENTROPY hits under app/src
android_network_boundary_scan=PASS 2026-06-12 no OkHttpClient/HttpURLConnection/Socket/HttpClient constructors in scanned app source
apk_path=app/build/outputs/apk/debug/app-debug.apk
apk_sha256=559ad0f6f02c031da01b35231ad3e4de4171446cc2fa4469c788f38cc5d4cc8b
```

Remaining Spec 007 work:

- Execute `OrbitCleanupMemoryReviewTest`, `OrbitDatabaseMigrationV8toV9Test`, and `MemoryRepositoryDelegateTest` on a connected device/emulator when available.
- Build/copy final APK to `dist/` only when ready for phone validation; do not commit generated APK artifacts.
- Update tasks/quickstart/roadmap/handoff again after connected/manual validation.
- Latest committed Spec 007 checkpoints include `d46f322`, `4dd1b82`, `e630fef`, `7bce9a3`, and `49e4a9b`; `screenshots/` remains untracked and must stay out of commits.

Important constraints:

- Spec 007 is not the KG backend. Do not add graph nodes/edges/entity resolution here.
- Pending/rejected candidates are not facts.
- Candidate/promoted memory text must not enter Atlas/cloud payloads until Spec 008 cloud controls and Spec 009 KG/backend policy exist.
- Diary remains pure memory; Memory Review belongs in Orbit.

## Current Status - 2026-06-04 Spec 006 Closeout

Authoritative current branch:

- `feature/006-approval-action-runtime-20260603`

Active goal status:

- The high-level Orbit MVP goal was previously marked `blocked` only because phone access was unavailable. The user resumed work, and repo-side progress is unblocked.
- Do not mark the goal complete yet. Spec 006 still needs final S24 manual confirmation and branch commit, and the broader MVP path continues into the next specs.

Spec status:

- `specs/005A-semantic-retrieval-grounded-ask/` and the 005B link/source-label follow-up are implemented and validated enough to serve as the retrieval/Ask MVP baseline.
- `specs/006-approval-action-runtime/` is the active branch and is implementation-complete locally.
- Remaining Spec 006 tasks:
  - `T006-039`: final S24 manual demo confirmation.
  - `T006-040`: closeout docs. This handoff, `quickstart.md`, and `docs/orbit-roadmap-queue-2026-06-02.md` have been updated with current evidence; update again after the user confirms the final S24 pass.
  - `T006-041`: commit the branch without generated APK artifacts.

Current APK:

```text
path=dist/orbit-mvp-debug-20260603-006.apk
sha256=05cb7026b5738b7c55f3cba7a0d168a4dabcb182270b2f18ad05863d7fad71c5
install_status=installed/launched/seeded on S24 SM-S928U1 / adb R5CWC2KX4GK on 2026-06-04
dist_git_status=ignored/untracked; do not commit APK artifacts
```

Latest Spec 006 verification:

```text
focused_local_gate=PASS 2026-06-04 `./gradlew :app:testDebugUnitTest --tests "com.orbit.app.diary.ui.EnvelopeCardTodoParseTest" --tests "com.orbit.app.diary.DiaryViewModelTest" :app:compileDebugKotlin :app:compileDebugAndroidTestKotlin`
targeted_s24_instrumented_repo_test=PASS 2026-06-04 `./gradlew :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.orbit.app.data.ActionsRepositoryDelegateTest` (7 tests passed; non-fatal appops warning only)
full_local_android_gate=PASS 2026-06-04 `./gradlew :app:compileDebugKotlin :app:testDebugUnitTest :build-logic:lint:test :app:lintDebug :app:assembleDebug :app:compileDebugAndroidTestKotlin`
diff_whitespace=PASS 2026-06-04 `git diff --check`
android_secret_scan=PASS 2026-06-04 no OPENAI_API_KEY/MONGODB_ATLAS_URI/mongodb+srv/MongoClient/ANTHROPIC_API_KEY/ZEROENTROPY hits under app/src
android_network_boundary_scan=PASS 2026-06-04 no OkHttpClient/HttpURLConnection/Socket/HttpClient constructors in scanned app source
s24_install_seed=PASS 2026-06-04 installed APK, launched `com.orbit.app/.diary.DiaryActivity`, broadcast debug seed
s24_local_todo_grouped_list=PASS 2026-06-05 user confirmed S24 now shows one envelope with multiple checklist items
```

Important Spec 006 implementation notes:

- Orbit tab now has an Action Drafts workspace backed by a local `ActionDraftParcel` projection over existing Room action proposal/source rows. No new durable action-draft table was added.
- Calendar approvals use typed fields and `Intent.ACTION_INSERT`. Orbit does not promise undo for Calendar because it cannot reverse an external Calendar insert.
- Local shopping-list approvals now create one derived list envelope with all checklist items stored in `todoMetaJson.items[]`. This intentionally corrects older Spec 003-era behavior that could explode a shopping list into one envelope per ingredient.
- Library/local search includes `todoMetaJson`, so a grouped list envelope can be found by an item such as `ginger`.
- Derived list cards render multiple checklist rows and allow item toggles by index.
- Proposal lifecycle audit and `skill_usage` updates cover success, failure, cancel, dismiss, duplicate confirm, and schema mismatch paths.
- Debug demo proposal seeding is debug-only; release returns unavailable and no production arbitrary proposal insert API was added.
- Android still never stores Atlas/OpenAI secrets and does not construct direct network clients outside the approved `:net` boundary.

Final S24 manual validation needed:

1. Ask the user to open Orbit manually if ADB screenshots show Android recents instead of the app.
2. In Orbit, confirm Action Drafts are visible.
3. Approve the seeded Calendar draft and verify Android Calendar insert opens with expected fields.
4. Return to Orbit and verify no misleading Orbit undo appears for Calendar.
5. Approve the newly seeded shopping-list/list draft.
6. Go to Diary and verify it appears as one grouped list envelope with multiple checklist items, not separate `salmon`/`miso`/`ginger` rows.
7. Toggle one checklist item.
8. Search Library for `ginger`; expect the single grouped list result from the new approval.
9. Dismiss one remaining draft and confirm it disappears.

Manual validation caveat:

- The S24 local database may still contain individual ingredient rows created by older APKs before the grouped-list fix. Those old rows are historical local data. The user confirmed on 2026-06-05 that the current build now creates one envelope with multiple checklist items for the newly approved list draft.
- Do not reset app data without explicit user approval, because it deletes local Orbit data. If the user wants a clean demo, use the Spec 006 install script with its reset mode only after confirming the destructive reset.

Next branch after Spec 006 closes:

- `007-memory-candidates-inspector` unless the user explicitly changes the roadmap.
- Keep AppFunctions/Spark/platform-agent interop deferred until approval/access exists.
- Keep BYOM/local model manager for Spec 022. Do not treat the existing AICore/Gemini Nano path as the final local AI architecture.

## Current Status - 2026-06-03 Late Session

Authoritative current branch:

- `feature/005a-semantic-retrieval-grounded-ask-20260603`

Spec status:

- `specs/005A-semantic-retrieval-grounded-ask/` is implemented through all locally verifiable tasks.
- `specs/005B-ai-assisted-link-rehydration/` exists as the follow-up slice for AI-assisted URL summary context and screenshot source wording.
- Remaining non-deferred 005A/005B work is complete:
  - `dist/orbit-mvp-debug-20260603-005b.apk` is installed/launched/seeded on the S24.
  - User reported Library semantic search/actions worked for the tested demo flows.
  - Uploaded S24 screenshots show Orbit grounded Ask answers with cited saved captures for recipe/startup-event queries.
  - One screenshot showed `from IntentResolver`; the fixed build filters that non-user-facing source label from Diary cards/details and compact memory context, and the user confirmed no more `from IntentResolver` after reinstall.

Current APK:

```text
path=dist/orbit-mvp-debug-20260603-005b.apk
sha256=7a80f2dc94b00766177eec6f80393bf4289c39c8252cd6f3a44d925c1369c885
install_status=installed/launched/seeded on Pixel_10_Pro Android 17 emulator and S24 SM-S928U1
```

Latest local verification:

```text
memory_gateway_typecheck=pass
memory_gateway_unit_tests=pass (39 tests)
retrieval_eval=pass (6/6)
latest_backend_gate=pass 2026-06-03 (`npm run typecheck && npm run test:unit && npm run eval:retrieval`)
non_s24_closeout_script=pass 2026-06-03 (`specs/005A-semantic-retrieval-grounded-ask/scripts/verify-non-s24-closeout.sh`)
memory_gateway_deploy=pass (production alias https://orbit-memory-gateway.vercel.app)
memory_gateway_deployment=https://orbit-memory-gateway-f7nk21ubl-richels-projects-834ef114.vercel.app
memory_gateway_deployment_id=dpl_6inFrjavbbapv6HF8Kg3o6VKKehX
live_semantic_smoke=pass (qr code -> demo-qr-customers-01; reschedule -> demo-calendar-01; flight receipt -> demo-flight-receipt-01; passport -> sensitive_refusal)
android_secret_scan=pass (no OPENAI_API_KEY/MONGODB_ATLAS_URI/mongodb+srv/MongoClient hits under app/src)
android_network_boundary_scan=pass (no OkHttpClient/HttpURLConnection/Socket/HttpClient constructors outside approved net path scan)
android_focused_tests=pass (Library, Orbit Ask, memory, scrubber, 005B prompt/source coverage)
android_compile=pass (:app:compileDebugKotlin, :app:compileDebugAndroidTestKotlin)
android_lint=pass (:app:lintDebug)
android_assemble=pass (:app:assembleDebug)
latest_full_local_android_ci=pass 2026-06-03 (`./gradlew :app:compileDebugKotlin :app:testDebugUnitTest :build-logic:lint:test :app:lintDebug :app:compileDebugAndroidTestKotlin :app:assembleDebug`)
android_connected_smoke=pass on Pixel_10_Pro Android 17 emulator (OrbitHomeNavigationTest, LibraryScreenTest, AskOrbitPanelTest)
rebuilt_apk_emulator_install_seed=pass; install-and-seed-device.sh --reset-data installed sha256 7a80f2dc94b00766177eec6f80393bf4289c39c8252cd6f3a44d925c1369c885 and seeded 20 demo envelopes
rebuilt_apk_repeat_seed=pass; second seed broadcast on same emulator data logged seeded demo envelopes count=20 with no DebugDemoSeedReceiver failure
rebuilt_apk_connected_smoke=pass; same 4 targeted connected tests passed after rebuilt APK install/seed
androidx_test_stack=upgraded to test core 1.7.0, espresso-core 3.7.0, ext-junit 1.3.0 to support Android 17 instrumentation
manual_s24_helper=pass; run specs/005A-semantic-retrieval-grounded-ask/scripts/install-and-seed-device.sh when the S24 is attached; `--reset-data` is available only for clean demo devices/emulators because it deletes local Orbit data
manual_s24_install_seed=pass; helper installed/launched/seeded APK sha256 7a80f2dc94b00766177eec6f80393bf4289c39c8252cd6f3a44d925c1369c885 on S24 SM-S928U1
manual_s24_semantic_demo=pass by user report and uploaded screenshots; Library/Orbit actions worked, and grounded Ask recipe/startup-event screenshots show cited captures
intentresolver_source_label_fix=pass in code/tests/install/S24 recheck; `SourceAppLabelDisplay` filters `IntentResolver` from Diary card/detail copy and compact memory context
debug_demo_seed_idempotency=pass; DebugDemoSeedReceiver uses stable text so repeat helper runs should hit duplicate suppression instead of creating timestamped duplicate captures
pre_s24_gstack_review=pass; review found and fixed timestamped debug seed text before rebuilding the APK
gstack_gbrain_probe=gbrain CLI missing from this shell PATH; gstack brain sync mode is off, so no GBrain sync was performed in this session
```

Important implementation notes:

- Android still never connects directly to Atlas and contains no Atlas/OpenAI secrets.
- Room/SQLCipher remains source of truth; Atlas is a compact cloud memory index.
- Library semantic results are filtered to local-backed envelopes before display/open.
- Grounded Ask returns cloud answers only when citations are local-backed; sensitive refusals are preserved even without local citations.
- Compact embedding input now includes capped source context and latest note evidence, while tests prove raw OCR/full screenshot/prompt/model response fields stay out.
- URL hydration summaries now receive compact `sourceAppLabel` and latest note via `IEnvelopeRepository.getUrlHydrationContext`; raw capture bodies do not cross that Binder path.

Next branch after manual validation:

- Proceed to `006-approval-action-runtime`.
- Before starting work, read `specs/005A-semantic-retrieval-grounded-ask/mvp-closeout-audit.md`; it captures the validated MVP baseline.

## Current Continuity Protocol - 2026-06-02

Before coding after a restart or context compaction, read:

1. `AGENTS.md`
2. `VISION-2026-05-22.md`
3. `docs/mvp-to-vision-execution-plan-2026-06-02.md`
4. `docs/orbit-execution-playbook-2026-06-02.md`
5. `docs/orbit-roadmap-queue-2026-06-02.md`
6. This `CODEX_HANDOFF.md`
7. The active spec folder in Spec Kit order.

Canonical next step:

- Finish Spec 005 closeout honestly before starting another feature branch.
- Treat `T005-061` as proof of a limited local cited retrieval preview, not proof of semantic/vector/LLM Ask.
- The next branch is Spec 005A: `005A-semantic-retrieval-grounded-ask`.
- Run `/speckit.specify`, `/speckit.plan`, and `/speckit.tasks` for 005A before implementation.
- Start Spec 006 only after 005A validates semantic retrieval and grounded Ask.
- Near-term capture-context/Clarify work is planned for refreshed Spec 011 (`011-manual-compose-capture-context`) so the `006-012` bridge remains intact.
- AppFunctions/Spark/platform-agent interop is deferred until approval exists and must not drive near-term branch order.

Current date context from the active conversation: Wednesday, June 3, 2026. User wants a real MVP first, then progressive features/testing through Spec Kit with gstack/gbrain used where they help.

## Workspace

- Repo path: `/Users/rgomez/Documents/macbookair/dev/capsule-app`
- Branch during orientation: `feature/004-active-intent-cleanup-20260518`
- Product name: Orbit
- Android package/application ID: `com.orbit.app`
- Repo name is `capsule-app`, but product identity is Orbit.

Important current git status seen during orientation:

```text
## feature/004-active-intent-cleanup-20260518
?? .claude/
?? AGENTS.md
?? VISION-2026-05-22.md
?? transcripts/
```

`AGENTS.md`, `VISION-2026-05-22.md`, and `transcripts/` were untracked but should be treated as important local planning context unless the user says otherwise.

## User Goal

The user said the repo has many planning docs, markdown files, vision pivots, stale specs, and current implementation drift. They asked Codex to understand:

- Current implementation
- Active Spec Kit work
- Current strategic vision
- Outdated/stale files

Then they asked whether MongoDB Atlas startup credits can be used for storage. They have been accepted into MongoDB for Startups and have roughly `$5000` Atlas credits.

After discussion, the working recommendation is:

- Use MongoDB Atlas now, but as a cloud memory/index backend.
- Do not replace the on-device Room/SQLCipher database.
- Do not connect Android directly to Atlas.
- Do not use MongoDB Atlas Device Sync/Realm Sync/Data API/App Services as the mobile sync layer because App Services/Device Sync/Data API reached EOL on September 30, 2025.
- Use Atlas through Orbit's own backend/gateway, with server-side credentials and Supabase Auth/JWT identity.

The user then asked to create this handoff before restarting the session.

## Top-Level Mental Model

Orbit is a local-first, cloud-augmented Android personal memory layer. It captures screenshots, clipboard text, and related activity signals, wraps each capture in an `IntentEnvelope`, and presents a quiet daybook/diary.

The strategic product is not a generic productivity dashboard, chatbot, or cloud archive. It is intended to become a mobile attention memory system:

- Captures small moments the user would otherwise lose.
- Organizes by why/intent, not just artifact type.
- Returns those captures as a narrative of the day.
- Builds toward agentic cleanup/action workflows grounded in saved memory.

The current implementation is much narrower than the May 22 vision. Do not silently change code to match the vision without a spec.

## Source-of-Truth Hierarchy

Use this order when docs conflict:

1. Current code and tests.
2. `AGENTS.md` local instructions.
3. `VISION-2026-05-22.md` for strategic direction.
4. `docs/orbit-roadmap-queue-2026-06-02.md` for branch order.
5. Active spec folder for the branch. As of 2026-06-03, close `specs/005-retrieval-and-ask-citations/` honestly, then create Spec 005A before Spec 006.
6. `docs/product-roadmap-audit-2026-05-12.md`.
7. `docs/spec-branch-reorganization-plan-2026-05-13.md`.
8. `.specify/memory/constitution.md`.
9. Older specs/docs/README as historical context only.

Important nuance:

- The May 22 vision supersedes old assumptions about AICore/Gemini Nano as the long-term local-AI foundation.
- The current code still has `LlmProvider`, `NanoLlmProvider`, `CloudLlmProvider`, and AICore/Gemini Nano references.
- Treat `NanoLlmProvider` as current/legacy local-provider implementation until a BYOM/local-model-manager spec lands.

## Current Strategic Vision

Primary source: `VISION-2026-05-22.md`.

The product is moving toward a 3-pillar information architecture:

- Diary: chronological daybook of saved moments. Pure memory, no queue pressure.
- Library: semantic retrieval, fuzzy search, filters by tags/types.
- Orbit: agent/action workspace for Active Intent cleanup, action drafts, and dedicated chat/workbench sessions.

Near-term UX vision:

- Clarify capture affordance: a lightweight overlay button opens a transparent Android dialog so the user can add intent/context at capture time.
- Active Intent cleanup: the agent helps resolve accumulated intent patterns without turning the diary into a task queue.
- Dedicated agent workspace: bottom navigation separates memory browsing from agentic workflows.

Agentic direction:

- Ground actions in the Knowledge Graph.
- Ask before acting when intent is ambiguous.
- Surface "Curious Agent" profile questions only when background clusters reveal a high-confidence pattern.
- Support dedicated chat/workbench sessions where users can attach prior envelopes as context.

Generative UI direction:

- Long-term target: "the chat IS the app".
- Strategic technology target: Google's A2UI JSON protocol plus a Jetpack Compose renderer.
- LLMs output UI intent/data, not arbitrary executable code or styling.
- Android app renders using Orbit's graphite/cream Quiet Almanac language.

Local AI direction:

- Current code: `NanoLlmProvider` and AICore/Gemini Nano references.
- Strategic target: BYOM/local-model-manager architecture using MLC LLM or LiteRT-LM via C++/JNI and Vulkan.
- Model tiers: Speed model for extraction/basic understanding, Intelligence model for deeper offline chat/A2UI, cloud gateway as zero-download default.
- Invariant: local mode remains a structural escape hatch. Cloud changes quality/latency, not feature scope.

## Current Implementation Reality

Android app:

- Gradle-based Android project.
- JDK 21 Temurin required.
- Package: `com.orbit.app`
- Compose UI.
- Launcher: `DiaryActivity`.
- Room + SQLCipher encrypted DB.
- Current Room version: 8.
- DB class: `app/src/main/java/com/orbit/app/data/OrbitDatabase.kt`
- Migrations: `app/src/main/java/com/orbit/app/data/OrbitMigrations.kt`

Multi-process architecture:

- Default process / UI: user-facing activities and ViewModels.
- `:capture`: clipboard/screenshot capture, overlay, action execution. Should not perform network or own corpus writes.
- `:ml`: encrypted Room DB owner, repository service, AI inference routing. Should not perform network.
- `:net`: sole network egress code path, URL fetch, LLM gateway, Supabase auth. Should not access corpus DB.

Manifest anchors:

- `app/src/main/AndroidManifest.xml`
- `OrbitOverlayService` runs in `:capture`.
- `EnvelopeRepositoryService` runs in `:ml`.
- `NetworkGatewayService` runs in `:net`.
- `ActionExecutorService` runs in `:capture`.

Important correction:

- Some docs/comments say only `:net` has the `INTERNET` permission. That is stale wording.
- Android permissions are app UID-wide, not process-scoped.
- The real boundary is architectural: package placement, lint rule, AIDL, tests, and code review.
- Source doc: `docs/android-architecture-verification-2026-05-13.md`.

Main AIDL surfaces:

- `IEnvelopeRepository`: UI/capture -> `:ml`
- `INetworkGateway`: `:ml` -> `:net`
- `IActionExecutor`: UI -> `:capture`
- `IAuditLog`: UI -> `:ml`

Key storage/domain classes:

- `IntentEnvelopeEntity.kt`
- `CaptureUnderstandingEntity.kt`
- `ActiveIntentEntity.kt`
- `EvidenceBundleEntity.kt`
- `EnvelopeStorageBackend.kt`
- `LocalRoomBackend.kt`
- `EnvelopeRepositoryImpl.kt`
- `EnvelopeRepositoryService.kt`

Key AI/network classes:

- `LlmProvider.kt`
- `NanoLlmProvider.kt`
- `CloudLlmProvider.kt`
- `LlmProviderRouter.kt`
- `NetworkGatewayImpl.kt`
- `NetworkGatewayService.kt`
- `LlmGatewayClient.kt`
- `LlmGatewayRequest.kt`
- `LlmGatewayResponse.kt`

Runtime flags:

- File: `app/src/main/java/com/orbit/app/RuntimeFlags.kt`
- `useLocalAi = false` by default.
- `clusterEmitEnabled = false` by default.
- `devClusterForceEmit = false`.
- `useNewVisualLanguage = true`.
- `clusterModelLabelLock = NanoLlmProvider.MODEL_LABEL`.

Important current AI behavior:

- `LlmProviderRouter.create(context, networkGateway)` chooses Nano only if `RuntimeFlags.useLocalAi && hasNanoCapableHardware()`.
- `hasNanoCapableHardware()` is currently a stub returning false.
- Therefore full router defaults to cloud and requires `INetworkGateway`.
- Many production call sites use `LlmProviderRouter.createPreferLocal(...)`, which returns `NanoLlmProvider()` and avoids cloud plumbing.
- `NanoLlmProvider` mostly has `TODO("AICore integration - US2")` stubs.
- Embeddings return null until AICore embedding integration.
- Action extraction returns empty unless forced unavailable.

Current UI:

- `DiaryActivity` is launcher.
- `DiaryViewModel` observes day content, clusters, and Active Intents.
- `DiaryScreen.kt` renders day content and the Active Intent cleanup panel.
- `EnvelopeDetailActivity`/`EnvelopeDetailScreen` show single capture detail and context note editing.
- Quiet Almanac visual language is enabled by default.

Current Active Intent UI:

- Active Intent cleanup panel title: "Needs follow-up".
- Filters: All, Needs context, Ready, Maybe old.
- Actions: View capture, Add context, Ask Orbit/Refresh review, resolve, archive.
- "Ask Orbit" currently creates or refreshes a local decision brief; it does not dispatch to cloud from Android yet.

## Active Spec Kit Work

Active branch/spec:

- Branch: `feature/004-active-intent-cleanup-20260518`
- Spec folder: `specs/004-capture-understanding/`

This spec currently means:

- Screenshot Cleanup + Active Intent.
- Room v8 sidecars.
- Basic local understanding.
- Active Intent projection/resolution.
- Compact Binder payloads.
- Cleanup UI.

It does not mean the older broad "capture understanding" plan.

Core Spec 004 constraints:

- Basic mode is deterministic and local.
- Basic mode may use foreground package, app labels/categories, URL/deeplink/canonical URL, timestamp, existing OCR/text signals, and local regexes.
- Basic mode must not call cloud LLMs, public fetch, oEmbed, Readability, browser automation, or VLM.
- Active Intent sidecars store compact evidence only.
- Raw screenshots, raw HTML, full OCR bodies, embeddings, prompts, and model responses must not cross Binder.
- Smart/Deep escalation is explicit, audited, and still routed through existing network boundaries.
- Ask Orbit chat, KG backend, autonomous agent planning, external writes, and BYOM local models are future work.

Spec 004 task status:

- `specs/004-capture-understanding/tasks.md` says Active Intent decision loop is implemented; cloud Ask Orbit still deferred.
- T004-001 through T004-053 are checked.

Spec 004 deferred items:

- T004-D01: backup/data extraction rules cleanup.
- T004-D02: existing direct DB calls outside `:ml` audit/remediation.
- T004-D03: AppFunctions dependency upgrade.
- T004-D04: wire Android Active Intent review dispatch through `:net` once Supabase Auth/session/provider keys are configured.
- T004-D05: KG/backend POC.
- T004-D06: confirmed action drafting from reviewed captures.

## Future Spec Landscape

Current placeholder specs:

- `005-retrieval-and-ask-citations`
- `006-approval-action-runtime`
- `007-memory-candidates-inspector`
- `008-cloud-controls-storage-budgeting`
- `009-kg-backend-poc`
- `010-agent-coordinator`
- `011-manual-compose`
- `012-resolution-semantics`

These are rebaselined placeholders. They are not implementation-ready. Regenerate full Spec Kit artifacts before implementing.

Existing real/history specs:

- `001-core-capture-overlay`: shipped.
- `002-intent-envelope-and-diary`: v1 model/diary foundation.
- `003-orbit-actions`: real worked branch, some stale task drift.
- `013-cloud-llm-routing`: cloud LLM routing skeleton; task checkboxes stale/historical.
- `014-edge-function-llm-gateway`: Supabase Edge Function gateway; closeout/status record.
- `015-visual-refit`: Quiet Almanac visual language.
- `016-intent-set-migration`: current intent set.
- `017-capture-feedback-actions`: duplicate capture behavior.

Roadmap slots:

- `018-capture-context-affordance`: currently empty/local future slot.
- `019-agent-workspace-ia`: currently empty/local future slot.
- `020-curious-agent-profiling`: currently empty/local future slot.
- `021-generative-ui-runtime`: named in vision but no concrete spec folder.
- `022-local-model-manager`: named in vision but no concrete spec folder.

## Stale or Conflicting Docs

Treat these carefully:

- `README.md`: useful public narrative, but stale where it frames AICore/Nano as final local foundation or Demo Day roadmap.
- `.specify/memory/PRD.md`: historical v1 Intent Envelope/Diary PRD. Useful for v1 contract, not current strategic truth.
- `.specify/memory/design.md`: older ratified visual architecture. High-level tone still useful; some exact type/color details drift from implemented Spec 015.
- `.specify/memory/constitution.md`: important principles, but some network permission phrasing is stale because Android does not provide process-scoped `INTERNET`.
- `docs/product-roadmap-audit-2026-05-12.md`: excellent product reset but predates May 22 vision.
- `docs/capture-understanding-stack-research-2026-05-12.md`: research allowed a broader Basic mode in places; active Spec 004 is narrower and forbids public fetch/cloud for Basic.
- Old specs under `specs/legacy/2026-05-13-roadmap-rebaseline`: source material only.

## Known Architectural Debt

Direct DB access outside `:ml` remains known debt. A search for `OrbitDatabase.getInstance` found callers in:

- `onboarding/PermissionAudit.kt`
- `cluster/ClusterDetectionWorker.kt`
- `audit/AuditLogRetentionWorker.kt`
- `settings/ExportService.kt`
- `OrbitApplication.kt`
- `audit/DebugDumpReceiver.kt`
- `data/ipc/EnvelopeRepositoryService.kt`
- `service/OrbitOverlayService.kt`
- `continuation/SoftDeleteRetentionWorker.kt`

Not all are equally bad; some are process-guarded or worker-related. But Spec 004 explicitly deferred a full audit/remediation.

Backup/data extraction:

- Manifest has `android:allowBackup="true"`.
- Existing backup/data extraction XML is template-like.
- This is a privacy risk and tracked outside Spec 004.

Nano/AICore:

- `NanoLlmProvider` stubs remain.
- Do not treat AICore/Gemini Nano as the final local-AI architecture.

Active Intent cloud review:

- Supabase gateway has an `active_intent_review` handler.
- Android builds compact `ActiveIntentReviewContext`.
- Android does not yet dispatch this through `:net`; current "Ask Orbit" creates local `ORBIT_REVIEW` evidence.

DateTimeParser TODO:

- `TODOS.md` T9 says ISO UTC zone conversion is broken.
- Current `DateTimeParser.kt` already uses `withZoneSameInstant(zone)` for `Z`/offset strings.
- Tried to run focused test:

```text
./gradlew :app:testDebugUnitTest --tests "com.orbit.app.ai.datetime.DateTimeParserTest"
```

Result:

```text
The operation couldn't be completed. Unable to locate a Java Runtime.
Please visit http://www.java.com for information on installing Java.
```

So the todo may be stale, but it was not verified because this shell cannot locate Java.

Critical environment blocker:

- Gradle cannot run until JDK 21/Temurin is installed/visible or `JAVA_HOME` is set.
- `/usr/libexec/java_home -V` also failed with no Java runtime visible.

## MongoDB Atlas Decision

The user has `$5000` MongoDB Atlas credits. The recommendation is to use those credits now for the parts Atlas is good at:

- Cloud memory index.
- Semantic/vector search.
- Derived memory documents.
- KG-ready relationship/fact documents.
- Search/Ask Orbit retrieval backend.
- POC/benchmarking against Supabase `pgvector` or Qdrant before deeper commitment.

Do not use Atlas for:

- Replacing local Room/SQLCipher.
- Direct mobile sync.
- Atlas Device Sync/Realm Sync/Data API/App Services.
- Raw screenshot/blob storage for MVP.

External source facts used:

- MongoDB Atlas App Services Admin API EOL notice says Atlas Device Sync, SDKs, Data API, GraphQL, Static Hosting, and HTTPS Endpoints reached EOL on September 30, 2025. Database triggers remain available.
  - https://www.mongodb.com/docs/api/doc/atlas-app-services-admin-api-v3
- MongoDB Data API docs say Data API reached EOL and is historical reference only.
  - https://www.mongodb.com/docs/atlas/app-services/data-api/
- Atlas Vector Search is available and uses `$vectorSearch` on Atlas clusters.
  - https://www.mongodb.com/docs/atlas/atlas-vector-search/
- MongoDB documents have a 16 MiB BSON document limit; GridFS exists for larger files, but object storage is usually cleaner for screenshots/blobs.
  - https://www.mongodb.com/docs/manual/core/gridfs/
- Atlas encrypts cluster storage/snapshot volumes at rest by default; customer key management and client-side encryption options exist, but cloud storage is not automatically user-sovereign.
  - https://www.mongodb.com/docs/atlas/security-kms-encryption/
  - https://www.mongodb.com/docs/atlas/architecture/current/data-encryption/

Recommended Atlas role in Orbit:

- "Cloud memory index", not "source of truth".
- Store compact derived data only.
- Keep local Room authoritative.
- Use server-side backend/gateway with Atlas credentials.
- Android talks to `:net`; `:net` talks to Orbit backend; backend talks to Atlas.

MVP Atlas collection idea:

Collection: `memory_items`

Fields:

- `userId`
- `envelopeId`
- `dayLocal`
- `createdAtMillis`
- `intent`
- `contentType`
- `title`
- `summary`
- `sourceAppLabel`
- `appCategory`
- `canonicalUrl`
- `domain`
- `compactEvidence`
- `embedding`
- `schemaVersion`
- `localContentHash`
- `updatedAt`
- `tombstonedAt`

Optional collection:

- `memory_audit_events`

Never store in Atlas for MVP:

- raw screenshots
- full OCR bodies
- raw HTML
- full clipboard bodies beyond compact capped excerpts
- prompts
- model responses
- embeddings tied to raw sensitive text without explicit policy
- Android/client secrets

Atlas setup status as of May 29, 2026:

- Env file: `supabase/functions/memory_gateway/.env.local`
- Git status: `.gitignore` was updated to ignore `supabase/functions/memory_gateway/.env.local`.
- Atlas URI host verified without printing secrets: `orbitcluster.zzkkmfu.mongodb.net`
- Target database: `orbit_dev`
- Target collection: `memory_items`
- Connection/write verification succeeded with a temporary Node script in `/private/tmp/orbit-mongo-check/setup-mongo.js`.
- The setup script created `orbit_dev.memory_items`.
- Indexes created:
  - `_id_`
  - `uniq_user_envelope` on `{ userId: 1, envelopeId: 1 }`, unique
  - `user_day_created` on `{ userId: 1, dayLocal: -1, createdAtMillis: -1 }`
  - `user_intent_created` on `{ userId: 1, intent: 1, createdAtMillis: -1 }`
  - `user_tombstone` on `{ userId: 1, tombstonedAt: 1 }`
- A probe document insert/delete succeeded.
- Current Atlas role: baseline metadata/search collection exists. Vector Search index has not been created yet because the exact embedding field/dimensions must be decided in Spec 005.

## Monday MVP Plan

Deadline target: Monday, June 1, 2026.

MVP that best matches the May 22 vision:

1. Diary stays local-first and working as-is.
2. Library becomes real: search across saved memories, backed by Atlas metadata/vector search.
3. Orbit becomes a workspace: Active Intent cleanup plus Ask Orbit over cited saved captures.
4. Cloud stores only compact derived data.

This creates a real "memory layer" feel without pretending full A2UI/KG/BYOM is complete.

Do not attempt before Monday:

- Room replacement.
- Full cloud sync product.
- Full KG backend.
- Full autonomous agent.
- A2UI renderer.
- BYOM/local model manager.
- Screenshot/blob cloud archive.

## Spec Kit Workflow Recommendation

### First branch: Atlas memory index/search

Create:

```text
feature/005-atlas-memory-index-search-20260530
```

Use existing placeholder folder:

```text
specs/005-retrieval-and-ask-citations/
```

Regenerate full Spec Kit artifacts for 005:

1. `/speckit.specify`
2. `/speckit.plan`
3. `/speckit.tasks`

Spec 005 should become:

- Atlas-backed cloud memory index.
- Library search.
- Ask Orbit answers with citations.
- Compact sync from local Room sidecars.
- Explicit cloud opt-in and audit.
- No raw capture material crossing the cloud boundary.

Add research/ADR decision inside 005:

```text
MongoDB Atlas is the cloud memory index, not the source of truth.
```

Architecture decisions for 005:

- Android `:ml` owns local data.
- Android `:net` owns network calls.
- Backend talks to Atlas.
- Supabase JWT still identifies/authenticates the user.
- Atlas credentials stay server-side.
- Binder payloads remain compact.

Implementation likely touches:

- Android gateway DTOs in `app/src/main/java/com/orbit/app/ai/gateway/` or a new memory gateway package.
- `INetworkGateway.aidl` or a narrow new AIDL surface if necessary.
- `NetworkGatewayImpl.kt`
- `NetworkGatewayService.kt`
- New `MemoryGatewayClient.kt` in `com.orbit.app.net`.
- `EnvelopeRepositoryImpl.kt` or a repository delegate in `:ml` to prepare compact memory records.
- New sync worker/delegate in `:ml`.
- Diary/Library ViewModel and UI files.
- Backend under `supabase/functions/` or a sibling gateway package, depending on current deployment path.

### Second branch: 3-pillar IA MVP

Create after or stacked on 005:

```text
feature/019-agent-workspace-ia-mvp-20260531
```

Scope:

- Add bottom navigation / shell for Diary, Library, Orbit.
- Diary remains existing daybook.
- Library points to new Atlas-backed search UI from 005.
- Orbit tab surfaces Active Intent cleanup and Ask Orbit entry point.

Do not build a full agent here. This is IA and workflow surfacing.

Use existing empty/placeholder slot if present:

```text
specs/019-agent-workspace-ia/
```

If the folder exists empty, create a minimal full Spec Kit artifact set. If time is tight, do a small spec/plan/tasks set focused only on MVP navigation and moving existing surfaces.

### Optional third branch: capture context affordance

Only after 005 and 019 are working:

```text
feature/018-capture-context-affordance-20260601
```

Scope:

- Lightweight capture-time context affordance.
- Transparent Android dialog or equivalent over current overlay flow.
- User can add "why this matters / what to do next" at capture time.

This is valuable for the vision, but lower priority than real Library/search + Orbit workspace before Monday.

## Proposed Delivery Timeline

### Day 1 - Saturday, May 30, 2026

Goals:

- Fix Java/JDK environment enough to run Gradle.
- Regenerate Spec 005 around Atlas Memory Index.
- Create `feature/005-atlas-memory-index-search-20260530`.
- Create Atlas project/cluster/collection/index.
- Add backend memory upsert/search endpoint.
- Verify backend with local curl/unit tests.

Acceptance for Day 1:

- Spec 005 has clear user stories and tasks.
- Backend can upsert a compact memory item to Atlas.
- Backend can search/query Atlas for that user.
- Android code still compiles if Java is fixed.

### Day 2 - Sunday, May 31, 2026

Goals:

- Android `:ml` prepares compact memory records from Room sidecars.
- Android `:net` sends records to backend.
- Library UI can search Atlas-backed memories.
- Search results show citations and open local `EnvelopeDetailActivity`.
- Audit rows for cloud sync/search.

Acceptance for Day 2:

- Capture -> local seal -> compact cloud index -> search -> open original capture.
- No raw screenshot/full OCR/prompt/model response leaves device.

### Day 3 - Monday, June 1, 2026

Goals:

- Create `feature/019-agent-workspace-ia-mvp-20260531` or continue stacked.
- Add Diary / Library / Orbit navigation.
- Move/surface Active Intent cleanup in Orbit tab.
- Add Ask Orbit over retrieved Atlas memory items with citations.
- Manual alpha demo path.

Acceptance for Monday MVP:

- User captures something.
- It appears in Diary.
- Compact version syncs/indexes in Atlas.
- Library finds it semantically/textually.
- Ask Orbit answers with cited capture cards.
- Tapping citation opens local capture detail.
- Orbit tab shows Active Intent cleanup.
- Cloud opt-in/audit is visible enough to be honest.

## Security/Privacy Invariants for Atlas Work

Preserve these:

- Local Room/SQLCipher remains source of truth.
- Android never contains Atlas database credentials.
- Network code only in `com.orbit.app.net.*` / `:net` process.
- No direct network calls from UI, capture, or ml packages.
- All cloud sync/search requests are authenticated.
- Cloud payloads are compact, derived, capped.
- Every derived cloud item has provenance back to `envelopeId`.
- User can delete/tombstone cloud mirror data.
- Cloud features degrade gracefully to local-only behavior.

Suggested explicit payload cap:

- `title`: <= 160 chars
- `summary`: <= 500 chars
- `compactEvidence.excerpt`: <= 240 chars
- no full capture body

## Existing Gateway Context

Supabase Edge Function gateway exists at:

```text
supabase/functions/llm_gateway/
```

It has:

- TypeScript handlers.
- Auth/JWT-related code.
- LLM request/response schemas.
- Active Intent review handler: `handlers/active_intent_review.ts`.

Current Android LLM gateway path:

- `CloudLlmProvider` -> `INetworkGateway.callLlmGateway()` -> `NetworkGatewayImpl` -> `LlmGatewayClient`.

`LlmGatewayClient` requires auth state:

- `AuthStateBinder.currentJwt()`
- default `NoSessionAuthStateBinder` returns unauthorized before network.

For Atlas memory work, decide whether to:

1. Add memory routes to the existing Supabase gateway structure, or
2. Create a sibling memory gateway/service.

For Monday, prefer the shortest path that keeps credentials server-side and does not disrupt the LLM gateway.

## Commands Known from Repo Instructions

Build:

```bash
./gradlew :app:assembleDebug
./gradlew :app:compileDebugKotlin
```

Unit tests:

```bash
./gradlew :app:testDebugUnitTest
```

Custom lint module tests:

```bash
./gradlew :build-logic:lint:test
```

Android lint:

```bash
./gradlew :app:lintDebug
```

Full CI-ish pipeline:

```bash
./gradlew :app:compileDebugKotlin :app:testDebugUnitTest :build-logic:lint:test :app:lintDebug :app:assembleDebug
```

Focused DateTimeParser test attempted but blocked by missing Java:

```bash
./gradlew :app:testDebugUnitTest --tests "com.orbit.app.ai.datetime.DateTimeParserTest"
```

## Build Environment Blocker

Before serious implementation, fix Java:

- Required: JDK 21 Temurin.
- Current shell error: "Unable to locate a Java Runtime."
- `/usr/libexec/java_home -V` also failed.
- `~/.gradle` did not exist in the sandbox shell.

Likely next steps:

- Check whether JDK is installed elsewhere.
- Set `JAVA_HOME` in shell/profile or Gradle config.
- If not installed, user may need to install Temurin 21 or point Codex to it.

Do not claim tests pass until this is fixed and tests are run.

## Files Read During Orientation

Important docs:

- `AGENTS.md`
- `VISION-2026-05-22.md`
- `README.md`
- `TODOS.md`
- `.specify/memory/constitution.md`
- `.specify/memory/PRD.md`
- `.specify/memory/design.md`
- `docs/product-roadmap-audit-2026-05-12.md`
- `docs/spec-branch-reorganization-plan-2026-05-13.md`
- `docs/android-architecture-verification-2026-05-13.md`
- `docs/demo-walkthrough-2026-05-20.md`
- `docs/orbit-agent-architecture-round-4-2026-05-12.md`
- `docs/orbit-agent-architecture-round-5-2026-05-12.md`
- `docs/orbit-agent-architecture-round-6-2026-05-12.md`
- `docs/capture-understanding-stack-research-2026-05-12.md`
- `docs/agent-stack-landscape-research-2026-05-12.md`

Important specs:

- `specs/004-capture-understanding/spec.md`
- `specs/004-capture-understanding/plan.md`
- `specs/004-capture-understanding/tasks.md`
- `specs/004-capture-understanding/quickstart.md`
- `specs/005-retrieval-and-ask-citations/spec.md`
- `specs/006-approval-action-runtime/spec.md`
- `specs/007-memory-candidates-inspector/spec.md`
- `specs/008-cloud-controls-storage-budgeting/spec.md`
- `specs/009-kg-backend-poc/spec.md`
- `specs/010-agent-coordinator/spec.md`
- `specs/011-manual-compose/spec.md`
- `specs/012-resolution-semantics/spec.md`
- `specs/013-cloud-llm-routing/*`
- `specs/014-edge-function-llm-gateway/*`
- `specs/015-visual-refit/*`
- `specs/016-intent-set-migration/*`
- `specs/017-capture-feedback-actions/*`

Important code:

- `app/src/main/AndroidManifest.xml`
- `app/build.gradle.kts`
- `app/src/main/java/com/orbit/app/RuntimeFlags.kt`
- `app/src/main/java/com/orbit/app/OrbitApplication.kt`
- `app/src/main/java/com/orbit/app/data/OrbitDatabase.kt`
- `app/src/main/java/com/orbit/app/data/OrbitMigrations.kt`
- `app/src/main/java/com/orbit/app/data/EnvelopeRepositoryImpl.kt`
- `app/src/main/java/com/orbit/app/data/ipc/EnvelopeRepositoryService.kt`
- `app/src/main/aidl/com/orbit/app/data/ipc/IEnvelopeRepository.aidl`
- `app/src/main/java/com/orbit/app/data/ActiveIntentRepository.kt`
- `app/src/main/java/com/orbit/app/data/ActiveIntentReviewContext.kt`
- `app/src/main/java/com/orbit/app/understanding/BasicUnderstandingEngine.kt`
- `app/src/main/java/com/orbit/app/understanding/BasicUnderstandingWriter.kt`
- `app/src/main/java/com/orbit/app/understanding/ActiveIntentProjector.kt`
- `app/src/main/java/com/orbit/app/understanding/IntentCategoryClassifier.kt`
- `app/src/main/java/com/orbit/app/understanding/CompletionKeyExtractor.kt`
- `app/src/main/java/com/orbit/app/understanding/ActiveIntentResolver.kt`
- `app/src/main/java/com/orbit/app/ai/LlmProvider.kt`
- `app/src/main/java/com/orbit/app/ai/NanoLlmProvider.kt`
- `app/src/main/java/com/orbit/app/ai/CloudLlmProvider.kt`
- `app/src/main/java/com/orbit/app/ai/LlmProviderRouter.kt`
- `app/src/main/java/com/orbit/app/net/NetworkGatewayImpl.kt`
- `app/src/main/java/com/orbit/app/net/NetworkGatewayService.kt`
- `app/src/main/java/com/orbit/app/net/LlmGatewayClient.kt`
- `app/src/main/java/com/orbit/app/diary/DiaryActivity.kt`
- `app/src/main/java/com/orbit/app/diary/DiaryViewModel.kt`
- `app/src/main/java/com/orbit/app/diary/BinderDiaryRepository.kt`
- `app/src/main/java/com/orbit/app/diary/ActiveIntentUiState.kt`
- `app/src/main/java/com/orbit/app/diary/ui/DiaryScreen.kt`
- `app/src/main/java/com/orbit/app/diary/EnvelopeDetailActivity.kt`
- `app/src/main/java/com/orbit/app/diary/EnvelopeDetailViewModel.kt`
- `app/src/main/java/com/orbit/app/diary/ui/EnvelopeDetailScreen.kt`
- `app/src/main/java/com/orbit/app/service/OrbitSealOrchestrator.kt`
- `app/src/main/java/com/orbit/app/overlay/PostCaptureOverlay.kt`
- `build-logic/lint/src/main/java/com/orbit/lint/NoHttpClientOutsideNetDetector.kt`
- `supabase/functions/llm_gateway/handlers/active_intent_review.ts`
- `supabase/functions/llm_gateway/lib/schemas.ts`

## Suggested Next Prompt After Restart

The user can say:

```text
Read CODEX_HANDOFF.md and continue. First fix/verify the Java/JDK blocker if possible, then help me regenerate Spec 005 as Atlas Memory Index + Library/Search + Ask citations MVP and create the feature branch.
```

## Workflow Addendum: Context7, Spec Kit, gstack, gbrain

The user later clarified they want to use:

- Context7 for current documentation grounding.
- Garry Tan's gstack workflow style.
- GBrain for work moving forward.

Current local tool status checked in this session:

- `specify` is installed at `/Users/rgomez/.local/bin/specify`.
- `specify workflow list` shows one installed workflow: `speckit` / "Full SDD Cycle", which runs `specify -> plan -> tasks -> implement` with review gates.
- `.specify/workflows/speckit/workflow.yml` exists locally.
- Spec Kit version in `.specify/init-options.json` is `0.7.1`.
- Git extension is installed/enabled: `specify extension list` reports `git` / "Git Branching Workflow" enabled.
- `gbrain` is now on PATH via Bun after shell startup sources `/Users/rgomez/.zshrc`.
- GStack does not expose a single `gstack` command in this repo, but its Codex skills are installed and available in `/Users/rgomez/.codex/skills`.
- Local clones exist outside the repo:
  - `/Users/rgomez/Documents/macbookair/dev/gbrain`
  - `/Users/rgomez/Documents/macbookair/dev/gstack`
- Context7 MCP tools are not exposed directly in this Codex session. If current library docs are needed and no Context7 MCP is available, use official web docs or install/configure Context7 MCP outside this session.

Update after user asked to download/install GBrain and GStack:

- Cloned GBrain to `/Users/rgomez/Documents/macbookair/dev/gbrain`.
- Cloned GStack to `/Users/rgomez/Documents/macbookair/dev/gstack`.
- Installed GBrain globally with Bun:
  - Command used: `bun install -g github:garrytan/gbrain`
  - Verified binary: `/Users/rgomez/.bun/bin/gbrain --version`
  - Version: `gbrain 0.41.29.0`
- Added Bun global bin to `/Users/rgomez/.zshrc`:
  - `export PATH="$HOME/.bun/bin:$PATH"`
- Initially initialized local GBrain with PGLite and no embedding provider:
  - Command used: `gbrain init --pglite --no-embedding`
  - Brain path: `/Users/rgomez/.gbrain/brain.pglite`
  - Current page count at initialization: `0 pages`
  - Search mode selected by GBrain: `conservative`
  - Verified with: `gbrain config get search.mode`
- User then added an OpenAI API key to their shell environment.
- Because the no-embedding PGLite brain had `0 pages`, it was backed up and reinitialized for OpenAI embeddings:
  - Backup path: `/Users/rgomez/.gbrain/brain.pglite.no-embedding.bak`
  - Command used: `gbrain init --pglite --embedding-model openai:text-embedding-3-large`
  - GBrain detected `OPENAI_API_KEY`.
  - GBrain selected OpenAI expansion/chat defaults during init.
  - After init auto-selected `tokenmax`, search mode was explicitly reset to `conservative`.
- Ran `gbrain doctor --json`:
  - Status: `warnings`
  - Health score: `70`
  - Important warning: no embeddings/API provider configured yet.
  - GStack detection later reported `gbrain_doctor_ok: true`.
- Installed GStack for Codex:
  - Command used: `/Users/rgomez/Documents/macbookair/dev/gstack/setup --host codex`
  - Codex skills location: `/Users/rgomez/.codex/skills`
  - Linked skills include `gstack-office-hours`, `gstack-autoplan`, `gstack-spec`, `gstack-review`, `gstack-qa`, `gstack-ship`, `gstack-sync-gbrain`, and others.
- Ran GStack GBrain detection:
  - `gbrain_on_path: true`
  - `gbrain_version: gbrain0.41.29.0`
  - `gbrain_config_exists: true`
  - `gbrain_engine: pglite`
  - `gbrain_doctor_ok: true`
  - `gbrain_mcp_mode: none`
  - `gstack_brain_sync_mode: off`
  - `gbrain_local_status: ok`
- Re-ran GStack setup after GBrain initialization. It completed and reported `gstack ready (codex)`, but also emitted:
  - `error: Script not found "gen:skill-docs:user"`
  - The process still exited `0`.
  - Treat this as a non-blocking GStack setup bug/quirk unless a gstack skill appears missing.

GBrain embedding status:

- `OPENAI_API_KEY` is configured in the user's shell environment.
- GBrain was reinitialized for `openai:text-embedding-3-large`.
- The prior ZeroEntropy warning is not relevant unless the embedding model is switched back to `zeroentropyai:zembed-1`.
- Search mode should remain `conservative` for cost control unless the user explicitly opts into `tokenmax`.
- A fresh `gbrain doctor` after reinit has not been recorded yet; rerun before relying on GBrain as a project memory surface.

Do not install optional GBrain skillpacks without asking the user. GBrain itself explicitly requested operator confirmation before running:

```bash
gbrain skillpack install --all
```

Spec Kit workflow facts verified from official docs:

- Spec Kit's core process is Spec -> Plan -> Tasks -> Implement.
- Workflows can automate multi-step SDD processes and support review gates/resume state.
- A workflow run persists state under `.specify/workflows/runs/<run_id>/`.

Local Spec Kit workflow shape:

```text
speckit.specify
review-spec gate
speckit.plan
review-plan gate
speckit.tasks
speckit.implement
```

How to adapt this for Orbit:

- Do not blindly run full `speckit` through implementation without human review. Use the gates deliberately because this repo has stale specs and vision pivots.
- For Spec 005, run or emulate:
  1. `speckit.specify` for Atlas Memory Index + Library/Search. Ask Orbit is P2/stretch only after retrieval works.
  2. Manual review against `VISION-2026-05-22.md`, `AGENTS.md`, Spec 004 constraints, and code reality.
  3. `speckit.plan`.
  4. Manual review for process boundaries, privacy payload constraints, and Monday MVP scope.
  5. `speckit.tasks`.
  6. Implement only the MVP task slice first.

## Spec 005 Progress Update - May 30, 2026

Branch now exists:

- `feature/005-atlas-memory-index-search-20260530`

Important vision correction after rereading `VISION-2026-05-22.md`:

- Full Ask Orbit is NOT the sensible immediate next move.
- The vision's near-term move is to make Orbit legible as Diary / Library / Orbit.
- Required MVP path is:
  1. Compact Atlas memory index.
  2. Library search with citations and View Capture.
  3. Minimal Diary / Library / Orbit entry point, with Orbit temporarily routing to existing Active Intent cleanup.
  4. Ask Orbit only as retrieval-grounded P2/stretch after Library works.
- Do not continue from an Ask-first framing.

Spec Kit artifacts created/updated:

- `specs/005-retrieval-and-ask-citations/spec.md`
- `specs/005-retrieval-and-ask-citations/plan.md`
- `specs/005-retrieval-and-ask-citations/research.md`
- `specs/005-retrieval-and-ask-citations/data-model.md`
- `specs/005-retrieval-and-ask-citations/contracts/memory-gateway-api.md`
- `specs/005-retrieval-and-ask-citations/contracts/android-memory-boundary.md`
- `specs/005-retrieval-and-ask-citations/quickstart.md`
- `specs/005-retrieval-and-ask-citations/tasks.md`

Backend memory gateway implemented under `supabase/functions/memory_gateway/`:

- TypeScript/Vitest package.
- Supabase JWT auth.
- Zod request schemas with capped compact payloads and recursive banned-field rejection.
- Atlas helper using server-side `MONGODB_ATLAS_URI`.
- Router for `memory_upsert`, `memory_tombstone`, `memory_search`.
- `memory_ask` exists only as a retrieval-grounded stretch endpoint and is tested that way.
- Vercel-style entry: `api/memory.ts`.
- `.env.local.example` added with placeholder-only values.
- `node_modules/` and `.env.local` are ignored.

Backend gates passed:

```bash
cd supabase/functions/memory_gateway
npm run typecheck
npm run test:unit
```

Android compact memory payload work started:

- Added `app/src/main/java/com/orbit/app/memory/MemoryPayloadCaps.kt`.
- Added `app/src/main/java/com/orbit/app/memory/MemoryModels.kt`.
- Added `app/src/main/java/com/orbit/app/memory/CompactMemoryIndexBuilder.kt`.
- Added `app/src/test/java/com/orbit/app/memory/CompactMemoryIndexBuilderTest.kt`.
- The builder is local/deterministic and maps envelope + continuation + note + Spec 004 sidecars into capped `MemoryIndexItem` records.
- It drops evidence bundles containing banned keys and does not emit raw screenshot/OCR/HTML/prompt/model-response fields.

Android toolchain status:

- Android Studio is installed and its bundled JDK is configured in `~/.zshrc`:
  - `JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"`
  - Java version verified: OpenJDK 21.0.10.
- Gradle 9.3.1 runs with the Android Studio JBR.

Android Spec 005 implementation now includes:

- AIDL memory gateway extension:
  - `INetworkGateway.callMemoryGateway(in MemoryGatewayRequestParcel)`.
  - JSON-in-string request/response parcels under `app/src/main/java/com/orbit/app/net/ipc/`.
  - `MemoryGatewayClient` in `com.orbit.app.net` only; Android does not hold Atlas credentials.
- Boundary tests:
  - `MemoryBoundaryPolicyTest` scans production Android source for Atlas URI/driver patterns.
- Local audit helpers:
  - `MemoryAudit` builds `MEMORY_INDEX_UPSERTED`, `MEMORY_INDEX_TOMBSTONED`, `MEMORY_SEARCH_REQUESTED`, `MEMORY_ASK_REQUESTED`, `MEMORY_GATEWAY_FAILED`, and `MEMORY_SYNC_SKIPPED` rows.
  - Tests prove raw query/payload text is not stored in audit extras.
- Compact index sync:
  - `MemoryIndexSyncCoordinator`, `RoomMemoryIndexSource`, `MemoryIndexSyncWorker`, and `MemoryIndexSyncScheduler`.
  - Sync is opt-in through `PrivacyPreferences.KEY_MEMORY_INDEXING_ENABLED` (`memory_indexing_enabled`, default false).
  - Repository mutations enqueue sync after local transactions commit:
    - seal/basic understanding refresh -> upsert
    - URL hydration success -> upsert
    - screenshot understanding refresh -> upsert
    - intent reassignment/restore -> upsert
    - delete/hard delete -> tombstone
- Library MVP:
  - `LibraryRepository`, `BinderLibraryRepository`, `LibraryViewModel`, `LibraryUiState`.
  - `LibraryScreen` renders search, loading, empty, unavailable, result rows, citation metadata, and View Capture action.
  - ViewModel tests cover loading/results/empty/unavailable/open-capture states.
  - `BinderLibraryRepository` filters cloud results through local `:ml` `getEnvelope` lookup before display, so backend-seeded or stale Atlas-only records cannot create dead `View Capture` links.
  - Added `IEnvelopeRepository.searchLocalEnvelopes(query, limit)` and a Room-backed local fallback search; Library merges local results after cloud filtering so the MVP remains usable when Atlas sync lags.
  - Local fallback titles are compacted for result cards instead of using full captured sentences as headings.
- Three-pillar entry:
  - `DiaryActivity` now has a bottom navigation shell for Diary / Library / Orbit.
  - Library routes to `LibraryScreen`.
  - Orbit routes to `OrbitCleanupScreen`, which wraps the existing Active Intent cleanup panel.
- Settings:
  - `SettingsScreen` now exposes a "Cloud memory index" toggle wired to `PrivacyPreferences.memoryIndexingEnabled`.
  - Default remains `false`.
- Search audit:
  - `IAuditLog` now has `appendEntry(in AuditEntryParcel entry)`.
  - `BinderLibraryRepository` writes `MEMORY_SEARCH_REQUESTED` rows through `:ml` after Library search success/failure.
  - Search audit stores query digest, result count, latency, and outcome; raw query text is not stored.
- Memory gateway deployment:
  - Added `supabase/functions/memory_gateway/vercel.json` route `/memory -> /api/memory`.
  - Added `supabase/functions/memory_gateway/deploy.sh`.
  - Added `supabase/functions/memory_gateway/README.md`.
  - Added `.vercelignore`.
  - Deployed on Vercel as `orbit-memory-gateway`; stable endpoint is `https://orbit-memory-gateway.vercel.app/memory`.
  - `local.properties` has `memory.gateway.url=https://orbit-memory-gateway.vercel.app/memory`.
  - Live unauthenticated smoke returned expected `401 UNAUTHORIZED`, proving the route is reachable.
  - Production env was corrected after Supabase resumed:
    - `SUPABASE_URL` was empty in Vercel production and caused `UNAUTHORIZED: Token verification failed`.
    - `MONGODB_DB` and `MONGODB_MEMORY_COLLECTION` were also reset with explicit values.
    - Latest production deploy alias: `https://orbit-memory-gateway.vercel.app`.
  - Backend seed signs in successfully as `orbit-debug@orbitassistant.com` and reaches the memory gateway.
  - After adding Atlas Network Access for `0.0.0.0/0`, `npm run demo:seed` passes end-to-end.
  - Added authenticated `memory_health` diagnostic request and wired `npm run demo:seed` to call it before upserts.
  - Deployed gateway pinned to Node `20.x` after Vercel had been building with Node `24.x`; this did not fix Atlas.
  - Current deployed health result after Atlas allowlist:
    - `mongodbAtlasUriConfigured=true`
    - `mongodbDbConfigured=true`
    - `mongodbMemoryCollectionConfigured=true`
    - `supabaseUrlConfigured=true`
    - `atlasOk=true`
  - Latest `npm run demo:seed` evidence:
    - 20 demo records upserted.
    - `search="startup event" count=4 top=demo-startup-event-01`
    - `search="flight receipt" count=1 top=demo-flight-receipt-01`
    - `search="recipe" count=3 top=demo-recipe-01`
    - `atlas_docs_found=20`
    - `atlas_banned_field_count=0`
    - `demo_seed_ok=yes`
- Real-device status:
  - User wirelessly connected an S24: `SM_S928U1`, ADB serial `adb-R5CWC2KX4GK-pvZWp4._adb-tls-connect._tcp`.
  - Updated debug APK installed on the S24.
  - `DiaryActivity` launches and renders the three-pillar bottom nav (`Diary`, `Library`, `Orbit`).
  - Library tab opens and displays the search field.
  - After `filters:null` was removed from the memory gateway wire request, Library search reaches the deployed gateway.
  - First gateway search showed Atlas demo records (`demo-startup-event-01`) that did not exist locally; Android now hides cloud-only results.
  - After adding local Room fallback search, S24 screenshots proved `startup` returns local-backed results and `View Capture` opens the saved local detail screen.
  - Follow-up S24 testing proved `reschedule` and `rescheduling` both surface expected captures after local query stemming.
  - Follow-up S24 testing proved repeated dentist seed captures collapse to one visible row and that row opens capture detail.
  - Kept successful screenshots only:
    - `dist/Screenshots/Screenshot_20260530_141106_Orbit.jpg`
    - `dist/Screenshots/Screenshot_20260530_141124_Orbit.jpg`
- Library failure copy now distinguishes auth/session failures from generic index failures:
    - `UNAUTHORIZED` -> "Library sign-in unavailable"
    - network/timeout -> "Library connection unavailable"
    - all other failures -> "Library index unavailable"
- Debug MVP demo seed:
  - Added `app/src/debug/java/com/orbit/app/debug/DebugDemoSeedReceiver.kt`.
  - Added debug-only manifest receiver action `com.orbit.app.debug.SEED_DEMO_MEMORY`.
  - This receiver seals 20 text demo envelopes through `EnvelopeRepositoryService` in `:ml`, so Room remains the source of truth and the normal repository path enqueues compact memory-index sync.
  - Because these are real local envelopes, Library results can open local capture detail after sync. This is stronger than the backend-only `npm run demo:seed` path, which proves gateway/Atlas behavior but uses deterministic envelope IDs that may not exist in local Room.
- Spec 005 process-boundary fix:
  - `MemoryIndexSyncWorker` no longer opens `OrbitDatabase` directly.
  - Added `IEnvelopeRepository.syncMemoryIndex(envelopeId, mode, reason)`.
  - Added `MemoryIndexSyncDelegate`, constructed by `EnvelopeRepositoryService` in `:ml`, which reads Room, builds the compact payload, binds to `:net`, and calls `INetworkGateway.callMemoryGateway`.
  - Current sync route is WorkManager/default process -> `:ml` repository binder -> compact request to `:net`. This preserves Room ownership in `:ml` and keeps Atlas/network credentials out of Android.

Android gates passed after these changes:

```bash
./gradlew :app:testDebugUnitTest --tests "com.orbit.app.memory.*"
./gradlew :app:testDebugUnitTest --tests "com.orbit.app.library.*"
./gradlew :app:testDebugUnitTest --tests "com.orbit.app.audit.AuditLogViewModelTest"
./gradlew :build-logic:lint:test :app:testDebugUnitTest :app:lintDebug
./gradlew :app:compileDebugKotlin
./gradlew :app:testDebugUnitTest
```

`./gradlew :app:compileDebugKotlin` also passed after adding the debug demo seed receiver and after refactoring memory sync through the `:ml` binder. Focused `com.orbit.app.memory.*` and `com.orbit.app.library.*` unit tests passed after the process-boundary refactor.
Full Android JVM unit tests also pass after the refactor.

Additional invariant checks passed:

```bash
rg -n "MONGODB_ATLAS_URI|mongodb\\+srv|MongoClient" app/src || true
rg -n "OrbitDatabase|getInstance\\(|RoomMemoryIndexSource|intentEnvelopeDao|captureUnderstandingDao" app/src/main/java/com/orbit/app/memory app/src/main/java/com/orbit/app/library app/src/main/java/com/orbit/app/net
./gradlew :build-logic:lint:test :app:lintDebug
```

The first command returned no Android Atlas credential/client hits. The second command shows direct Room access only in `RoomMemoryIndexSource` / `MemoryIndexSyncDelegate`, which are wired by `EnvelopeRepositoryService` in `:ml`, plus unrelated hashing helpers.

Known remaining Spec 005 gaps:

- `T005-047` real Compose/navigation UI tests are complete:
  - `OrbitHomeNavigationTest` covers Diary / Library / Orbit bottom-bar selection.
  - `LibraryScreenTest` covers query submission, result rendering, and result -> local capture open state.
- `T005-058` to `T005-060` are now complete for MVP evidence:
  - local debug seed created 20 local captures;
  - backend seed reported `atlas_banned_field_count=0`;
  - S24 proved Library search -> local capture detail.
- Atlas sync from the phone still needs follow-up if we want cloud-only search to be sufficient. It is no longer MVP-blocking because local fallback search keeps Library local-first and tappable.
- Ask Orbit exists as a thin local cited retrieval preview. It is not semantic/vector/LLM Ask and should not be treated as the finished AI layer. Screenshot testing showed lexical retrieval can rank weak keyword matches; that is the reason Spec 005A is now required before Spec 006.
- Device work preference from the user: for phone interactions, give the user exact manual steps instead of spending context driving ADB. Ask for screenshots/logs only at useful checkpoints.

Current test APK:

- Path: `dist/orbit-mvp-debug-20260530.apk`
- Alternate dated copies: `dist/orbit-mvp-debug-20260531.apk`, `dist/orbit-mvp-debug-20260602.apk`
- SHA-256 after Spec 005 closure / Ask demo proof: `b1d9e70a0ee03312933289db39d126283b2d2df4164ff129b422ddc581e3d26c`
- SHA-256 after Ask false-positive guardrails and current docs closeout: `621a24ec4d398cbf96ce5810daa4231ee71073f310212e7a2ef4a09f6d11ad98`
- Latest screenshot-driven fix:
  - `MemoryDisplayText` prefers cancellation/reschedule/status sentences over brand-only titles when no generated summary captures the point.
  - Diary cards, detail titles, Library local fallback rows, and compact memory index titles all share that helper.
  - `OrbitTheme` now uses `OrbitType.QuietAlmanac.materialTypography()` globally instead of the older default Material typography.
  - Material label typography now uses the body sans rather than caption mono; mono remains explicit for `MonoLabel`/section-label surfaces.
  - Library local fallback search expands simple inflection stems, so `reschedule` and `rescheduling` both search the root `reschedul`.
  - Library display collapses duplicate-looking rows after envelope-id dedupe, addressing repeated debug seed batches.
  - Orbit tab includes a thin cited Ask panel that answers only from local matching captures. Weak cloud Ask matches are deliberately not accepted by Android yet.
  - Ask Orbit resets its question/answer state when the user leaves the Orbit tab for Diary or Library.
  - `cancelled` Ask queries also search reschedule/postpone/moved-style variants so user phrasing can match saved cancellation/rescheduling evidence.
  - Ask citation rows are compact and use `Open` instead of large repeated `View Capture` cards.
  - Ask no longer renders a standalone `Best match:` paragraph; cited answers show a small `Found N related captures` label followed by compact citation rows.
  - Ask and Library search controls use matching compact placeholder fields, 8dp search buttons, neutral surface colors, and quieter focused borders.
  - Collapsed follow-ups no longer show large queue counts; counts remain available only after expanding review.
  - Follow-ups now filters Active Intent sidecars to concrete next-action signals only: explicit reply-language chat captures, chat captures with a found completion key, QR/order/event/coupon captures with a found completion key, and user-requested Orbit review rows. Product pages, recipes, places, gifts, read/watch items, unknowns, and stale captures stay in Diary/Library instead of becoming a queue.
  - Orbit tab wraps Ask + Follow-ups in a vertical scroll container, so expanded Follow-ups can be scrolled on device.
  - Follow-ups dedupes repeated content rows by normalized clue/category/action and keeps the newest representative row. This addresses repeated debug seed/capture batches showing the same coupon several times.
  - Capture-side already-saved exists for exact text/canonical URL at seal time. New guard adds post-Basic-understanding duplicate protection for screenshot/OCR-derived normalized content: duplicate understanding content is still stored as compact sidecar evidence, but it no longer projects another Active Intent.
  - Duplicate-understanding suppression now emits `ACTIVE_INTENT_DUPLICATE_SUPPRESSED` audit rows with only capture ids, content hash, and outcome metadata; no raw capture text.
  - Diary no longer renders Follow-ups on current or previous days. Follow-ups is now only an Orbit workspace surface.
  - Follow-ups display dedupe now builds token fingerprints from meaningful content words, so clipped/OCR-drift variants of the same capture row collapse more aggressively.
  - Library/Ask local search now includes user note/context text, so a screenshot annotated as a QR code can be found by `qr code` even when OCR text does not contain that phrase.
  - Matching note/context can drive the Library result title, summary, and evidence snippet for screenshots.
  - Multi-token Library search requires all meaningful tokens in the displayed/cited result, avoiding weak `code`-only matches for `qr code`.
  - Spec 005 closure adds repository-level proof that the limited local Ask preview can answer three controlled cited demo questions (`startup event`, `flight receipt`, `recipe`) and refuse an unsupported passport-number question. This is not enough to claim reliable semantic Ask.
- Focused verification passed:
  - `:app:compileDebugKotlin`
  - `:app:testDebugUnitTest --tests "com.orbit.app.orbit.*" --tests "com.orbit.app.library.*" --tests "com.orbit.app.memory.*"`
  - `:app:compileDebugAndroidTestKotlin`
  - `:app:assembleDebug`
- 2026-06-03 Spec 005 cleanup verification passed after pausing 005A implementation:
  - Backend `npm run typecheck`
  - Backend `npm run test:unit` — 4 files / 17 tests passed
  - Android focused gate: `:app:testDebugUnitTest --tests "com.orbit.app.library.*" --tests "com.orbit.app.orbit.*" --tests "com.orbit.app.memory.*" :app:compileDebugKotlin`
  - Full Android gate: `:app:compileDebugKotlin :app:testDebugUnitTest :build-logic:lint:test :app:lintDebug :app:compileDebugAndroidTestKotlin :app:assembleDebug`

Next repo-side steps:

1. User can install `dist/orbit-mvp-debug-20260602.apk` to verify the latest Library `qr code` behavior and limited Ask guardrails on S24.
2. Current closeout branch is `feature/005-atlas-memory-index-search-20260530`.
3. A stacked branch named `feature/005a-semantic-retrieval-grounded-ask-20260603` exists, and planning artifacts exist under `specs/005A-semantic-retrieval-grounded-ask/`.
4. Spec 005 closeout gates passed on 2026-06-03; after optional manual S24 confirmation, switch back to 005A and start implementation at `T005A-006`.
5. Start Spec 006 approval action runtime only after 005A is validated.
6. Do not jump directly to Spec 018. Capture-context work is planned for refreshed Spec 011 unless the roadmap queue is explicitly changed.

gstack-inspired operating rules to apply:

- Start with "office hours": surface assumptions, risks, and stop signs before code.
- Use a "confusion protocol": if architecture, privacy, or spec/source-of-truth conflict is unclear, stop and reconcile rather than guessing.
- Prevent the four common agent failure modes: wrong assumptions, overcomplexity, unrelated edits, imperative hacks where declarative/config/spec changes are safer.
- Ship through verifiable goals: every branch needs a demo path and validation gate.
- Keep tasks tight and independently testable.
- Use `docs/orbit-execution-playbook-2026-06-02.md` for continuity rules and `docs/orbit-roadmap-queue-2026-06-02.md` for branch order.

GBrain desired use:

- Use GBrain as persistent project memory for decisions, specs, architecture notes, and session handoffs once installed/configured.
- Treat it as agent memory/workflow context, not as Orbit's product backend.
- Do not conflate GBrain with MongoDB Atlas. Atlas is for Orbit's cloud memory index MVP; GBrain is for the development workflow memory layer.
- Before using GBrain operationally, install/configure it explicitly and record where its brain store lives. Do not silently create a second source of truth.

## Important Cautions for the Next Agent

- Do not start coding against Atlas before updating Spec 005. The user explicitly asked how to proceed systematically with Spec Kit and branches.
- Do not overbuild. Monday MVP matters.
- Do not replace Room.
- Do not use Realm/Device Sync/Data API.
- Do not store raw screenshots/full OCR/prompts/model responses in Atlas.
- Do not put Atlas credentials in Android.
- Do not bypass `:net`.
- Do not make Ask Orbit the next required MVP move; Library/search and the three-pillar entry point come first.
- Do not treat `NanoLlmProvider` as final architecture.
- Do not trust old task checkboxes without checking code.
- If code changes are requested, use `apply_patch` for manual edits.
- If tests fail because Java is missing, report that plainly and fix environment only with user approval if installing is needed.
