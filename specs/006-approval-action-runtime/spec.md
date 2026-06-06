# Feature Specification: Approval Action Runtime

**Feature Branch**: `feature/006-approval-action-runtime-20260603`
**Created**: 2026-06-03
**Status**: Draft
**Input**: Validated Spec 005A/005B MVP plus product direction: Orbit should help users close loops from captured intent, but every external or durable action must be explicit, cited, local-first, and user-approved.

## Current Reality

Spec 003 already landed substantial action infrastructure: `action_proposal`, `action_execution`, `appfunction_skill`, `skill_usage`, `ActionExtractionWorker`, `ActionExecutorService` in `:capture`, `IActionExecutor`, Calendar intent dispatch, local/external todo handlers, Diary chips, preview sheet, audit rows, and undo plumbing.

Spec 006 is not a rebuild. It is the product-grade approval runtime branch that reconciles this existing code with the current Orbit vision and the validated Spec 005 retrieval baseline.

Known implementation gaps to resolve:

- Action proposals are discoverable mostly in Diary cards, not in the Orbit action workspace where users expect loop-closing work.
- Non-calendar preview sheets fall back to raw JSON, which is not acceptable for a user-approved action surface.
- `tasks.createTodo` schema currently describes a single `title`, while `TodoActionHandler` expects `items`, `parentEnvelopeId`, and `proposalId`.
- `proposalId` cannot be model-generated because it only exists after persistence; the approval/runtime layer must inject it before local todo execution.
- The current generic undo behavior is misleading for Calendar because Orbit cannot undo a system Calendar save.
- Debug demo seeding creates captures but not deterministic action proposals, so S24 action-runtime validation is not reliable.
- Action extraction depends on charger/unmetered WorkManager conditions and LLM quality; the MVP demo needs deterministic proposal fixtures while preserving production extraction constraints.

## User Scenarios & Testing

### User Story 1 - Review Action Drafts In Orbit (Priority: P1)

As a user, I want Orbit to gather actionable things it found in my saved captures into a quiet approval workspace, so I can decide what to do without hunting through each diary card.

**Why this priority**: This is the product bridge from memory to action. Without a clear approval workspace, action runtime exists in code but not in the core Orbit loop.

**Independent Test**: Seed demo captures and proposals, open Orbit, see action drafts for calendar/list/reply-like items, open each draft, inspect source evidence, approve or dismiss without raw JSON.

**Acceptance Scenarios**:

1. **Given** seeded action proposals, **When** the user opens Orbit, **Then** pending action drafts appear as cited cards with source capture context and no raw JSON.
2. **Given** an action draft, **When** the user taps it, **Then** Orbit shows an approval sheet with editable fields, evidence link, side-effect disclosure, and Confirm/Dismiss controls.
3. **Given** an action draft is dismissed, **When** the user returns to Orbit or Diary, **Then** that proposal no longer appears and an `ACTION_DISMISSED` audit row exists.

---

### User Story 2 - Approve Calendar Drafts (Priority: P1)

As a user, I want Orbit to turn a saved event, appointment, ticket, or flight into a calendar draft that I review first, then hand off to the system Calendar app only after I confirm.

**Why this priority**: Calendar approval is the cleanest first proof that Orbit can close a loop while preserving no-silent-writes and no calendar-provider permissions.

**Independent Test**: Seed or capture an event-like memory, approve its calendar draft on S24, verify the system Calendar insert screen opens with the expected title/time/location/notes and Orbit records confirm/execute audit rows.

**Acceptance Scenarios**:

1. **Given** a captured event with a proposed calendar draft, **When** the user confirms the approval sheet, **Then** Orbit launches `Intent.ACTION_INSERT` to Calendar with prefilled fields and no `READ_CALENDAR` or `WRITE_CALENDAR` permission.
2. **Given** required calendar fields are missing, **When** the approval sheet opens, **Then** the user can fill them in before Confirm is enabled.
3. **Given** no Calendar handler is available, **When** the user confirms, **Then** Orbit shows a user-facing failure and records `ACTION_FAILED` with a reason.

---

### User Story 3 - Approve Local List/Task Drafts (Priority: P2)

As a user, I want Orbit to turn captured lists or errands into local Orbit follow-up envelopes, so I can close loops without exporting my data to another app.

**Why this priority**: This validates the "grocery list for the recipe" and "things I meant to deal with" vision while staying local-first.

**Independent Test**: Seed a shopping/list capture, approve the local list draft, verify one derived list envelope appears in Diary/Library with multiple checklist items, can be marked done, and cites the source capture/proposal.

**Acceptance Scenarios**:

1. **Given** a captured list, **When** the user approves a local todo/list draft, **Then** Orbit creates a single derived local list envelope with `todoMetaJson.items[]` and provenance back to the source proposal.
2. **Given** a derived list envelope, **When** the user toggles a checkbox, **Then** the state updates locally and no network request is made.
3. **Given** the user rejects the draft, **When** the capture appears later in Diary/Library, **Then** the rejected draft is not re-surfaced unless extraction materially changes in a later spec.

---

### User Story 4 - Make Action Outcomes Trustworthy (Priority: P2)

As a user, I want every action attempt to have visible outcome, undo/failure copy, and audit provenance, so I can trust that Orbit did not silently act or hide errors.

