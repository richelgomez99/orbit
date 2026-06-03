# Contract: Android Memory Boundary

**Spec**: [../spec.md](../spec.md)

## Process Responsibilities

`:ml`:

- Reads Room v8 envelope, continuation, note, and Spec 004 sidecar data.
- Builds compact `MemoryIndexItem` payloads.
- Writes local audit rows for requested/completed/skipped memory operations.
- Does not open sockets.
- Does not include raw screenshots, full OCR, raw HTML, prompts, or model responses in outbound payloads.

`:net`:

- Holds the HTTP client.
- Sends memory gateway requests.
- Adds Supabase JWT auth through existing auth/session wiring.
- Returns typed responses over Binder.
- Does not read Room directly.
- Does not hold Atlas credentials.

`:ui`:

- Renders Library and Ask surfaces.
- Opens local envelope detail by `envelopeId`.
- Does not access Room or network directly.

`:capture`:

- No memory gateway access in MVP.

## AIDL Shape

Implementation may either extend `INetworkGateway` or introduce a narrowly scoped `IMemoryGateway` bound service in `:net`. Either choice must preserve the same invariant: callers pass JSON-in-string parcels and receive JSON-in-string parcels; `:net` is the sole egress process.

Required operations:

- `upsertMemoryIndex(String requestJson): String responseJson`
- `tombstoneMemoryIndex(String requestJson): String responseJson`
- `searchMemoryIndex(String requestJson): String responseJson`
- `askMemoryIndex(String requestJson): String responseJson`

## Payload Builder Rules

The compact builder in `:ml` must:

- Use the local envelope id as provenance.
- Prefer Spec 004 `CaptureUnderstandingEntity` title/summary/category when present.
- Include at most 5 evidence snippets.
- Cap fields according to `data-model.md`.
- Include a digest of the payload for audit.
- Return a skipped result when only banned/raw fields could make the item useful.

## Audit Rules

Local audit rows are required for:

- Upsert requested/completed/failed/skipped.
- Tombstone requested/completed/failed.
- Search requested/completed/failed.
- Ask requested/completed/failed.

Audit rows must not include raw query text or memory text when a digest and result count are sufficient.

## UI Rules

Library result rows must show:

- Title or fallback.
- Date.
- Source label/domain when present.
- Summary/excerpt.
- Citation indicator.
- View Capture action.

Ask answers must show:

- Answer text or insufficient-evidence fallback.
- Citation chips/cards.
- View Capture action per citation.

No Ask answer may hide its citations.
