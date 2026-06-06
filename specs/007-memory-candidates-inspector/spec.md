# Feature Specification: Memory Candidates Inspector

**Feature Branch**: `feature/007-memory-candidates-inspector-20260605`
**Created**: 2026-06-05
**Status**: Drafted for implementation after Spec 006 checkpoint `ba2dcf7`

## Summary

Spec 007 creates Orbit's first user-facing memory trust surface. It lets users inspect, accept, reject, and correct candidate memories before Orbit treats them as durable profile facts, preferences, relationships, or patterns.

This is not the knowledge graph backend. It is the consent and provenance layer that must exist before KG, curious profiling, or agent planning can safely personalize behavior.

## Current Implementation Context

Orbit already has:

- Local Room/SQLCipher source of truth in the `:ml` process.
- Intent envelopes, notes, continuations, capture understandings, evidence bundles, active intents, action proposals, action executions, skills, and audit rows.
- Diary, Library, and Orbit tabs.
- Spec 005A semantic retrieval / grounded Ask baseline.
- Spec 006 action draft approval runtime, including grouped local list envelopes and action audit paths.

Orbit does not yet have:

- Memory candidate rows.
- Promoted memory/profile fact rows.
- A user-facing memory inspector.
- Durable candidate acceptance/rejection/correction semantics.
- Knowledge graph entities/edges.
- Curious Agent profile questions.
- Agent planning that uses memory candidates.

## User Stories

### User Story 1 - Review Candidate Memories In Orbit (P1)

As a user, I want Orbit to show what it might remember about me, with the source captures that support it, so I can decide whether it is useful or wrong before the app uses it.

**Why this priority**: Orbit's vision depends on a private picture of the user. That picture must be inspectable and user-controlled before it influences agent behavior.

**Independent Test**: Seed local candidate memories, open Orbit, verify Memory Review shows candidate cards with label, type, confidence copy, sensitivity, source count, and Open Source.

**Acceptance Scenarios**:

1. **Given** pending memory candidates exist, **When** the user opens Orbit, **Then** a Memory Review section appears after action drafts and shows compact candidate cards.
2. **Given** a candidate has supporting captures, **When** the user taps Open Source, **Then** Orbit opens the source capture detail locally.
3. **Given** there are no candidates, **When** the user opens Orbit, **Then** no noisy empty queue is shown.

### User Story 2 - Accept Or Reject Candidate Memories (P1)

As a user, I want to accept or reject a candidate memory, so Orbit learns only what I approve and stops surfacing wrong assumptions.

**Why this priority**: Nothing should become a profile fact silently. Acceptance/rejection is the core safety contract.

**Independent Test**: Accept one candidate and reject another; verify the accepted item creates a promoted memory row with provenance, the rejected item leaves a negative signal, and both actions write audit rows.

**Acceptance Scenarios**:

1. **Given** a pending candidate, **When** the user accepts it, **Then** Orbit promotes it to a local promoted memory row and hides the candidate from pending review.
2. **Given** a pending candidate, **When** the user rejects it, **Then** Orbit marks it rejected, records the reason if provided, and does not promote a fact.
3. **Given** a candidate is accepted or rejected, **When** the user repeats the action, **Then** the decision is idempotent and does not create duplicate promoted rows or duplicate audit entries.

### User Story 3 - Correct Candidate Memory Copy (P2)

As a user, I want to correct a candidate's wording before accepting it, so Orbit remembers the fact the way I actually mean it.

**Why this priority**: Edits are the first explicit user-authored memory signal and are safer than model-only text.

**Independent Test**: Edit a candidate label/fact text, accept it, and verify the promoted memory stores the edited text, original candidate id, supporting sources, and audit provenance.

**Acceptance Scenarios**:

1. **Given** a candidate card, **When** the user taps Edit, **Then** Orbit shows a compact local edit sheet with editable label and fact text.
2. **Given** the user accepts edited text, **When** the promoted memory is created, **Then** it stores the edited display label and marks source as user confirmed.
3. **Given** edited text is empty or too long, **When** the user tries to save, **Then** Orbit blocks the action with local validation copy.

### User Story 4 - Keep Sensitive Or Local-Only Candidates Safe (P2)

As a user, I want sensitive or local-only candidate memories to stay local and require explicit confirmation, so Orbit does not externalize private profile facts.

**Why this priority**: The candidate layer is where privacy failures would become durable. The branch must set conservative defaults.

**Independent Test**: Seed sensitive/local-only candidates; verify they are labeled, cannot be auto-promoted, do not sync through compact memory/cloud payloads, and require explicit user confirmation.

