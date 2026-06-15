# Data Model: Capture Context Affordance

No new persistent database table is planned.

## Existing Entity: IntentEnvelope

Context targets an existing saved envelope id.

| Field | Usage |
| --- | --- |
| `id` | Required target for context save. |
| `contentType` | Used only for display copy if needed. |
| `dayLocal` | Existing Diary placement; not changed by context. |
| `isArchived` / deletion state | Context surface should fail gracefully if target is gone. |

## Existing Entity: EnvelopeNote

The existing latest-note row remains the storage location for user context.

| Field | Usage |
| --- | --- |
| `envelopeId` | Must equal target envelope id. |
| `text` | Trimmed non-blank user context. |
| `updatedAt` | Repository-owned write timestamp. |

Invariant: Spec 018 must not add a second source of context truth.

## UI/Boundary Model: CaptureContextDraft

Transient draft used by the focused context surface.

| Field | Type | Rules |
| --- | --- | --- |
| `targetEnvelopeId` | String | Non-blank saved envelope id. |
| `origin` | Enum/String | `NEW_CAPTURE`, `DUPLICATE_CAPTURE`, or `DETAIL_FALLBACK`. |
| `text` | String | Trimmed before save; blank rejected locally. |
| `createdAtMillis` | Long | UI state only, not persistence authority. |

## UI/Boundary Model: CaptureContextResult

Transient save result.

| Result | Meaning |
| --- | --- |
| `Saved` | Note was written through existing Binder/repository path. |
| `Blank` | Trimmed text was empty; no Binder call. |
| `Unavailable` | Binder/repository unavailable; keep draft and allow retry. |
| `TargetMissing` | Envelope no longer exists or cannot be opened. |
| `Failed` | Unexpected error; no raw content in message/log payload. |

## Payload Safety

Allowed payload fields are ids, origin enum, capped user text, timestamps, and simple reason codes. Never include raw screenshots, raw OCR/full text, model responses, prompts, embeddings, JWTs, cookies, API keys, or raw HTML in logs, Binder diagnostics, or audit metadata.
