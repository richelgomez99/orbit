# Implementation Plan: Atlas Memory Index + Cited Library

**Branch**: `feature/005-atlas-memory-index-search-20260530`
**Date**: 2026-05-30
**Spec**: [spec.md](spec.md)
**Input**: Speckit specify output for Atlas-backed Library/Search + cited Ask Orbit MVP.

## Summary

Build the first real Library surface by indexing compact memory records in MongoDB Atlas through a backend memory gateway. Android remains local-first: Room is authoritative, Atlas is an opt-in cloud index, and all network calls stay behind `:net`. Ask Orbit is a P2/stretch layer over retrieval, not the next required move.

The MVP sequence is:

1. Backend `memory_gateway` with JWT auth, Atlas client, user-scoped upsert/tombstone/search endpoints. Ask may exist as a stretch endpoint only after search works.
2. Android compact memory payload builder in `:ml` from existing envelope + Spec 004 sidecars.
3. Android `:net` memory gateway client + AIDL surface.
4. Library search UI over cited results.
5. Minimal Diary/Library/Orbit entry point if needed to make Library visible.
6. Stretch: Ask Orbit answer surface over retrieved records with citations and refusal behavior.

## Technical Context

**Languages/Versions**: Kotlin/Android, TypeScript/Node 20, MongoDB Atlas.
**Primary Dependencies**: Room/SQLCipher, AIDL, WorkManager, OkHttp in `:net`, Supabase JWT auth, MongoDB Node driver in backend.
**Storage**: Local Room v8 remains source of truth; Atlas `orbit_dev.memory_items` is a compact index.
**Testing**: Gradle JVM/unit/androidTest, custom lint, Vitest for backend.
**Target Platform**: Android app plus backend gateway function.
**Project Type**: Android mobile app + TypeScript backend gateway.
**Performance Goals**: Library query returns usable results under 2s on normal network; sync payload stays bounded under 16 KiB per memory item for MVP.
**Constraints**: No Android Atlas credentials; no raw artifact upload; no network outside `:net`; no cloud source of truth; Ask is not allowed to outrank Library/search.
**Scale/Scope**: MVP single-user/dev cluster first, with backend tests for multi-user isolation before alpha.

## Constitution Check

### Principle I - Local-First Supremacy

PASS. Room remains authoritative. Atlas unavailability degrades Library/Ask only. Capture, Diary, detail, and Active Intent continue locally.

### Principle III - Intent Before Artifact

PASS. Memory records are indexed around `IntentEnvelope` provenance, intent, source context, and compact understanding, not raw screenshots as isolated artifacts.

### Principle VI - Privilege Separation

PASS WITH IMPLEMENTATION GATE. Android memory network calls must be added only under `com.orbit.app.net.*` and exposed through `INetworkGateway` or a narrowly scoped `IMemoryGateway` binder hosted in `:net`. `:ml` builds compact payloads but does not open sockets.

### Principle VIII - Collect Only What You Use

PASS WITH PAYLOAD TESTS. Atlas fields are only those needed for Library, search filters, citations, tombstone propagation, and Ask grounding. Raw artifact bodies are explicitly banned.

### Principle IX - User-Sovereign Cloud Escape Hatch

CONDITIONAL PASS. This feature uses cloud storage/search, not only LLM inference. Development can proceed behind debug/dev opt-in, but external alpha requires cloud category controls and audit visibility before making full sovereignty claims.

### Principle X - Sovereign Cloud Storage

CONDITIONAL PASS. Atlas is a managed cloud index for MVP, not the final schema-per-tenant Orbit Cloud. This is acceptable as a dev/MVP backend if:

- Atlas stores compact derived index records only.
- The local audit log and consent ledger never leave the device.
- Every document is user-scoped.
- Export/delete hooks are represented as gateway contracts, even if UI is later.
- The plan records that Atlas is an MVP index, not the long-term full Orbit Cloud storage model.

### Principle XI - Consent-Aware Prompt Assembly

PASS WITH SCOPE LIMIT. Ask uses retrieval records returned from the memory gateway. Any LLM prompt assembly that crosses the cloud gateway must use compact, consent-filtered memory snippets and produce local audit rows. Server-side prompt assembly over raw stored user bodies is prohibited.

### Principle XII - Provenance Or It Did Not Happen

PASS. Every Atlas memory document and Ask citation carries local `envelopeId` provenance.

## Project Structure

