# Quickstart: Atlas Memory Index + Cited Library

**Spec**: [spec.md](spec.md)

## Preflight

1. Confirm local env exists and is ignored:

```bash
git status --ignored --short supabase/functions/memory_gateway
```

Expected: `.env.local` is ignored.

2. Confirm Atlas setup:

```bash
cd /private/tmp/orbit-mongo-check
node setup-mongo.js
```

Expected:

```text
setup_ok=yes
indexes=_id_,uniq_user_envelope,user_day_created,user_intent_created,user_tombstone
```

3. Confirm Atlas Network Access allows the deployed gateway to connect. If
   Vercel returns `ATLAS_UNAVAILABLE` while the same URI connects locally,
   the likely cause is Atlas allowing the developer machine IP but not
   Vercel outbound traffic. MVP workaround:

```text
MongoDB Atlas -> Security -> Network Access -> Add IP Address -> 0.0.0.0/0
```

Use a clear temporary comment such as `MVP Vercel memory gateway`; tighten
before a real production launch.

4. Confirm no Android Atlas credentials:

```bash
rg -n "MONGODB_ATLAS_URI|mongodb\\+srv|MongoClient" app/src || true
```

Expected: no matches.

## Backend Validation

```bash
cd supabase/functions/memory_gateway
npm install
npm run typecheck
npm run test:unit
```

Required tests:

- Auth rejects missing/invalid JWT.
- User A cannot read or mutate user B memory records.
- Banned fields are recursively rejected.
- Upsert creates/updates by `{ userId, envelopeId }`.
- Tombstone hides records from search.
- Ask returns citations or insufficient evidence.

## Android Validation

Before building a debug APK for live Library search, `local.properties` must include:

```properties
memory.gateway.url=https://orbit-memory-gateway.vercel.app/memory
supabase.debug.email=<normal Supabase Auth debug user>
supabase.debug.password=<debug user password>
```

Do not use service-role credentials in Android. The debug email/password are only for seeding a normal Supabase session in the `:net` process.

```bash
./gradlew :app:compileDebugKotlin
./gradlew :app:testDebugUnitTest
./gradlew :build-logic:lint:test
./gradlew :app:lintDebug
./gradlew :app:assembleDebug
```

## Demo Data

There are two demo paths. Use the debug app seed for the real MVP proof because it creates local Room envelopes first; the normal sync path then indexes those exact envelope IDs in Atlas, so Library result rows can open the original local capture.

### Preferred: Debug App Seed

1. Build and install a debug APK that includes `app/src/debug/java/com/orbit/app/debug/DebugDemoSeedReceiver.kt`.
2. In Orbit on the device, enable:

```text
Settings -> Cloud memory index
```

3. Seed 20 local demo envelopes through the normal `:ml` repository service:

```bash
adb shell am broadcast -a com.orbit.app.debug.SEED_DEMO_MEMORY
```

This receiver exists only in `app/src/debug/`; release APKs do not include it. Each seeded envelope follows the normal local Room seal path and enqueues compact memory-index sync through WorkManager.

4. Wait for network-constrained WorkManager jobs to run, then open Library and search:

```text
startup event
flight receipt
recipe
```

5. Tap at least 3 results and verify each opens the local capture detail.

### Backend-Only Seed

Use this as a gateway/Atlas proof, not as the final local-open proof. It writes deterministic demo IDs directly through the deployed gateway; those IDs only open local details if matching local envelopes also exist.

```bash
cd supabase/functions/memory_gateway
npm run demo:seed
```

The seed script signs in with `supabase.debug.email` / `supabase.debug.password`, upserts demo records through `memory.gateway.url`, runs searches for `startup event`, `flight receipt`, and `recipe`, and inspects Atlas for banned fields when `MONGODB_ATLAS_URI` is available in `.env.local`.

Expected backend MVP output includes:

```text
gateway_health_atlas_ok=yes
search="startup event" count=<non-zero> top=demo-startup-event-01
search="flight receipt" count=<non-zero> top=demo-flight-receipt-01
search="recipe" count=<non-zero> top=demo-recipe-01
atlas_docs_found=20
atlas_banned_field_count=0
demo_seed_ok=yes
```

The seeded dataset covers:
   - product
   - event/ticket
   - receipt/order
   - recipe
   - place/travel
   - read/watch later
   - chat action

