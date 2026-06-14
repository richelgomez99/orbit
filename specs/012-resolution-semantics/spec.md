# Feature Specification: Resolution Semantics

**Feature Branch**: `feature/012-resolution-semantics-20260613`
**Created**: 2026-06-13
**Status**: Draft for implementation
**Input**: Roadmap row 8 after Spec 011: model duplicate, conflict, stale, done, dismissed, snoozed, and not-now states so Follow-ups and agent loops have durable meaning.

## Current Reality

Orbit already has partial lifecycle state:

- `SealResultParcel.alreadySaved(...)` and `DUPLICATE_CAPTURE_ATTEMPT` audit rows for exact text/canonical URL recaptures.
- Basic-understanding duplicate suppression for screenshot/OCR-derived normalized content.
- `ActiveIntentStatus` and `ResolutionReason` for Active Intent rows.
- `ActionProposalState` plus action confirm/dismiss/invalidated paths.
- Derived list envelopes with `todoMetaJson.items[].done`.
- Archive, soft-delete, trash retention, and cluster dismiss paths.

These states are fragmented. Audit rows explain what happened, but the product still lacks one durable semantic layer that answers: "Should Orbit surface this again, treat it as done, suppress it until later, or preserve it only as memory?"

## User Scenarios And Testing

### User Story 1 - Recapture Updates Meaning Instead Of Creating Noise (Priority: P1)

As a user, if I save the same thing again, Orbit should recognize it as renewed attention to the same memory instead of creating repeated Follow-ups or duplicate Diary pressure.

**Why this priority**: Duplicate screenshots/captures are the clearest real-world failure mode seen on device. Resolution semantics must start by making recapture behavior explicit.

**Independent Test**: Given an existing capture and a duplicate save attempt, the repository writes a compact resolution receipt tied to the existing envelope and duplicate attempt while avoiding another Active Intent projection.

**Acceptance Scenarios**:

1. **Given** an existing active envelope, **When** an exact duplicate save returns `AlreadySaved`, **Then** Orbit records a `DUPLICATE_RECAPTURE` receipt with existing envelope id, matched-by reason, wall-clock timestamp, and no raw capture body.
2. **Given** Basic understanding detects duplicate OCR/content hash, **When** it suppresses Active Intent projection, **Then** Orbit records a receipt that links new capture id to the canonical existing capture id.
3. **Given** duplicate receipts exist, **When** Orbit builds Follow-ups, **Then** duplicate copies do not appear as separate work items.

---

### User Story 2 - Dismiss, Not Now, And Snooze Are Different (Priority: P1)

As a user, I want to dismiss a bad suggestion permanently, say "not now" without losing the memory, or snooze a follow-up until later, because those are different intentions.

**Why this priority**: Follow-ups and agent plans cannot be trusted if every rejection is treated the same. "No thanks" and "later" must affect surfacing differently.

**Independent Test**: Given an Active Intent or action draft, user resolution choices write receipts and change future surfacing according to the chosen semantic.

**Acceptance Scenarios**:

1. **Given** an Active Intent follow-up, **When** the user taps Dismiss, **Then** a `DISMISSED` receipt is written and the item no longer appears in active Follow-ups.
2. **Given** an Active Intent follow-up, **When** the user taps Not now, **Then** a `NOT_NOW` receipt is written and the item is hidden from the current cleanup view without claiming completion.
3. **Given** an Active Intent follow-up, **When** the user snoozes it until a timestamp, **Then** a `SNOOZED` receipt is written with `effectiveUntilMillis`, and the item may return after that time.
4. **Given** an action draft is dismissed, **When** extraction runs again without material evidence change, **Then** the same draft is not re-surfaced.

---

### User Story 3 - Done Means User-Confirmed Loop Closure (Priority: P2)

As a user, when I mark a local list item or intent as done, Orbit should know that this loop was closed by me, not inferred by a model.

**Why this priority**: The agent's future behavior depends on knowing what the user actually closed. Done must be user-confirmed and provenance-backed.

**Independent Test**: Given a derived list envelope, marking all items done writes a meaningful completion receipt while individual checkbox toggles remain lightweight.

**Acceptance Scenarios**:

1. **Given** a derived list envelope, **When** the final incomplete item is marked done, **Then** Orbit records a `DONE` receipt for the list envelope with parent/proposal provenance.
2. **Given** a user reopens an item after completion, **When** the item is unchecked, **Then** Orbit records a `REOPENED` receipt or invalidates the prior completion without deleting history.
3. **Given** an Active Intent is resolved with `REPLIED_OR_DONE`, **When** the state changes to resolved, **Then** a `RESOLVED` receipt links the Active Intent, source envelope, and user action.

---

### User Story 4 - Stale, Conflict, And Source-Deleted States Are Honest (Priority: P2)

As a user, I want Orbit to stop acting on stale or conflicting evidence while preserving why a memory changed state.

**Why this priority**: Resolution semantics are the trust layer for KG and agent planning. The agent must know when evidence is invalid, stale, or superseded.

**Independent Test**: Given source deletion, action schema invalidation, or conflicting user corrections, receipts explain why a candidate/intent/action is no longer active and downstream surfaces honor it.

**Acceptance Scenarios**:

