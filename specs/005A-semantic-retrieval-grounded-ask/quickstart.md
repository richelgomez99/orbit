# Quickstart: Semantic Retrieval And Grounded Ask

**Spec**: [spec.md](spec.md)

## Environment

Backend `.env.local` for `supabase/functions/memory_gateway` needs:

```properties
MONGODB_ATLAS_URI=...
MONGODB_DB=orbit_dev
MONGODB_MEMORY_COLLECTION=memory_items
SUPABASE_URL=...
SUPABASE_JWT_SECRET=...
OPENAI_API_KEY=...
MEMORY_EMBEDDING_MODEL=text-embedding-3-small
MEMORY_EMBEDDING_DIMENSIONS=1536
```

Do not add `OPENAI_API_KEY`, Atlas URI, or provider secrets to Android `local.properties`.

## Preflight

```bash
rg -n "OPENAI_API_KEY|MONGODB_ATLAS_URI|mongodb\\+srv|MongoClient" app/src || true
```

Expected: no Android matches.

```bash
cd supabase/functions/memory_gateway
npm run typecheck
npm run test:unit
npm run eval:retrieval
```

## Vector Index Setup

Create or verify Atlas Vector Search index `memory_embedding_v1` on `memory_items`:

- vector path: `embedding`
- dimensions: `1536`
- similarity: `cosine`
- filters: `userId`, `tombstonedAt`, `intent`, `appCategory`, `dayLocal`

Expected script:

```bash
cd supabase/functions/memory_gateway
npm run vector:index
```

## Demo Seed And Embed

1. Install debug APK from Spec 005 or later.
2. Enable cloud memory index in Settings.
3. Seed local demo memories on device.
4. Run or wait for compact sync.
5. Embed stale records:

```bash
cd supabase/functions/memory_gateway
npm run memory:embed-stale -- --user-id <supabase-user-id>
```

Expected output:

```json
{
  "ok": true,
  "model": "openai:text-embedding-3-small",
  "dimensions": 1536,
  "embedded": 20,
  "failed": 0
}
```

## Retrieval Eval

```bash
cd supabase/functions/memory_gateway
npm run eval:retrieval
```

Required fixture outcomes:

```text
ok=true
passed=6
total=6
failures=[]
```

## Android Validation

```bash
./gradlew :app:compileDebugKotlin
./gradlew :app:testDebugUnitTest --tests "com.orbit.app.library.*" --tests "com.orbit.app.orbit.*" --tests "com.orbit.app.memory.*"
./gradlew :build-logic:lint:test :app:lintDebug
./gradlew :app:compileDebugAndroidTestKotlin
./gradlew :app:assembleDebug
```

## Manual S24 Demo

1. Connect the S24 and run:

```bash
specs/005A-semantic-retrieval-grounded-ask/scripts/install-and-seed-device.sh
```

2. Open Library and search:
   - `qr code`
   - `startup event`
   - `flight receipt`
   - `recipe`
   - `reschedule`
3. Verify expected local-backed capture appears at or near the top and opens detail.
4. Open Orbit and ask:
   - `What startup event did I save?`
   - `Which flight receipt did I save recently?`
   - `What recipe did I want to try?`
   - `What is my passport number?`
5. Verify first three cite saved captures and passport-number refuses.
6. Disable network/cloud memory index and verify Diary/detail/Follow-ups still work and Ask/Library degrade to local cited retrieval.

## Closeout Evidence

Record in this file after implementation:

```text
backend_typecheck=pass (npm run typecheck)
backend_unit_tests=pass (39 tests)
retrieval_eval=pass (6/6)
latest_backend_gate=pass 2026-06-03 (`npm run typecheck && npm run test:unit && npm run eval:retrieval`)
non_s24_closeout_script=pass 2026-06-03 (`specs/005A-semantic-retrieval-grounded-ask/scripts/verify-non-s24-closeout.sh`)
live_semantic_smoke=pass; qr code -> demo-qr-customers-01, reschedule -> demo-calendar-01, flight receipt -> demo-flight-receipt-01, passport question -> sensitive_refusal
memory_gateway_deploy=pass; production_alias=https://orbit-memory-gateway.vercel.app; deployment=https://orbit-memory-gateway-f7nk21ubl-richels-projects-834ef114.vercel.app; deployment_id=dpl_6inFrjavbbapv6HF8Kg3o6VKKehX
android_unit_tests=pass (full :app:testDebugUnitTest)
android_lint=pass (:app:lintDebug)
android_compile=pass (:app:compileDebugKotlin, :app:compileDebugAndroidTestKotlin, :app:assembleDebug)
latest_full_local_android_ci=pass 2026-06-03 (`./gradlew :app:compileDebugKotlin :app:testDebugUnitTest :build-logic:lint:test :app:lintDebug :app:compileDebugAndroidTestKotlin :app:assembleDebug`)
android_connected_smoke=pass on Pixel_10_Pro Android 17 emulator after AndroidX Test upgrade; OrbitHomeNavigationTest, LibraryScreenTest, AskOrbitPanelTest
rebuilt_apk_emulator_install_seed=pass; install-and-seed-device.sh --reset-data installed sha256 7a80f2dc94b00766177eec6f80393bf4289c39c8252cd6f3a44d925c1369c885 and seeded 20 demo envelopes
rebuilt_apk_repeat_seed=pass; second seed broadcast on same emulator data logged seeded demo envelopes count=20 with no DebugDemoSeedReceiver failure
rebuilt_apk_connected_smoke=pass; same 4 targeted connected tests passed after rebuilt APK install/seed
build_logic_lint_tests=pass (:build-logic:lint:test)
android_secret_scan=pass (no OPENAI_API_KEY/MONGODB_ATLAS_URI/mongodb+srv/MongoClient hits under app/src)
android_network_boundary_scan=pass (no OkHttpClient/HttpURLConnection/Socket/HttpClient constructors outside approved net path scan)
debug_build_config=pass (MEMORY_GATEWAY_URL=https://orbit-memory-gateway.vercel.app/memory)
apk_path=dist/orbit-mvp-debug-20260603-005b.apk
apk_sha256=7a80f2dc94b00766177eec6f80393bf4289c39c8252cd6f3a44d925c1369c885
debug_seed_idempotency=pass (debug demo seed uses stable text; repeat helper runs hit duplicate suppression instead of timestamped duplicates)
pre_s24_gstack_review=pass; fixed one issue before phone validation: timestamped debug seed text would have created repeat demo duplicates
emulator_smoke_screenshot=dist/device-smoke/pixel10pro-diary-launch-20260603.png
rebuilt_apk_emulator_screenshot=dist/device-smoke/pixel10pro-rebuilt-005b-20260603.png
manual_s24_helper=pass (bash -n; no-device path fails cleanly)
manual_s24_install_seed=pass on S24 SM-S928U1; install-and-seed-device.sh installed/launched sha256 7a80f2dc94b00766177eec6f80393bf4289c39c8252cd6f3a44d925c1369c885 and seeded demo memories
manual_s24_semantic_demo=pass by user report; uploaded screenshots show grounded Ask recipe/startup-event answers with cited captures, and user reported Library/Orbit actions worked
intentresolver_source_label_fix=pass; focused JVM tests passed, fixed APK installed on S24, and user confirmed no more `from IntentResolver`
known_limits=Android filters cloud citations to local-backed envelopes; cloud semantic retrieval is available only after compact sync + embedding backfill.
```

See `manual-s24-validation.md` for the exact phone checklist.
See `mvp-closeout-audit.md` for the requirement-by-requirement evidence ledger.
