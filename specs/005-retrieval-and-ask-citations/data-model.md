# Data Model: Atlas Memory Index + Cited Library

**Spec**: [spec.md](spec.md)
**Plan**: [plan.md](plan.md)
**Date**: 2026-05-30

## Overview

Spec 005 introduces a cloud index model, not a replacement database. Local Room entities remain authoritative. Atlas documents are compact, user-scoped projections from local envelopes and Spec 004 sidecars.

## Atlas Collection: `memory_items`

Purpose: Searchable compact memory record for one source envelope.

Unique key:

- `{ userId: 1, envelopeId: 1 }`

### Fields

| Field | Type | Required | Notes |
| --- | --- | --- | --- |
| `_id` | ObjectId | yes | Atlas-generated |
| `userId` | string | yes | From verified Supabase JWT, never client supplied |
| `envelopeId` | string | yes | Local Room envelope id |
| `schemaVersion` | number | yes | Start at `1` |
| `kind` | string | yes | `REGULAR`, `DIGEST`, `DERIVED` if indexed |
| `dayLocal` | string | yes | `YYYY-MM-DD` |
| `createdAtMillis` | number | yes | Source envelope created time |
| `updatedAtMillis` | number | yes | Last index update time |
| `intent` | string | yes | Envelope intent or Active Intent category |
| `contentType` | string | yes | Text/image/link/derived display type |
| `title` | string | no | <= 140 chars |
| `summary` | string | no | <= 500 chars |
| `sourceAppLabel` | string | no | Display label only |
| `appCategory` | string | no | Existing app category |
| `canonicalUrl` | string | no | If already known |
| `domain` | string | no | If already known |
| `tags` | string[] | yes | Compact normalized tags, max 20 |
| `evidence` | MemoryEvidenceSnippet[] | yes | Max 5 snippets |
| `compactText` | string | yes | Concatenated capped title/summary/snippets for lexical search, <= 1600 chars |
| `localContentHash` | string | no | Digest only, never raw body |
| `embedding` | number[] | no | Deferred until vector policy is locked |
| `embeddingModel` | string | no | Required iff `embedding` present |
| `embeddingDimensions` | number | no | Required iff `embedding` present |
| `tombstonedAt` | number | no | Hidden from normal results when set |
| `deletedAt` | number | no | Optional hard-delete/export lifecycle marker |

### Banned Fields

The document MUST NOT contain:

- `rawScreenshot`
- `imageBytes`
- `rawOcr`
- `ocrText`
- `rawHtml`
- `clipboardText`
- `prompt`
- `modelResponse`
- `auditLog`
- `accessToken`
- `refreshToken`
- `mongodbUri`
- Any field ending in `Secret`, `Token`, or `Key`

## Embedded Entity: `MemoryEvidenceSnippet`

Purpose: Capped evidence/citation unit.

| Field | Type | Required | Notes |
| --- | --- | --- | --- |
| `kind` | string | yes | `TITLE`, `SUMMARY`, `OCR_HINT`, `URL_METADATA`, `APP_CONTEXT`, `ACTIVE_INTENT`, `NOTE`, `DERIVED` |
| `label` | string | yes | User-facing label, <= 80 chars |
| `excerpt` | string | no | <= 240 chars |
| `source` | string | yes | `envelope`, `understanding`, `continuation`, `note`, or `active_intent` |
| `confidence` | number | no | 0.0 to 1.0 |
| `hash` | string | no | Digest for the compact excerpt/source |
| `createdAtMillis` | number | no | Source event time |

## Backend Request: `MemoryUpsertRequest`

Client sends:

```json
{
  "type": "memory_upsert",
  "requestId": "uuid",
  "payload": {
    "item": {
      "envelopeId": "local-id",
      "schemaVersion": 1,
      "kind": "REGULAR",
      "dayLocal": "2026-05-30",
      "createdAtMillis": 1780150000000,
      "intent": "READ_OR_WATCH_LATER",
      "contentType": "image",
      "title": "Startup event flyer",
      "summary": "Saved event details for later review.",
      "sourceAppLabel": "Chrome",
      "appCategory": "browser",
      "tags": ["event", "startup"],
      "evidence": [],
      "compactText": "Startup event flyer Saved event details for later review."
    }
  }
}
```