**Acceptance Scenarios**:

1. **Given** a sensitive candidate, **When** it appears in Memory Review, **Then** it shows clear sensitive/local-only copy and requires an explicit accept action.
2. **Given** compact memory sync runs, **When** local-only candidates exist, **Then** they are absent from Atlas/cloud payloads.
3. **Given** Ask Orbit or action drafting runs, **When** a candidate is pending/rejected, **Then** it is not used as a factual profile claim.

## Functional Requirements

- **FR-007-001**: Orbit MUST add local Room sidecars for memory candidates and promoted memories, with FK provenance to source captures where possible.
- **FR-007-002**: Candidate memory MUST be stored in `:ml` and surfaced through compact Binder projections; raw screenshots, raw OCR bodies, prompts, model responses, embeddings, and full evidence payloads MUST NOT cross Binder.
- **FR-007-003**: Candidate cards MUST show plain-language label/fact text, candidate kind, confidence/sensitivity copy, source count, and actions to open source, accept, reject, and edit.
- **FR-007-004**: Accepting a candidate MUST create or update one promoted memory row with provenance and mark the candidate promoted.
- **FR-007-005**: Rejecting a candidate MUST mark it rejected or suppressed and MUST NOT create promoted memory.
- **FR-007-006**: Editing a candidate before acceptance MUST store the user-edited display text in promoted memory and audit the correction.
- **FR-007-007**: Sensitive and local-only candidates MUST require explicit confirmation and MUST NOT be included in compact cloud memory sync payloads.
- **FR-007-008**: Candidate/promoted memory decisions MUST write local audit rows with candidate id, promoted memory id if any, decision, source ids, and no raw sensitive content in the audit extra JSON.
- **FR-007-009**: The feature MUST provide deterministic debug seeding for demo/testing without adding an arbitrary production candidate insertion API.
- **FR-007-010**: Pending/rejected candidates MUST NOT be treated as facts by Ask Orbit, action drafting, or retrieval answer copy.
- **FR-007-011**: Candidate source deletion MUST invalidate candidates/promoted memories that only depend on deleted captures, or at least make them visibly stale before KG/deletion semantics become richer in Spec 012.
- **FR-007-012**: The branch MUST remain local-first: no direct Android Atlas clients, no network clients outside `:net`, and no new cloud requirement for reviewing or deciding candidates.

## Non-Goals

- Full knowledge graph backend, entity resolution, graph traversal, or graph UI.
- Curious Agent profiling questions.
- Autonomous agent planning or use of memory in plans.
- Atlas/cloud candidate sync beyond explicit exclusion and payload-safety tests.
- Automatic promotion from repeated behavior beyond deterministic local fixtures.
- Third-party contacts/calendar/email profile import.
- A settings-wide memory manager if the Orbit tab surface is sufficient for the MVP branch.

## Edge Cases

- Source capture is deleted before candidate review.
- Candidate has multiple supporting captures and one is deleted.
- Candidate is duplicated by debug seed or repeated extraction.
- Candidate is sensitive but low confidence.
- Candidate text contains account numbers, passport-like identifiers, or private contact details.
- User accepts, rejects, or edits while the binder service reconnects.
- App restarts after promotion but before UI refresh.
- Existing promoted memory conflicts with a candidate.

## Success Criteria

- **SC-007-001**: Seeded memory candidates appear in Orbit within 5 seconds with no empty-state queue pressure when none exist.
- **SC-007-002**: Accepting a candidate creates exactly one promoted memory row with supporting capture ids and audit provenance.
- **SC-007-003**: Rejecting a candidate records negative signal and prevents the candidate from reappearing as pending.
- **SC-007-004**: Edited acceptance stores user-edited text and preserves original source/provenance.
- **SC-007-005**: Local-only/sensitive candidates remain local and are excluded from compact cloud memory sync payloads.
- **SC-007-006**: Local gates pass: `:app:compileDebugKotlin`, `:app:testDebugUnitTest`, `:build-logic:lint:test`, `:app:lintDebug`, `:app:assembleDebug`, and `:app:compileDebugAndroidTestKotlin`.

## Assumptions

- Spec 006 remains the current stacked base and may still need phone-only final validation later.
- The first candidate generation path can be deterministic/debug-seeded plus simple local heuristics from accepted action outcomes or explicit notes; LLM-generated candidate production can follow after cloud/model wiring matures.
- The Orbit tab is the correct first home for Memory Review because it is the agent/action layer, while Diary remains memory browsing.
