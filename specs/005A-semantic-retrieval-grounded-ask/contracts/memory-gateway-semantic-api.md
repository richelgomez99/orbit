# Contract: Memory Gateway Semantic Retrieval API

**Spec**: `005A-semantic-retrieval-grounded-ask`

## Boundary

Android continues to call the memory gateway only through `INetworkGateway.callMemoryGateway(...)` in the `:net` process. Android sends JSON-in-string parcels. The backend owns Atlas and embedding-provider credentials.

No Android code may import MongoDB drivers, OpenAI clients, Atlas URIs, or provider API keys.

## New Request Types

### `memory_embed_stale`

Dev/admin operation for stale compact memory records.

```json
{
  "type": "memory_embed_stale",
  "requestId": "550e8400-e29b-41d4-a716-446655440000",
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
  "requestId": "550e8400-e29b-41d4-a716-446655440000",
  "embedded": 20,
  "skipped": 0,
  "failed": 0,
  "modelLabel": "openai:text-embedding-3-small",
  "dimensions": 1536
}
```

### `memory_semantic_search`

```json
{
  "type": "memory_semantic_search",
  "requestId": "550e8400-e29b-41d4-a716-446655440001",
  "payload": {
    "query": "startup event",
    "filters": {},
    "limit": 10,
    "mode": "hybrid"
  }
}
```

Response:

```json
{
  "type": "memory_semantic_search_response",
  "requestId": "550e8400-e29b-41d4-a716-446655440001",
  "results": [
    {
      "envelopeId": "demo-startup-event-01",
      "rank": 1,
      "score": 0.91,
      "semanticScore": 0.84,
      "lexicalScore": 0.72,
      "contextScore": 0.2,
      "retrievalMode": "hybrid",
      "embeddingModel": "openai:text-embedding-3-small",
      "title": "Startup event ticket",
      "summary": "Founder event ticket for Monday",
      "dayLocal": "2026-05-30",
      "createdAtMillis": 1780000000000,
      "intent": "REFERENCE",
      "sourceAppLabel": "Gmail",
      "domain": "example.com",
      "matchedEvidence": []
    }
  ]
}
```

### `memory_grounded_ask`

```json
{
  "type": "memory_grounded_ask",
  "requestId": "550e8400-e29b-41d4-a716-446655440002",
  "payload": {
    "question": "What is my passport number?",
    "filters": {},
    "limit": 5,
    "allowSynthesis": false
  }
}
```

Sensitive refusal response:

```json
{
  "type": "memory_grounded_ask_response",
  "requestId": "550e8400-e29b-41d4-a716-446655440002",
  "answer": {
    "status": "sensitive_refusal",
    "answer": "I could not find saved evidence for that.",
    "citations": [],
    "candidates": [],
    "modelLabel": "hybrid/openai:text-embedding-3-small",
    "retrievalMode": "hybrid",
    "confidence": 0.0,
    "limitations": ["Sensitive identifier questions require exact cited evidence."]
  }
}
```

## Error Codes

Extend the existing memory gateway error enum with:

- `EMBEDDING_PROVIDER_UNAVAILABLE`
- `VECTOR_INDEX_UNAVAILABLE`
- `VECTOR_DIMENSION_MISMATCH`
- `SEMANTIC_SEARCH_UNAVAILABLE`
- `GROUNDED_ASK_UNAVAILABLE`

Errors must use bounded public messages and operator logs without raw memory text.

## Ranking Contract

Hybrid ranking must be deterministic for a fixed response set:

```text
finalScore =
  semanticWeight * normalizedSemanticScore
  + lexicalWeight * normalizedLexicalScore
  + contextNoteBonus
  + exactPhraseBonus
  + sourceTypeBonus
  + recencyTieBreaker
```

Implementation may tune weights, but tests must pin the screenshot-derived fixtures.

## Refusal Contract

Ask returns `answered` only when:

- at least one local-backed citation exists;
- top score exceeds the configured threshold;
- top result is sufficiently separated from weak/noisy candidates for sensitive questions;
- sensitive identifier policy allows the answer;
- every answer claim maps to one or more citations.

Otherwise return:

- `insufficient_evidence` for normal low-confidence questions;
- `sensitive_refusal` for sensitive identifier questions without exact evidence;
- `provider_unavailable` when semantic/LLM provider fails and no acceptable fallback exists.
