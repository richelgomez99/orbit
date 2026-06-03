# Implementation Plan: Semantic Retrieval And Grounded Ask

**Branch**: `feature/005a-semantic-retrieval-grounded-ask-20260603` | **Date**: 2026-06-03 | **Spec**: [spec.md](spec.md)
**Input**: Feature specification from `/specs/005A-semantic-retrieval-grounded-ask/spec.md`

## Summary

Add semantic retrieval and grounded Ask on top of Spec 005's compact memory index. Backend generates embeddings from compact memory text, stores vector metadata in Atlas, queries Atlas Vector Search with user/tombstone filters, merges semantic and lexical scores, and returns cited results. Android keeps the existing Diary/Library/Orbit surfaces but prefers the semantic gateway when available and falls back to local cited retrieval when cloud/embeddings are disabled.

## Technical Context

**Language/Version**: Kotlin Android (JDK 21), TypeScript/Node 20 for `supabase/functions/memory_gateway`.
**Primary Dependencies**: Android Compose/Room/SQLCipher/AIDL/WorkManager, MongoDB Node driver, Zod, Vitest, OpenAI embeddings via backend HTTP/client.
**Storage**: Room + SQLCipher authoritative on device; MongoDB Atlas `orbit_dev.memory_items` compact index.
**Testing**: Gradle JVM/unit/lint/instrumented compile gates; Vitest backend unit tests.
**Target Platform**: Android debug MVP plus Vercel/Supabase memory gateway.
**Project Type**: Mobile app + backend gateway.
**Performance Goals**: Library/Ask should degrade quickly to local fallback if semantic gateway is unavailable; backend search limit <= 20; embedding batch bounded.
**Constraints**: No Android Atlas/OpenAI secrets; no raw artifact upload; no network outside `:net`; every answer cited or refused.
**Scale/Scope**: MVP corpus scale: hundreds to low thousands of compact memory items per user; branch must be correct before optimized.

## Constitution Check

- Principle I Local-first supremacy: PASS. Room remains source of truth; semantic cloud index degrades to local fallback.
- Principle III Intent before artifact: PASS. Context notes, intent, and compact evidence are part of embedding/ranking.
- Principle VI Privilege separation: PASS WITH GATE. Backend owns Atlas/OpenAI. Android routes only through `:net`.
- Principle VIII Collect only what you use: PASS WITH TESTS. Embedding input allow-list and banned-field tests are required.
- Principle IX User-sovereign cloud escape hatch: PASS. Local cited retrieval remains available.
- Principle XII Provenance or it did not happen: PASS. Every answer/result cites local envelope IDs and Android gates dead links.

## Project Structure

### Documentation

```text
specs/005A-semantic-retrieval-grounded-ask/
├── spec.md
├── plan.md
├── research.md
├── data-model.md
├── quickstart.md
├── contracts/
│   └── memory-gateway-semantic-api.md
└── tasks.md
```

### Source Code

```text
supabase/functions/memory_gateway/
├── types.ts
├── index.ts
├── lib/
│   ├── atlas.ts
│   ├── embeddings.ts
│   ├── hybridSearch.ts
│   ├── groundedAsk.ts
│   └── schemas.ts
├── scripts/
│   ├── create-vector-index.mjs
│   ├── embed-stale.mjs
│   └── eval-retrieval.mjs
└── test/
    ├── embedding_policy.test.ts
    ├── vector_search.test.ts
    ├── hybrid_search.test.ts
    └── grounded_ask.test.ts

app/src/main/java/com/orbit/app/memory/
├── MemoryModels.kt
├── MemoryGatewayDtos.kt
├── MemoryIndexSyncCoordinator.kt
└── MemoryAudit.kt

app/src/main/java/com/orbit/app/library/
├── LibraryRepository.kt
└── LibraryViewModel.kt

app/src/main/java/com/orbit/app/orbit/
├── AskOrbitRepository.kt
└── AskOrbitViewModel.kt

app/src/test/java/com/orbit/app/library/
app/src/test/java/com/orbit/app/orbit/
app/src/test/java/com/orbit/app/memory/
```

**Structure Decision**: Extend Spec 005 gateway and DTOs rather than adding a second backend. The product surface stays stable; retrieval internals change behind the existing Library/Ask repositories.

## Implementation Strategy

### Phase 0 - Spec Lock

Write the full 005A artifact set, then verify stale docs point to 005A before 006.

### Phase 1 - Backend Embedding Policy And Schema

Add embedding metadata to TypeScript/Kotlin DTOs and Zod schemas. Implement compact embedding input builder and tests that prove banned raw fields cannot enter the provider request.

### Phase 2 - Atlas Vector Index And Embedding Jobs

Add scripts for vector index creation and stale embedding backfill. Implement bounded backend batch embedding with model/dimension metadata and failure states.

### Phase 3 - Hybrid Semantic Search

Add `memory_semantic_search` endpoint. Use Atlas Vector Search when vectors exist, lexical fallback when not, and deterministic hybrid ranking. Preserve user/tombstone filters.

### Phase 4 - Grounded Ask

Add `memory_grounded_ask` endpoint. Gate answer/refusal by retrieval confidence and sensitive-identifier policy. Optional LLM synthesis may be added only after retrieval fixtures pass; otherwise extractive cited answer is acceptable.

### Phase 5 - Android Integration

Extend Android DTOs and repositories to prefer semantic/grounded endpoints when cloud semantic indexing is enabled, while preserving current local fallback and local-existence filtering.

### Phase 6 - Evaluation And APK

Add screenshot-derived fixtures across backend and Android tests. Run backend and Android gates. Build debug APK only after fixtures pass.

## Complexity Tracking

| Decision | Why Needed | Simpler Alternative Rejected Because |
| --- | --- | --- |
| Atlas Vector Search | Spec 005 token retrieval cannot rank semantic intent reliably. | More token heuristics would keep failing real Ask phrasing. |
| Backend embeddings | Android cannot hold provider secrets and local model manager is future work. | Android embeddings would violate boundary or pull Spec 022 forward. |
| Hybrid ranking | Vector-only search can be semantically nearby but wrong. | Pure vector ranking may repeat the startup/flight noise problem. |
| Sensitive refusal policy | Personal-memory Ask can expose unrelated numbers. | Generic low-confidence threshold is not strict enough for identifiers. |
