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
npm run memory:embed-stale
```

Expected output:

```text
embedding_model=openai:text-embedding-3-small
embedding_dimensions=1536
embedded_count>=20
failed_count=0
```

## Retrieval Eval

```bash
cd supabase/functions/memory_gateway
npm run eval:retrieval
```

Required fixture outcomes:

```text
startup_event_top1=demo-startup-event-01
flight_receipt_top1=demo-flight-receipt-01
recipe_relevant=true
qr_code_context_match=true
reschedule_recall=true
cancelled_rescheduled_recall=true
passport_number_status=sensitive_refusal
duplicate_collapse=true
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

1. Install the latest debug APK.
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
backend_typecheck=
backend_unit_tests=
retrieval_eval=
android_unit_tests=
android_lint=
android_compile=
apk_sha256=
manual_s24_semantic_demo=
known_limits=
```