## Demo Script

1. Use the debug app seed or capture at least 20 real local envelopes.
2. Confirm compact index sync runs with the memory index setting enabled.
3. Inspect Atlas documents for banned fields. The backend seed script performs this automatically when Atlas env is present; for debug-app seeding, inspect sampled Atlas records for the same banned fields listed below.
4. Open Library.
5. Search `startup event`, `flight receipt`, and `recipe`.
6. Tap at least 3 results and verify each opens the local capture detail.
7. Optional stretch after Library is working - Ask Orbit:
   - `What was that startup event I saved?`
   - `Which receipts did I save recently?`
   - `What recipe did I want to try?`
8. Verify every answer includes citations and View Capture actions.
9. Ask an unsupported question.
10. Verify Orbit refuses or shows cited nearest matches.
11. Disable network and verify Diary/detail/Active Intent still work.

Current real-device checkpoint:

- S24 debug install renders Diary / Library / Orbit bottom navigation.
- Settings has a debug-only `Seed demo memories` row that creates 20 real local Room envelopes.
- Library search now has two safety layers:
  - cloud results are filtered through local `:ml` lookup before display, so Atlas-only demo/stale records cannot produce dead `View Capture` links.
  - local Room search is merged as a fallback, so the MVP remains usable when Atlas sync lags.
- S24 screenshots on 2026-05-30 proved:
  - Library search for `startup` returns local-backed rows.
  - `View Capture` opens the saved local detail screen.
  - the opened detail shows the seeded capture text and provenance.
- Memory gateway endpoint is deployed and reachable; unauthenticated curl returns the expected `401 UNAUTHORIZED`.

2026-05-30 MVP result:

```text
manual_s24_library_search=startup
manual_s24_result_source=local_room_fallback
manual_s24_view_capture=opened_saved_detail
manual_s24_mvp_loop=pass
manual_s24_search_reschedule=reschedule_and_rescheduling_both_match
manual_s24_duplicate_collapse=dentist_rows_collapsed_to_one
automated_ask_demo=startup_event_flight_receipt_recipe_answered_with_citations
automated_ask_refusal=passport_number_insufficient_evidence
physical_s24_ask_retest=not_run_no_adb_devices_attached_2026_06_02
ask_status=limited_local_cited_retrieval_preview_not_semantic_vector_or_llm_ask
semantic_ask_status=deferred_to_spec_005A
latest_debug_apk_sha256=77aa34f8b7f2cd5cc6262614b04dcebfd772c16791c62d91ac389ad4387efab9
latest_debug_apk_sha256_with_ask_orbit=00ddf61f45ff8d8f83399d9655ac9644846bf68eb1d4577e6270516d974ba176
latest_debug_apk_sha256_with_ask_orbit_quality_fixes=206ce7913bd409a50598d1e0f2e55f056f22c74246799f67aa3e5166af8d2fbc
latest_debug_apk_sha256_with_ask_orbit_compact_citations=4cfcac695702db528003021663f0d84dedb0ed8c3ad53e321441acd10ec94118
latest_debug_apk_sha256_with_unified_search_ui=71167e32fdd94f6cd1ddce2595272f89e947864d2d2c02478156fc5a311cbcab
latest_debug_apk_sha256_with_neutral_search_controls=f675c4d8db80ac9602dd993e1bc2488acaac210116e33b8b85ebf040aef3fd33
latest_debug_apk_sha256_with_quiet_followups=955e3bd135c2e81ab9689aff73aefae6b3cc4328fc245aa08eda3698fb29179f
latest_debug_apk_sha256_with_followup_contract=9d44cc9a6553a29d86aeba3d2d5eff2d1ca7e558cbc2f7af3f0d6cd29fecf6b6
latest_debug_apk_sha256_with_orbit_scroll=741190dcd8619a2d64c6eff3b0bae721f17862fd43e88e676c360564faa7f52e
latest_debug_apk_sha256_with_followup_dedupe=961ec1972c9499dc5d8903a31fe2198e93192a1f434bd0a50d88760e83eda49a
latest_debug_apk_sha256_with_understanding_dedupe=414f0de99925c5381a8f5af4f2abaf284a49ed92d81fc6d6794d2e7f1103b771
latest_debug_apk_sha256_with_duplicate_audit=6380963ca6e2772337f3207416487dd7f4ee968a5c75751cf3128f5b711b7ed5
latest_debug_apk_sha256_with_diary_followups_removed=fbcc77dbde56ae88210994cec131cbf8857c341d1c886f0e463a50b1bac78ea4
latest_debug_apk_sha256_with_note_context_search=6a994f7bd1812f5c72e8b75832cd8aed9c095dc9c81d1df547928f8abfabb563
latest_debug_apk_sha256_with_spec005_closure=b1d9e70a0ee03312933289db39d126283b2d2df4164ff129b422ddc581e3d26c
latest_debug_apk_sha256_with_ask_false_positive_guardrails=621a24ec4d398cbf96ce5810daa4231ee71073f310212e7a2ef4a09f6d11ad98
latest_debug_apk_note=content-salient titles prefer cancellation/reschedule/status sentences; Library local search stems reschedule/rescheduling and collapses duplicate-looking results; global labels use body sans while mono remains explicit; Orbit tab includes thin cited Ask panel; Ask resets when leaving Orbit, answers only from local matching captures, maps cancelled to reschedule/postpone-style captures, uses compact citation rows, Library/Ask use matching compact neutral search controls with quiet surface colors, Follow-ups now surfaces only concrete next-action captures instead of every Active Intent sidecar, expanded Orbit tab content scrolls, duplicate Follow-ups collapse by normalized content while keeping the newest row, duplicate screenshot-derived understanding content no longer projects another Active Intent, duplicate-understanding suppression writes a compact no-raw-text audit marker, Diary no longer renders Follow-ups, Follow-ups content dedupe now tolerates clipped/OCR-drift token differences, Library/Ask local search includes user note/context text, note matches can drive result title/summary/evidence, multi-token searches such as qr code require all meaningful tokens instead of matching weak code-only results, and Spec 005 closure adds repository-level proof that the limited local Ask preview answers three controlled cited demo questions and refuses unsupported questions.
closeout_branch=feature/005-atlas-memory-index-search-20260530
closeout_backend_gate_2026_06_03=typecheck_passed_unit_17_passed
closeout_android_gate_2026_06_03=compile_unit_lint_androidtest_compile_assemble_passed
closeout_apk_sha256_2026_06_03=621a24ec4d398cbf96ce5810daa4231ee71073f310212e7a2ef4a09f6d11ad98
```

