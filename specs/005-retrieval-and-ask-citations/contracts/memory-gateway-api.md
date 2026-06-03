# Contract: Memory Gateway API

**Spec**: [../spec.md](../spec.md)
**Owner**: `supabase/functions/memory_gateway`

## Transport

MVP uses JSON over HTTPS behind the same operational deployment style as `llm_gateway`.

All endpoints:

- Method: `POST`
- Auth: `Authorization: Bearer <Supabase JWT>`
- Success HTTP status: `200`
- Auth failure HTTP status: `401`
- Content-Type: `application/json`

The backend verifies JWT and derives `userId` from `sub`. Client-supplied `userId` is ignored or rejected.

## Standard Error

```json
{
  "type": "error",
  "requestId": "uuid-or-empty",
  "ok": false,
  "error": {
    "code": "UNAUTHORIZED",
    "message": "Token verification failed"
  }
}
```

Error codes:

- `UNAUTHORIZED`
- `VALIDATION_FAILED`
- `ATLAS_UNAVAILABLE`
- `NOT_FOUND`
- `INSUFFICIENT_EVIDENCE`
- `INTERNAL`

## `POST /memory/upsert`

Request:

```json
{
  "type": "memory_upsert",
  "requestId": "2e57810d-8858-40c7-a0c8-f1332a9458d5",
  "payload": {
    "item": {
      "envelopeId": "env_123",
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
      "evidence": [
        {
          "kind": "OCR_HINT",
          "label": "Clue",
          "excerpt": "Demo day, June 4, doors at 6 PM",
          "source": "understanding",
          "confidence": 0.78
        }
      ],
      "compactText": "Startup event flyer Saved event details for later review. Demo day, June 4, doors at 6 PM"
    }
  }
}
```

Response:

```json
{
  "type": "memory_upsert_response",
  "requestId": "2e57810d-8858-40c7-a0c8-f1332a9458d5",
  "ok": true,
  "data": {
    "envelopeId": "env_123",
    "upserted": true,
    "indexedAtMillis": 1780150005000
  }
}
```

Rules:

- Server stamps authenticated `userId`.
- Upsert key is `{ userId, envelopeId }`.
- Recursive banned-field validation runs before Atlas write.

## `POST /memory/tombstone`

Request:

```json
{
  "type": "memory_tombstone",
  "requestId": "uuid",
  "payload": {
    "envelopeId": "env_123",
    "reason": "LOCAL_DELETE",
    "tombstonedAtMillis": 1780150006000
  }
}
```

Response:

```json
{
  "type": "memory_tombstone_response",
  "requestId": "uuid",
  "ok": true,
  "data": {
    "envelopeId": "env_123",
    "matched": true,
    "modified": true
  }
}
```

## `POST /memory/search`

Request:

```json
{
  "type": "memory_search",
  "requestId": "uuid",
  "payload": {
    "query": "startup event",
    "filters": {
      "dayLocalStart": "2026-05-01",
      "dayLocalEnd": "2026-05-30",
      "intent": null,
      "appCategory": null
    },
    "limit": 10
  }
}
```

Response:

```json
{
  "type": "memory_search_response",
  "requestId": "uuid",
  "ok": true,
  "data": {
    "results": [
      {
        "envelopeId": "env_123",
        "rank": 1,
        "score": 8.4,
        "title": "Startup event flyer",
        "summary": "Saved event details for later review.",
        "dayLocal": "2026-05-30",
        "createdAtMillis": 1780150000000,
        "intent": "READ_OR_WATCH_LATER",
        "sourceAppLabel": "Chrome",
        "domain": "example.com",
        "matchedEvidence": []
      }
    ]
  }
}
```

Rules:

- Excludes tombstoned records.
- Max `limit` is 20.
- Response never includes `compactText` unless explicitly needed and capped.

## `POST /memory/ask`

Request:

```json
{
  "type": "memory_ask",
  "requestId": "uuid",
  "payload": {
    "question": "What was that startup event I saved?",
    "filters": {},
    "limit": 5
  }
}
```

Answered response:

```json
{
  "type": "memory_ask_response",
  "requestId": "uuid",
  "ok": true,
  "data": {
    "status": "answered",
    "answer": "You saved a startup event flyer for Demo Day on June 4.",
    "citations": [
      {
        "citationId": "c1",
        "envelopeId": "env_123",
        "title": "Startup event flyer",
        "excerpt": "Demo day, June 4, doors at 6 PM",
        "dayLocal": "2026-05-30",
        "sourceAppLabel": "Chrome"
      }
    ],
    "candidates": [],
    "modelLabel": "deterministic/extractive"
  }
}
```

Insufficient evidence response:

```json
{
  "type": "memory_ask_response",
  "requestId": "uuid",
  "ok": true,
  "data": {
    "status": "insufficient_evidence",
    "answer": "I could not find enough saved evidence to answer that.",
    "citations": [],
    "candidates": [],
    "modelLabel": "deterministic/extractive"
  }
}
```
