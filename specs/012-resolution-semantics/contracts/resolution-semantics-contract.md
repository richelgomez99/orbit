# Contract: Resolution Semantics

## Repository/Binder Surface

### Write Receipt

`recordResolution(receipt: ResolutionReceiptParcel): Boolean`

Writes a compact resolution receipt inside `:ml`. Returns false only for validation failure.

Validation:

- `targetType`, `targetId`, `kind`, `actor`, and `occurredAtMillis` are required.
- `SNOOZED` requires `effectiveUntilMillis > occurredAtMillis`.
- `metadataJson` must be bounded and must reject banned keys.
- `DONE`/`RESOLVED` require `actor = USER` or an existing user-confirmed runtime path.

### Query Latest Verdict

`getResolutionVerdict(targetType: String, targetId: String, nowMillis: Long): ResolutionVerdictParcel`

Returns a compact verdict for Follow-ups, agent planning, and UI gating.

### Observe/Query Receipts For Debug

Debug-only or local Settings surface may list recent receipts by envelope id for validation. This is not a normal product feed.

## Hook Contracts

### Duplicate Capture

When `sealWithResult` returns `STATUS_ALREADY_SAVED`, repository code records:

- `targetType = ENVELOPE`
- `targetId = existingEnvelopeId`
- `kind = DUPLICATE_RECAPTURE`
- `actor = DUPLICATE_DETECTOR`
- `reason = matchedBy`
- metadata: duplicate attempt id if available, no raw content

### Basic Understanding Duplicate Suppression

When Basic understanding suppresses duplicate Active Intent projection, it records:

- `targetType = ENVELOPE`
- `targetId = existingCaptureId`
- `relatedType = ENVELOPE`
- `relatedId = newCaptureId`
- `kind = DUPLICATE_RECAPTURE`
- `actor = DUPLICATE_DETECTOR`
- `reason = CONTENT_HASH`

### Action Proposal Dismissal

When a proposal is dismissed:

- `targetType = ACTION_PROPOSAL`
- `targetId = proposalId`
- `envelopeId = source envelope id`
- `kind = DISMISSED`
- `actor = USER`

### Action Proposal Invalidation

When schema/runtime invalidates a proposal:

- `kind = INVALIDATED` or `STALE`
- `actor = ACTION_RUNTIME`
- `reason = schema_invalidated`, `schema_mismatch`, or equivalent bounded code

### Todo Aggregate Completion

When a derived list transitions from not-all-done to all-done:

- `targetType = TODO_LIST`
- `targetId = envelopeId`
- `kind = DONE`
- `actor = USER`
- metadata: item count and derived proposal id

When it transitions from all-done to not-all-done:

- `kind = REOPENED`
- `actor = USER`
- `invalidatesReceiptId` points at the latest completion receipt when available.

## Surfacing Rules

Given receipts for a target:

1. Latest `INVALIDATED` or `SOURCE_DELETED` wins.
2. Latest `REOPENED` after `DONE` returns `ACTIVE`.
3. Latest `DONE` or `RESOLVED` returns `RESOLVED`.
4. Latest unexpired `SNOOZED` returns `HIDDEN_UNTIL`.
5. Latest `DISMISSED` returns `DISMISSED`.
6. Latest `NOT_NOW` returns `DISMISSED` for current cleanup views but not for Library/Diary memory browsing.
7. Otherwise return `ACTIVE`.
