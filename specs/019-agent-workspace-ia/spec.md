# Feature Specification: Agent Workspace IA

**Feature Branch**: `feature/019-agent-workspace-ia-20260614`  
**Created**: 2026-06-14  
**Status**: Draft  
**Input**: Roadmap row 10: formalize the durable three-pillar IA after core loop semantics exist.

## Context

`DiaryActivity` already hosts the three primary tabs: Diary, Library, and Orbit. Spec 019 should therefore harden and document the existing IA instead of rebuilding navigation. The main current gap is route-boundary hygiene: transient Library/Ask state can leak across workspace switches unless each tab owns reset semantics intentionally.

## User Scenarios & Testing

### User Story 1 - Diary Remains Pure Memory (Priority: P1)

The user can switch back to Diary and see the chronological daybook without search or agent state taking over the surface.

**Why this priority**: Diary is the quiet memory surface. If it inherits search/chat pressure, Orbit stops feeling like a daybook.

**Independent Test**: Enter search/Ask state in another tab, switch to Diary, and verify Diary renders independently with its own paging/manual compose state.

### User Story 2 - Library Owns Retrieval State (Priority: P2)

The user can search Library, leave it, and return to a clean retrieval surface rather than stale query/results unless a future spec explicitly adds persistent saved searches.

**Why this priority**: The user already noticed exact/stale search state leaking across tabs. Library should feel like a retrieval workspace with predictable reset boundaries.

**Independent Test**: Search Library, switch to Diary or Orbit, return to Library, and verify query/results/loading/errors reset.

### User Story 3 - Orbit Owns Agent And Action State (Priority: P3)

The Orbit tab is the execution/agent workspace for Ask, action drafts, memory review, follow-ups, and plans. Leaving Orbit clears transient Ask text/answer while durable drafts/follow-ups remain.

**Why this priority**: Orbit needs to be an agent workspace, not a generic overflow tab. Transient chats can reset; durable queue state must remain because it is stored in local data.

**Independent Test**: Ask a question, leave Orbit, return, and verify Ask resets while action drafts/follow-ups still come from repository flows.

## Edge Cases

- Switching away during an in-flight Library search or Ask request should cancel/reset transient state without crashing.
- Opening a capture from Library or Orbit must still use `EnvelopeDetailActivity`.
- Manual compose should remain launched from Diary only in this branch.
- Resetting transient UI state must not delete envelopes, notes, action drafts, Active Intents, memory candidates, or receipts.

## Requirements

### Functional Requirements

- **FR-019-001**: The bottom navigation MUST keep exactly three product pillars: Diary, Library, Orbit.
- **FR-019-002**: Diary MUST remain the chronological memory surface and MUST NOT render Ask/search/action queues.
- **FR-019-003**: Library MUST own retrieval UI and reset transient query/results/error/loading state when leaving the tab.
- **FR-019-004**: Orbit MUST own agent/action surfaces and reset transient Ask state when leaving the tab.
- **FR-019-005**: Durable repository-backed state such as action drafts, memory candidates, Active Intents, and receipts MUST NOT be cleared by tab switches.
- **FR-019-006**: Tests MUST cover tab reset behavior without requiring a phone.

## Key Entities

- **OrbitHomeTab**: Existing enum for `DIARY`, `LIBRARY`, `ORBIT`.
- **LibraryUiState**: Transient retrieval state to reset on route exit.
- **AskOrbitUiState**: Transient question/answer state already reset on Orbit exit.

## Success Criteria

- **SC-019-001**: Library query/results reset when leaving Library.
- **SC-019-002**: Ask Orbit state resets when leaving Orbit.
- **SC-019-003**: Full non-phone gate passes.

## Assumptions

- The existing `DiaryActivity` tab host remains the right shell for MVP.
- This branch does not add Compose Navigation or deep-link route graphs.
- Persisted chat sessions are deferred to a later dedicated agent workbench spec.
