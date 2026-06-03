# Data Model: Semantic Retrieval And Grounded Ask

**Spec**: `005A-semantic-retrieval-grounded-ask`
**Date**: 2026-06-03

## Existing Base Entity: `MemoryIndexItem`

Spec 005 already stores compact user-scoped memory records in Atlas and Kotlin DTOs:

| Field | Type | Notes |
| --- | --- | --- |
| `userId` | string | Backend-authenticated Supabase subject. Never client-supplied. |
| `envelopeId` | string | Local Room envelope provenance. |
| `schemaVersion` | number | Current compact-memory schema is `1`. |
| `kind` | string | Capture or derived memory kind. |
| `dayLocal` | `YYYY-MM-DD` | Local day for filtering/tie-breaks. |
| `createdAtMillis` | number | Capture creation time. |
| `intent` | string | Current intent/category. |
| `contentType` | string | Text, screenshot, URL, etc. |
| `title` | string? | Capped display title. |
| `summary` | string? | Capped summary. |
| `sourceAppLabel` | string? | App/source label. |
| `appCategory` | string? | App category/filter. |
| `canonicalUrl` | string? | Capped canonical URL when known. |
| `domain` | string? | URL domain when known. |
| `tags` | string[] | Capped tags. |
| `evidence` | `MemoryEvidenceSnippet[]` | Up to 5 capped snippets. |
| `compactText` | string | Capped compact searchable text. |
| `localContentHash` | string? | Local duplicate/debug hash. |
| `tombstonedAt` | number? | Excluded from visible search. |

## Added Fields: Embedded Memory

These fields are added to Atlas documents and Kotlin/TypeScript DTOs only after policy is locked.

| Field | Type | Required | Notes |
| --- | --- | --- | --- |
| `embedding` | number[] | no | 1536 floats for `openai:text-embedding-3-small`; omitted when not embedded or disabled. |
| `embeddingModel` | string | iff `embedding` | Initial value `openai:text-embedding-3-small`. |
| `embeddingDimensions` | number | iff `embedding` | Initial value `1536`. |
| `embeddingInputHash` | string | iff `embedding` | Digest of compact embedding input, not raw text. |
| `embeddedAtMillis` | number | iff `embedding` | Backend embedding time. |
| `embeddingStatus` | enum | yes | `none`, `ready`, `failed`, `stale`, `disabled`. |
| `embeddingErrorCode` | string? | no | Bounded code only, no raw provider body. |

Rules:

- `embedding` is never generated from raw screenshot bytes, full OCR, raw HTML, full clipboard body, prompt, model response, audit row, access token, refresh token, API key, or Atlas URI.
- `embeddingInputHash` changes when compact title/summary/tags/context/evidence/compactText changes.
- Documents with mismatched `embeddingDimensions` or `embeddingModel` are treated as stale.

## Entity: `EmbeddingPolicy`

Stored as backend config and mirrored in docs/tests.

| Field | Type | Initial Value |
| --- | --- | --- |
| `provider` | string | `openai` |
| `model` | string | `text-embedding-3-small` |
| `modelLabel` | string | `openai:text-embedding-3-small` |
| `dimensions` | number | `1536` |
| `similarity` | string | `cosine` |
| `inputMaxChars` | number | `2000` or lower after implementation validation |
| `allowedInputFields` | string[] | `title`, `summary`, `tags`, `user note/context`, `source labels`, `capped evidence`, `compactText` |
| `consentCategory` | string | `cloud_memory_index_semantic` |

## Request: `memory_embed_stale`

Backend-only or admin/dev operation to embed stale compact memory records.

```json
{
  "type": "memory_embed_stale",
  "requestId": "uuid-v4",
  "payload": {
    "limit": 50,
    "dryRun": false
  }
}
```

Response:

```json
{
  "type": "memory_embed_stale_response",
  "requestId": "uuid-v4",
  "embedded": 20,
  "skipped": 2,
  "failed": 0,
  "modelLabel": "openai:text-embedding-3-small",
  "dimensions": 1536
}
```

## Request: `memory_semantic_search`

