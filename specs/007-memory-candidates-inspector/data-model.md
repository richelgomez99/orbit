# Data Model: Memory Candidates Inspector

## Existing Tables Reused

- `intent_envelope`: source captures and derived envelopes.
- `capture_understanding`: compact understanding sidecar.
- `evidence_bundle`: compact evidence sidecar.
- `action_proposal` / `action_execution` / `skill_usage`: action behavior that can later produce candidates.
- `audit_log`: immutable local audit trail.
- `invalidation_record`: capture invalidation signal.

## New Table: `memory_candidate`

Purpose: hold plausible memories that Orbit may ask the user to confirm, but must not treat as facts.

Columns:

| Column | Type | Notes |
| --- | --- | --- |
| `id` | TEXT PK | UUID string. |
| `candidateKind` | TEXT | `PROFILE_FACT`, `PREFERENCE`, `INTEREST`, `PATTERN`, `RELATIONSHIP`, `CORRECTION_RULE`. |
| `state` | TEXT | `PENDING`, `ASKED`, `PROMOTED`, `REJECTED`, `EXPIRED`, `INVALIDATED`. |
| `displayLabel` | TEXT | Plain-language card title, max 200 chars. |
| `subject` | TEXT | Structured fact subject, max 160 chars. |
| `predicate` | TEXT | Structured fact predicate, max 80 chars. |
| `objectValue` | TEXT | Structured fact object, max 300 chars. |
| `confidence` | REAL | 0.0 to 1.0. |
| `sensitivity` | TEXT | `NORMAL`, `SENSITIVE`, `LOCAL_ONLY`. |
| `supportingEnvelopeIdsJson` | TEXT | JSON array of envelope ids, max 50. |
| `supportingEvidenceIdsJson` | TEXT nullable | JSON array of evidence ids, max 50. |
| `supportingFeedbackIdsJson` | TEXT nullable | JSON array of feedback/action ids, max 50. |
| `askUserCopy` | TEXT nullable | Optional prompt copy, max 240 chars. |
| `createdAt` | INTEGER | Epoch millis. |
| `updatedAt` | INTEGER | Epoch millis. |
| `expiresAt` | INTEGER nullable | Optional expiry. |
| `decidedAt` | INTEGER nullable | Set on promote/reject/expire/invalidate. |
| `decisionReason` | TEXT nullable | User/system reason code. |
| `modelLabel` | TEXT nullable | Producer model or `debug_seed`. |
| `promptVersion` | TEXT nullable | Producer prompt version. |
| `source` | TEXT | `DEBUG_SEED`, `USER_DECLARATION`, `ACTION_PATTERN`, `CAPTURE_PATTERN`, `MODEL_PROPOSED`. |

Indexes:

- `state`
- `candidateKind`
- `sensitivity`
- `createdAt`
- `expiresAt`

Uniqueness:

- Unique normalized active candidate key over `(candidateKind, subject, predicate, objectValue)` for non-terminal states where possible. SQLite partial unique index can enforce `state IN ('PENDING', 'ASKED')`.

## New Table: `memory_candidate_support`

Purpose: make provenance and source deletion testable without parsing JSON arrays in every query. `supportingEnvelopeIdsJson` remains a bounded denormalized display/cache field, but this junction table is the queryable source of truth.

Columns:

| Column | Type | Notes |
| --- | --- | --- |
| `candidateId` | TEXT | FK to `memory_candidate.id`, cascade delete. |
| `envelopeId` | TEXT | FK to `intent_envelope.id`, cascade delete. |
| `evidenceId` | TEXT nullable | Optional evidence id. |
| `supportType` | TEXT | `CAPTURE`, `ACTION`, `NOTE`, `UNDERSTANDING`, `USER_CONFIRMATION`. |
| `createdAt` | INTEGER | Epoch millis. |

Primary key:

- `(candidateId, envelopeId, supportType)`

Indexes:

- `candidateId`
- `envelopeId`

## New Table: `promoted_memory`

Purpose: represent memory Orbit may reuse after explicit user acceptance.

Columns:

| Column | Type | Notes |
| --- | --- | --- |
| `id` | TEXT PK | UUID string. |
| `candidateId` | TEXT nullable | FK to `memory_candidate.id`, SET NULL on delete. |
| `memoryKind` | TEXT | `DECLARED_PROFILE_FACT`, `CONFIRMED_PREFERENCE`, `RECURRING_INTEREST`, `BEHAVIOR_PATTERN`, `CORRECTION_RULE`, `RELATIONSHIP`. |
| `state` | TEXT | `ACTIVE`, `DISABLED`, `FORGOTTEN`, `INVALIDATED`. |
| `displayLabel` | TEXT | User-visible label. |
| `subject` | TEXT | Structured fact subject. |
| `predicate` | TEXT | Structured fact predicate. |
| `objectValue` | TEXT | Structured fact object. |
| `confidenceLabel` | TEXT | `DECLARED`, `CONFIRMED`, `HIGH`, `MEDIUM`, `LOW`. |
| `sensitivity` | TEXT | `NORMAL`, `SENSITIVE`, `LOCAL_ONLY`. |
| `source` | TEXT | `USER_DECLARED`, `USER_CONFIRMED`, `REPEATED_BEHAVIOR`, `CORRECTION`. |
| `supportingEnvelopeIdsJson` | TEXT | JSON array of source envelope ids. |
| `supportingEvidenceIdsJson` | TEXT nullable | JSON array of evidence ids. |
| `supportingFeedbackIdsJson` | TEXT nullable | JSON array of feedback/action ids. |
| `createdAt` | INTEGER | Epoch millis. |
| `updatedAt` | INTEGER | Epoch millis. |
| `validFrom` | INTEGER | Epoch millis. |
| `validTo` | INTEGER nullable | Optional temporal validity. |
| `invalidatedAt` | INTEGER nullable | Set when forgotten/invalidated. |
| `lastUsedAt` | INTEGER nullable | Future use tracing. |
| `useCount` | INTEGER | Starts at 0. |

Indexes:

- `state`
- `memoryKind`
- `sensitivity`
- `candidateId`
- `updatedAt`

## New Table: `promoted_memory_support`

Purpose: queryable provenance for accepted memories.

Columns mirror `memory_candidate_support`:

- `memoryId`
- `envelopeId`
- `evidenceId`
- `supportType`
- `createdAt`

Primary key:

- `(memoryId, envelopeId, supportType)`

On source envelope deletion, cascade removes support rows; repository cleanup invalidates source-less promoted memories.

## DTO: `MemoryCandidateParcel`

Compact Binder projection:

- `id`
- `candidateKind`
- `state`
- `displayLabel`
- `factText`
- `confidenceLabel`
- `sensitivity`
- `sourceCount`
- `primarySourceEnvelopeId`
- `primarySourceTitle`
- `primarySourceDay`
- `askUserCopy`
- `createdAt`
- `updatedAt`

No raw capture body, raw OCR, prompt, model response, embedding, or full evidence JSON.

## DTO: `PromotedMemoryParcel`

Compact Binder projection:

- `id`
- `memoryKind`
- `state`
- `displayLabel`
- `factText`
- `confidenceLabel`
- `sensitivity`
- `sourceCount`
- `lastUsedAt`
- `useCount`

## State Transitions

Memory candidate:

```text
PENDING -> ASKED -> PROMOTED
PENDING -> PROMOTED
PENDING -> REJECTED
PENDING -> EXPIRED
PENDING -> INVALIDATED
ASKED -> REJECTED
ASKED -> EXPIRED
ASKED -> INVALIDATED
```

Promoted memory:

```text
ACTIVE -> DISABLED
ACTIVE -> FORGOTTEN
ACTIVE -> INVALIDATED
DISABLED -> ACTIVE
```

## Deletion / Invalidation

- Deleting a source capture cascades through explicit FK tables where possible.
- Because `supportingEnvelopeIdsJson` is denormalized, repository code must invalidate candidates/promoted memories whose full support set is no longer valid.
- Full deletion semantics are completed in Spec 012; this branch must at least avoid showing source-less candidates as trustworthy.