1. **Given** a source envelope is soft-deleted, **When** dependent Active Intent rows are invalidated, **Then** a `SOURCE_DELETED` receipt links the dependent row to the deleted envelope.
2. **Given** an action execution fails with schema invalidation, **When** the proposal is marked invalidated, **Then** a `STALE` or `INVALIDATED` receipt prevents blind retry.
3. **Given** the user corrects a promoted memory or graph fact later, **When** the correction conflicts with an older fact, **Then** Spec 012 records the semantic shape, even if full conflict UI waits for a later graph branch.

## Edge Cases

- A receipt references an envelope that is later hard-deleted by retention.
- A user snoozes an item and then deletes the source capture.
- A duplicate recapture matches a soft-deleted or archived envelope.
- Two duplicate attempts race against the same canonical envelope.
- A model suggests completion, but the user has not confirmed it.
- A local list contains zero valid items or malformed `todoMetaJson`.
- Not-now and snooze receipts expire while the app is offline.
- Cloud indexing is disabled; resolution semantics still work locally.

## Requirements

### Functional Requirements

- **FR-012-001**: Orbit MUST persist local resolution receipts for semantically meaningful lifecycle transitions: duplicate recapture, dismissed, not-now, snoozed, done, reopened, resolved, stale, invalidated, source-deleted, and conflict.
- **FR-012-002**: Resolution receipts MUST be local-first Room data owned by `:ml`; UI/capture processes access them only through existing/new Binder repository methods.
- **FR-012-003**: Receipts MUST carry compact provenance ids only: target type/id, optional envelope id, optional related id, reason, actor, timestamps, and bounded metadata. Raw screenshots, raw OCR bodies, prompts, model responses, embeddings, JWTs, cookies, and API keys MUST NOT be stored.
- **FR-012-004**: Exact duplicate save attempts MUST write a `DUPLICATE_RECAPTURE` receipt for the canonical existing envelope.
- **FR-012-005**: Basic-understanding duplicate suppression MUST write a `DUPLICATE_RECAPTURE` receipt or equivalent compact resolution marker for screenshot/OCR-derived duplicates.
- **FR-012-006**: Dismiss, not-now, and snooze MUST be represented as distinct semantic states and MUST change future Follow-up surfacing differently.
- **FR-012-007**: Snooze receipts MUST include an `effectiveUntilMillis` and MUST not permanently claim the loop is resolved.
- **FR-012-008**: Done/resolved receipts MUST require a user-confirmed local action or explicit user confirmation; model inference alone MUST NOT mark something done.
- **FR-012-009**: Derived list completion MUST create a meaningful completion receipt when all items are done; individual checkbox toggles MAY remain unaudited unless they change aggregate completion state.
- **FR-012-010**: Reopening a completed item or intent MUST preserve history by adding a new receipt or invalidating the prior completion; it MUST NOT delete prior receipts.
- **FR-012-011**: Action proposal dismissal and schema invalidation MUST create receipts that prevent unchanged duplicate proposals from reappearing as active work.
- **FR-012-012**: Source deletion/archival invalidation MUST create receipts for dependent Active Intent/action/graph rows where the dependency exists.
- **FR-012-013**: Follow-up and agent-planning reads MUST be able to ask whether a target is active, hidden until later, dismissed, resolved, or invalidated.
- **FR-012-014**: Every receipt write MUST also emit or be correlated with an audit row where existing audit policy requires one.
- **FR-012-015**: The branch MUST provide deterministic unit tests for receipt creation, surfacing rules, and no-raw-payload constraints.

### Key Entities

- **ResolutionReceipt**: Durable local semantic record describing how Orbit should treat a target going forward.
- **ResolutionTarget**: Compact reference to the thing being resolved, such as an envelope, Active Intent, action proposal, derived todo item/list, memory candidate, graph fact, or duplicate attempt.
- **ResolutionKind**: Semantic type: duplicate recapture, dismissed, not-now, snoozed, done, reopened, resolved, stale, invalidated, source-deleted, conflict.
- **ResolutionActor**: Who/what caused the receipt: user, system, duplicate detector, action runtime, retention/deletion cascade, or future agent.
- **Surfacing Verdict**: Read-time projection of receipts into `ACTIVE`, `HIDDEN_UNTIL`, `DISMISSED`, `RESOLVED`, `INVALIDATED`, or `STALE`.

## Success Criteria

### Measurable Outcomes

- **SC-012-001**: Duplicate exact-text/canonical-url saves and Basic-understanding duplicate suppressions produce compact receipts without adding duplicate Follow-up rows.
- **SC-012-002**: Dismiss, not-now, and snooze can be independently tested and yield different surfacing verdicts.
- **SC-012-003**: Completing and reopening a derived local list creates durable semantic history without raw payload leakage.
- **SC-012-004**: Repository/unit tests cover at least duplicate, dismiss, not-now, snooze, done, reopened, and invalidated cases.
- **SC-012-005**: Full non-phone gate passes: `./gradlew :app:compileDebugKotlin :app:testDebugUnitTest :build-logic:lint:test :app:lintDebug :app:assembleDebug :app:compileDebugAndroidTestKotlin`.

## Assumptions

- Spec 011 is the base: users can add context and manual text captures, but physical phone validation may still be pending.
- A new Room migration is acceptable if it is additive, compact, and covered by source-ready tests.
- Initial UI can be minimal if repository semantics and Orbit Follow-up surfacing are correct.
- KG conflict resolution UI may remain a later branch, but this branch should define the receipt shape it will use.
- Cloud/MongoDB/Atlas stores do not become authoritative for resolution state in this branch.
