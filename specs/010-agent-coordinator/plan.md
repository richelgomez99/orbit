# Implementation Plan: Agent Coordinator

**Branch**: `feature/010-agent-coordinator-20260612` | **Date**: 2026-06-12 | **Spec**: [spec.md](./spec.md)  
**Input**: Feature specification from `/specs/010-agent-coordinator/spec.md`

## Summary

Spec 010 builds Orbit's first approval-first agent coordinator. It should produce cited plan drafts, gap questions, and action-review steps from local evidence, KG provenance, and registered AppFunction/action surfaces. Deterministic local planning lands first; model-assisted planning is optional and added only after the surface and validation are working.

## Technical Context

**Language/Version**: Kotlin, Android Room/SQLCipher, AIDL Binder  
**Primary Dependencies**: Existing Ask/Library retrieval, Spec 006 actions, Spec 009 graph adapter, Spec 008 cloud controls  
**Storage**: No new persistent plan table planned; use compact Binder projections and bounded audit metadata  
**Testing**: JVM coordinator tests, Binder parcel/source-ready tests, Android lint, assemble  
**Target Platform**: Android app with `:ml` storage owner and `:ui` Binder clients  
**Project Type**: Mobile app agent orchestration foundation  
**Performance Goals**: Plan responses bounded and fast; no UI-blocking long model path without fallback  
**Constraints**: No autonomous writes, no direct network outside `:net`, no uncited facts, no raw trace/payload storage  
**Scale/Scope**: First coordinator over local evidence/action/KG surfaces; no full chat sessions

## Constitution Check

- **Principle I - Local-First Supremacy**: PASS if deterministic coordinator works without cloud/model calls.
- **Principle III - Intent Before Artifact**: PASS if plans cite intent envelopes and user-approved memory.
- **Principle VI - Privilege Separation**: PASS if UI reaches coordinator through Binder and model calls use existing router.
- **Principle VIII - Collect Only What You Use**: PASS if no raw traces/prompts/model responses are stored.
- **Principle IX - Cloud Escape Hatch**: PASS if model assistance is optional and respects Spec 008 gates.
- **Principle XII - Provenance**: GATE. No plan claim/action may lack local evidence or registered function provenance.

## Project Structure

```text
specs/010-agent-coordinator/
├── spec.md
├── research.md
├── data-model.md
├── quickstart.md
├── contracts/
│   └── agent-coordinator-contract.md
└── tasks.md

app/src/main/java/com/orbit/app/agent/
├── AgentModels.kt
├── AgentCoordinator.kt
└── DeterministicAgentPlanner.kt

app/src/main/java/com/orbit/app/data/ipc/
├── AgentPlanParcel.kt
├── AgentPlanStepParcel.kt
├── AgentEvidenceParcel.kt
└── AgentQuestionParcel.kt
```

## Implementation Strategy

### Phase A - Contract And Models

Add pure coordinator models and tests that prove no uncited plan steps and no executable actions.

### Phase B - Deterministic Local Coordinator

Build a coordinator that can:

- retrieve/accept explicit evidence refs;
- produce refusals on weak evidence;
- ask gap questions on ambiguity;
- reference existing action proposals/functions as approval-required steps.

### Phase C - Binder Projection

Expose a compact `planAgentRequest` Binder/repository method and source-ready tests for payload caps.

### Phase D - Orbit Surface

Add a minimal Orbit tab plan surface that reuses existing design language and opens captures/action approval paths instead of executing.

### Phase E - Model Assistance

Add model-assisted step wording/selection behind `allowModelAssist`, Spec 008 cloud controls, and `LlmProviderRouter`. Validate all model output against local ids/functions before display.

### Phase F - Validation And Closeout

Run focused coordinator tests, full non-phone gate, and update roadmap/handoff.

## Complexity Tracking

| Violation | Why Needed | Simpler Alternative Rejected Because |
| --- | --- | --- |
| New coordinator layer | Agent needs orchestration across Ask/KG/actions | Packing this into Ask would blur retrieval answers with action planning |
| Binder parcels | UI cannot read Room directly | Direct DAO access violates process boundary |
| Optional model validation | LLM may improve plan quality | Trusting raw model output would violate provenance and approval requirements |