**Why this priority**: Approval runtime is only credible if failures and external side effects are honest.

**Independent Test**: Trigger success, dismiss, schema mismatch, missing handler, and undo-window paths; verify UI state, proposal state, execution rows, skill usage, and audit rows.

**Acceptance Scenarios**:

1. **Given** a confirmed action dispatches successfully, **When** Orbit returns to foreground, **Then** the proposal is no longer pending and the execution outcome is auditable.
2. **Given** a user taps undo within the supported window for a reversible/local action, **When** the cancellation succeeds, **Then** Orbit records a cancellation and removes the success affordance.
3. **Given** schema validation fails, **When** Confirm is tapped, **Then** no external intent fires, the proposal becomes invalidated, and the failure is visible.

## Edge Cases

- The `:capture` executor cannot bind to `:ml`.
- The target Calendar/share app is missing or rejects the intent.
- A proposal was generated against an older schema version.
- The source envelope was deleted or archived before approval.
- The user confirms twice rapidly.
- WorkManager extracts the same proposal more than once.
- Local-only mode is enabled or the device is offline.
- The action draft contains sensitive identifiers or redacted content.

## Requirements

### Functional Requirements

- **FR-006-001**: Orbit MUST expose pending action drafts in the Orbit tab, not only inline in Diary.
- **FR-006-002**: Every action draft MUST cite the source envelope and allow opening that local capture before approval.
- **FR-006-003**: Approval sheets MUST render typed fields for supported action kinds; raw `argsJson` MUST NOT be shown to normal users.
- **FR-006-004**: Calendar approvals MUST use `Intent.ACTION_INSERT` and MUST NOT add `READ_CALENDAR` or `WRITE_CALENDAR`.
- **FR-006-005**: Local todo/list approvals MUST create one derived local list envelope through the `:ml` binder, with all approved checklist items in `todoMetaJson.items[]` and provenance to source envelope and proposal.
- **FR-006-006**: Proposal schema, handler expectations, LLM extraction output, runtime augmentation, and debug fixtures MUST agree for `calendar.createEvent` and `tasks.createTodo`.
- **FR-006-007**: Confirm, dismiss, execute, fail, invalidate, and undo/cancel paths MUST write local audit rows with proposal/execution/function identifiers.
- **FR-006-008**: Execution MUST remain in `:capture`; database mutations MUST remain in `:ml`; any network inference MUST route through `:net`.
- **FR-006-009**: The branch MUST provide deterministic debug demo proposals so S24 validation does not depend on charger/unmetered WorkManager timing.
- **FR-006-010**: The app MUST degrade without cloud/local model availability: existing pending proposals remain reviewable, but no new model-generated proposals are required.
- **FR-006-011**: Calendar execution MUST NOT present an Orbit undo affordance unless Orbit can actually reverse the external side effect.
- **FR-006-012**: Execution failures MUST be visible to users in the action workspace or equivalent UI, not only in audit logs.

### Non-Goals

- No autonomous external writes.
- No direct Calendar provider writes.
- No Gmail/Google Tasks/Todoist APIs.
- No AppFunctions/Spark/platform-agent interop; that waits for approval and a downstream spec.
- No new knowledge graph schema; proposal provenance remains envelope/proposal/audit based until Spec 009.
- No BYOM/local model manager work; Spec 022 owns that.

### Key Entities

- **ActionProposal**: Existing local Room row representing one candidate action tied to a source envelope.
- **ActionExecution**: Existing local Room row representing one user-approved dispatch attempt.
- **AppFunctionSkill**: Existing local schema/handler registry entry for built-in Orbit actions.
- **ActionDraft**: User-facing projection of proposal + source envelope + skill metadata for Orbit/Diary approval UI. This can be a Binder/UI DTO and does not require a new table unless implementation proves otherwise.
- **Derived List Envelope**: Existing `IntentEnvelope` with `todoMetaJson.items[]`, created by approved local list/task actions. It represents the list as one artifact, not one Diary row per item.

## Success Criteria

### Measurable Outcomes

- **SC-006-001**: On S24, seeded calendar and list action drafts appear in Orbit within 5 seconds of opening the app.
- **SC-006-002**: On S24, approving a calendar draft opens the system Calendar insert UI with expected fields and no added calendar permissions.
- **SC-006-003**: On S24, approving a local list draft creates one local derived list envelope that is searchable in Library and visible in Diary with multiple checklist items.
- **SC-006-004**: JVM/instrumented tests cover success, dismiss, failure, schema mismatch, duplicate, and no-network-boundary paths.
- **SC-006-005**: `./gradlew :app:compileDebugKotlin :app:testDebugUnitTest :build-logic:lint:test :app:lintDebug :app:assembleDebug` passes, and Android secret/network scans remain clean.

## Assumptions

- The validated Spec 005A memory gateway and Library/Orbit retrieval baseline is the starting point.
- The S24 remains the primary physical validation device.
- Debug-only fixture creation is acceptable for deterministic demo validation, but production proposal extraction remains governed by `ActionExtractionWorker` and `LlmProvider`.
- Existing Room v8 action tables are sufficient for this branch; new tables require explicit plan justification.