```text
specs/005-retrieval-and-ask-citations/
├── spec.md
├── plan.md
├── research.md
├── data-model.md
├── quickstart.md
├── tasks.md
└── contracts/
    ├── memory-gateway-api.md
    └── android-memory-boundary.md

supabase/functions/memory_gateway/
├── api/memory.ts
├── index.ts
├── package.json
├── tsconfig.json
├── types.ts
├── lib/
│   ├── auth.ts
│   ├── atlas.ts
│   ├── errors.ts
│   ├── response.ts
│   └── schemas.ts
└── test/
    ├── auth.test.ts
    ├── atlas.test.ts
    ├── payload_safety.test.ts
    └── router.test.ts

app/src/main/aidl/com/orbit/app/net/ipc/
└── memory gateway parcels or extension to INetworkGateway

app/src/main/java/com/orbit/app/net/
└── MemoryGatewayClient.kt

app/src/main/java/com/orbit/app/data/
└── compact memory payload builder owned by :ml

app/src/main/java/com/orbit/app/library/
└── Library search UI, repository, ViewModel, state

app/src/main/java/com/orbit/app/orbit/
└── Ask Orbit cited answer surface when introduced in this spec
```

**Structure Decision**: Add a sibling backend gateway rather than mixing Atlas into `llm_gateway`, because memory index operations are not LLM capability calls and need different payload safety tests. Reuse auth/error/response patterns from `llm_gateway` where possible.

## Implementation Phases

### Phase 0 - Speckit Lock

Write and review `spec.md`, `plan.md`, `research.md`, `data-model.md`, `contracts/`, `quickstart.md`, and `tasks.md`. Confirm branch and Atlas setup.

### Phase 1 - Backend Memory Gateway

Create `supabase/functions/memory_gateway` with:

- Supabase JWT auth.
- MongoDB Atlas connection from `MONGODB_ATLAS_URI`.
- `POST /memory/upsert`
- `POST /memory/tombstone`
- `POST /memory/search`
- Optional stretch: `POST /memory/ask`
- payload validation and banned-field tests.

### Phase 2 - Android Boundary

Add a compact-memory sync boundary:

- `:ml` builds compact records from Room v8 envelope/understanding sidecars.
- `:net` sends records/search/ask requests.
- AIDL decision: extend the existing `INetworkGateway` with `callMemoryGateway(in MemoryGatewayRequestParcel)`.
  This keeps a single bound `:net` egress service, reuses the established caller-UID guard, and preserves the same JSON-in-string parcel pattern as the LLM gateway. A separate `IMemoryGateway` would add another service binding without improving privilege separation for the MVP.
- Audit rows are written locally through existing `:ml` repository/audit seams.
- No Atlas dependency enters Android.

### Phase 3 - Library

Add Library search UI and state:

- Query box.
- Filters for day, intent, source/app category, content type.
- Result list with title, summary, date, source label, citation chip, and View Capture.
- Offline/unavailable states.

### Phase 4 - Three-Pillar Entry Point

Add the smallest useful entry point for the vision:

- Diary remains chronological memory.
- Library hosts search and filters.
- Orbit can temporarily route to the existing Active Intent cleanup surface.

Do not build a marketing landing page or generic chat-first UI.

### Phase 5 - Ask Orbit With Citations (Stretch)

Add Ask flow using retrieved memory records:

- Retrieval first.
- Answer only from cited records.
- Refusal/fallback when insufficient evidence.
- View capture for every citation.

### Phase 6 - Validation and Dogfood

Run backend tests, Android compile/tests/lint, seed 20 capture demo, inspect Atlas documents, and record quickstart results.

## Validation Gates

Backend:

```bash
cd supabase/functions/memory_gateway
npm install
npm run typecheck
npm run test:unit
```

Android:

```bash
./gradlew :app:compileDebugKotlin
./gradlew :app:testDebugUnitTest
./gradlew :build-logic:lint:test
./gradlew :app:lintDebug
./gradlew :app:assembleDebug
```

Payload safety:

```bash
rg -n "MONGODB_ATLAS_URI|mongodb\\+srv|MongoClient" app/src || true
rg -n "rawScreenshot|rawOcr|rawHtml|prompt|modelResponse" supabase/functions/memory_gateway app/src/main/java/com/orbit/app
```

## Complexity Tracking

| Concern | Why Needed | Simpler Alternative Rejected Because |
| --- | --- | --- |
| New `memory_gateway` backend | Atlas credentials must remain server-side and memory operations differ from LLM calls | Putting Atlas in Android violates Principle VI and credential safety |
| Compact memory document | Search/Ask need cloud-indexable text | Uploading raw capture bodies violates Principles VIII/X/XII |
| Cited Ask stretch | Trust requires citations if Ask lands | Generic chatbot answers would violate provenance and product thesis |

## Review Gates

- Review spec against `VISION-2026-05-22.md` and this plan before implementation.
- Review payload schema before adding gateway code.
- Review Android process boundary before adding any network client or AIDL method.
- Review quickstart results before stacking Spec 019 UI shell work.
