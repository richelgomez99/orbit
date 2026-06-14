# Data Model: Resolution Semantics

## ResolutionReceipt

Room entity: `resolution_receipt`

| Field | Type | Notes |
| --- | --- | --- |
| `id` | `String` | UUID or deterministic id for idempotent duplicate paths. |
| `targetType` | `ResolutionTargetType` | `ENVELOPE`, `ACTIVE_INTENT`, `ACTION_PROPOSAL`, `TODO_LIST`, `TODO_ITEM`, `MEMORY_CANDIDATE`, `GRAPH_FACT`, `DUPLICATE_ATTEMPT`. |
| `targetId` | `String` | Compact local id for the target. |
| `envelopeId` | `String?` | Source/canonical envelope when available. |
| `relatedType` | `String?` | Optional second target type, e.g. duplicate source capture. |
| `relatedId` | `String?` | Optional second target id. |
| `kind` | `ResolutionKind` | Semantic event. |
| `actor` | `ResolutionActor` | `USER`, `SYSTEM`, `DUPLICATE_DETECTOR`, `ACTION_RUNTIME`, `RETENTION`, `AGENT`. |
| `reason` | `String?` | Bounded machine reason, e.g. `EXACT_TEXT`, `CANONICAL_URL`, `schema_invalidated`. |
| `occurredAtMillis` | `Long` | Wall-clock event time. |
| `effectiveUntilMillis` | `Long?` | Required for `SNOOZED`; optional for temporary not-now policies. |
| `invalidatesReceiptId` | `String?` | Used by `REOPENED`/conflict correction to preserve history. |
| `metadataJson` | `String?` | Compact bounded metadata. No raw content or model text. |

### Indexes

- `targetType,targetId,occurredAtMillis`
- `envelopeId,occurredAtMillis`
- `kind,occurredAtMillis`
- `effectiveUntilMillis`

## ResolutionKind

- `DUPLICATE_RECAPTURE`
- `DISMISSED`
- `NOT_NOW`
- `SNOOZED`
- `DONE`
- `REOPENED`
- `RESOLVED`
- `STALE`
- `INVALIDATED`
- `SOURCE_DELETED`
- `CONFLICT`

## ResolutionActor

- `USER`
- `SYSTEM`
- `DUPLICATE_DETECTOR`
- `ACTION_RUNTIME`
- `RETENTION`
- `AGENT`

## ResolutionTargetType

- `ENVELOPE`
- `ACTIVE_INTENT`
- `ACTION_PROPOSAL`
- `TODO_LIST`
- `TODO_ITEM`
- `MEMORY_CANDIDATE`
- `GRAPH_FACT`
- `DUPLICATE_ATTEMPT`

## SurfacingVerdict

Not necessarily persisted. Computed from latest applicable receipts.

| Verdict | Meaning |
| --- | --- |
| `ACTIVE` | Can surface normally. |
| `HIDDEN_UNTIL` | Suppressed until a timestamp. |
| `DISMISSED` | User dismissed or not interested; hide until materially new evidence. |
| `RESOLVED` | User confirmed done/resolved. |
| `INVALIDATED` | Source/action/schema invalidated the target. |
| `STALE` | Target should not be retried without refresh. |

## Privacy Rules

`metadataJson` may contain:

- ids
- enum names
- counts
- timestamps
- short machine reason codes
- source labels already present in compact UI parcels

`metadataJson` must not contain:

- raw screenshots
- full OCR/text bodies
- prompts
- model responses
- embeddings
- JWTs, cookies, API keys, access tokens
- raw HTML or public-fetch bodies
