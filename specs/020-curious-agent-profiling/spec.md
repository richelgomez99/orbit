# Feature Specification: Curious Agent Profiling

**Feature Branch**: `feature/020-curious-agent-profiling-20260614`  
**Created**: 2026-06-14  
**Status**: Draft  
**Input**: Roadmap row 11: ask sparse, high-leverage profile questions after KG and agent coordinator foundations.

## Summary

Spec 020 adds Orbit's first "Curious Agent" behavior: sparse, dismissible, evidence-backed questions that improve future relevance. It must not infer profile facts silently. A question is a proposal to the user, not a claim about the user.

## User Stories

### User Story 1 - Ask A Grounded Profile Question (P1)

As a user, when Orbit has repeated evidence around a topic or intent gap, it can ask one concise question with cited sources so I can clarify what those saves mean.

**Independent Test**: Given promoted memories or graph facts about repeated agent-tooling captures, the local question generator proposes "How should Orbit treat these agent tooling saves?" with source ids and choices, not a silent profile fact.

### User Story 2 - Dismiss Or Answer Without Noise (P1)

As a user, I can dismiss a curious question or answer it. Dismissals reduce future surfacing; answers create explicit, provenance-backed memory candidates/facts only through existing approval paths.

**Independent Test**: Dismiss a question and verify it no longer appears as active; answer a question and verify no profile fact is written without provenance and user confirmation.

### User Story 3 - Keep Questions Sparse (P2)

As a user, I should not see a wall of profile questions. Orbit should show at most a small number of high-confidence questions in the Orbit workspace.

**Independent Test**: Seed many possible signals and verify the presenter caps active questions and sorts by evidence strength.

## Edge Cases

- Sparse evidence should produce no question.
- Sensitive identifiers should not appear in question text.
- Dismissed questions should not immediately reappear.
- Deleted/invalidated source evidence should suppress or stale the question.
- Cloud/model unavailability should not block deterministic local questions.

## Requirements

- **FR-020-001**: Curious questions MUST cite local evidence ids.
- **FR-020-002**: Curious questions MUST be optional and dismissible.
- **FR-020-003**: Answers MUST be treated as user-authored evidence and must not become active profile facts without provenance.
- **FR-020-004**: The generator MUST refuse to emit questions from fewer than the configured minimum supporting sources.
- **FR-020-005**: The Orbit UI MUST cap active curious questions and avoid empty-state pressure.
- **FR-020-006**: No raw screenshots, raw OCR, prompts, model responses, embeddings, JWTs, cookies, API keys, or raw HTML may cross Binder for question display.
- **FR-020-007**: The first implementation MUST work locally without cloud/model calls.

## Key Entities

- **CuriousQuestionCandidate**: Local derived question proposal with question text, choices, source ids, confidence, and status.
- **CuriousQuestionAnswer**: User answer/dismissal/correction signal.
- **EvidenceRef**: Compact local source reference, typically promoted memory id, graph target id, or envelope id.

## Success Criteria

- **SC-020-001**: Deterministic generator emits no question with insufficient evidence.
- **SC-020-002**: Generated questions carry source ids and capped copy only.
- **SC-020-003**: Dismissed questions do not reappear in active UI state.
- **SC-020-004**: Full non-phone gate passes.

## Non-Goals

- No autonomous profile fact promotion.
- No cloud/model-based persona inference.
- No push notifications or proactive interruptions.
- No AppFunctions/Spark/platform-agent interop.
- No A2UI/generative UI runtime.
