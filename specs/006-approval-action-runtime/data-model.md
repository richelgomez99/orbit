# Data Model: Approval Action Runtime

## Existing Durable Tables

Spec 006 should reuse the existing Room v8 action tables unless implementation proves a migration is required.

### `action_proposal`

Represents one candidate action tied to a source envelope.

Important fields:

- `id`
- `envelopeId`
- `functionId`
- `schemaVersion`
- `argsJson`
- `previewTitle`
- `previewSubtitle`
- `confidence`
- `provenance`
- `state`
- `sensitivityScope`
- `createdAt`
- `stateChangedAt`

State expectations:

- `PROPOSED`: visible in Orbit Action Drafts and Diary chip row.
- `CONFIRMED`: hidden from pending surfaces after user confirmation.
- `DISMISSED`: hidden from pending surfaces after user rejection.
- `INVALIDATED`: hidden and auditable after schema/handler mismatch.

### `action_execution`

Represents one confirmed dispatch attempt.

Important fields:

- `id`
- `proposalId`
- `functionId`
- `outcome`
- `outcomeReason`
- `dispatchedAt`
- `completedAt`
- `latencyMs`
- `episodeId`

Outcomes:

- `DISPATCHED` or `SUCCESS`: side effect reached system UI or local write completed.
- `FAILED`: handler/schema/binder/system failure.
- `USER_CANCELLED`: undo/cancel path.

### `appfunction_skill`

Local registry of built-in action functions. Spec 006 must ensure schema and handler expectations match for:

- `calendar.createEvent`
- `tasks.createTodo`
- `share.delegate` remains disabled or explicitly delegated only.
- `cluster.summarize` remains out of scope unless already surfaced elsewhere.

### `skill_usage`

Aggregated invocation history for future agent planning. Spec 006 should keep writing it from `recordActionInvocation`.

## New Projection: `ActionDraft`

No table unless required. This is a UI/Binder projection for Orbit.

Fields:

- `proposalId`
- `sourceEnvelopeId`
- `functionId`
- `displayName`
- `previewTitle`
- `previewSubtitle`
- `sourceTitle`
- `sourceAppLabel`
- `sourceDayLocal`
- `confidence`
- `sensitivityScope`
- `sideEffect`
- `reversibility`
- `createdAt`

Source:

- `action_proposal`
- `appfunction_skill`
- `intent_envelope` / current envelope view projection

## Contract Alignment: `tasks.createTodo`

Model-facing proposal shape:

```json
{
  "parentEnvelopeId": "source-envelope-id",
  "target": "local",
  "items": [
    { "text": "buy salmon", "dueEpochMillis": 1780527600000 }
  ]
}
```

Executor-facing shape after approval-layer augmentation:

```json
{
  "parentEnvelopeId": "source-envelope-id",
  "proposalId": "persisted-proposal-id",
  "target": "local",
  "items": [
    { "text": "buy salmon", "dueEpochMillis": 1780527600000 }
  ]
}
```

Rules:

- `items` is required and non-empty.
- `parentEnvelopeId` is required for local target execution.
- `proposalId` is required for local target execution, but it is injected by the approval/runtime layer because the LLM does not know the proposal id at extraction time.
- `target` defaults to `local`.
- External target remains optional/delegated and must use Android share sheet semantics.

## Contract Alignment: `calendar.createEvent`

Target shape:

```json
{
  "title": "Founder office hours",
  "startEpochMillis": 1780596000000,
  "endEpochMillis": 1780599600000,
  "location": "Venue",
  "notes": "Evidence summary",
  "tzId": "America/New_York"
}
```

Rules:

- `title` and `startEpochMillis` are required before Confirm.
- `endEpochMillis` may default to +1 hour.
- Calendar dispatch must use `Intent.ACTION_INSERT`; Orbit does not write directly to Calendar.

## Audit Events

Required audit lifecycle:

- `ACTION_PROPOSED`: proposal created.
- `ACTION_CONFIRMED`: user approved draft.
- `ACTION_DISMISSED`: user rejected draft.
- `ACTION_EXECUTED`: action dispatched or local write succeeded.
- `ACTION_FAILED`: schema, binder, handler, target, or user-cancelled failure.

Every audit row must include enough IDs to reconstruct proposal lifecycle:

- `proposalId`
- `functionId`
- `executionId` when available
- `outcome` and `reason` when available

## Outcome Projection

The Orbit workspace needs a projection for recently executed or failed drafts in addition to pending proposals.

Minimum fields:

- `proposalId`
- `executionId`
- `functionId`
- `sourceEnvelopeId`
- `previewTitle`
- `outcome`
- `outcomeReason`
- `completedAt`
- `sideEffect`
- `reversibility`

This projection can be DAO/Binder-only and should not require a new table because `action_execution` already stores the durable outcome.