2026-06-03 Ask status correction:

- Ask in this branch is deterministic local cited retrieval. It does not use embeddings, Atlas Vector Search, a local model, or an LLM answer synthesizer.
- Screenshot testing showed the token-only path can rank weak keyword matches for real questions such as startup event, flight receipt, and unsupported passport-number queries.
- Spec 005A owns the semantic retrieval and grounded Ask upgrade: embedding policy, vector index, hybrid retrieval, answer/refusal thresholds, and evaluation fixtures from the screenshot failures.

## Payload Inspection Checklist

Process-boundary check:

```bash
rg -n "OrbitDatabase|getInstance\\(|RoomMemoryIndexSource|intentEnvelopeDao|captureUnderstandingDao" \
  app/src/main/java/com/orbit/app/memory app/src/main/java/com/orbit/app/library app/src/main/java/com/orbit/app/net
```

Expected: direct Room access appears only in `RoomMemoryIndexSource` and `MemoryIndexSyncDelegate`, which are constructed by `EnvelopeRepositoryService` in `:ml`. `MemoryIndexSyncWorker` must bind to `IEnvelopeRepository.syncMemoryIndex(...)`; it must not open Room directly.

For sampled backend request bodies and Atlas documents, verify absent:

- raw screenshots
- image bytes
- full OCR bodies
- raw HTML
- prompts
- model responses
- access tokens
- refresh tokens
- Atlas URI
- local audit log rows

## Monday MVP Done Definition

- Branch exists: `feature/005-atlas-memory-index-search-20260530`.
- Spec Kit artifacts are locked.
- Backend can upsert/search/tombstone against Atlas in dev.
- Android can build compact payloads and send through `:net`.
- Library search shows local-backed cited results; cloud-only/stale Atlas hits are hidden.
- Diary/Library entry point is clear enough for demo use.
- Optional stretch: Ask Orbit answers or refuses with citations.
- No banned fields appear in Atlas.
- Local core product works with Atlas down.
