# Research: Agent Coordinator

**Date**: 2026-06-12  
**Branch**: `feature/010-agent-coordinator-20260612`

## Inputs Reviewed

- `VISION-2026-05-22.md`
- `docs/orbit-roadmap-queue-2026-06-02.md`
- `.specify/memory/constitution.md`
- Spec 005/005A retrieval and grounded Ask artifacts
- Spec 006 approval action runtime artifacts
- Spec 007 memory candidates inspector artifacts
- Spec 008 cloud controls artifacts
- Spec 009 KG backend POC artifacts and commits

## Current State

- Library/Ask can retrieve cited evidence and refuse unsupported questions.
- Action proposals/drafts exist and require user confirmation before execution.
- Memory candidates can be accepted/rejected with provenance.
- Local graph facts/provenance exist behind `GraphBackendAdapter`.
- Cloud Ask and cloud AI routing are separately controllable.

## Decisions

### D-010-001: Coordinator Is A Planner, Not An Executor

**Decision**: Spec 010 returns cited plans, questions, and action draft references. It does not execute actions.

**Rationale**: Orbit's trust boundary is approval-first. Execution already has a dedicated approval runtime.

**Rejected**: Letting the agent call `IActionExecutor` or external intents directly.

### D-010-002: Product Surface Before Model Assistance

**Decision**: Implement deterministic local plan surfaces first, then model-assisted wording/selection at the end of the spec.

**Rationale**: The user needs a real product loop that works without LLM availability. Models improve quality, not feature scope.

**Rejected**: Waiting for local/BYOM models before building the coordinator surface.

### D-010-003: Evidence And Function Schemas Are The Only Trusted Inputs

**Decision**: The coordinator trusts local evidence ids, KG why-this projections, action proposals, and AppFunction schemas. Model output is untrusted until validated.

**Rationale**: The agent should not invent appointments, identities, or available actions.

**Rejected**: Letting a model generate free-form action JSON that bypasses registered function schemas.

### D-010-004: No New Persistent Plan Table For V1

**Decision**: Start with on-demand `AgentPlanDraft` projections plus bounded audit receipts. Add a persistent session/workbench table only when the UI requires durable chat history.

**Rationale**: Existing Ask and action surfaces are not yet durable chat sessions. Adding session storage now risks schema churn before the product shape is proven.

**Rejected**: Building full chat/session persistence in this branch.

### D-010-005: Cloud/LLM Assistance Is A Final Layer

**Decision**: If added, model assistance routes through `LlmProviderRouter`/existing provider boundaries and is gated by Spec 008 controls.

**Rationale**: Spec 008 exists specifically to make deeper cloud/agent behavior user-sovereign.

**Rejected**: A new network client, direct gateway call from UI, or cloud-only coordinator.

## Open Questions

- Whether the first UI should be an Orbit tab panel under Ask, or a separate "Agent plan" card fed by the existing Ask input.
- Whether agent receipts need a new `AuditAction.AGENT_PLAN_REQUESTED` now or can initially reuse existing Ask/action audit rows plus no-content logs.
- Whether model-assisted planning should use the current cloud gateway action-extraction capability or a new explicit `agent_plan` capability.

## Non-Goals Confirmed

- AppFunctions/Spark/platform-agent interop remains deferred.
- A2UI remains deferred.
- BYOM/local model manager remains deferred.
