# Feature Specification: Agent Coordinator

**Feature Branch**: `feature/010-agent-coordinator-20260612`
**Created**: 2026-06-12
**Status**: Draft for implementation
**Input**: Roadmap row 6 after Spec 009 KG backend POC

## Summary

Spec 010 moves Orbit from cited Ask and standalone action drafts toward an approval-first agent coordinator. The coordinator turns a user request into a small, cited plan: what evidence Orbit found, what is missing, what action drafts are possible, and what the user must approve before anything external happens.

This is not autonomous execution. It is the first agent workbench layer over existing retrieval, KG provenance, memory candidates, and action proposals.

## User Stories And Tests

### User Story 1 - Get A Cited Plan Before Acting (P1)

As a user, I can ask Orbit to help close a loop from saved captures, and Orbit returns a compact plan with cited evidence instead of immediately executing.

**Independent Test**: Given a saved dentist reschedule capture and action capability inventory, when the user asks "help me reschedule the dentist", the coordinator returns a plan with the source capture citation, a gap/confirmation step, and an action draft/proposal step that requires approval.

### User Story 2 - Ask A Gap-Filling Question When Ambiguous (P1)

As a user, if Orbit does not have enough evidence or has multiple plausible targets, it asks a focused question instead of guessing.

**Independent Test**: Given multiple event captures matching "that event", when the user asks "draft a reply to that event", the coordinator returns choices or a question with cited candidates and no external write.

### User Story 3 - Respect Local/Cloud Controls And Refuse Safely (P1)

As a privacy-sensitive user, disabling cloud Ask/AI routing still leaves a local deterministic coordinator that can cite saved evidence, ask questions, or refuse unsupported requests.

**Independent Test**: With cloud AI routing disabled, the coordinator returns a local plan/refusal without invoking `CloudLlmProvider` or the memory gateway.

### User Story 4 - Model Assistance Improves The Plan, Not The Trust Boundary (P2)

As a user, when model assistance is enabled, Orbit may improve wording and step selection, but all facts and actions still come from local cited evidence and registered functions.

**Independent Test**: Given model assistance enabled, the coordinator sends only compact evidence/action schemas, rejects uncited model claims, and falls back to deterministic planning if the model response is unavailable or invalid.

## Functional Requirements

- **FR-010-001**: The coordinator MUST produce an `AgentPlanDraft` with an outcome: `PLAN`, `ASK_USER`, `REFUSE`, or `ERROR`.
- **FR-010-002**: Every plan step that references user memory MUST carry at least one local evidence reference: envelope id, graph target id, action proposal id, or promoted memory id.
- **FR-010-003**: The coordinator MUST NOT execute external writes. It may only return drafts/proposals requiring existing approval paths.
- **FR-010-004**: The coordinator MUST use registered AppFunction metadata before suggesting an action step.
- **FR-010-005**: The coordinator MUST ask a gap-filling question or return choices when target evidence is ambiguous.
- **FR-010-006**: The coordinator MUST refuse unsupported requests rather than inventing facts, identities, identifiers, appointments, or source content.
- **FR-010-007**: Binder parcels MUST be compact and MUST NOT carry raw screenshots, full OCR, prompts, embeddings, model responses, JWTs, cookies, or API keys.
- **FR-010-008**: Cloud/model assistance MUST respect Spec 008 controls and existing `LlmProviderRouter` behavior.
- **FR-010-009**: Model assistance MUST be optional; deterministic local planning remains the fallback.
- **FR-010-010**: Any LLM output MUST be treated as untrusted suggestions and validated against local evidence/action schemas before display.
- **FR-010-011**: Agent traces/audit rows MUST store only digests, ids, counts, model labels, outcomes, and refusal reasons unless a later explicit policy expands this.
- **FR-010-012**: The UI MUST present action steps as review/approval affordances, not as completed work.

## Key Entities

- **AgentRequest**: User request plus optional attached envelope ids and local mode flags.
- **AgentPlanDraft**: Bounded coordinator output containing outcome, summary, steps, questions, evidence refs, limitations, and model provenance.
- **AgentPlanStep**: One step in a plan, such as inspect evidence, ask question, draft action, or open capture.
- **AgentEvidenceRef**: Compact reference to a local source: envelope, graph target, promoted memory, or action proposal.
- **AgentQuestion**: A focused gap-filling question or choice set.
- **AgentTraceReceipt**: Local bounded audit metadata for a coordinator run.

## Non-Goals

- No AppFunctions/Spark/platform-agent interop.
- No autonomous external writes.
- No general browser automation.
- No A2UI/generative UI runtime; native static UI is enough for this branch.
- No BYOM/local model manager.
- No new external graph vendor.
- No background proactive curious-agent profiling.

## Stop Signs

- A task gives the coordinator direct network access outside `:net`.
- A task stores prompts/model responses/raw content in local traces.
- A task makes the model's plan executable without existing user approval.
- A task uses uncited model output as fact.
- A task makes cloud AI required for the core coordinator path.