Server behavior:

- Verify JWT.
- Ignore any client-supplied `userId`.
- Validate banned fields recursively.
- Upsert by authenticated `userId` + `envelopeId`.
- Return upsert status and indexed timestamp.

## Backend Request: `MemoryTombstoneRequest`

Purpose: Hide deleted/local-invalidated records from results.

Fields:

- `requestId`
- `payload.envelopeId`
- `payload.reason`: `LOCAL_DELETE`, `USER_DISABLED_CLOUD`, `SOURCE_INVALIDATED`, `HARD_PURGE`
- `payload.tombstonedAtMillis`

Server behavior:

- Verify JWT.
- Update only authenticated user's matching document.
- Return `matched` and `modified`.

## Backend Request: `MemorySearchRequest`

Fields:

- `requestId`
- `payload.query`: string, 1-500 chars
- `payload.filters.dayLocalStart`
- `payload.filters.dayLocalEnd`
- `payload.filters.intent`
- `payload.filters.appCategory`
- `payload.limit`: 1-20, default 10

Server behavior:

- Verify JWT.
- Search only `userId`.
- Exclude `tombstonedAt != null`.
- Return ranked `MemorySearchResult[]`.

## Backend Request: `MemoryAskRequest`

Fields:

- `requestId`
- `payload.question`: string, 1-1000 chars
- `payload.filters`
- `payload.limit`: 1-10, default 5

Server behavior:

- Verify JWT.
- Retrieve memory records.
- If insufficient evidence, return `status=insufficient_evidence` plus cited candidates.
- If sufficient, return answer with citations. MVP can be extractive/deterministic; LLM generation must use only compact records.

## Backend Response: `MemorySearchResult`

| Field | Type | Notes |
| --- | --- | --- |
| `envelopeId` | string | Opens local detail |
| `rank` | number | 1-based |
| `score` | number | Backend-specific ranking score |
| `title` | string | Capped |
| `summary` | string | Capped |
| `dayLocal` | string | Display date |
| `createdAtMillis` | number | Display time |
| `intent` | string | Filter/display |
| `sourceAppLabel` | string? | Display |
| `domain` | string? | Display |
| `matchedEvidence` | MemoryEvidenceSnippet[] | Max 3 snippets |

## Backend Response: `AskOrbitAnswer`

| Field | Type | Notes |
| --- | --- | --- |
| `status` | string | `answered` or `insufficient_evidence` |
| `answer` | string | <= 1200 chars, empty or refusal text when insufficient |
| `citations` | Citation[] | 1-5 for answered responses |
| `candidates` | MemorySearchResult[] | Used for fallback |
| `modelLabel` | string | `deterministic/extractive` or gateway model label |

## Citation

| Field | Type | Notes |
| --- | --- | --- |
| `citationId` | string | Stable id in response |
| `envelopeId` | string | Local detail target |
| `title` | string | Display title |
| `excerpt` | string | <= 240 chars |
| `dayLocal` | string | Display date |
| `sourceAppLabel` | string? | Display |

## Local Audit Event Details

Audit rows stay in Room only. Suggested event names:

- `MEMORY_INDEX_UPSERTED`
- `MEMORY_INDEX_TOMBSTONED`
- `MEMORY_SEARCH_REQUESTED`
- `MEMORY_ASK_REQUESTED`
- `MEMORY_GATEWAY_FAILED`
- `MEMORY_SYNC_SKIPPED`

Details JSON should include:

- `provider`: `mongodb_atlas`
- `endpoint`: `upsert`, `tombstone`, `search`, `ask`
- `envelopeId` when applicable
- `requestId`
- `payloadDigest` or `queryDigest`
- `resultCount`
- `latencyMs`
- `outcome`
- `errorKind` when applicable

Never include raw query text if a digest is sufficient for audit. Never include memory body text in the audit details.
