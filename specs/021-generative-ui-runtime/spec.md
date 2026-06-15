# Feature Specification: Generative UI Runtime

**Feature Branch**: `feature/021-generative-ui-runtime-20260614`  
**Created**: 2026-06-14  
**Status**: Draft  
**Input**: Roadmap row 12, `VISION-2026-05-22.md`, and current Orbit agent/action surfaces.

## User Stories

### US1 - Native Agent Results

As an Orbit user, when the agent finds a cited plan or asks a gap-filling question, I see a native Orbit surface instead of a dense text blob.

**Acceptance Criteria**

1. Given an existing `AgentPlanParcel`, when Orbit renders the result, then title, summary, questions, steps, limitations, and evidence appear as known Orbit components.
2. Given the same plan, when rich rendering is unavailable, then Orbit can degrade to bounded plain text without losing the plan's citations.

### US2 - Safe Component Boundary

As a privacy-conscious user, I need generated or model-assisted outputs to be constrained to safe known UI components.

**Acceptance Criteria**

1. The runtime accepts only app-defined typed component models.
2. Unknown component kinds, raw provider JSON, prompts, screenshots, embeddings, and uncited source text are not renderable payloads.
3. Actions rendered by the runtime remain display/approval affordances only; execution still goes through existing Spec 006 confirmation paths.

### US3 - Orbit Design Consistency

As a user, agent surfaces should feel like Orbit, not an arbitrary web page.

**Acceptance Criteria**

1. Components use the existing Quiet Almanac Material theme and local Compose primitives.
2. The runtime does not expose arbitrary colors, fonts, layout CSS, scripts, markdown HTML, or remote assets.
3. Text is capped and layout-safe for mobile widths.

## Requirements

- **FR-021-001**: Add a typed document/component model for Orbit agent UI.
- **FR-021-002**: Add an adapter from current `AgentPlanParcel` to the typed document model.
- **FR-021-003**: Add a Compose renderer for the initial component set: section title, body text, question choices, plan steps, evidence rows, and limitations.
- **FR-021-004**: Add a deterministic text fallback renderer for the same document model.
- **FR-021-005**: Wire the Orbit tab agent-plan result to the typed renderer without changing the Binder contract.
- **FR-021-006**: Add tests proving bounded output, citation preservation, unknown/external UI exclusion by construction, and text fallback behavior.

## Non-Goals

- No external A2UI library dependency in this slice.
- No LLM-generated JSON parser in this slice.
- No durable chat sessions.
- No BYOM/local-model-manager work.
- No new Room schema or Binder payload shape.
- No direct action execution from generative UI.
- No AppFunctions/Spark/platform-agent interop.

## Stop Signs

- Stop if the implementation requires raw screenshots, full OCR, provider responses, prompts, embeddings, or arbitrary JSON to cross process boundaries.
- Stop if an action can execute without the existing approval path.
- Stop if rich UI cannot degrade to cited text.
- Stop if renderer styling can be model/provider-controlled.