```json
{
  "type": "memory_semantic_search",
  "requestId": "uuid-v4",
  "payload": {
    "query": "flight receipt",
    "filters": {
      "dayLocalStart": null,
      "dayLocalEnd": null,
      "intent": null,
      "appCategory": null
    },
    "limit": 10,
    "mode": "hybrid"
  }
}
```

`mode` values:

- `hybrid`: lexical + vector + local policy metadata.
- `vector_only`: dev/eval only.
- `lexical_only`: fallback/eval only.

Response result fields:

| Field | Type | Notes |
| --- | --- | --- |
| `envelopeId` | string | Local provenance. |
| `rank` | number | Final rank. |
| `score` | number | Final hybrid score. |
| `semanticScore` | number? | Vector similarity score when available. |
| `lexicalScore` | number? | Deterministic lexical score. |
| `contextScore` | number? | User note/context bonus. |
| `recencyScore` | number? | Tie-breaker only. |
| `retrievalMode` | string | `hybrid`, `vector_only`, `lexical_only`, `local_fallback`. |
| `embeddingModel` | string? | Model used for semantic score. |
| `title` | string? | Display title. |
| `summary` | string? | Display summary. |
| `matchedEvidence` | `MemoryEvidenceSnippet[]` | Capped citation evidence. |
| `duplicateRepresentativeId` | string? | Representative if duplicate collapse happened. |
| `localExists` | boolean? | Android-side gate before display. |

## Request: `memory_grounded_ask`

```json
{
  "type": "memory_grounded_ask",
  "requestId": "uuid-v4",
  "payload": {
    "question": "Which flight receipt did I save recently?",
    "filters": {},
    "limit": 5,
    "allowSynthesis": false
  }
}
```

Answer fields:

| Field | Type | Notes |
| --- | --- | --- |
| `status` | enum | `answered`, `insufficient_evidence`, `sensitive_refusal`, `provider_unavailable`. |
| `answer` | string | Short answer or refusal. |
| `citations` | `Citation[]` | Required for `answered`. |
| `candidates` | `HybridRetrievalResult[]` | Optional nearest matches for non-sensitive insufficiency. |
| `modelLabel` | string | Retrieval/answer model label. |
| `retrievalMode` | string | Hybrid/fallback mode. |
| `confidence` | number | 0-1 calibrated branch-local confidence. |
| `limitations` | string[] | Short limitations, no raw debug data. |

## Evaluation Fixture: `RetrievalEvalFixture`

```json
{
  "id": "flight-receipt-top1",
  "query": "Which flight receipt did I save recently?",
  "kind": "ask",
  "expectedStatus": "answered",
  "expectedTopEnvelopeId": "demo-flight-receipt-01",
  "disallowedTopEnvelopeIds": ["demo-mom-flight-chat-01", "demo-dentist-travel-conflict-01"],
  "requiresCitation": true
}
```

Fixture categories:

- `library_search`
- `ask`
- `refusal`
- `fallback`
- `dedupe`

## Atlas Indexes

Existing Spec 005 indexes remain:

- `uniq_user_envelope`
- `user_day_created`
- `user_intent_created`
- `user_tombstone`

005A adds Atlas Vector Search index `memory_embedding_v1`:

- vector path: `embedding`
- dimensions: `1536`
- similarity: `cosine`
- filters: `userId`, `tombstonedAt`, `intent`, `appCategory`, `dayLocal`

## Audit Events

Add or extend local-only audit event names:

- `MEMORY_EMBED_REQUESTED`
- `MEMORY_EMBED_COMPLETED`
- `MEMORY_EMBED_FAILED`
- `MEMORY_SEMANTIC_SEARCH_REQUESTED`
- `MEMORY_SEMANTIC_SEARCH_COMPLETED`
- `MEMORY_SEMANTIC_SEARCH_FALLBACK`
- `ASK_GROUNDED_REQUESTED`
- `ASK_GROUNDED_ANSWERED`
- `ASK_GROUNDED_REFUSED`

Audit payload rule: model labels, counts, latency, digests, request ID, result count, and outcome only. No raw query text if a digest is sufficient for the existing audit pattern; no raw memory text.
